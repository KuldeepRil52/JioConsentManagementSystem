package com.jio.digigov.notification.service.bootstrap;

import com.jio.digigov.notification.dto.masterlist.MasterListEntry;
import com.jio.digigov.notification.dto.onboarding.json.EventConfigDataFile;
import com.jio.digigov.notification.dto.onboarding.json.MasterLabelDataFile;
import com.jio.digigov.notification.dto.onboarding.json.TemplateDataFile;
import com.jio.digigov.notification.enums.MasterListDataSource;
import com.jio.digigov.notification.service.onboarding.OnboardingDataLoader;
import com.jio.digigov.notification.util.TenantContextHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Bootstrap service that validates system notification configuration on startup.
 *
 * This service:
 * - Loads and validates JSON configuration files at startup
 * - Provides metadata about available templates, events, and master labels
 * - Can be used to sync new configuration to existing tenant databases
 *
 * NOTE: This service is DISABLED. Use SystemOnboardingService instead.
 *
 * @author Notification Service Team
 * @version 1.0
 * @since 2025-01-08
 */
// @Service  // DISABLED - SystemOnboardingService handles bootstrap
@Slf4j
public class SystemNotificationBootstrapService {

    private final OnboardingDataLoader dataLoader;
    private final MongoTemplate mongoTemplate; // Shared DB template

    public SystemNotificationBootstrapService(
            OnboardingDataLoader dataLoader,
            @Qualifier("sharedMongoTemplate") MongoTemplate mongoTemplate) {
        this.dataLoader = dataLoader;
        this.mongoTemplate = mongoTemplate;

        // Log which bean was injected
        log.info("SystemNotificationBootstrapService initialized with MongoTemplate: {}",
                mongoTemplate.getClass().getName());
        log.info("MongoTemplate database factory: {}",
                mongoTemplate.getDb().getName());
    }

    /**
     * Bootstrap validation on application startup.
     * Validates that all JSON files are loaded correctly.
     *
     * NOTE: DISABLED - Use SystemOnboardingService instead
     */
    // @EventListener(ApplicationReadyEvent.class)  // DISABLED
    public void validateSystemNotificationConfiguration() {
        log.info("=================================================================");
        log.info("System Notification Bootstrap - Validating Configuration");
        log.info("=================================================================");

        try {
            // Set system context for bootstrap operations
            TenantContextHolder.setTenantId("default");
            TenantContextHolder.setBusinessId("SYSTEM");
            log.info("Bootstrap context set: tenantId=default, businessId=SYSTEM");

            // Validate data is loaded
            if (!dataLoader.isDataLoaded()) {
                log.error("System notification data not loaded!");
                throw new IllegalStateException("System notification configuration failed to load");
            }

            // Get counts
            List<TemplateDataFile> templates = dataLoader.getAllTemplates();
            List<EventConfigDataFile> eventConfigs = dataLoader.getAllEventConfigs();
            List<MasterLabelDataFile> masterLabels = dataLoader.getAllMasterLabels();

            log.info("Configuration Summary:");
            log.info("  - Templates:          {} templates loaded", templates.size());
            log.info("  - Event Configs:      {} event configurations loaded", eventConfigs.size());
            log.info("  - Master Labels:      {} master labels loaded", masterLabels.size());

            // Validate unique constraints
            validateTemplateUniqueness(templates);
            validateEventConfigUniqueness(eventConfigs);
            validateMasterLabelUniqueness(masterLabels);

            // Log template breakdown by event
            logTemplateBreakdown(templates);

            // Log event breakdown
            logEventConfigBreakdown(eventConfigs);

            // Log master label breakdown by data source
            logMasterLabelBreakdown(masterLabels);

            log.info("-----------------------------------------------------------------");
            log.info("Syncing with Shared Database...");
            log.info("-----------------------------------------------------------------");

            // Sync to shared database
            syncToSharedDatabase(templates, eventConfigs, masterLabels);

            log.info("=================================================================");
            log.info("System Notification Bootstrap - Validation Successful");
            log.info("=================================================================");

        } catch (Exception e) {
            log.error("=================================================================");
            log.error("System Notification Bootstrap - Validation FAILED");
            log.error("=================================================================");
            log.error("Error during system notification bootstrap: {}", e.getMessage());
            throw new IllegalStateException("System notification bootstrap failed", e);
        } finally {
            // Clear tenant context after bootstrap
            TenantContextHolder.clear();
            log.info("Bootstrap context cleared");
        }
    }

