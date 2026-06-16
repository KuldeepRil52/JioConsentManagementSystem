package com.jio.schedular.service;

import com.jio.schedular.client.audit.AuditManager;
import com.jio.schedular.client.audit.request.Actor;
import com.jio.schedular.client.audit.request.AuditRequest;
import com.jio.schedular.client.audit.request.Context;
import com.jio.schedular.client.audit.request.Resource;
import com.jio.schedular.client.notification.NotificationManager;
import com.jio.schedular.constant.Constants;
import com.jio.schedular.dto.*;
import com.jio.schedular.enums.*;
import com.jio.schedular.entity.ConsentHandle;
import com.jio.schedular.entity.SchedularStats;
import com.jio.schedular.entity.TenantRegistry;
import com.jio.schedular.multitenancy.TenantMongoTemplateProvider;
import com.jio.schedular.repository.ConsentHandleRepository;
import com.jio.schedular.repository.SchedularStatsRepository;
import com.jio.schedular.repository.TenantRegistryRepository;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.ThreadContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.time.LocalDateTime;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Service class for handling consent handle expiry operations with multi-tenant parallel processing
 *
 * Changes:
 * - Added SchedularStatsRepository integration to record per-tenant summaries similar to other services.
 * - Added proper shutdown handling via @PreDestroy.
 * - Process methods now return TenantResult (expiredCount + affected set) to allow stats resources to be recorded.
 */
@Service
@Slf4j
public class ConsentHandleExpiryService {

    private final ConsentHandleRepository consentHandleRepository;
    private final TenantRegistryRepository tenantRegistryRepository;
    private final TenantMongoTemplateProvider tenantMongoTemplateProvider;
    private final NotificationManager notificationManager;
    private final SchedularStatsRepository statsRepository;
    private ExecutorService executorService;
    private final AuditManager auditManager;

    @Value("${schedular.job.consent-handle-expiry.thread-pool-size:10}")
    private int threadPoolSize;

    @Autowired
    public ConsentHandleExpiryService(ConsentHandleRepository consentHandleRepository,
                                     TenantRegistryRepository tenantRegistryRepository,
                                     TenantMongoTemplateProvider tenantMongoTemplateProvider,
                                     NotificationManager notificationManager,
                                     SchedularStatsRepository statsRepository,
                                     AuditManager auditManager) {
        this.consentHandleRepository = consentHandleRepository;
        this.tenantRegistryRepository = tenantRegistryRepository;
        this.tenantMongoTemplateProvider = tenantMongoTemplateProvider;
        this.notificationManager = notificationManager;
        this.statsRepository = statsRepository;
        this.auditManager = auditManager;
    }

    @PostConstruct
    public void init() {
        // Create a thread pool for parallel processing after @Value injection
        this.executorService = Executors.newFixedThreadPool(threadPoolSize);
        log.info("Initialized ConsentHandleExpiryService with thread pool size: {}", threadPoolSize);
    }

