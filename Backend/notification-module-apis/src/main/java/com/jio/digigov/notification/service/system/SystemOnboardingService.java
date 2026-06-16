package com.jio.digigov.notification.service.system;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jio.digigov.notification.dto.NotificationDetails;
import com.jio.digigov.notification.dto.SmtpDetails;
import com.jio.digigov.notification.dto.onboarding.json.EventConfigDataFile;
import com.jio.digigov.notification.dto.onboarding.json.MasterLabelDataFile;
import com.jio.digigov.notification.dto.onboarding.json.TemplateDataFile;
import com.jio.digigov.notification.entity.NotificationConfig;
import com.jio.digigov.notification.entity.event.EventConfiguration;
import com.jio.digigov.notification.entity.TenantMasterListConfig;
import com.jio.digigov.notification.dto.masterlist.MasterListEntry;
import com.jio.digigov.notification.enums.EventPriority;
import com.jio.digigov.notification.enums.NotificationType;
import com.jio.digigov.notification.enums.ProviderType;
import com.jio.digigov.notification.enums.ScopeLevel;
import com.jio.digigov.notification.enums.MasterListDataSource;
import com.jio.digigov.notification.util.MongoTemplateProvider;
import com.jio.digigov.notification.util.TenantContextHolder;
import com.jio.digigov.notification.service.template.TemplateService;
import com.jio.digigov.notification.dto.request.template.CreateTemplateRequestDto;
import com.jio.digigov.notification.dto.request.template.EmailTemplateDto;
import com.jio.digigov.notification.dto.request.template.SmsTemplateDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.core.io.Resource;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Service for auto-onboarding system-wide notification configurations.
 *
 * Loads static configurations from JSON files and saves them to shared database
 * on application startup if not already present.
 */
@Service
@Slf4j
public class SystemOnboardingService {

    private final MongoTemplateProvider mongoTemplateProvider;
    private final ObjectMapper objectMapper;
    private final Environment environment;
    private final TemplateService templateService;

    /**
     * Constructor with explicit bean injection.
     */
    public SystemOnboardingService(
            MongoTemplateProvider mongoTemplateProvider,
            ObjectMapper objectMapper,
            Environment environment,
            @Qualifier("templateService") TemplateService templateService) {
        this.mongoTemplateProvider = mongoTemplateProvider;
        this.objectMapper = objectMapper;
        this.environment = environment;
        this.templateService = templateService;
    }

    @Value("${system.notification.config-file}")
    private Resource configFile;

    @Value("${system.notification.templates-file}")
    private Resource templatesFile;

    @Value("${system.notification.event-configs-file}")
    private Resource eventConfigsFile;

    @Value("${system.notification.master-labels-file}")
    private Resource masterLabelsFile;

    private static final String SYSTEM_BUSINESS_ID = "SYSTEM";
    private static final String SYSTEM_TENANT_ID = "SYSTEM";

    /**
     * DTO to hold detailed onboarding status information.
     */
    public static class OnboardingStatus {
        private final boolean configNeeded;
        private final boolean masterLabelsNeeded;
        private final List<String> missingTemplateEvents;
        private final List<String> missingEventConfigs;

        public OnboardingStatus(boolean configNeeded, boolean masterLabelsNeeded,
                                List<String> missingTemplateEvents, List<String> missingEventConfigs) {
            this.configNeeded = configNeeded;
            this.masterLabelsNeeded = masterLabelsNeeded;
            this.missingTemplateEvents = missingTemplateEvents;
            this.missingEventConfigs = missingEventConfigs;
        }

        public boolean isAnyOnboardingNeeded() {
            return configNeeded || masterLabelsNeeded ||
                   !missingTemplateEvents.isEmpty() || !missingEventConfigs.isEmpty();
        }

        public boolean isConfigNeeded() { return configNeeded; }
        public boolean isMasterLabelsNeeded() { return masterLabelsNeeded; }
        public List<String> getMissingTemplateEvents() { return missingTemplateEvents; }
        public List<String> getMissingEventConfigs() { return missingEventConfigs; }
    }

    /**
     * Checks what system notification components need to be onboarded.
     * Returns detailed status of what's missing in the shared database.
     */
    public OnboardingStatus checkOnboardingStatus() {
        try {
            MongoTemplate sharedTemplate = mongoTemplateProvider.getTemplate(SYSTEM_TENANT_ID);

            // Check if config exists
            Query configQuery = new Query(Criteria.where("businessId").is(SYSTEM_BUSINESS_ID));
            boolean configExists = sharedTemplate.exists(configQuery, NotificationConfig.class);
            log.debug("System notification config exists: {}", configExists);

            // Check if master labels exist
            boolean masterLabelsExist = sharedTemplate.exists(new Query(), TenantMasterListConfig.class);
            log.debug("Master labels config exists: {}", masterLabelsExist);

            // Check for missing templates
            List<String> missingTemplateEvents = findMissingTemplates(sharedTemplate);
            log.debug("Missing template events: {}", missingTemplateEvents.size());

            // Check for missing event configurations
            List<String> missingEventConfigs = findMissingEventConfigurations(sharedTemplate);
            log.debug("Missing event configurations: {}", missingEventConfigs.size());

            return new OnboardingStatus(!configExists, !masterLabelsExist,
                                        missingTemplateEvents, missingEventConfigs);

        } catch (Exception e) {
            log.error("Error checking onboarding status");
            // On error, assume everything needs onboarding
            return new OnboardingStatus(true, true, new ArrayList<>(), new ArrayList<>());
        }
    }