    /**
     * Validates that templates have unique eventType + recipientType combinations.
     */
    private void validateTemplateUniqueness(List<TemplateDataFile> templates) {
        Map<String, Long> duplicates = templates.stream()
                .collect(Collectors.groupingBy(
                        t -> t.getEventType() + "|" + t.getRecipientType(),
                        Collectors.counting()
                ))
                .entrySet().stream()
                .filter(e -> e.getValue() > 1)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        if (!duplicates.isEmpty()) {
            log.error("Duplicate templates found (eventType|recipientType): {}", duplicates);
            throw new IllegalStateException("Duplicate templates detected in templates.json");
        }

        log.debug("Template uniqueness validation passed - no duplicates found");
    }

    /**
     * Validates that event configs have unique eventType.
     */
    private void validateEventConfigUniqueness(List<EventConfigDataFile> eventConfigs) {
        Map<String, Long> duplicates = eventConfigs.stream()
                .collect(Collectors.groupingBy(
                        EventConfigDataFile::getEventType,
                        Collectors.counting()
                ))
                .entrySet().stream()
                .filter(e -> e.getValue() > 1)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        if (!duplicates.isEmpty()) {
            log.error("Duplicate event configurations found: {}", duplicates);
            throw new IllegalStateException("Duplicate event configurations detected in event-configs.json");
        }

        log.debug("Event config uniqueness validation passed - no duplicates found");
    }

    /**
     * Validates that master labels have unique labelName.
     */
    private void validateMasterLabelUniqueness(List<MasterLabelDataFile> masterLabels) {
        Map<String, Long> duplicates = masterLabels.stream()
                .collect(Collectors.groupingBy(
                        MasterLabelDataFile::getLabelName,
                        Collectors.counting()
                ))
                .entrySet().stream()
                .filter(e -> e.getValue() > 1)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        if (!duplicates.isEmpty()) {
            log.error("Duplicate master labels found: {}", duplicates);
            throw new IllegalStateException("Duplicate master labels detected in master-labels.json");
        }

        log.debug("Master label uniqueness validation passed - no duplicates found");
    }

