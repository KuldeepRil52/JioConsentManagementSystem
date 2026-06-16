package com.jio.digigov.notification.service.onboarding.impl;

import com.jio.digigov.notification.util.MongoTemplateProvider;
import com.jio.digigov.notification.util.TenantContextHolder;
import com.jio.digigov.notification.dto.request.onboarding.OnboardingSetupRequestDto;
import com.jio.digigov.notification.entity.onboarding.OnboardingJob;
import com.jio.digigov.notification.enums.OnboardingJobStatus;
import com.jio.digigov.notification.enums.OnboardingStep;
import com.jio.digigov.notification.repository.onboarding.OnboardingJobRepository;
import com.jio.digigov.notification.service.audit.AuditEventService;
import com.jio.digigov.notification.service.masterlist.TenantMasterListConfigService;
import com.jio.digigov.notification.entity.event.EventConfiguration;
import com.jio.digigov.notification.service.event.EventConfigurationService;
import com.jio.digigov.notification.dto.request.masterlist.CreateMasterListRequestDto;
import com.jio.digigov.notification.dto.masterlist.MasterListEntry;
import com.jio.digigov.notification.entity.TenantMasterListConfig;
import com.jio.digigov.notification.entity.template.NotificationTemplate;
import com.jio.digigov.notification.entity.NotificationConfig;
import com.jio.digigov.notification.enums.EventType;
import com.jio.digigov.notification.enums.ScopeLevel;
import com.jio.digigov.notification.enums.NotificationType;
import com.jio.digigov.notification.enums.NotificationChannel;
import com.jio.digigov.notification.enums.TemplateStatus;
import com.jio.digigov.notification.enums.ProviderType;
import com.jio.digigov.notification.enums.RecipientType;
import com.jio.digigov.notification.enums.MasterListDataSource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Asynchronous service for processing onboarding operations in the background.
 *
 * This service handles the actual creation of templates, event configurations,
 * and master list labels. It runs in a separate thread pool and updates job
 * progress in real-time.
 *
 * Processing Steps:
 * 1. EXTRACT_LABELS (0-30%) - Extract master labels from templates loaded from JSON
 * 2. CREATE_MASTER_LIST (30-50%) - Copy all labels from JSON to tenant DB, init empty mappings
 * 3. CREATE_TEMPLATES (50-80%) - Copy templates from JSON to tenant DB, build event-to-label & label-to-event mappings
 * 4. CREATE_EVENT_CONFIGS (80-95%) - Copy event configs from JSON to tenant DB
 * 5. FINALIZE (95-100%) - Save master list with mappings, complete job
 *
 * @author Notification Service Team
 * @version 2.0
 * @since 2025-01-21
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class OnboardingAsyncService {

    private final MongoTemplateProvider mongoTemplateProvider;
    private final OnboardingJobRepository jobRepository;

    // Services for actual creation
    private final TenantMasterListConfigService masterListService;
    private final EventConfigurationService eventConfigurationService;
    private final AuditEventService auditEventService;

    /**
     * Processes onboarding asynchronously.
     *
     * This method runs in a background thread and performs all necessary
     * setup for a new tenant/business.
     *
     * @param jobId Job identifier
     * @param tenantId Tenant identifier
     * @param businessId Business identifier
     * @param scopeLevel Scope level for created templates
     * @param request Onboarding setup request
     */
    @Async("taskExecutor")
    public void processOnboarding(
            String jobId,
            String tenantId,
            String businessId,
            ScopeLevel scopeLevel,
            OnboardingSetupRequestDto request) {

        log.info("Starting async onboarding: jobId={}, tenant={}, business={}, scope={}",
                jobId, tenantId, businessId, scopeLevel);

        // Get tenant-specific MongoTemplate
        MongoTemplate mongoTemplate = mongoTemplateProvider.getTemplate(tenantId);

        // Set tenant context for other services (if they use it)
        TenantContextHolder.setTenantId(tenantId);
        TenantContextHolder.setBusinessId(businessId);

        try {
            // Step 0: Update status to IN_PROGRESS
            updateJobStatus(mongoTemplate, jobId, OnboardingJobStatus.IN_PROGRESS, OnboardingStep.EXTRACT_LABELS, 0);

            // Step 0.5: Copy notification configuration from shared DB to tenant DB (0-10%)
            log.info("[{}] Step 0.5: Copying notification configuration from shared DB", jobId);
            copyNotificationConfiguration(mongoTemplate, jobId, tenantId, businessId);
            updateJobProgress(mongoTemplate, jobId, OnboardingStep.EXTRACT_LABELS, 10);

            // Step 1: Extract required labels from shared DB templates (10-30%)
            log.info("[{}] Step 1: Extracting master labels from shared DB templates", jobId);
            MongoTemplate sharedTemplate = mongoTemplateProvider.getTemplate("SYSTEM");
            Set<String> requiredLabels = extractRequiredLabels(sharedTemplate);
            updateJobProgress(mongoTemplate, jobId, OnboardingStep.EXTRACT_LABELS, 30);

            // Step 2: Copy master list from shared DB (30-50%)
            log.info("[{}] Step 2: Copying master list configuration from shared DB", jobId);
            int masterListCreated = createMasterList(
                    sharedTemplate,
                    mongoTemplate,
                    jobId,
                    tenantId,
                    businessId,
                    request.getCreateMasterList(),
                    requiredLabels
            );
            updateJobProgress(mongoTemplate, jobId, OnboardingStep.CREATE_MASTER_LIST, 50);

            // Step 3: Create templates (50-80%)
            int templatesCreated = 0;
            int templatesFailed = 0;
            int smsCount = 0;
            int emailCount = 0;

            // Build event-to-label and label-to-event mappings from templates
            Map<EventType, Set<String>> eventToLabelMappings = new HashMap<>();
            Map<String, Set<EventType>> labelToEventMappings = new HashMap<>();

            if (request.getCreateTemplates()) {
                log.info("[{}] Step 3: Copying notification templates from shared DB", jobId);
                Map<String, Object> templateResults = createTemplates(
                        sharedTemplate,
                        mongoTemplate,
                        jobId,
                        tenantId,
                        businessId,
                        scopeLevel
                );
                templatesCreated = (Integer) templateResults.get("created");
                templatesFailed = (Integer) templateResults.get("failed");
                smsCount = (Integer) templateResults.get("smsCount");
                emailCount = (Integer) templateResults.get("emailCount");
                eventToLabelMappings = (Map<EventType, Set<String>>) templateResults.get("eventToLabelMappings");
                labelToEventMappings = (Map<String, Set<EventType>>) templateResults.get("labelToEventMappings");
            } else {
                log.info("[{}] Step 3: Skipped (createTemplates=false)", jobId);
            }
            updateJobProgress(mongoTemplate, jobId, OnboardingStep.CREATE_TEMPLATES, 80);

            // Step 4: Create event configurations (80-95%)
            int eventConfigsCreated = 0;
            int eventConfigsFailed = 0;

            if (request.getCreateEventConfigurations()) {
                log.info("[{}] Step 4: Copying event configurations from shared DB", jobId);
                Map<String, Integer> eventConfigResults = createEventConfigurations(
                        sharedTemplate,
                        mongoTemplate,
                        jobId,
                        tenantId,
                        businessId
                );
                eventConfigsCreated = eventConfigResults.get("created");
                eventConfigsFailed = eventConfigResults.get("failed");
            } else {
                log.info("[{}] Step 4: Skipped (createEventConfigurations=false)", jobId);
            }
            updateJobProgress(mongoTemplate, jobId, OnboardingStep.CREATE_EVENT_CONFIGS, 95);

            // Step 5: Finalize - Save master list with mappings (95-100%)
            log.info("[{}] Step 5: Finalizing onboarding - saving master list with mappings", jobId);

            // Update master list with event-to-label mappings
            if (request.getCreateMasterList() && !eventToLabelMappings.isEmpty()) {
                Optional<TenantMasterListConfig> configOpt = masterListService.getTenantConfig(tenantId);
                if (configOpt.isPresent()) {
                    TenantMasterListConfig config = configOpt.get();

                    // Update event mappings using helper methods
                    for (Map.Entry<EventType, Set<String>> entry : eventToLabelMappings.entrySet()) {
                        config.addEventToLabelMapping(entry.getKey(), entry.getValue());
                    }

                    // Save updated config
                    masterListService.updateTenantConfig(tenantId, config);
                    log.info("[{}] Updated master list with {} event-to-label mappings", jobId, eventToLabelMappings.size());
                }
            }
            finalizeJob(
                    mongoTemplate,
                    jobId,
                    masterListCreated,
                    templatesCreated,
                    templatesFailed,
                    smsCount,
                    emailCount,
                    eventConfigsCreated,
                    eventConfigsFailed
            );

            log.info("[{}] Onboarding completed successfully", jobId);

        } catch (Exception e) {
            log.error("[{}] Onboarding failed: {}", jobId, e.getMessage());
            updateJobStatus(
                    mongoTemplate,
                    jobId,
                    OnboardingJobStatus.FAILED,
                    null,
                    0,
                    e.getMessage()
            );

            // Audit failed onboarding (no httpRequest available in async context)
            auditEventService.auditTenantOnboarding(
                    jobId,
                    "FAILED",
                    tenantId,
                    businessId,
                    jobId // Using jobId as transactionId
            );

        } finally {
            TenantContextHolder.clear();
        }
    }

    /**
     * Extracts required master labels from all template definitions in shared DB.
     *
     * @param sharedTemplate Shared DB MongoTemplate
     * @return Set of required master label identifiers
     */
    private Set<String> extractRequiredLabels(MongoTemplate sharedTemplate) {
        // Query all SYSTEM templates from shared DB
        Query query = new Query(Criteria.where("businessId").is("SYSTEM"));
        List<NotificationTemplate> templates = sharedTemplate.find(query, NotificationTemplate.class);

        Set<String> labels = new HashSet<>();

        // Extract labels from SMS arguments
        for (NotificationTemplate template : templates) {
            if (template.getSmsConfig() != null && template.getSmsConfig().getArgumentsMap() != null) {
                labels.addAll(template.getSmsConfig().getArgumentsMap().values());
            }

            // Extract labels from Email arguments
            if (template.getEmailConfig() != null) {
                if (template.getEmailConfig().getArgumentsSubjectMap() != null) {
                    labels.addAll(template.getEmailConfig().getArgumentsSubjectMap().values());
                }
                if (template.getEmailConfig().getArgumentsBodyMap() != null) {
                    labels.addAll(template.getEmailConfig().getArgumentsBodyMap().values());
                }
            }
        }

        log.info("Extracted {} unique master labels from {} templates in shared DB",
                labels.size(), templates.size());

        return labels;
    }

    /**
     * Creates master list configuration by copying from shared DB.
     *
     * If createMasterList=true, copies ALL labels from shared DB.
     * If createMasterList=false, copies ONLY required labels extracted from templates.
     *
     * @param sharedTemplate Shared DB MongoTemplate
     * @param mongoTemplate Tenant-specific MongoTemplate
     * @param jobId Job identifier
     * @param tenantId Tenant identifier
     * @param businessId Business identifier
     * @param createAll Whether to create all labels or only required ones
     * @param requiredLabels Set of required label identifiers
     * @return Number of labels created
     */
    private int createMasterList(
            MongoTemplate sharedTemplate,
            MongoTemplate mongoTemplate,
            String jobId,
            String tenantId,
            String businessId,
            boolean createAll,
            Set<String> requiredLabels) {

        // Query master list config from shared DB
        Query query = new Query(Criteria.where("isActive").is(true));
        TenantMasterListConfig sharedConfig = sharedTemplate.findOne(query, TenantMasterListConfig.class);

        if (sharedConfig == null || sharedConfig.getMasterListConfig() == null) {
            log.warn("[{}] No ACTIVE master list configuration found in shared DB", jobId);
            return 0;
        }

        Map<String, MasterListEntry> allLabels = sharedConfig.getMasterListConfig();
        Map<String, MasterListEntry> labelsToCreate;

        if (createAll) {
            // Copy all labels
            labelsToCreate = allLabels;
            log.info("[{}] Copying ALL {} master labels from shared DB", jobId, allLabels.size());
        } else {
            // Copy only required labels
            labelsToCreate = allLabels.entrySet().stream()
                    .filter(entry -> requiredLabels.contains(entry.getKey()))
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
            log.info("[{}] Copying {} required master labels from shared DB (out of {} total)",
                    jobId, labelsToCreate.size(), allLabels.size());
        }

        // Check if master list configuration already exists
        Optional<TenantMasterListConfig> existingConfig =
                masterListService.getTenantConfig(tenantId);

        try {
            if (existingConfig.isPresent() && !createAll) {
                // Master list exists and we only need to add required labels
                log.info("[{}] Master list configuration exists, merging {} required labels",
                        jobId, labelsToCreate.size());

                // Initialize with empty event mappings (will be updated after templates are created)
                masterListService.addLabelsToConfig(
                        tenantId,
                        labelsToCreate,
                        new HashMap<>(),
                        "Updated with labels required for templates during onboarding"
                );

                log.info("[{}] Successfully merged {} labels into existing master list",
                        jobId, labelsToCreate.size());

            } else {
                // Create new master list configuration
                log.info("[{}] Creating new master list configuration with {} labels",
                        jobId, labelsToCreate.size());

                CreateMasterListRequestDto request = CreateMasterListRequestDto.builder()
                        .masterListConfig(labelsToCreate)
                        .eventMappings(new HashMap<>()) // Empty initially, will be updated after templates
                        .description("Master list configuration copied from shared DB during onboarding")
                        .build();

                masterListService.createMasterListConfig(tenantId, request);

                log.info("[{}] Successfully created master list with {} labels",
                        jobId, labelsToCreate.size());
            }

            return labelsToCreate.size();

        } catch (Exception e) {
            log.error("[{}] Failed to create/update master list: {}", jobId, e.getMessage());
            throw new RuntimeException("Master list creation/update failed: " + e.getMessage(), e);
        }
    }

    /**
     * Copies templates from shared DB to tenant DB.
     * Changes businessId and scopeLevel to tenant-specific values.
     * Builds event-to-label and label-to-event mappings dynamically from template arguments.
     *
     * @param sharedTemplate Shared DB MongoTemplate
     * @param mongoTemplate Tenant-specific MongoTemplate
     * @param jobId Job identifier
     * @param tenantId Tenant identifier
     * @param businessId Business identifier
     * @param scopeLevel Scope level
     * @return Map with "created", "failed", "smsCount", "emailCount", "eventToLabelMappings", "labelToEventMappings"
     */
    private Map<String, Object> createTemplates(
            MongoTemplate sharedTemplate,
            MongoTemplate mongoTemplate,
            String jobId,
            String tenantId,
            String businessId,
            ScopeLevel scopeLevel) {

        // Query all SYSTEM templates from shared DB
        Query query = new Query(Criteria.where("businessId").is("SYSTEM"));
        List<NotificationTemplate> sharedTemplates = sharedTemplate.find(query, NotificationTemplate.class);

        int created = 0;
        int failed = 0;
        int smsCount = 0;
        int emailCount = 0;

        // Build mappings dynamically
        Map<EventType, Set<String>> eventToLabelMappings = new HashMap<>();
        Map<String, Set<EventType>> labelToEventMappings = new HashMap<>();

        for (NotificationTemplate systemTemplate : sharedTemplates) {
            try {
                // Track labels for this event
                Set<String> labelsForEvent = new HashSet<>();

                // Clone template and update businessId and scopeLevel
                NotificationTemplate tenantTemplate = NotificationTemplate.builder()
                        .templateId(systemTemplate.getTemplateId())  // Use existing templateId
                        .businessId(businessId)  // Change to tenant businessId
                        .scopeLevel(scopeLevel)  // Change to TENANT scopeLevel
                        .eventType(systemTemplate.getEventType())
                        .language(systemTemplate.getLanguage())
                        .type(systemTemplate.getType())
                        .channelType(systemTemplate.getChannelType())
                        .recipientType(systemTemplate.getRecipientType())
                        .providerType(systemTemplate.getProviderType())
                        .status(systemTemplate.getStatus())
                        .version(systemTemplate.getVersion())  // Copy version from SYSTEM
                        .smsConfig(systemTemplate.getSmsConfig())
                        .emailConfig(systemTemplate.getEmailConfig())
                        .build();

                mongoTemplate.save(tenantTemplate);
                log.debug("[{}] Copied {} template for event: {}",
                        jobId, systemTemplate.getChannelType(), systemTemplate.getEventType());

                // Count by channel type
                if (NotificationChannel.SMS.equals(systemTemplate.getChannelType())) {
                    smsCount++;
                } else if (NotificationChannel.EMAIL.equals(systemTemplate.getChannelType())) {
                    emailCount++;
                }
                created++;

                // Collect labels from template arguments
                if (systemTemplate.getSmsConfig() != null && systemTemplate.getSmsConfig().getArgumentsMap() != null) {
                    labelsForEvent.addAll(systemTemplate.getSmsConfig().getArgumentsMap().values());
                }
                if (systemTemplate.getEmailConfig() != null) {
                    if (systemTemplate.getEmailConfig().getArgumentsSubjectMap() != null) {
                        labelsForEvent.addAll(systemTemplate.getEmailConfig().getArgumentsSubjectMap().values());
                    }
                    if (systemTemplate.getEmailConfig().getArgumentsBodyMap() != null) {
                        labelsForEvent.addAll(systemTemplate.getEmailConfig().getArgumentsBodyMap().values());
                    }
                }

                // Build event-to-label mapping
                if (!labelsForEvent.isEmpty()) {
                    try {
                        EventType eventTypeEnum = EventType.valueOf(systemTemplate.getEventType());
                        eventToLabelMappings.merge(eventTypeEnum, labelsForEvent, (existing, newLabels) -> {
                            existing.addAll(newLabels);
                            return existing;
                        });

                        // Build label-to-event mapping (reverse mapping)
                        for (String label : labelsForEvent) {
                            labelToEventMappings.computeIfAbsent(label, k -> new HashSet<>()).add(eventTypeEnum);
                        }
                    } catch (IllegalArgumentException e) {
                        log.warn("[{}] Unknown event type: {}, skipping mapping", jobId, systemTemplate.getEventType());
                    }
                }

            } catch (Exception e) {
                log.error("[{}] Failed to copy template {}: {}",
                        jobId, systemTemplate.getTemplateId(), e.getMessage(), e);
                failed++;
            }
        }

        log.info("[{}] Templates: {} created ({} SMS, {} Email), {} failed",
                jobId, created, smsCount, emailCount, failed);
        log.info("[{}] Built mappings: {} event-to-label, {} label-to-event",
                jobId, eventToLabelMappings.size(), labelToEventMappings.size());

        Map<String, Object> results = new HashMap<>();
        results.put("created", created);
        results.put("failed", failed);
        results.put("smsCount", smsCount);
        results.put("emailCount", emailCount);
        results.put("eventToLabelMappings", eventToLabelMappings);
        results.put("labelToEventMappings", labelToEventMappings);

        return results;
    }

    /**
     * Copies event configurations from shared DB to tenant DB.
     * Changes businessId to tenant-specific value.
     *
     * @param sharedTemplate Shared DB MongoTemplate
     * @param mongoTemplate Tenant-specific MongoTemplate
     * @param jobId Job identifier
     * @param tenantId Tenant identifier
     * @param businessId Business identifier
     * @return Map with "created", "failed"
     */
    private Map<String, Integer> createEventConfigurations(
            MongoTemplate sharedTemplate,
            MongoTemplate mongoTemplate,
            String jobId,
            String tenantId,
            String businessId) {

        // Query all SYSTEM event configurations from shared DB
        Query query = new Query(Criteria.where("businessId").is("SYSTEM"));
        List<EventConfiguration> sharedConfigs = sharedTemplate.find(query, EventConfiguration.class);

        int created = 0;
        int failed = 0;

        for (EventConfiguration systemConfig : sharedConfigs) {
            try {
                // Clone event configuration and update businessId
                EventConfiguration tenantConfig = EventConfiguration.builder()
                        .configId(null)  // Will be auto-generated
                        .businessId(businessId)  // Change to tenant businessId
                        .scopeLevel(systemConfig.getScopeLevel())
                        .eventType(systemConfig.getEventType())
                        .notifications(systemConfig.getNotifications())
                        .priority(systemConfig.getPriority())
                        .isActive(systemConfig.getIsActive())
                        .build();

                // Save to tenant database
                mongoTemplate.save(tenantConfig);

                log.debug("[{}] Copied event config for: {}", jobId, systemConfig.getEventType());
                created++;

            } catch (Exception e) {
                log.error("[{}] Failed to copy event config for {}: {}",
                        jobId, systemConfig.getEventType(), e.getMessage(), e);
                failed++;
            }
        }

        log.info("[{}] Event configurations: {} created, {} failed",
                jobId, created, failed);

        Map<String, Integer> results = new HashMap<>();
        results.put("created", created);
        results.put("failed", failed);

        return results;
    }

    /**
     * Finalizes the job with results.
     *
     * @param mongoTemplate Tenant-specific MongoTemplate
     * @param jobId Job identifier
     * @param masterListCreated Number of master labels created
     * @param templatesCreated Number of templates created
     * @param templatesFailed Number of templates that failed
     * @param smsCount Number of SMS templates
     * @param emailCount Number of Email templates
     * @param eventConfigsCreated Number of event configs created
     * @param eventConfigsFailed Number of event configs that failed
     */
    private void finalizeJob(
            MongoTemplate mongoTemplate,
            String jobId,
            int masterListCreated,
            int templatesCreated,
            int templatesFailed,
            int smsCount,
            int emailCount,
            int eventConfigsCreated,
            int eventConfigsFailed) {

        // Build results
        OnboardingJob.OnboardingResults results = OnboardingJob.OnboardingResults.builder()
                .masterListLabels(OnboardingJob.OnboardingResults.MasterListResult.builder()
                        .created(masterListCreated)
                        .failed(0)
                        .build())
                .templates(OnboardingJob.OnboardingResults.TemplateResult.builder()
                        .created(templatesCreated)
                        .failed(templatesFailed)
                        .smsCount(smsCount)
                        .emailCount(emailCount)
                        .build())
                .eventConfigurations(OnboardingJob.OnboardingResults.EventConfigResult.builder()
                        .created(eventConfigsCreated)
                        .failed(eventConfigsFailed)
                        .build())
                .build();

        // Determine final status
        OnboardingJobStatus finalStatus;
        if (templatesFailed > 0 || eventConfigsFailed > 0) {
            finalStatus = OnboardingJobStatus.COMPLETED_WITH_ERRORS;
        } else {
            finalStatus = OnboardingJobStatus.COMPLETED;
        }

        // Update job
        Query query = new Query(Criteria.where("jobId").is(jobId));
        Update update = new Update()
                .set("status", finalStatus)
                .set("currentStep", OnboardingStep.FINALIZE)
                .set("progressPercentage", 100)
                .set("results", results)
                .set("completedAt", LocalDateTime.now())
                .set("updatedAt", LocalDateTime.now());

        mongoTemplate.updateFirst(query, update, OnboardingJob.class);

        // Retrieve job to get tenant and business IDs for audit
        OnboardingJob job = mongoTemplate.findOne(query, OnboardingJob.class);
        if (job != null) {
            // Audit completed onboarding (no httpRequest available in async context)
            auditEventService.auditTenantOnboarding(
                    jobId,
                    "COMPLETED",
                    TenantContextHolder.getTenantId(), // Get tenantId from context
                    job.getBusinessId(),
                    jobId // Using jobId as transactionId
            );
        }

        log.info("[{}] Job finalized with status: {}", jobId, finalStatus);
    }

    /**
     * Updates job status.
     *
     * @param mongoTemplate Tenant-specific MongoTemplate
     * @param jobId Job identifier
     * @param status New status
     * @param step Current step (optional)
     * @param progress Progress percentage
     * @param errors Error messages (optional)
     */
    private void updateJobStatus(
            MongoTemplate mongoTemplate,
            String jobId,
            OnboardingJobStatus status,
            OnboardingStep step,
            int progress,
            String... errors) {

        Query query = new Query(Criteria.where("jobId").is(jobId));
        Update update = new Update()
                .set("status", status)
                .set("updatedAt", LocalDateTime.now());

        if (step != null) {
            update.set("currentStep", step);
        }

        if (progress >= 0) {
            update.set("progressPercentage", progress);
        }

        if (status == OnboardingJobStatus.IN_PROGRESS) {
            update.set("startedAt", LocalDateTime.now());
        } else if (status == OnboardingJobStatus.COMPLETED ||
                status == OnboardingJobStatus.FAILED ||
                status == OnboardingJobStatus.COMPLETED_WITH_ERRORS) {
            update.set("completedAt", LocalDateTime.now());
        }

        if (errors.length > 0) {
            update.push("errors").each((Object[]) errors);
        }

        mongoTemplate.updateFirst(query, update, OnboardingJob.class);
    }

    /**
     * Updates job progress.
     *
     * @param mongoTemplate Tenant-specific MongoTemplate
     * @param jobId Job identifier
     * @param step Current step
     * @param progress Progress percentage
     */
    private void updateJobProgress(
            MongoTemplate mongoTemplate,
            String jobId,
            OnboardingStep step,
            int progress) {

        Query query = new Query(Criteria.where("jobId").is(jobId));
        Update update = new Update()
                .set("currentStep", step)
                .set("progressPercentage", progress)
                .set("updatedAt", LocalDateTime.now());

        mongoTemplate.updateFirst(query, update, OnboardingJob.class);
    }

    /**
     * Copies notification configuration from shared DB to tenant DB.
     * Changes scopeLevel from SYSTEM to TENANT.
     *
     * @param mongoTemplate Tenant-specific MongoTemplate
     * @param jobId Job identifier
     * @param tenantId Tenant identifier
     * @param businessId Business identifier (SYSTEM for tenant-level config)
     */
    private void copyNotificationConfiguration(
            MongoTemplate mongoTemplate,
            String jobId,
            String tenantId,
            String businessId) {

        try {
            // Get shared database template
            MongoTemplate sharedTemplate = mongoTemplateProvider.getTemplate("SYSTEM");

            // Find SYSTEM-level configuration in shared DB
            Query query = new Query(Criteria.where("businessId").is("SYSTEM"));
            NotificationConfig systemConfig =
                    sharedTemplate.findOne(query, NotificationConfig.class);

            if (systemConfig == null) {
                log.warn("[{}] No SYSTEM notification configuration found in shared DB, skipping copy", jobId);
                return;
            }

            log.info("[{}] Found SYSTEM notification configuration: configId={}, providerType={}",
                    jobId, systemConfig.getConfigId(), systemConfig.getProviderType());

            // Check if tenant already has a configuration
            Query tenantQuery = new Query(Criteria.where("businessId").is(businessId));
            boolean tenantConfigExists = mongoTemplate.exists(tenantQuery,
                    NotificationConfig.class);

            if (tenantConfigExists) {
                log.info("[{}] Tenant already has notification configuration, skipping copy", jobId);
                return;
            }

            // Create new configuration for tenant with TENANT scope
            NotificationConfig.NotificationConfigBuilder builder = NotificationConfig.builder()
                    .businessId(businessId)
                    .configId(UUID.randomUUID().toString())
                    .scopeLevel("TENANT")
                    .providerType(systemConfig.getProviderType());

            // Copy based on provider type - separate configurationJson and smtpDetails
            if (systemConfig.getProviderType() == ProviderType.DIGIGOV) {
                if (systemConfig.getConfigurationJson() == null) {
                    log.error("[{}] SYSTEM DigiGov config has null configurationJson", jobId);
                    throw new IllegalStateException("Invalid SYSTEM DigiGov configuration");
                }

                // Deep copy DigiGov config
                builder.configurationJson(systemConfig.getConfigurationJson());
                builder.smtpDetails(null); // No SMTP for DigiGov
                log.info("[{}] Copied DigiGov configuration to tenant", jobId);

            } else if (systemConfig.getProviderType() == ProviderType.SMTP) {
                if (systemConfig.getSmtpDetails() == null) {
                    log.error("[{}] SYSTEM SMTP config has null smtpDetails", jobId);
                    throw new IllegalStateException("Invalid SYSTEM SMTP configuration");
                }

                // Deep copy SMTP config
                builder.smtpDetails(systemConfig.getSmtpDetails());
                builder.configurationJson(null); // No DigiGov for SMTP
                log.info("[{}] Copied SMTP configuration to tenant", jobId);

            } else {
                log.error("[{}] Unknown provider type: {}", jobId, systemConfig.getProviderType());
                throw new IllegalStateException("Unknown provider type: " + systemConfig.getProviderType());
            }

            NotificationConfig tenantConfig = builder.build();

            // Save to tenant database
            mongoTemplate.save(tenantConfig);

            log.info("[{}] Successfully copied {} notification configuration to tenant DB: configId={}, scopeLevel=TENANT",
                    jobId, systemConfig.getProviderType(), tenantConfig.getConfigId());

        } catch (Exception e) {
            log.error("[{}] Failed to copy notification configuration: {}", jobId, e.getMessage());
            // Don't fail onboarding if config copy fails - continue with other steps
        }
    }
}