    /**
     * Finds templates that exist in templates.json but not in shared database.
     */
    private List<String> findMissingTemplates(MongoTemplate sharedTemplate) {
        List<String> missingEvents = new ArrayList<>();

        try {
            if (!templatesFile.exists()) {
                log.warn("Templates file not found: {}", templatesFile);
                return missingEvents;
            }

            // Load all templates from JSON
            List<TemplateDataFile> templates = objectMapper.readValue(
                    templatesFile.getInputStream(),
                    new TypeReference<List<TemplateDataFile>>() {}
            );

            // Check each template event
            for (TemplateDataFile templateData : templates) {
                String eventType = templateData.getEventType();

                // Check if ANY template exists for this event (EMAIL or SMS)
                Criteria templateCriteria = Criteria.where("eventType").is(eventType);
                Query templateQuery = new Query(templateCriteria);
                boolean templateExists = sharedTemplate.exists(templateQuery, "notification_templates");

                if (!templateExists) {
                    missingEvents.add(eventType);
                }
            }

        } catch (Exception e) {
            log.error("Error finding missing templates");
        }

        return missingEvents;
    }

    /**
     * Finds event configurations that exist in event-configs.json but not in shared database.
     */
    private List<String> findMissingEventConfigurations(MongoTemplate sharedTemplate) {
        List<String> missingEvents = new ArrayList<>();

        try {
            if (!eventConfigsFile.exists()) {
                log.warn("Event configs file not found: {}", eventConfigsFile);
                return missingEvents;
            }

            // Load all event configs from JSON
            List<EventConfigDataFile> eventConfigs = objectMapper.readValue(
                    eventConfigsFile.getInputStream(),
                    new TypeReference<List<EventConfigDataFile>>() {}
            );

            // Check each event config
            for (EventConfigDataFile eventConfigData : eventConfigs) {
                String eventType = eventConfigData.getEventType();

                Criteria eventCriteria = Criteria.where("eventType").is(eventType)
                        .and("businessId").is(SYSTEM_BUSINESS_ID);
                Query eventQuery = new Query(eventCriteria);
                boolean eventExists = sharedTemplate.exists(eventQuery, EventConfiguration.class);

                if (!eventExists) {
                    missingEvents.add(eventType);
                }
            }

        } catch (Exception e) {
            log.error("Error finding missing event configurations");
        }

        return missingEvents;
    }

    /**
     * Performs selective auto-onboarding of system notification components.
     * Only onboards what's missing based on the provided status.
     */
    public void autoOnboardSystemNotifications(OnboardingStatus status) {
        log.info("=== Starting System Notification Auto-Onboarding ===");

        try {
            MongoTemplate sharedTemplate = mongoTemplateProvider.getTemplate(SYSTEM_TENANT_ID);

            // DEBUG: Log the actual class and database
            log.info("DEBUG: sharedTemplate class: {}", sharedTemplate.getClass().getName());
            try {
                log.info("DEBUG: sharedTemplate database name from getDb(): {}", sharedTemplate.getDb().getName());
            } catch (Exception e) {
                log.error("DEBUG: Error getting database name: {}", e.getMessage());
            }

            int stepNum = 1;
            int totalSteps = (status.isConfigNeeded() ? 1 : 0) +
                            (status.isMasterLabelsNeeded() ? 1 : 0) +
                            (!status.getMissingTemplateEvents().isEmpty() ? 1 : 0) +
                            (!status.getMissingEventConfigs().isEmpty() ? 1 : 0);

            // Step 1: Load and save notification configuration (if needed)
            if (status.isConfigNeeded()) {
                log.info("Step {}/{}: Loading system notification configuration...", stepNum++, totalSteps);
                loadAndSaveConfiguration(sharedTemplate);
            } else {
                log.info("System notification configuration already exists, skipping");
            }

            // Step 2: Load and save master labels FIRST (before templates) (if needed)
            if (status.isMasterLabelsNeeded()) {
                log.info("Step {}/{}: Loading system master labels...", stepNum++, totalSteps);
                loadAndSaveMasterLabels(sharedTemplate);
            } else {
                log.info("Master labels already exist, skipping");
            }

            // Step 3: Load and save templates (only missing ones)
            if (!status.getMissingTemplateEvents().isEmpty()) {
                log.info("Step {}/{}: Loading {} new system templates...", stepNum++, totalSteps,
                         status.getMissingTemplateEvents().size());
                log.info("Missing template events: {}", status.getMissingTemplateEvents());
                loadAndSaveTemplates(sharedTemplate, status.getMissingTemplateEvents());
            } else {
                log.info("All templates already exist, skipping");
            }

            // Step 4: Load and save event configurations (only missing ones)
            if (!status.getMissingEventConfigs().isEmpty()) {
                log.info("Step {}/{}: Loading {} new event configurations...", stepNum++, totalSteps,
                         status.getMissingEventConfigs().size());
                log.info("Missing event configs: {}", status.getMissingEventConfigs());
                loadAndSaveEventConfigurations(sharedTemplate, status.getMissingEventConfigs());
            } else {
                log.info("All event configurations already exist, skipping");
            }

            log.info("=== System Notification Auto-Onboarding Completed Successfully ===");

        } catch (Exception e) {
            log.error("Failed to auto-onboard system notifications");
            throw new RuntimeException("System notification onboarding failed: " + e.getMessage(), e);
        }
    }

