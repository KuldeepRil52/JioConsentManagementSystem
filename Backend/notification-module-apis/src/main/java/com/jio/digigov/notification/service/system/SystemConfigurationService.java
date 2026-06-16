package com.jio.digigov.notification.service.system;

import com.jio.digigov.notification.entity.EmailTemplate;
import com.jio.digigov.notification.entity.NotificationConfig;
import com.jio.digigov.notification.entity.SMSTemplate;
import com.jio.digigov.notification.entity.event.EventConfiguration;
import com.jio.digigov.notification.util.MongoTemplateProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * Service for managing system-wide notification configurations.
 *
 * Provides CRUD operations for system notification configurations,
 * templates, and event configurations stored in shared database.
 *
 * @author Notification Service Team
 * @since 2025-01-21
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SystemConfigurationService {

    private final MongoTemplateProvider mongoTemplateProvider;

    @Value("${system.notification.tenant-id:SYSTEM}")
    private String systemTenantId;

    @Value("${system.notification.business-id:SYSTEM}")
    private String systemBusinessId;

    /**
     * Gets the system notification configuration.
     *
     * @return Optional containing the configuration if exists
     */
    public Optional<NotificationConfig> getConfiguration() {
        try {
            MongoTemplate sharedTemplate = mongoTemplateProvider.getTemplate(systemTenantId);

            Query query = Query.query(Criteria.where("businessId").is(systemBusinessId));
            NotificationConfig config = sharedTemplate.findOne(query, NotificationConfig.class);

            return Optional.ofNullable(config);

        } catch (Exception e) {
            log.error("Failed to get system notification configuration");
            return Optional.empty();
        }
    }

    /**
     * Gets all system event configurations.
     *
     * @return List of event configurations
     */
    public List<EventConfiguration> getAllEventConfigurations() {
        try {
            MongoTemplate sharedTemplate = mongoTemplateProvider.getTemplate(systemTenantId);

            Query query = Query.query(
                    Criteria.where("businessId").is(systemBusinessId)
                            .and("isActive").is(true)
            );

            return sharedTemplate.find(query, EventConfiguration.class);

        } catch (Exception e) {
            log.error("Failed to get system event configurations");
            return List.of();
        }
    }

    /**
     * Gets a specific event configuration by event type.
     *
     * @param eventType The event type
     * @return Optional containing the event configuration if exists
     */
    public Optional<EventConfiguration> getEventConfiguration(String eventType) {
        try {
            MongoTemplate sharedTemplate = mongoTemplateProvider.getTemplate(systemTenantId);

            Query query = Query.query(
                    Criteria.where("businessId").is(systemBusinessId)
                            .and("eventType").is(eventType)
                            .and("isActive").is(true)
            );

            EventConfiguration config = sharedTemplate.findOne(query, EventConfiguration.class);
            return Optional.ofNullable(config);

        } catch (Exception e) {
            log.error("Failed to get event configuration for eventType: {}", eventType);
            return Optional.empty();
        }
    }

    /**
     * Gets all system email templates.
     *
     * @return List of email templates
     */
    public List<EmailTemplate> getAllEmailTemplates() {
        try {
            MongoTemplate sharedTemplate = mongoTemplateProvider.getTemplate(systemTenantId);

            Query query = Query.query(Criteria.where("businessId").is(systemBusinessId));
            return sharedTemplate.find(query, EmailTemplate.class);

        } catch (Exception e) {
            log.error("Failed to get system email templates");
            return List.of();
        }
    }

    /**
     * Gets all system SMS templates.
     *
     * @return List of SMS templates
     */
    public List<SMSTemplate> getAllSmsTemplates() {
        try {
            MongoTemplate sharedTemplate = mongoTemplateProvider.getTemplate(systemTenantId);

            Query query = Query.query(Criteria.where("businessId").is(systemBusinessId));
            return sharedTemplate.find(query, SMSTemplate.class);

        } catch (Exception e) {
            log.error("Failed to get system SMS templates");
            return List.of();
        }
    }

    /**
     * Gets email template by event type.
     *
     * @param eventType The event type
     * @return Optional containing the email template if exists
     */
    public Optional<EmailTemplate> getEmailTemplateByEventType(String eventType) {
        try {
            MongoTemplate sharedTemplate = mongoTemplateProvider.getTemplate(systemTenantId);

            Query query = Query.query(
                    Criteria.where("businessId").is(systemBusinessId)
                            .and("eventType").is(eventType)
            );

            EmailTemplate template = sharedTemplate.findOne(query, EmailTemplate.class);
            return Optional.ofNullable(template);

        } catch (Exception e) {
            log.error("Failed to get email template for eventType: {}", eventType);
            return Optional.empty();
        }
    }

    /**
     * Gets SMS template by event type.
     *
     * @param eventType The event type
     * @return Optional containing the SMS template if exists
     */
    public Optional<SMSTemplate> getSmsTemplateByEventType(String eventType) {
        try {
            MongoTemplate sharedTemplate = mongoTemplateProvider.getTemplate(systemTenantId);

            Query query = Query.query(
                    Criteria.where("businessId").is(systemBusinessId)
                            .and("eventType").is(eventType)
            );

            SMSTemplate template = sharedTemplate.findOne(query, SMSTemplate.class);
            return Optional.ofNullable(template);

        } catch (Exception e) {
            log.error("Failed to get SMS template for eventType: {}", eventType);
            return Optional.empty();
        }
    }

    /**
     * Checks if system notification configuration exists.
     *
     * @return true if configuration exists
     */
    public boolean configurationExists() {
        return getConfiguration().isPresent();
    }

    /**
     * Gets count of system templates.
     *
     * @return Array with [emailCount, smsCount]
     */
    public long[] getTemplateCounts() {
        try {
            MongoTemplate sharedTemplate = mongoTemplateProvider.getTemplate(systemTenantId);

            Query query = Query.query(Criteria.where("businessId").is(systemBusinessId));

            long emailCount = sharedTemplate.count(query, EmailTemplate.class);
            long smsCount = sharedTemplate.count(query, SMSTemplate.class);

            return new long[]{emailCount, smsCount};

        } catch (Exception e) {
            log.error("Failed to get template counts");
            return new long[]{0, 0};
        }
    }
}
