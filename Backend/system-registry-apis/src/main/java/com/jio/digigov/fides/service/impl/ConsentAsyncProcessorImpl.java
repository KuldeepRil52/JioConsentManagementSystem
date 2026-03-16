package com.jio.digigov.fides.service.impl;

import com.jio.digigov.fides.client.audit.AuditManager;
import com.jio.digigov.fides.client.audit.request.Actor;
import com.jio.digigov.fides.client.audit.request.AuditRequest;
import com.jio.digigov.fides.client.audit.request.Context;
import com.jio.digigov.fides.client.audit.request.Resource;
import com.jio.digigov.fides.client.notification.NotificationManager;
import com.jio.digigov.fides.config.MultiTenantMongoConfig;
import com.jio.digigov.fides.constant.Constants;
import com.jio.digigov.fides.dto.ConsentWithdrawalDataItems;
import com.jio.digigov.fides.dto.CustomerIdentifiers;
import com.jio.digigov.fides.dto.HandlePreference;
import com.jio.digigov.fides.entity.*;
import com.jio.digigov.fides.enumeration.*;
import com.jio.digigov.fides.enumeration.NotificationEvent;
import com.jio.digigov.fides.integration.factory.DbDataMaskerFactory;
import com.jio.digigov.fides.integration.mask.DbDataMasker;
import com.jio.digigov.fides.model.SystemExecutionResult;
import com.jio.digigov.fides.service.ConsentAsyncProcessor;
import com.jio.digigov.fides.service.UnCommonDataExtractor;
import com.jio.digigov.fides.util.AuditHashUtil;
import com.jio.digigov.fides.util.DatasetYamlUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.ThreadContext;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Async processor for handling consent withdrawal jobs.
 * 
 * Responsibilities:
 * - Process withdrawal jobs asynchronously in a tenant-scoped manner
 * - Orchestrate data masking across multiple systems and integrations
 * - Track PII items and their deletion/deferral status
 * - Audit all operations and send notifications
 * - Handle errors gracefully with proper logging and job status updates
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ConsentAsyncProcessorImpl implements ConsentAsyncProcessor {

    private static final String JOB_PROCESSING_START = "Starting async processing for ConsentWithdrawalJob: {}";
    private static final String JOB_PROCESSING_COMPLETE = "Completed async processing for ConsentWithdrawalJob: {}";
    private static final String PROCESSING_STATUS = "PROCESSING";
    private static final String COMPLETED_STATUS = "COMPLETED";
    private static final String FAILED_STATUS = "FAILED";

    private final MultiTenantMongoConfig mongoConfig;
    private final AuditManager auditManager;
    private final NotificationManager notificationManager;
    private final UnCommonDataExtractor unCommonDataExtractor;

    /**
     * Asynchronously processes a consent withdrawal job.
     * 
     * Workflow:
     * 1. Validate and load job with tenant-specific MongoTemplate
     * 2. Update job status to PROCESSING
     * 3. Extract consent data items (deletable vs. deferred)
     * 4. Iterate through all business systems and mask data accordingly
     * 5. Track PII item statuses (pseudonymized vs. deferred)
     * 6. Aggregate results and update job with withdrawal data
     * 7. Update notification events and send notifications
     * 8. Audit all operations
     * 
     * @param job the ConsentWithdrawalJob to process
     */
    @Async
    @Override
    public void process(ConsentWithdrawalJob job) {
        MongoTemplate mongoTemplate = null;
        String tenantId = job.getTenantId();
        String jobId = job.getId();

        List<DataFiduciarySystem> dataFiduciarySystems = new ArrayList<>();
        List<PIIItem> piiItemsList = new ArrayList<>();

        try {
            log.info(JOB_PROCESSING_START, jobId);

            // Validate job and obtain tenant-scoped MongoTemplate
            mongoTemplate = mongoConfig.getMongoTemplateForTenant(tenantId);

            if (job == null) {
                log.error("ConsentWithdrawalJob validation failed: job is null for jobId={}", jobId);
                return;
            }

            // Update job status to PROCESSING
            job.setStatus(PROCESSING_STATUS);
            job.setUpdatedAt(Instant.now());
            mongoTemplate.save(job);
            log.debug("Job status updated to PROCESSING for jobId={}", jobId);

            // Retrieve and validate consent
            Consent consent = mongoTemplate.findOne(
                    new Query(Criteria.where("consentId").is(job.getConsentId())),
                    Consent.class
            );

            if (consent == null) {
                String errorMsg = String.format("Consent not found for consentId=%s, jobId=%s", job.getConsentId(), jobId);
                log.error(errorMsg);
                throw new IllegalStateException(errorMsg);
            }

            List<String> dpIds = consent.getPreferences() != null ? consent.getPreferences().stream()
                    .filter(p -> p.getProcessorActivityList() != null)
                    .flatMap(p -> p.getProcessorActivityList().stream())
                    .filter(pa -> pa.getProcessActivityInfo() != null && pa.getProcessActivityInfo().getProcessorId() != null)
                    .map(pa -> pa.getProcessActivityInfo().getProcessorId())
                    .distinct()
                    .collect(Collectors.toList()) : List.of();

            // Retrieve data processor ID with "Own Use"
            DataProcessor dp = mongoTemplate.findOne(
                    new Query(Criteria.where("businessId").is(consent.getBusinessId())
                            .and("dataProcessorId").in(dpIds)
                            .and("details").is("Own Use")
                    ),
                    DataProcessor.class
            );
            if (dp == null) {
                log.info("No DataProcessor with 'Own Use' found for businessId={}", consent.getBusinessId());

                // Construct DataFiduciary result
                DataFiduciary fiduciary = new DataFiduciary();
                fiduciary.setOverallStatus(ExecutionStatus.NOT_APPLICABLE);
                fiduciary.setExecutedAt(Instant.now());

                // Build withdrawal data
                WithdrawalData withdrawalData = new WithdrawalData();
                withdrawalData.setDataFiduciary(fiduciary);

                // Update job with withdrawal data
                job.setWithdrawalData(withdrawalData);
                job.setStatus(COMPLETED_STATUS);
                job.setUpdatedAt(Instant.now());
                mongoTemplate.save(job);
                
                // Update notification event
                updateNotificationEvent(mongoTemplate, job, withdrawalData);
                
                return;
            }

            // Extract withdrawal data items (deletable vs. deferred)
            ConsentWithdrawalDataItems withdrawalItems =
                unCommonDataExtractor.extractWithdrawalDataItems(
                    consent, tenantId, consent.getBusinessId()
                );
            Set<String> deletableItems = withdrawalItems.getDeletableItems();
            Map<String, Set<String>> deferredItemsWithConsentIds = withdrawalItems.getDeferredItems();
            Set<String> deferredItems = deferredItemsWithConsentIds.keySet();
            
            log.info("Extracted withdrawal data items - deletable with TRAI: {}, deferred: {}", 
                    deletableItems.size(), deferredItems.size());

            CustomerIdentifiers customerIdentifiers = consent.getCustomerIdentifiers();
            if (customerIdentifiers == null) {
                String errorMsg = String.format("CustomerIdentifiers missing for consentId=%s", job.getConsentId());
                log.error(errorMsg);
                throw new IllegalStateException(errorMsg);
            }

            // Retrieve all business systems for masking
            List<SystemRegister> systems = mongoTemplate.find(
                    new Query(
                            Criteria.where("businessId").is(job.getBusinessId())
                                    .and("isDeleted").is(false)
                    ),
                    SystemRegister.class
            );

            if (systems.isEmpty()) {
                String errorMsg = String.format("No active SystemRegister found for businessId=%s", job.getBusinessId());
                log.error(errorMsg);
                throw new IllegalStateException(errorMsg);
            }

            log.info("Found {} systems to process for businessId={}", systems.size(), job.getBusinessId());

            // Process each system independently
            for (SystemRegister system : systems) {
                processSystemMasking(
                        system,
                        mongoTemplate,
                        deletableItems,
                        deferredItems,
                        customerIdentifiers,
                        dataFiduciarySystems,
                        piiItemsList,
                        job,
                        deferredItemsWithConsentIds
                );
            }

            // Build withdrawal data and aggregate results
            buildAndSaveWithdrawalData(mongoTemplate, job, dataFiduciarySystems, piiItemsList, consent, tenantId, dp);

            log.info(JOB_PROCESSING_COMPLETE, jobId);

        } catch (Exception ex) {
            log.error("Consent withdrawal processing failed for jobId={}, consentId={}", 
                    jobId, job != null ? job.getConsentId() : "UNKNOWN", ex);
            
            if (job != null) {
                job.setStatus(FAILED_STATUS);
                job.setFailureReason(String.format("Processing failed: %s", ex.getMessage()));
            }
        } finally {
            // Ensure job status is persisted even in case of errors
            if (job != null && mongoTemplate != null) {
                job.setUpdatedAt(Instant.now());
                mongoTemplate.save(job);
                log.debug("Job finalized: jobId={}, status={}", jobId, job.getStatus());
            }
        }
    }

    /**
     * Processes data masking for a single system within a consent withdrawal.
     * 
     * Steps:
     * 1. Fetch integration and dataset
     * 2. Determine TRAI-deferred items
     * 3. Execute masking via appropriate DbDataMasker
     * 4. Track PII items and their statuses
     * 5. Update system execution status and audit
     * 
     * @param system the SystemRegister to process
     * @param mongoTemplate tenant-scoped template
     * @param deletableItems items eligible for deletion
     * @param deferredItems items already deferred
     * @param customerIdentifiers customer identity for masking
     * @param dataFiduciarySystems accumulator for system results
     * @param piiItemsList accumulator for all PII items
     * @param job the withdrawal job for context
     */
    private void processSystemMasking(
            SystemRegister system,
            MongoTemplate mongoTemplate,
            Set<String> deletableItems,
            Set<String> deferredItems,
            CustomerIdentifiers customerIdentifiers,
            List<DataFiduciarySystem> dataFiduciarySystems,
            List<PIIItem> piiItemsList,
            ConsentWithdrawalJob job,
            Map<String, Set<String>> deferredItemsWithConsentIds) {

        String sysId = system.getId();
        List<PIIItem> systemPiiItems = new ArrayList<>();

        try {
            log.debug("Processing masking for system: {}", sysId);

            // Fetch integration for this system
            DbIntegration integration = mongoTemplate.findOne(
                    new Query(
                            Criteria.where("systemId").is(sysId)
                                    .and("isDeleted").is(false)
                    ),
                    DbIntegration.class
            );

            if (integration == null) {
                String errorMsg = String.format("DbIntegration not found for systemId=%s", sysId);
                log.error(errorMsg);
                throw new IllegalStateException(errorMsg);
            }

            // Fetch dataset for this integration
            Dataset dataset = mongoTemplate.findOne(
                    new Query(
                            Criteria.where("datasetId").is(integration.getDatasetId())
                                    .and("isDeleted").is(false)
                    ),
                    Dataset.class
            );

            if (dataset == null) {
                String errorMsg = String.format("Dataset not found for datasetId=%s", integration.getDatasetId());
                log.error(errorMsg);
                throw new IllegalStateException(errorMsg);
            }

            // Extract TRAI-specific items from dataset
            Set<String> traiItems = DatasetYamlUtil.extractTraiDataItems(dataset.getDatasetYaml());
            log.debug("TRAI data items for system {}: {}", sysId, traiItems);

            // Determine deferred items based on TRAI regulations
            Set<String> traiDeferredItems = deletableItems.stream()
                    .filter(traiItems::contains)
                    .collect(Collectors.toSet());

            // Combine with pre-existing deferred items
            Set<String> finalDeferredItems = new HashSet<>(deferredItems);
            finalDeferredItems.addAll(traiDeferredItems);

            // Calculate final deletable items (exclude TRAI-deferred)
            Set<String> finalDeletableItems = deletableItems.stream()
                    .filter(item -> !traiDeferredItems.contains(item))
                    .collect(Collectors.toSet());

            log.info("System {} - Deletable: {}, Deferred: {} (TRAI: {})", 
                    sysId, finalDeletableItems.size(), finalDeferredItems.size(), traiDeferredItems.size());

            // Execute masking if there are items to delete
            if (!finalDeletableItems.isEmpty()) {
                executeMasking(
                        integration,
                        dataset,
                        finalDeletableItems,
                        customerIdentifiers,
                        sysId
                );
            } else {
                log.debug("No deletable items for system {}, skipping masking", sysId);
            }

            // Track all PII items and their statuses
            buildPiiItems(systemPiiItems, piiItemsList, finalDeletableItems, finalDeferredItems, traiDeferredItems, sysId, deferredItemsWithConsentIds);

            // Record system execution status
            recordSystemExecutionStatus(
                    system,
                    dataFiduciarySystems,
                    dataset,
                    systemPiiItems,
                    sysId
            );

            // Audit successful processing
            logJobCompletedAudit(
                    job.getId(),
                    job.getConsentId(),
                    job.getTenantId(),
                    job.getBusinessId(),
                    integration.getConnectionDetails(),
                    dataset.getDatasetYaml(),
                    List.copyOf(finalDeletableItems),
                    List.copyOf(finalDeferredItems),
                    ActionType.DELETED,
                    sysId
            );

        } catch (Exception e) {
            log.error("Masking failed for system {}: {}", sysId, e.getMessage(), e);

            // Record system failure
            DataFiduciarySystem failedStatus = new DataFiduciarySystem();
            failedStatus.setSystemId(sysId);
            failedStatus.setDeletionType("Non-Reversible Pseudonymization");
            failedStatus.setStatus(ExecutionStatus.FAILED);
            failedStatus.setExecutedAt(Instant.now());
            failedStatus.setDeferredReason(e.getMessage());
            dataFiduciarySystems.add(failedStatus);

            log.warn("System {} marked as FAILED, continuing with next system", sysId);
        }
    }

    /**
     * Executes the data masking operation via the appropriate DbDataMasker implementation.
     * 
     * @param integration database integration details
     * @param dataset dataset definition with YAML
     * @param itemsToMask field names to mask
     * @param customerIdentifiers customer identity
     * @param systemId system identifier for logging
     */
    private void executeMasking(
            DbIntegration integration,
            Dataset dataset,
            Set<String> itemsToMask,
            CustomerIdentifiers customerIdentifiers,
            String systemId) {

        try {
            log.debug("Preparing masker for dbType={}, systemId={}", integration.getDbType(), systemId);

            DbDataMasker masker = DbDataMaskerFactory.getMasker(integration.getDbType());

            masker.maskData(
                    integration.getConnectionDetails(),
                    dataset.getDatasetYaml(),
                    itemsToMask,
                    customerIdentifiers
            );

            log.info("Data masking completed successfully for system {}, masked {} items", systemId, itemsToMask.size());

        } catch (Exception e) {
            log.error("Masking operation failed for system {}: {}", systemId, e.getMessage(), e);
            throw new RuntimeException(String.format("Masking failed for system %s: %s", systemId, e.getMessage()), e);
        }
    }

    /**
     * Builds and tracks PII items with their statuses (pseudonymized vs. deferred).
     * 
     * @param systemPiiItems system-level PII accumulator
     * @param allPiiItems global PII accumulator
     * @param deletableItems successfully masked items
     * @param deferredItems deferred items
     * @param traiDeferredItems TRAI-specific deferred items
     * @param systemId system identifier
     */
    private void buildPiiItems(
            List<PIIItem> systemPiiItems,
            List<PIIItem> allPiiItems,
            Set<String> deletableItems,
            Set<String> deferredItems,
            Set<String> traiDeferredItems,
            String systemId,
            Map<String, Set<String>> deferredItemsWithConsentIds) {

        // Record pseudonymized items
        deletableItems.forEach(item -> {
            PIIItem pii = new PIIItem(item, systemId, "DF", PIIStatus.PSEUDONYMIZED);
            systemPiiItems.add(pii);
            allPiiItems.add(pii);
        });

        // Record deferred items with reasons
        deferredItems.forEach(item -> {
            PIIItem pii = new PIIItem(item, systemId, "DF", PIIStatus.DEFERRED);

            if (traiDeferredItems.contains(item)) {
                pii.setDeferredReasonType(DeferredReasonType.TRAI_REGULATION);
            } else {
                pii.setDeferredReasonType(DeferredReasonType.ACTIVE_CONSENT);
                Set<String> consentIds = deferredItemsWithConsentIds.get(item);
                pii.setConsentIds(consentIds);
            }

            pii.resolveDeferredReason();
            systemPiiItems.add(pii);
            allPiiItems.add(pii);
        });

        log.debug("Tracked {} PII items for system {}", systemPiiItems.size(), systemId);
    }

    /**
     * Records the execution status for a single system based on PII item statuses.
     * 
     * Status determination:
     * - COMPLETED: all items pseudonymized or no items
     * - DEFERRED: at least one deferred item
     * - PENDING: mixed/incomplete state
     * 
     * @param system the SystemRegister
     * @param dataFiduciarySystems accumulator
     * @param dataset dataset for name extraction
     * @param systemPiiItems PII items for this system
     * @param systemId system identifier
     */
    private void recordSystemExecutionStatus(
            SystemRegister system,
            List<DataFiduciarySystem> dataFiduciarySystems,
            Dataset dataset,
            List<PIIItem> systemPiiItems,
            String systemId) {

        DataFiduciarySystem sysStatus = new DataFiduciarySystem();
        sysStatus.setSystemId(systemId);
        sysStatus.setDeletionType("Non-Reversible Pseudonymization");
        sysStatus.setYamlMappings(DatasetYamlUtil.extractDatasetName(dataset.getDatasetYaml()));

        boolean hasItems = !systemPiiItems.isEmpty();
        boolean allPseudonymized = hasItems &&
                systemPiiItems.stream().allMatch(p -> p.getStatus() == PIIStatus.PSEUDONYMIZED);
        boolean anyDeferred = systemPiiItems.stream()
                .anyMatch(p -> p.getStatus() == PIIStatus.DEFERRED);

        // Determine overall status
        if (!hasItems || allPseudonymized) {
            sysStatus.setStatus(ExecutionStatus.COMPLETED);
            sysStatus.setDeferredReason(null);
            log.debug("System {} completed: all items pseudonymized", systemId);
        } else if (anyDeferred) {
            sysStatus.setStatus(ExecutionStatus.DEFERRED);

            String deferredReasons = systemPiiItems.stream()
                    .filter(p -> p.getStatus() == PIIStatus.DEFERRED)
                    .map(PIIItem::getDeferredReasonMessage)
                    .distinct()
                    .collect(Collectors.joining(", "));

            sysStatus.setDeferredReason(deferredReasons);
            log.debug("System {} deferred: {}", systemId, deferredReasons);
        } else {
            sysStatus.setStatus(ExecutionStatus.PENDING);
            log.debug("System {} pending: mixed state", systemId);
        }

        sysStatus.setExecutedAt(Instant.now());
        dataFiduciarySystems.add(sysStatus);
    }

    /**
     * Builds withdrawal data, updates job, and triggers notifications.
     * 
     * @param mongoTemplate tenant-scoped template
     * @param job the withdrawal job
     * @param dataFiduciarySystems system execution results
     * @param piiItemsList all PII items
     * @param consent the original consent
     * @param tenantId tenant identifier
     */
    private void buildAndSaveWithdrawalData(
            MongoTemplate mongoTemplate,
            ConsentWithdrawalJob job,
            List<DataFiduciarySystem> dataFiduciarySystems,
            List<PIIItem> piiItemsList,
            Consent consent,
            String tenantId,
            DataProcessor dp) {

        try {
            log.debug("Building withdrawal data for jobId={}", job.getId());

            EnumSet<ExecutionStatus> statuses = dataFiduciarySystems.stream()
                    .map(DataFiduciarySystem::getStatus)
                    .collect(Collectors.toCollection(() -> EnumSet.noneOf(ExecutionStatus.class)));

            ExecutionStatus overallStatus =
                    statuses.contains(ExecutionStatus.FAILED) ? ExecutionStatus.FAILED :
                    statuses.contains(ExecutionStatus.DEFERRED) ? ExecutionStatus.DEFERRED :
                    ExecutionStatus.COMPLETED;

            // Construct DataFiduciary result
            DataFiduciary fiduciary = new DataFiduciary();
            fiduciary.setSystems(dataFiduciarySystems);
            fiduciary.setOverallStatus(overallStatus);
            fiduciary.setExecutedAt(Instant.now());

            // Build withdrawal data
            WithdrawalData withdrawalData = new WithdrawalData();
            withdrawalData.setDataFiduciary(fiduciary);
            withdrawalData.setPiiItems(piiItemsList);

            // Update job with withdrawal data
            job.setWithdrawalData(withdrawalData);
            job.setStatus(COMPLETED_STATUS);
            job.setUpdatedAt(Instant.now());
            mongoTemplate.save(job);

            log.info("Withdrawal data saved - jobId={}, systems={}, piiItems={}", 
                    job.getId(), dataFiduciarySystems.size(), piiItemsList.size());

            // Update notification event
            updateNotificationEvent(mongoTemplate, job, withdrawalData);

        } catch (Exception e) {
            log.error("Failed to build and save withdrawal data for jobId={}: {}", job.getId(), e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Updates the notification event record with withdrawal data.
     * 
     * @param mongoTemplate tenant-scoped template
     * @param job the withdrawal job
     * @param withdrawalData the completion withdrawal data
     */
    private void updateNotificationEvent(
            MongoTemplate mongoTemplate,
            ConsentWithdrawalJob job,
            WithdrawalData withdrawalData) {

        try {
            log.debug("Updating notification event for eventId={}", job.getEventId());

            Query eventQuery = new Query(Criteria.where("eventId").is(job.getEventId()));
            Update eventUpdate = new Update()
                    .set("withdrawal_data", withdrawalData)
                    .set("updatedAt", Instant.now());

            mongoTemplate.updateFirst(
                    eventQuery,
                    eventUpdate,
                    com.jio.digigov.fides.entity.NotificationEvent.class
            );

            log.debug("Notification event updated successfully");

        } catch (Exception e) {
            log.warn("Failed to update notification event for eventId={}: {}", job.getEventId(), e.getMessage());
            // Non-critical; continue
        }
    }

    /**
     * Sends withdrawal notification to all relevant processors.
     * 
     * @param consent the original consent
     * @param job the withdrawal job
     */
    private void sendWithdrawalNotification(Consent consent, ConsentWithdrawalJob job) {

        try {
            log.debug("Preparing withdrawal notification for consentId={}", consent.getConsentId());

            Map<String, Object> eventPayload = new HashMap<>();
            // Extract processor IDs from processor activities in consent preferences
            List<String> allProcessorIds = (consent.getPreferences() != null ? consent.getPreferences() : List.<HandlePreference>of()).stream()
                    .filter(p -> p.getProcessorActivityList() != null)
                    .flatMap(p -> p.getProcessorActivityList().stream())
                    .filter(pa -> pa.getProcessActivityInfo() != null && pa.getProcessActivityInfo().getProcessorId() != null)
                    .map(pa -> pa.getProcessActivityInfo().getProcessorId())
                    .distinct()
                    .collect(Collectors.toList());
            eventPayload.put("consentId", consent.getConsentId());

            notificationManager.initiateConsentNotification(
                    NotificationEvent.DATA_DELETION_NOTIFICATION,
                    job.getTenantId(),
                    consent.getBusinessId(),
                    consent.getCustomerIdentifiers(),
                    allProcessorIds,
                    eventPayload,
                    LANGUAGE.ENGLISH,
                    job.getId()
            );

            log.info("Withdrawal notification sent to {} processors for consentId={}", allProcessorIds.size(), consent.getConsentId());

        } catch (Exception e) {
            log.error("Failed to send withdrawal notification for consentId={}: {}", 
                    consent.getConsentId(), e.getMessage(), e);
            // Non-critical; don't fail the job
        }
    }

    /**
     * Logs an audit record for the consent withdrawal operation.
     * 
     * Records:
     * - Actor: system job
     * - Action: data deletion
     * - Resource: consent
     * - Context: transaction ID, source IP
     * - Extra: sanitized connection details, dataset hash, items deleted/deferred
     * 
     * Failure to log audit does not fail the job.
     * 
     * @param jobId job identifier
     * @param consentId consent identifier
     * @param tenantId tenant identifier
     * @param businessId business identifier
     * @param connectionDetails database connection (sanitized before audit)
     * @param datasetYaml dataset definition
     * @param deletableItems items successfully masked
     * @param deferredItems items deferred
     * @param actionType the audit action type
     * @param systemId system identifier
     */
    public void logJobCompletedAudit(
            String jobId,
            String consentId,
            String tenantId,
            String businessId,
            Map<String, Object> connectionDetails,
            String datasetYaml,
            List<String> deletableItems,
            List<String> deferredItems,
            ActionType actionType,
            String systemId) {

        try {
            log.debug("Creating audit record for jobId={}, consentId={}, actionType={}", jobId, consentId, actionType);

            // Build audit actor (system job)
            Actor actor = Actor.builder()
                    .id(jobId)
                    .role(Constants.SYSTEM)
                    .type(Constants.JOB_ID)
                    .build();

            // Build audit resource
            Resource resource = Resource.builder()
                    .type(Constants.CONSENT_ID)
                    .id(consentId)
                    .build();

            // Build audit context
            String sourceIp = ThreadContext.get(Constants.SOURCE_IP);
            String txnId = ThreadContext.get(Constants.TXN_ID_THREAD_CONTEXT);

            Context context = Context.builder()
                    .ipAddress(sourceIp != null && !"-".equals(sourceIp) ? sourceIp : Constants.IP_ADDRESS)
                    .txnId(txnId != null && !"-".equals(txnId) ? txnId : UUID.randomUUID().toString())
                    .build();

            // Build extra context (sanitized)
            Map<String, Object> extra = new HashMap<>();

            Map<String, Object> safeConnectionDetails = new HashMap<>(connectionDetails);
            safeConnectionDetails.remove("password");
            safeConnectionDetails.remove("token");
            safeConnectionDetails.remove("secret");

            extra.put("connectionDetails", safeConnectionDetails);
            extra.put("datasetYamlHash", AuditHashUtil.sha256(datasetYaml));
            extra.put("deletableItems", deletableItems);
            extra.put("deferredItems", deferredItems);
            extra.put("systemId", systemId);

            // Build and log audit request
            AuditRequest auditRequest = AuditRequest.builder()
                    .actor(actor)
                    .businessId(businessId)
                    .group(String.valueOf(Group.DATA))
                    .component(AuditComponent.DATA_ERASURE_JOB)
                    .actionType(actionType)
                    .resource(resource)
                    .initiator(Constants.SYSTEM)
                    .context(context)
                    .extra(extra)
                    .build();

            auditManager.logAudit(auditRequest, tenantId);
            log.debug("Audit record created successfully");

        } catch (Exception e) {
            log.error("Audit logging failed for jobId={}, consentId={}, actionType={}: {}", 
                    jobId, consentId, actionType, e.getMessage(), e);
            // Audit failure is non-critical; don't fail the job
        }
    }
}