    /**
     * Loads notification configuration from JSON and saves to shared DB.
     * Supports both legacy format (single provider) and new format (multiple providers with activeProvider key).
     */
    private void loadAndSaveConfiguration(MongoTemplate sharedTemplate) throws IOException {
        if (!configFile.exists()) {
            log.warn("System notification config file not found: {}", configFile);
            return;
        }

        String configJson = new String(configFile.getInputStream().readAllBytes());
        configJson = resolvePlaceholders(configJson);

        Map<String, Object> configMap = objectMapper.readValue(configJson, new TypeReference<>() {});

        NotificationConfig config;

        // Check if this is the new format with multiple providers
        if (configMap.containsKey("activeProvider") && configMap.containsKey("configurations")) {
            // New format: multiple providers with activeProvider selector
            String activeProvider = (String) configMap.get("activeProvider");
            log.info("Loading configuration with active provider: {}", activeProvider);

            Map<String, Object> configurations = (Map<String, Object>) configMap.get("configurations");

            if (!configurations.containsKey(activeProvider)) {
                throw new RuntimeException("Active provider '" + activeProvider + "' not found in configurations");
            }

            Map<String, Object> providerConfig = (Map<String, Object>) configurations.get(activeProvider);
            ProviderType providerType = ProviderType.valueOf(activeProvider);

            config = buildNotificationConfig(providerConfig, configMap, providerType);
            log.info("Loaded {} provider configuration from multi-provider config file", activeProvider);

        } else {
            // Legacy format: single provider (backward compatibility)
            log.info("Loading configuration in legacy format (single provider)");

            ProviderType providerType = ProviderType.valueOf((String) configMap.get("providerType"));
            config = buildNotificationConfig(configMap, configMap, providerType);
            log.info("Loaded configuration in legacy format");
        }

        log.info("About to save config: configId={}, businessId={}, scopeLevel={}, providerType={}, collection=notification_configurations",
                config.getConfigId(), config.getBusinessId(), config.getScopeLevel(), config.getProviderType());

        NotificationConfig savedConfig = sharedTemplate.save(config);
        log.info("Saved system notification config: configId={}, businessId={}, providerType={}, _id={}",
                savedConfig.getConfigId(), savedConfig.getBusinessId(), savedConfig.getProviderType(), savedConfig.getId());

        // Verify the config was saved by querying it back immediately
        Query verifyQuery = new Query(Criteria.where("businessId").is(SYSTEM_BUSINESS_ID));
        NotificationConfig verifyConfig = sharedTemplate.findOne(verifyQuery, NotificationConfig.class);

        if (verifyConfig != null) {
            log.info("✓ VERIFICATION SUCCESS: Config found immediately after save with configId={}, providerType={}",
                    verifyConfig.getConfigId(), verifyConfig.getProviderType());
        } else {
            log.error("✗ VERIFICATION FAILED: Config NOT found immediately after save!");
            throw new RuntimeException("Failed to verify saved configuration");
        }
    }

    /**
     * Builds NotificationConfig with separated configurationJson/smtpDetails based on provider type.
     */
    private NotificationConfig buildNotificationConfig(Map<String, Object> providerConfig,
                                                       Map<String, Object> scopeConfig,
                                                       ProviderType providerType) {
        NotificationConfig.NotificationConfigBuilder builder = NotificationConfig.builder()
                .configId((String) providerConfig.get("configId"))
                .businessId(SYSTEM_BUSINESS_ID)
                .scopeLevel((String) scopeConfig.get("scopeLevel"))
                .providerType(providerType);

        if (providerType == ProviderType.DIGIGOV) {
            // Build DigiGov configuration - load into configurationJson
            NotificationDetails details = objectMapper.convertValue(
                    providerConfig.get("configurationJson"), NotificationDetails.class);

            builder.configurationJson(details);
            builder.smtpDetails(null); // No SMTP for DigiGov
            log.info("Built SYSTEM DigiGov configuration");

        } else if (providerType == ProviderType.SMTP) {
            // Build SMTP configuration - use smtpDetails from JSON
            Map<String, Object> smtpData = (Map<String, Object>) providerConfig.get("smtpDetails");

            SmtpDetails smtpDetails = SmtpDetails.builder()
                    .serverAddress((String) smtpData.get("serverAddress"))
                    .port(parseInteger(smtpData.get("port"), 587))
                    .fromEmail((String) smtpData.get("fromEmail"))
                    .username((String) smtpData.get("username"))
                    .password((String) smtpData.get("password"))
                    .tlsSsl((String) smtpData.getOrDefault("tlsSsl", "TLS"))
                    .senderDisplayName((String) smtpData.get("senderDisplayName"))
                    .smtpAuthEnabled(parseBoolean(smtpData.get("smtpAuthEnabled"), true))
                    .connectionTimeout(parseInteger(smtpData.get("smtpConnectionTimeout"), 5000))
                    .smtpConnectionTimeout(parseInteger(smtpData.get("smtpConnectionTimeout"), 5000))
                    .smtpSocketTimeout(parseInteger(smtpData.get("smtpSocketTimeout"), 5000))
                    .replyTo((String) smtpData.get("replyTo"))
                    .testEmail((String) smtpData.get("testEmail"))
                    .build();

            builder.smtpDetails(smtpDetails);
            builder.configurationJson(null); // No DigiGov for SMTP
            log.info("Built SYSTEM SMTP configuration");

        } else {
            throw new IllegalArgumentException("Unknown provider type: " + providerType);
        }

        return builder.build();
    }