    @PreDestroy
    public void shutdown() {
        if (executorService != null) {
            log.info("Shutting down ConsentHandleExpiryService executor");
            executorService.shutdown();
            try {
                if (!executorService.awaitTermination(30, java.util.concurrent.TimeUnit.SECONDS)) {
                    executorService.shutdownNow();
                }
            } catch (InterruptedException e) {
                executorService.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }

    private record TenantResult(int expiredCount, Set<ConsentHandle> affectedConsents) {}

    /**
     * Expires consent handles for all active tenants in parallel
     * 
     * @param batchSize Number of records to process in each batch
     * @param periodType Type of period (MONTHS, YEARS, DAYS, etc.)
     * @param periodValue Value of the period
     * @return Total number of expired consent handles across all tenants
     */
    @Transactional
    public int expirePendingConsentHandles(int batchSize, String periodType, int periodValue) {

        UUID runId = UUID.randomUUID();
        Instant start = Instant.now();

        log.info("[{}] Starting multi-tenant consent handle expiry - Period: {} {} - runId={}",
                JobName.CONSENT_HANDLE_EXPIRY_JOB, periodValue, periodType, runId);

        // Get all active tenants
        List<TenantRegistry> activeTenants = tenantRegistryRepository.findByStatus(Status.ACTIVE);
        log.info("Found {} active tenants for processing", activeTenants.size());
        
        if (activeTenants == null || activeTenants.isEmpty()) {
            log.warn("No active tenants found for consent handle expiry");
            return 0;
        }
        
        // Calculate expiry date once for all tenants
        LocalDateTime expiryDate = calculateExpiryDate(periodType, periodValue);

        // Prepare tenant id list and submit tasks in parallel
        List<String> tenantIds = activeTenants.stream().map(TenantRegistry::getTenantId).toList();

        List<CompletableFuture<TenantResult>> futures = tenantIds.stream()
                .map(tenantId -> CompletableFuture.supplyAsync(
                        () -> processTenantConsentHandles(tenantId, batchSize, expiryDate, runId), executorService))
                .toList();

        // Wait for all to complete
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        int totalExpired = 0;
        int errorCount = 0;

        // Collect results and save per-tenant summaries
        for (int i = 0; i < tenantIds.size(); i++) {
            String tenantId = tenantIds.get(i);
            try {
                TenantResult res = futures.get(i).join();
                int expired = res.expiredCount();
                totalExpired += expired;
                List<com.jio.schedular.dto.Resource> tenantResources;

                if(res.affectedConsents().isEmpty()) {
                    log.info("No consent handles expired for tenant: {}", tenantId);
                    tenantResources = List.of(createResource(Constants.TENANT_ID, tenantId, tenantId));
                } else {
                    log.info("Total consent handles expired for tenant {}: {}", tenantId, expired);
                    tenantResources = res.affectedConsents().stream()
                        .map(ch -> createResource(Constants.CONSENT_HANDLE_ID, ch.getConsentHandleId(), ch.getBusinessId()))
                        .toList();
                }

                long duration = Duration.between(start, Instant.now()).toMillis();

                statsRepository.saveTenantSummary(tenantId, SchedularStats.builder()
                        .runId(runId.toString())
                        .jobName(JobName.CONSENT_HANDLE_EXPIRY_JOB)
                        .group(Group.CONSENT_HANDLE)
                        .action(Action.CONSENT_HANDLE_EXPIRY)
                        .summaryType(SummaryType.TENANT_SUMMARY)
                        .status(SchedularStatus.SUCCESS)
                        .resources(tenantResources)
                        .totalAffected(expired)
                        .timestamp(java.time.LocalDateTime.now())
                        .durationMillis(duration)
                        .details(Map.of(
                                "batchSize", batchSize,
                                "periodType", periodType,
                                "periodValue", periodValue
                        ))
                        .build());

            } catch (Exception ex) {
                errorCount++;
                long duration = Duration.between(start, Instant.now()).toMillis();
                statsRepository.saveTenantSummary(tenantId, SchedularStats.builder()
                        .runId(runId.toString())
                        .jobName(JobName.CONSENT_HANDLE_EXPIRY_JOB)
                        .group(Group.CONSENT_HANDLE)
                        .action(Action.CONSENT_HANDLE_EXPIRY)
                        .summaryType(SummaryType.TENANT_SUMMARY)
                        .status(SchedularStatus.FAILED)
                        .errorCount(1)
                        .lastError(ex.getMessage())
                        .timestamp(java.time.LocalDateTime.now())
                        .durationMillis(duration)
                        .details(Map.of(
                                "batchSize", batchSize,
                                "periodType", periodType,
                                "periodValue", periodValue
                        ))
                        .build());
            }
        }

        log.info("[{}] Completed | runId={} | expired={} | errors={}", JobName.CONSENT_HANDLE_EXPIRY_JOB, runId, totalExpired, errorCount);
        return totalExpired;
    }

    /**
     * Processes consent handles for a specific tenant
     * 
     * @param tenantId The tenant ID to process
     * @param batchSize Number of records to process in each batch
     * @param expiryDate The expiry date threshold
     * @return TenantResult with expired count and affected consent handles
     */
    private TenantResult processTenantConsentHandles(String tenantId, int batchSize, LocalDateTime expiryDate, UUID runId) {
        log.info("Processing consent handles for tenant: {}", tenantId);
        
        try {
            // Set tenant context for this thread
            ThreadContext.put("tenantId", tenantId);
            
            int tenantExpiredCount = 0;
            int offset = 0;
            boolean hasMoreRecords = true;
            Set<ConsentHandle> affected = new HashSet<>();

            while (hasMoreRecords) {
                // Fetch pending consent handles in batches for this tenant
                Pageable pageable = PageRequest.of(offset, batchSize);
                List<ConsentHandle> pendingHandles = findPendingConsentHandlesForTenant(tenantId, expiryDate, pageable);

                if (pendingHandles == null || pendingHandles.isEmpty()) {
                    hasMoreRecords = false;
                    log.debug("No more pending consent handles found for tenant: {}", tenantId);
                } else {
                    // Process the batch
                    TenantResult batchResult = processBatch(pendingHandles, tenantId, runId);
                    tenantExpiredCount += batchResult.expiredCount();
                    affected.addAll(batchResult.affectedConsents());

                    log.debug("Processed batch {} for tenant {} - Expired {} consent handles",
                            offset + 1, tenantId, batchResult.expiredCount());

                    // Check if we got fewer records than batch size (last batch)
                    if (pendingHandles.size() < batchSize) {
                        hasMoreRecords = false;
                    } else {
                        offset++;
                    }
                }
            }

            log.info("Completed processing for tenant: {}. Expired: {} consent handles", tenantId, tenantExpiredCount);
            return new TenantResult(tenantExpiredCount, affected);
            
        } catch (Exception e) {
            log.error("Error processing consent handles for tenant: {}", tenantId, e);
            return new TenantResult(0, Set.of());
        } finally {
            // Clear tenant context
            ThreadContext.clearAll();
        }
    }

    /**
     * Finds pending consent handles for a specific tenant
     * 
     * @param tenantId The tenant ID
     * @param expiryDate The expiry date threshold
     * @param pageable Pagination information
     * @return List of pending consent handles for the tenant
     */
    private List<ConsentHandle> findPendingConsentHandlesForTenant(String tenantId, LocalDateTime expiryDate, Pageable pageable) {
        MongoTemplate mongoTemplate = tenantMongoTemplateProvider.getMongoTemplate(tenantId);
        
        Criteria criteria = new Criteria();
        criteria.and("status").is(ConsentHandleStatus.PENDING);
        criteria.and("createdAt").lt(expiryDate);
        
        Query query = new Query(criteria);
        query.with(pageable);
        
        return mongoTemplate.find(query, ConsentHandle.class);
    }

    /**
     * Processes a batch of consent handles for expiry
     * 
     * @param consentHandles List of consent handles to process
     * @param tenantId The tenant ID for logging
     * @return TenantResult with expired count and affected consent handles
     */
    private TenantResult processBatch(List<ConsentHandle> consentHandles, String tenantId, UUID runId) {
        int expiredCount = 0;
        Set<ConsentHandle> affected = new HashSet<>();

        for (ConsentHandle consentHandle : consentHandles) {
            try {
                // Update consent handle status to expired
                consentHandle.setStatus(ConsentHandleStatus.EXPIRED);
                consentHandleRepository.save(consentHandle, tenantId);
                
                // Trigger notification for expired consent handle
                try {
                    triggerExpiryNotification(consentHandle, tenantId);
                } catch (Exception e) {
                    log.error("Failed to trigger notification for consent handle: {} for tenant: {}", 
                            consentHandle.getId(), tenantId, e);
                    // Continue processing even if notification fails
                }
                // Log audit event for expired/updated consent handle
                this.logConsentHandleExpiryAudit(runId, consentHandle, ActionType.UPDATED);
                
                expiredCount++;
                affected.add(consentHandle);

                log.debug("Expired consent handle: {} for tenant: {}", consentHandle.getId(), tenantId);
                
            } catch (Exception e) {
                log.error("Error expiring consent handle: {} for tenant: {}", consentHandle.getId(), tenantId, e);
            }
        }
        
        return new TenantResult(expiredCount, affected);
    }

    /**
     * Triggers notification for expired consent handle
     * 
     * @param consentHandle The expired consent handle
     * @param tenantId The tenant ID
     */
    private void triggerExpiryNotification(ConsentHandle consentHandle, String tenantId) {
        try {
            CustomerIdentifiers customerIdentifiers = consentHandle.getCustomerIdentifiers();
            String businessId = consentHandle.getBusinessId();
            
            if (customerIdentifiers != null) {
                notificationManager.initiateConsentHandleNotification(
                        NotificationEvent.CONSENT_REQUEST_EXPIRED,
                        tenantId,
                        businessId,
                        customerIdentifiers,
                        null, // processor activity IDs
                        null, // event payload
                        LANGUAGE.ENGLISH,
                        consentHandle.getConsentHandleId()
                );
                
                log.debug("Triggered CONSENT_REQUEST_EXPIRED notification for consent handle: {}", 
                        consentHandle.getConsentHandleId());
            } else {
                log.warn("Cannot trigger notification - customer identifiers are null for consent handle: {}", 
                        consentHandle.getConsentHandleId());
            }
        } catch (Exception e) {
            log.error("Error triggering notification for consent handle: {}", consentHandle.getConsentHandleId(), e);
        }
    }

    /**
     * Calculates the expiry date based on period type and value
     * 
     * @param periodType Type of period (MONTHS, YEARS, DAYS, etc.)
     * @param periodValue Value of the period
     * @return LocalDateTime representing the expiry date
     */
    private LocalDateTime calculateExpiryDate(String periodType, int periodValue) {
        LocalDateTime now = LocalDateTime.now();
        
        try {
            Period period = Period.valueOf(periodType.toUpperCase());
            
            return switch (period) {
                case DAYS -> now.minus(periodValue, ChronoUnit.DAYS);
                case WEEKS -> now.minus(periodValue, ChronoUnit.WEEKS);
                case MONTHS -> now.minus(periodValue, ChronoUnit.MONTHS);
                case YEARS -> now.minus(periodValue, ChronoUnit.YEARS);
            };
            
        } catch (IllegalArgumentException e) {
            log.error("Invalid period type: {}. Defaulting to MONTHS", periodType);
            return now.minus(periodValue, ChronoUnit.MONTHS);
        }
    }

    private com.jio.schedular.dto.Resource createResource(String type, String id, String businessId) {
        return com.jio.schedular.dto.Resource.builder()
            .type(type)
            .id(id)
            .businessId(businessId)
            .build();
    }

    /**
     * Modular function to log consent handle audit events
     * Can be used in both create and update consent handle flows
     *
     * @param consentHandle The consent handle affected
     * @param actionType The action type (UPDATED)
     */
    public void logConsentHandleExpiryAudit(UUID runId, ConsentHandle consentHandle, ActionType actionType) {
        try {
            String tenantId = ThreadContext.get(Constants.TENANT_ID_HEADER);

            Actor actor = Actor.builder()
                    .id(runId.toString())
                    .role(Constants.SCHEDULAR)
                    .type(Constants.RUN_ID)
                    .build();

            Resource resource = Resource.builder()
                    .type(Constants.CONSENT_HANDLE_ID)
                    .id(consentHandle.getConsentHandleId())
                    .build();

            Context context = Context.builder()
                    .ipAddress(ThreadContext.get(Constants.SOURCE_IP) != null && !ThreadContext.get(Constants.SOURCE_IP).equals("-")
                            ? ThreadContext.get(Constants.SOURCE_IP) : UUID.randomUUID().toString())
                    .txnId(ThreadContext.get(Constants.TXN_ID_THREAD_CONTEXT) != null && !ThreadContext.get(Constants.TXN_ID_THREAD_CONTEXT).equals("-")
                            ? ThreadContext.get(Constants.TXN_ID_THREAD_CONTEXT) : Constants.IP_ADDRESS)
                    .build();

            Map<String, Object> extra = new HashMap<>();
            
            extra.put(Constants.DATA, consentHandle);

            AuditRequest auditRequest = AuditRequest.builder()
                    .actor(actor)
                    .businessId(consentHandle.getBusinessId())
                    .group(String.valueOf(Group.CONSENT_HANDLE))
                    .component(AuditComponent.CONSENT_HANDLE_EXPIRED)
                    .actionType(actionType)
                    .resource(resource)
                    .initiator(Constants.SCHEDULAR)
                    .context(context)
                    .extra(extra)
                    .build();

            this.auditManager.logAudit(auditRequest, tenantId);
        } catch (Exception e) {
            log.error("Audit logging failed for run id: {}, consent handle id: {}, action: {}, error: {}", 
                    runId.toString(), consentHandle.getConsentHandleId(), actionType, e.getMessage(), e);
        }
    }
}