    /**
     * Logs template breakdown by event type.
     */
    private void logTemplateBreakdown(List<TemplateDataFile> templates) {
        log.info("Template Breakdown by Event Type:");

        Map<String, Long> eventCounts = templates.stream()
                .collect(Collectors.groupingBy(
                        TemplateDataFile::getEventType,
                        Collectors.counting()
                ));

        eventCounts.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> log.info("    {} : {} template(s)", entry.getKey(), entry.getValue()));
    }

    /**
     * Logs event config breakdown.
     */
    private void logEventConfigBreakdown(List<EventConfigDataFile> eventConfigs) {
        log.info("Event Configuration Breakdown:");

        eventConfigs.stream()
                .sorted((a, b) -> a.getEventType().compareTo(b.getEventType()))
                .forEach(config -> {
                    int recipientCount = 0;
                    if (Boolean.TRUE.equals(config.getNotifyDataPrincipal())) recipientCount++;
                    if (Boolean.TRUE.equals(config.getNotifyDataFiduciary())) recipientCount++;
                    if (Boolean.TRUE.equals(config.getNotifyDataProcessor())) recipientCount++;
                    if (Boolean.TRUE.equals(config.getNotifyDpo())) recipientCount++;

                    log.info("    {} : {} recipient type(s), priority={}",
                            config.getEventType(), recipientCount, config.getPriority());
                });
    }

    /**
     * Logs master label breakdown by data source.
     */
    private void logMasterLabelBreakdown(List<MasterLabelDataFile> masterLabels) {
        log.info("Master Label Breakdown by Data Source:");

        Map<String, Long> sourceCounts = masterLabels.stream()
                .collect(Collectors.groupingBy(
                        MasterLabelDataFile::getDataSource,
                        Collectors.counting()
                ));

        sourceCounts.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> log.info("    {} : {} label(s)", entry.getKey(), entry.getValue()));
    }

    /**
     * Gets all unique event types from templates.
     */
    public Set<String> getAvailableEventTypes() {
        return dataLoader.getAllTemplates().stream()
                .map(TemplateDataFile::getEventType)
                .collect(Collectors.toSet());
    }

    /**
     * Gets all unique recipient types from templates.
     */
    public Set<String> getAvailableRecipientTypes() {
        return dataLoader.getAllTemplates().stream()
                .map(TemplateDataFile::getRecipientType)
                .collect(Collectors.toSet());
    }

    /**
     * Gets all available master label names.
     */
    public Set<String> getAvailableMasterLabels() {
        return dataLoader.getAllMasterLabels().stream()
                .map(MasterLabelDataFile::getLabelName)
                .collect(Collectors.toSet());
    }

    /**
     * Syncs JSON configuration to shared database.
     * Only inserts new records that don't exist.
     */
    private void syncToSharedDatabase(
            List<TemplateDataFile> templates,
            List<EventConfigDataFile> eventConfigs,
            List<MasterLabelDataFile> masterLabels) {

        log.info("Starting sync to shared database...");

        // Log event types being synced
        Set<String> eventTypes = templates.stream()
                .map(TemplateDataFile::getEventType)
                .collect(java.util.stream.Collectors.toSet());
        log.info("Event types in JSON: {}", eventTypes);

        // Sync templates
        int newTemplates = syncTemplates(templates);

        // Sync event configs
        int newEventConfigs = syncEventConfigs(eventConfigs);
        log.info("  - Event Configs:      {} new, {} existing (skipped)",
                newEventConfigs, eventConfigs.size() - newEventConfigs);

        // Sync master labels (creates master_list_config)
        int masterLabelCount = syncMasterLabels(masterLabels);
        if (masterLabelCount > 0) {
            log.info("  - Master List Config: Created with {} labels", masterLabelCount);
        } else {
            log.info("  - Master List Config: Already exists (skipped)");
        }

        log.info("Shared database sync completed successfully");
    }

    /**
     * Syncs templates to shared DB.
     * Returns count of new templates inserted.
     */
    private int syncTemplates(List<TemplateDataFile> templates) {
        int newCount = 0;
        int skippedCount = 0;

        log.info("Syncing {} template definitions from JSON...", templates.size());

        for (TemplateDataFile template : templates) {
            // Templates are unique by: eventType + recipientType + language + channelType
            // For system templates, we store SMS and EMAIL as separate entries with language="english"

            // Check SMS template (if exists)
            if (template.getSms() != null) {
                Query smsQuery = new Query()
                        .addCriteria(Criteria.where("eventType").is(template.getEventType()))
                        .addCriteria(Criteria.where("recipientType").is(template.getRecipientType()))
                        .addCriteria(Criteria.where("language").is("english"))
                        .addCriteria(Criteria.where("channelType").is("SMS"));

                boolean exists = mongoTemplate.exists(smsQuery, "system_templates");

                if (!exists) {
                    Map<String, Object> smsDoc = new HashMap<>();
                    smsDoc.put("eventType", template.getEventType());
                    smsDoc.put("recipientType", template.getRecipientType());
                    smsDoc.put("language", "english");
                    smsDoc.put("channelType", "SMS");
                    smsDoc.put("config", template.getSms());
                    smsDoc.put("createdAt", LocalDateTime.now());
                    smsDoc.put("updatedAt", LocalDateTime.now());
                    smsDoc.put("_class", "com.jio.digigov.notification.entity.SystemTemplate");

                    mongoTemplate.insert(smsDoc, "system_templates");
                    newCount++;
                    log.info("✓ Inserted SMS template: {} - {} - english",
                            template.getEventType(), template.getRecipientType());
                } else {
                    skippedCount++;
                    log.debug("⊘ Skipped SMS template (already exists): {} - {} - english",
                            template.getEventType(), template.getRecipientType());
                }
            }

            // Check EMAIL template (if exists)
            if (template.getEmail() != null) {
                Query emailQuery = new Query()
                        .addCriteria(Criteria.where("eventType").is(template.getEventType()))
                        .addCriteria(Criteria.where("recipientType").is(template.getRecipientType()))
                        .addCriteria(Criteria.where("language").is("english"))
                        .addCriteria(Criteria.where("channelType").is("EMAIL"));

                boolean exists = mongoTemplate.exists(emailQuery, "system_templates");

                if (!exists) {
                    Map<String, Object> emailDoc = new HashMap<>();
                    emailDoc.put("eventType", template.getEventType());
                    emailDoc.put("recipientType", template.getRecipientType());
                    emailDoc.put("language", "english");
                    emailDoc.put("channelType", "EMAIL");
                    emailDoc.put("config", template.getEmail());
                    emailDoc.put("createdAt", LocalDateTime.now());
                    emailDoc.put("updatedAt", LocalDateTime.now());
                    emailDoc.put("_class", "com.jio.digigov.notification.entity.SystemTemplate");

                    mongoTemplate.insert(emailDoc, "system_templates");
                    newCount++;
                    log.info("✓ Inserted EMAIL template: {} - {} - english",
                            template.getEventType(), template.getRecipientType());
                } else {
                    skippedCount++;
                    log.debug("⊘ Skipped EMAIL template (already exists): {} - {} - english",
                            template.getEventType(), template.getRecipientType());
                }
            }
        }

        log.info("Template sync summary: {} inserted, {} skipped (already exist)", newCount, skippedCount);
        return newCount;
    }

    /**
     * Syncs event configs to shared DB.
     * Returns count of new configs inserted.
     */
    private int syncEventConfigs(List<EventConfigDataFile> eventConfigs) {
        int newCount = 0;

        for (EventConfigDataFile config : eventConfigs) {
            // Check if event config exists (unique: eventType)
            Query query = new Query()
                    .addCriteria(Criteria.where("eventType").is(config.getEventType()));

            boolean exists = mongoTemplate.exists(query, "system_event_configs");

            if (!exists) {
                // Create document
                Map<String, Object> doc = new HashMap<>();
                doc.put("eventType", config.getEventType());
                doc.put("notifyDataPrincipal", config.getNotifyDataPrincipal());
                doc.put("notifyDataFiduciary", config.getNotifyDataFiduciary());
                doc.put("notifyDataProcessor", config.getNotifyDataProcessor());
                doc.put("notifyDpo", config.getNotifyDpo());
                doc.put("notifyCms", config.getNotifyCms());
                doc.put("priority", config.getPriority());
                doc.put("description", config.getDescription());
                doc.put("createdAt", LocalDateTime.now());
                doc.put("updatedAt", LocalDateTime.now());
                doc.put("_class", "com.jio.digigov.notification.entity.SystemEventConfig");

                mongoTemplate.insert(doc, "system_event_configs");
                newCount++;

                log.debug("Inserted new event config: {}", config.getEventType());
            }
        }

        return newCount;
    }

    /**
     * Syncs master labels to shared DB.
     * Creates or updates a single master_list_config document with all labels.
     * Returns count of labels in configuration.
     */
    private int syncMasterLabels(List<MasterLabelDataFile> masterLabels) {
        String dbName = mongoTemplate.getDb().getName();
        log.info("Syncing {} master labels to {}.master_list_config", masterLabels.size(), dbName);

        // Check if master_list_config already exists
        Query query = new Query();
        boolean configExists = mongoTemplate.exists(query, "master_list_config");

        if (configExists) {
            log.info("Master list configuration already exists in database, skipping sync");
            return 0;
        }

        // Convert all labels to MasterListEntry map
        Map<String, MasterListEntry> masterListConfig = new HashMap<>();

        for (MasterLabelDataFile label : masterLabels) {
            MasterListEntry.MasterListEntryBuilder entryBuilder = MasterListEntry.builder()
                    .dataSource(MasterListDataSource.valueOf(label.getDataSource()))
                    .path(label.getPath())
                    .defaultValue(label.getDefaultValue());

            // Add DB-specific fields
            if ("DB".equals(label.getDataSource())) {
                entryBuilder.collection(label.getCollection());
                if (label.getQuery() != null) {
                    Map<String, String> queryMap = new HashMap<>();
                    label.getQuery().forEach((k, v) -> queryMap.put(k, v != null ? v.toString() : null));
                    entryBuilder.query(queryMap);
                }
            }

            // Add GENERATE-specific fields
            if ("GENERATE".equals(label.getDataSource())) {
                entryBuilder.generator(label.getGenerator());
                entryBuilder.config(label.getConfig());
            }

            MasterListEntry entry = entryBuilder.build();
            masterListConfig.put(label.getLabelName(), entry);

            log.debug("  Added label: {} (dataSource={})", label.getLabelName(), label.getDataSource());
        }

        // Create master list configuration document
        Map<String, Object> configDoc = new HashMap<>();
        configDoc.put("masterListConfig", masterListConfig);
        configDoc.put("eventMappings", new HashMap<>()); // Empty initially
        configDoc.put("description", "System default master list configuration");
        configDoc.put("version", 1);
        configDoc.put("isActive", true);
        configDoc.put("createdAt", LocalDateTime.now());
        configDoc.put("updatedAt", LocalDateTime.now());
        configDoc.put("_class", "com.jio.digigov.notification.entity.TenantMasterListConfig");

        // Insert the configuration
        try {
            mongoTemplate.insert(configDoc, "master_list_config");
            log.info("✓ Created master_list_config with {} labels", masterListConfig.size());

            // Verify
            long count = mongoTemplate.count(new Query(), "master_list_config");
            log.info("Verification: {}.master_list_config now contains {} document(s)", dbName, count);

            return masterListConfig.size();
        } catch (Exception e) {
            log.error("✗ Failed to create master_list_config: {}", e.getMessage());
            return 0;
        }
    }
}