    /**
     * Determines TLS/SSL type from old smtpUseTls/smtpUseSsl flags.
     */
    private String determineTlsSslType(Map<String, Object> smtpData) {
        Boolean useTls = (Boolean) smtpData.get("smtpUseTls");
        Boolean useSsl = (Boolean) smtpData.get("smtpUseSsl");

        if (useTls != null && useTls) {
            return "TLS";
        } else if (useSsl != null && useSsl) {
            return "SSL";
        }
        return "NONE";
    }

    /**
     * Parse boolean value from Object (handles String and Boolean types)
     */
    private Boolean parseBoolean(Object value, Boolean defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        if (value instanceof String) {
            return Boolean.parseBoolean((String) value);
        }
        return defaultValue;
    }

    /**
     * Parse integer value from Object (handles String and Integer types)
     */
    private Integer parseInteger(Object value, Integer defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Integer) {
            return (Integer) value;
        }
        if (value instanceof String) {
            try {
                return Integer.parseInt((String) value);
            } catch (NumberFormatException e) {
                return defaultValue;
            }
        }
        return defaultValue;
    }

    /**
     * Loads templates from JSON and creates them using TemplateService.
     *
     * This ensures proper provider-specific flow:
     * - For SMTP: Validates and saves directly
     * - For DigiGov: Registers with DigiGov API, gets templateId, then saves
     *
     * @param sharedTemplate MongoDB template for shared database
     * @param eventsToOnboard List of event types to onboard (null or empty = onboard all)
     */
    private void loadAndSaveTemplates(MongoTemplate sharedTemplate, List<String> eventsToOnboard) throws IOException {
        if (!templatesFile.exists()) {
            log.warn("System templates file not found: {}", templatesFile);
            return;
        }

        // Get notification config to determine provider type
        log.info("Querying for config with businessId={} using sharedTemplate", SYSTEM_BUSINESS_ID);
        Query query = Query.query(Criteria.where("businessId").is(SYSTEM_BUSINESS_ID));
        NotificationConfig config = sharedTemplate.findOne(query, NotificationConfig.class);

        if (config == null) {
            log.error("✗ Config query returned null! businessId={}", SYSTEM_BUSINESS_ID);
            throw new RuntimeException("System notification config must be created before templates");
        }

        log.info("✓ Config found via sharedTemplate: configId={}, providerType={}",
                 config.getConfigId(), config.getProviderType());

        ProviderType providerType = config.getProviderType();
        log.info("Creating system templates with provider: {}", providerType);

        // Load master list config for label validation
        TenantMasterListConfig masterListConfig = sharedTemplate.findOne(new Query(), TenantMasterListConfig.class);
        if (masterListConfig == null) {
            log.warn("Master list configuration not found. Template label validation will be skipped.");
        }

        List<TemplateDataFile> templates = objectMapper.readValue(
                templatesFile.getInputStream(),
                new TypeReference<List<TemplateDataFile>>() {}
        );

        int emailCount = 0;
        int smsCount = 0;
        int failed = 0;

        // Set tenant context for TemplateService
        TenantContextHolder.setTenantId(SYSTEM_TENANT_ID);
        TenantContextHolder.setBusinessId(SYSTEM_BUSINESS_ID);

        for (TemplateDataFile templateData : templates) {
            String eventType = templateData.getEventType();

            // Skip if this event is not in the list of events to onboard
            if (eventsToOnboard != null && !eventsToOnboard.isEmpty() && !eventsToOnboard.contains(eventType)) {
                log.debug("Skipping template for event {} (not in onboarding list)", eventType);
                continue;
            }

            // Validate that all master labels referenced in template exist
            if (masterListConfig != null) {
                validateMasterLabels(eventType, templateData, masterListConfig);
            }

            // Create Email Template using TemplateService
            if (templateData.getEmail() != null) {
                try {
                    // Check if email template already exists in shared database
                    // Use FULL unique constraint: businessId + eventType + language + type + channelType + recipientType + providerType
                    Criteria emailCriteria = Criteria.where("businessId").is(SYSTEM_BUSINESS_ID)
                            .and("eventType").is(eventType)
                            .and("language").is("english")
                            .and("type").is(NotificationType.NOTIFICATION.name())
                            .and("channelType").is("EMAIL")
                            .and("recipientType").is(templateData.getRecipientType())
                            .and("providerType").is(providerType.name());
                    Query emailQuery = new Query(emailCriteria);
                    boolean emailExists = sharedTemplate.exists(emailQuery, "notification_templates");

                    if (emailExists) {
                        log.info("Email template for event {} already exists in shared database (businessId={}, recipientType={}, providerType={}), skipping creation",
                                eventType, SYSTEM_BUSINESS_ID, templateData.getRecipientType(), providerType);
                        emailCount++;
                        continue;
                    }

                    // Resolve placeholders in email fields
                    String emailBody = resolvePlaceholders(templateData.getEmail().getCompleteHtmlContent());
                    String fromName = resolvePlaceholders(templateData.getEmail().getFromName());
                    String from = resolvePlaceholders(templateData.getEmail().getFrom());
                    String replyTo = resolvePlaceholders(templateData.getEmail().getReplyTo());

                    EmailTemplateDto emailDto = new EmailTemplateDto();
                    emailDto.setTemplateSubject(templateData.getEmail().getSubject());
                    emailDto.setTemplateBody(emailBody);
                    emailDto.setTemplateFromName(fromName);
                    emailDto.setEmailType("HTML");
                    emailDto.setTo(List.of("system@example.com")); // Placeholder
                    emailDto.setCc(List.of("admin@dpdp.system"));
                    emailDto.setFrom(from);
                    emailDto.setReplyTo(replyTo);
                    emailDto.setTemplateDetails(
                            (eventType + " email template").substring(0, Math.min(50, (eventType + " email template").length()))
                    );
                    emailDto.setArgumentsSubjectMap(templateData.getEmail().getSubjectArguments());
                    emailDto.setArgumentsBodyMap(templateData.getEmail().getBodyArguments());

                    CreateTemplateRequestDto emailRequest = new CreateTemplateRequestDto();
                    emailRequest.setEventType(eventType);
                    emailRequest.setEmailTemplate(emailDto);
                    emailRequest.setLanguage("english");
                    emailRequest.setProviderType(providerType);
                    emailRequest.setRecipientType(templateData.getRecipientType());

                    String transactionId = "SYS-ONBOARD-EMAIL-" + eventType;

                    templateService.createTemplate(
                            emailRequest,
                            SYSTEM_TENANT_ID,
                            SYSTEM_BUSINESS_ID,
                            ScopeLevel.BUSINESS,
                            NotificationType.NOTIFICATION,
                            transactionId,
                            null  // No HTTP context for system onboarding
                    );

                    log.info("Created email template for event: {} (provider: {})", eventType, providerType);
                    emailCount++;

                    // Update event-to-label mappings
                    updateEventToLabelMappings(eventType, templateData, sharedTemplate);

                } catch (Exception e) {
                    String errorMsg = e.getMessage();
                    // Handle known DigiGov errors gracefully
                    if (errorMsg != null && (errorMsg.contains("JNG147") || errorMsg.contains("Email Cli is not whitelisted"))) {
                        log.warn("Email template for event {} skipped: Email address not whitelisted in DigiGov. This needs to be configured separately.", eventType);
                    } else {
                        log.error("Failed to create email template for event {}: {}", eventType, errorMsg);
                        failed++;
                    }
                }
            }

            // Create SMS Template using TemplateService (only for DigiGov)
            if (templateData.getSms() != null && providerType == ProviderType.DIGIGOV) {
                try {
                    // Check if SMS template already exists in shared database
                    // Use FULL unique constraint: businessId + eventType + language + type + channelType + recipientType + providerType
                    Criteria smsCriteria = Criteria.where("businessId").is(SYSTEM_BUSINESS_ID)
                            .and("eventType").is(eventType)
                            .and("language").is("english")
                            .and("type").is(NotificationType.NOTIFICATION.name())
                            .and("channelType").is("SMS")
                            .and("recipientType").is(templateData.getRecipientType())
                            .and("providerType").is(providerType.name());
                    Query smsQuery = new Query(smsCriteria);
                    boolean smsExists = sharedTemplate.exists(smsQuery, "notification_templates");

                    if (smsExists) {
                        log.info("SMS template for event {} already exists in shared database (businessId={}, recipientType={}, providerType={}), skipping creation",
                                eventType, SYSTEM_BUSINESS_ID, templateData.getRecipientType(), providerType);
                        smsCount++;
                        continue;
                    }

                    // Resolve placeholders in SMS fields
                    String smsTemplate = resolvePlaceholders(templateData.getSms().getTemplate());
                    String dltEntityId = resolvePlaceholders(templateData.getSms().getDltEntityId());
                    String dltTemplateId = resolvePlaceholders(templateData.getSms().getDltTemplateId());
                    String from = templateData.getSms().getFrom() != null ?
                                  resolvePlaceholders(templateData.getSms().getFrom()) : "SYSTEM";

                    SmsTemplateDto smsDto = new SmsTemplateDto();
                    smsDto.setTemplate(smsTemplate);
                    smsDto.setDltEntityId(dltEntityId);
                    smsDto.setDltTemplateId(dltTemplateId);
                    smsDto.setFrom(from);
                    smsDto.setWhiteListedNumber(List.of("9876543210")); // Required by DigiGov for testing
                    smsDto.setOprCountries(List.of("IN")); // India
                    smsDto.setTemplateDetails(
                            (eventType + " SMS template").substring(0, Math.min(50, (eventType + " SMS template").length()))
                    );
                    smsDto.setArgumentsMap(templateData.getSms().getArguments());

                    CreateTemplateRequestDto smsRequest = new CreateTemplateRequestDto();
                    smsRequest.setEventType(eventType);
                    smsRequest.setSmsTemplate(smsDto);
                    smsRequest.setLanguage("english");
                    smsRequest.setProviderType(providerType);
                    smsRequest.setRecipientType(templateData.getRecipientType());

                    String transactionId = "SYS-ONBOARD-SMS-" + eventType;

                    templateService.createTemplate(
                            smsRequest,
                            SYSTEM_TENANT_ID,
                            SYSTEM_BUSINESS_ID,
                            ScopeLevel.BUSINESS,
                            NotificationType.NOTIFICATION,
                            transactionId,
                            null  // No HTTP context for system onboarding
                    );

                    log.info("Created SMS template for event: {} (provider: {})", eventType, providerType);
                    smsCount++;

                    // Update event-to-label mappings
                    updateEventToLabelMappings(eventType, templateData, sharedTemplate);

                } catch (Exception e) {
                    String errorMsg = e.getMessage();
                    // Handle known DigiGov errors gracefully
                    if (errorMsg != null && (errorMsg.contains("JNG307") || errorMsg.contains("Dlt Template ID is already present"))) {
                        log.warn("SMS template for event {} skipped: DLT Template ID already registered in DigiGov. Update the DLT IDs in templates.json if you want to create new templates.", eventType);
                    } else {
                        log.error("Failed to create SMS template for event {}: {}", eventType, errorMsg);
                        failed++;
                    }
                }
            } else if (templateData.getSms() != null && providerType == ProviderType.SMTP) {
                log.warn("Skipping SMS template for event {} (SMTP provider does not support SMS)", eventType);
            }
        }

        log.info("Template creation completed: {} email, {} SMS created, {} failed (provider: {})",
                 emailCount, smsCount, failed, providerType);

        if (failed > 0) {
            log.warn("{} templates failed to create. Application will continue, but some system notifications may not work.", failed);
        }
    }

    /**
     * Loads event configurations from JSON and saves to shared DB.
     *
     * @param sharedTemplate MongoDB template for shared database
     * @param eventsToOnboard List of event types to onboard (null or empty = onboard all)
     */
    private void loadAndSaveEventConfigurations(MongoTemplate sharedTemplate, List<String> eventsToOnboard) throws IOException {
        if (!eventConfigsFile.exists()) {
            log.warn("System event configs file not found: {}", eventConfigsFile);
            return;
        }

        List<EventConfigDataFile> eventConfigs = objectMapper.readValue(
                eventConfigsFile.getInputStream(),
                new TypeReference<List<EventConfigDataFile>>() {}
        );

        int created = 0;
        int updated = 0;
        for (EventConfigDataFile eventConfigData : eventConfigs) {
            String eventType = eventConfigData.getEventType();

            // Skip if this event is not in the list of events to onboard
            if (eventsToOnboard != null && !eventsToOnboard.isEmpty() && !eventsToOnboard.contains(eventType)) {
                log.debug("Skipping event config for {} (not in onboarding list)", eventType);
                continue;
            }

            // Check if event configuration already exists
            Criteria eventCriteria = Criteria.where("eventType").is(eventType)
                    .and("businessId").is(SYSTEM_BUSINESS_ID);
            Query eventQuery = new Query(eventCriteria);
            EventConfiguration existingConfig = sharedTemplate.findOne(eventQuery, EventConfiguration.class);

            // Build notifications config
            EventConfiguration.NotificationsConfig.NotificationsConfigBuilder notifConfigBuilder =
                    EventConfiguration.NotificationsConfig.builder();

            // Data Principal settings
            if (eventConfigData.getNotifyDataPrincipal() != null && eventConfigData.getNotifyDataPrincipal()) {
                notifConfigBuilder.dataPrincipal(EventConfiguration.RecipientSettings.builder()
                        .enabled(true)
                        .channels(Arrays.asList("SMS", "EMAIL"))
                        .method("DIRECT")
                        .build());
            }

            // Data Fiduciary settings
            if (eventConfigData.getNotifyDataFiduciary() != null && eventConfigData.getNotifyDataFiduciary()) {
                notifConfigBuilder.dataFiduciary(EventConfiguration.RecipientSettings.builder()
                        .enabled(true)
                        .channels(Collections.singletonList("EMAIL"))
                        .method("CALLBACK")
                        .build());
            }

            // Data Processor settings
            if (eventConfigData.getNotifyDataProcessor() != null && eventConfigData.getNotifyDataProcessor()) {
                notifConfigBuilder.dataProcessor(EventConfiguration.RecipientSettings.builder()
                        .enabled(true)
                        .channels(Collections.singletonList("EMAIL"))
                        .method("CALLBACK")
                        .build());
            }

            // DPO settings
            if (eventConfigData.getNotifyDpo() != null && eventConfigData.getNotifyDpo()) {
                notifConfigBuilder.dataProtectionOfficer(EventConfiguration.RecipientSettings.builder()
                        .enabled(true)
                        .channels(Collections.singletonList("EMAIL"))
                        .method("DIRECT")
                        .build());
            }

            if (existingConfig != null) {
                // Update existing event configuration with new values from JSON
                existingConfig.setNotifications(notifConfigBuilder.build());
                existingConfig.setPriority(EventPriority.valueOf(eventConfigData.getPriority()));
                existingConfig.setIsActive(true);

                sharedTemplate.save(existingConfig);
                log.info("Updated event configuration for {} with new values from event-configs.json", eventType);
                updated++;
            } else {
                // Create new event configuration
                EventConfiguration eventConfig = EventConfiguration.builder()
                        .configId("EC_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 8))
                        .businessId(SYSTEM_BUSINESS_ID)
                        .scopeLevel(ScopeLevel.TENANT)
                        .eventType(eventConfigData.getEventType())
                        .notifications(notifConfigBuilder.build())
                        .priority(EventPriority.valueOf(eventConfigData.getPriority()))
                        .isActive(true)
                        .build();

                sharedTemplate.save(eventConfig);
                log.info("Created new event configuration for {}", eventType);
                created++;
            }
        }

        log.info("Event configurations processed: {} created, {} updated", created, updated);
    }

    /**
     * Loads master labels from JSON and merges with existing labels in shared DB.
     * - If labelName doesn't exist: adds it
     * - If labelName exists but path/dataSource/defaultValue changed: updates it
     * - If no master list config exists: creates new one
     */
    private void loadAndSaveMasterLabels(MongoTemplate sharedTemplate) throws IOException {
        if (!masterLabelsFile.exists()) {
            log.warn("System master labels file not found: {}", masterLabelsFile);
            return;
        }

        List<MasterLabelDataFile> masterLabels = objectMapper.readValue(
                masterLabelsFile.getInputStream(),
                new TypeReference<List<MasterLabelDataFile>>() {}
        );

        // Try to load existing master list config
        TenantMasterListConfig masterListConfig = sharedTemplate.findOne(new Query(), TenantMasterListConfig.class);

        Map<String, MasterListEntry> masterListConfigMap;
        int added = 0;
        int updated = 0;
        int unchanged = 0;

        if (masterListConfig != null) {
            // Merge with existing config
            log.info("Master list configuration exists, merging with new labels from master-labels.json");
            masterListConfigMap = masterListConfig.getMasterListConfig();
            if (masterListConfigMap == null) {
                masterListConfigMap = new HashMap<>();
            }
        } else {
            // Create new config
            log.info("No master list configuration found, creating new one");
            masterListConfigMap = new HashMap<>();
            masterListConfig = new TenantMasterListConfig();
            masterListConfig.setDescription("System-wide master list configuration");
            masterListConfig.setVersion(1);
            masterListConfig.setIsActive(true);
        }

        // Process each label from JSON
        for (MasterLabelDataFile labelData : masterLabels) {
            MasterListEntry.MasterListEntryBuilder entryBuilder = MasterListEntry.builder()
                    .dataSource(MasterListDataSource.valueOf(labelData.getDataSource()))
                    .path(labelData.getPath())
                    .defaultValue(labelData.getDefaultValue());

            // Add DB-specific fields
            if ("DB".equals(labelData.getDataSource())) {
                entryBuilder.collection(labelData.getCollection());
                if (labelData.getQuery() != null) {
                    Map<String, String> queryMap = new HashMap<>();
                    labelData.getQuery().forEach((k, v) -> queryMap.put(k, v != null ? v.toString() : null));
                    entryBuilder.query(queryMap);
                }
            }

            // Add GENERATE-specific fields
            if ("GENERATE".equals(labelData.getDataSource())) {
                entryBuilder.generator(labelData.getGenerator());
                entryBuilder.config(labelData.getConfig());
            }

            MasterListEntry newEntry = entryBuilder.build();
            String labelName = labelData.getLabelName();

            // Check if label exists and if it needs updating
            MasterListEntry existingEntry = masterListConfigMap.get(labelName);

            if (existingEntry == null) {
                // New label - add it
                masterListConfigMap.put(labelName, newEntry);
                log.debug("Added new master label: {}", labelName);
                added++;
            } else {
                // Label exists - check if it changed
                boolean changed = !existingEntry.getDataSource().equals(newEntry.getDataSource()) ||
                                !Objects.equals(existingEntry.getPath(), newEntry.getPath()) ||
                                !Objects.equals(existingEntry.getDefaultValue(), newEntry.getDefaultValue()) ||
                                !Objects.equals(existingEntry.getCollection(), newEntry.getCollection()) ||
                                !Objects.equals(existingEntry.getGenerator(), newEntry.getGenerator());

                if (changed) {
                    // Update with new values
                    masterListConfigMap.put(labelName, newEntry);
                    log.debug("Updated master label: {} (dataSource or path or defaultValue changed)", labelName);
                    updated++;
                } else {
                    log.trace("Master label {} unchanged", labelName);
                    unchanged++;
                }
            }
        }

        // Save the merged config
        masterListConfig.setMasterListConfig(masterListConfigMap);
        sharedTemplate.save(masterListConfig);

        log.info("Master labels processed: {} added, {} updated, {} unchanged (total: {} labels)",
                added, updated, unchanged, masterListConfigMap.size());
    }

    /**
     * Updates event-to-label mappings in TenantMasterListConfig after template creation.
     * Links the eventType to all labels used in the template.
     *
     * @param eventType Event type string
     * @param templateData Template data containing label references
     * @param sharedTemplate MongoDB template for shared database
     */
    private void updateEventToLabelMappings(String eventType, TemplateDataFile templateData,
                                           MongoTemplate sharedTemplate) {
        try {
            // Load master list config
            TenantMasterListConfig masterListConfig = sharedTemplate.findOne(new Query(), TenantMasterListConfig.class);
            if (masterListConfig == null) {
                log.warn("Cannot update event-to-label mappings: master list config not found");
                return;
            }

            // Extract labels from template
            Set<String> labels = extractLabelsFromTemplate(templateData);
            if (labels.isEmpty()) {
                log.debug("No labels found in template for event: {}", eventType);
                return;
            }

            // Convert string eventType to EventType enum
            com.jio.digigov.notification.enums.EventType eventTypeEnum;
            try {
                eventTypeEnum = com.jio.digigov.notification.enums.EventType.fromValue(eventType);
            } catch (Exception e) {
                log.warn("Cannot convert eventType '{}' to enum. Skipping event-to-label mapping update.", eventType);
                return;
            }

            // Use the entity's helper method to update mappings
            // This handles both eventToLabels and labelToEvents mappings
            masterListConfig.addEventToLabelMapping(eventTypeEnum, labels);

            // Save updated master list config
            sharedTemplate.save(masterListConfig);
            log.debug("Updated event-to-label mappings for {}: {} labels", eventType, labels.size());

        } catch (Exception e) {
            log.error("Failed to update event-to-label mappings for event {}: {}", eventType, e.getMessage());
            // Don't fail template creation if mapping update fails
        }
    }

    /**
     * Extracts label names from template argumentsMap.
     * Labels are the values in the argumentsMap (e.g., {"name": "MASTER_LABEL_USER_NAME"}).
     */
    private Set<String> extractLabelsFromTemplate(TemplateDataFile templateData) {
        Set<String> labels = new HashSet<>();

        // Extract from email template
        if (templateData.getEmail() != null) {
            if (templateData.getEmail().getSubjectArguments() != null) {
                labels.addAll(templateData.getEmail().getSubjectArguments().values());
            }
            if (templateData.getEmail().getBodyArguments() != null) {
                labels.addAll(templateData.getEmail().getBodyArguments().values());
            }
        }

        // Extract from SMS template
        if (templateData.getSms() != null) {
            if (templateData.getSms().getArguments() != null) {
                labels.addAll(templateData.getSms().getArguments().values());
            }
        }

        return labels;
    }

    /**
     * Validates that all labels referenced in the template exist in master labels config.
     * Logs warnings for missing labels but does not fail.
     *
     * @param eventType Event type for logging
     * @param templateData Template data containing label references
     * @param masterListConfig Master list configuration
     */
    private void validateMasterLabels(String eventType, TemplateDataFile templateData,
                                     TenantMasterListConfig masterListConfig) {
        if (masterListConfig == null || masterListConfig.getMasterListConfig() == null) {
            log.warn("Master list configuration not found. Cannot validate labels for event: {}", eventType);
            return;
        }

        Set<String> referencedLabels = extractLabelsFromTemplate(templateData);
        Map<String, MasterListEntry> existingLabels = masterListConfig.getMasterListConfig();

        for (String labelName : referencedLabels) {
            if (labelName == null || labelName.trim().isEmpty()) {
                continue;
            }

            if (!existingLabels.containsKey(labelName)) {
                log.warn("Template for event {} references master label '{}' which does not exist in master-labels.json. " +
                        "This template may fail at runtime.", eventType, labelName);
            }
        }
    }

    /**
     * Resolves ${VARIABLE_NAME:defaultValue} placeholders in JSON string.
     * Uses Spring Environment to resolve values.
     */
    private String resolvePlaceholders(String json) {
        Pattern pattern = Pattern.compile("\\$\\{([^:}]+)(?::([^}]*))?\\}");
        Matcher matcher = pattern.matcher(json);
        StringBuffer result = new StringBuffer();

        while (matcher.find()) {
            String varName = matcher.group(1);
            String defaultValue = matcher.group(2);

            String value = environment.getProperty(varName);
            if (value == null) {
                value = defaultValue != null ? defaultValue : "";
            }

            matcher.appendReplacement(result, Matcher.quoteReplacement(value));
        }
        matcher.appendTail(result);

        return result.toString();
    }
}
