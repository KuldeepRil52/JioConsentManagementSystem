package com.jio.digigov.notification.service.template.impl;

import com.jio.digigov.notification.entity.NotificationConfig;
import com.jio.digigov.notification.entity.template.NotificationTemplate;
import com.jio.digigov.notification.enums.NotificationChannel;
import com.jio.digigov.notification.enums.ScopeLevel;
import com.jio.digigov.notification.enums.TemplateStatus;
import com.jio.digigov.notification.exception.TemplateNotFoundException;
import com.jio.digigov.notification.service.template.TemplateService;
import com.jio.digigov.notification.util.TenantContextHolder;
import com.jio.digigov.notification.util.MongoTemplateProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Implementation of TemplateService for Kafka consumer operations.
 * Provides template resolution and validation for SMS, Email, and Callback notifications.
 */
@Slf4j
@Service("templateService")
@RequiredArgsConstructor
public class TemplateServiceImpl implements TemplateService {

    private final MongoTemplateProvider mongoTemplateProvider;

    @Override
    public String resolveTemplate(String templateId, Map<String, Object> arguments, String tenantId) {
        log.info("Resolving template ID: {} for tenant: {} with arguments: {}", templateId, tenantId, arguments);

        TenantContextHolder.setTenant(tenantId);

        try {
            MongoTemplate tenantMongoTemplate = mongoTemplateProvider.getTemplate(tenantId);

            Query templateQuery = new Query()
                    .addCriteria(Criteria.where("templateId").is(templateId));

            NotificationTemplate template = tenantMongoTemplate.findOne(templateQuery, NotificationTemplate.class);

            if (template == null) {
                log.warn("Template not found with ID: {} in tenant_db_{}", templateId, tenantId);
                throw new TemplateNotFoundException("Template not found with ID: " + templateId);
            }

            String content = getTemplateContent(template);
            if (content == null || content.trim().isEmpty()) {
                throw new TemplateNotFoundException("Template content is empty for ID: " + templateId);
            }

            String resolvedContent = resolvePlaceholders(content, arguments);

            log.info("Template resolved successfully for ID: {}", templateId);
            return resolvedContent;

        } finally {
            TenantContextHolder.clear();
        }
    }

    @Override
    public String validateTemplateExists(String eventType, String channelType, String tenantId,
                                       String businessId, String language, String recipientType) {
        log.info("Validating template existence: eventType={}, channelType={}, tenantId={}, businessId={}, language={}, recipientType={}",
                eventType, channelType, tenantId, businessId, language, recipientType);

        TenantContextHolder.setTenant(tenantId);

        String effectiveRecipientType = (recipientType == null || recipientType.trim().isEmpty())
            ? "DATA_PRINCIPAL" : recipientType;

        try {
            MongoTemplate tenantMongoTemplate = mongoTemplateProvider.getTemplate(tenantId);

            List<String> languagesToTry = buildLanguageFallbackList(language, businessId, tenantMongoTemplate);

            for (String lang : languagesToTry) {
                String templateId = findTemplateForLanguage(eventType, channelType, businessId, tenantId, lang,
                        effectiveRecipientType, tenantMongoTemplate);
                if (templateId != null) {
                    log.info("Template found: eventType={}, channelType={}, language={}, templateId={}",
                            eventType, channelType, lang, templateId);
                    return templateId;
                }
            }

            String error = String.format("No %s template found for eventType=%s, businessId=%s, tried languages=%s",
                    channelType, eventType, businessId, languagesToTry);
            log.error(error);
            throw new TemplateNotFoundException(error);

        } finally {
            TenantContextHolder.clear();
        }
    }

    private String getTemplateContent(NotificationTemplate template) {
        if (template.getSmsConfig() != null && template.getSmsConfig().getTemplate() != null) {
            return template.getSmsConfig().getTemplate();
        }
        if (template.getEmailConfig() != null && template.getEmailConfig().getTemplateBody() != null) {
            return template.getEmailConfig().getTemplateBody();
        }
        return null;
    }

    private String resolvePlaceholders(String content, Map<String, Object> arguments) {
        if (arguments == null || arguments.isEmpty()) {
            return content;
        }

        String resolvedContent = content;
        for (Map.Entry<String, Object> entry : arguments.entrySet()) {
            if (entry.getKey() != null) {
                String placeholder = "{{" + entry.getKey() + "}}";
                String value = entry.getValue() != null ? entry.getValue().toString() : "";
                resolvedContent = resolvedContent.replace(placeholder, value);
            }
        }
        return resolvedContent;
    }

    private List<String> buildLanguageFallbackList(String requestedLanguage, String businessId,
            MongoTemplate mongoTemplate) {
        List<String> languages = new ArrayList<>();

        if (requestedLanguage != null && !requestedLanguage.trim().isEmpty()) {
            languages.add(requestedLanguage.toLowerCase());
        }

        try {
            String businessDefaultLanguage = getBusinessDefaultLanguage(businessId, mongoTemplate);
            if (businessDefaultLanguage != null && !languages.contains(businessDefaultLanguage.toLowerCase())) {
                languages.add(businessDefaultLanguage.toLowerCase());
            }
        } catch (Exception e) {
            log.warn("Failed to get business default language for businessId={}: {}", businessId, e.getMessage());
        }

        if (!languages.contains("english")) {
            languages.add("english");
        }

        log.debug("Language fallback order: {}", languages);
        return languages;
    }

    private String getBusinessDefaultLanguage(String businessId, MongoTemplate mongoTemplate) {
        try {
            Query query = new Query(Criteria.where("businessId").is(businessId)
                    .and("isActive").is(true));
            NotificationConfig config = mongoTemplate.findOne(query, NotificationConfig.class);
            if (config != null && config.getConfigurationJson() != null) {
                String defaultLanguage = config.getConfigurationJson().getDefaultLanguage();
                if (defaultLanguage != null) {
                    return defaultLanguage.toLowerCase();
                }
            }
        } catch (Exception e) {
            log.warn("Failed to query business configuration for default language, businessId={}: {}",
                    businessId, e.getMessage());
        }
        return null;
    }

    private String findTemplateForLanguage(String eventType, String channelType, String businessId, String tenantId,
                                         String language, String recipientType, MongoTemplate mongoTemplate) {
        try {
            NotificationChannel channel = NotificationChannel.valueOf(channelType.toUpperCase());

            // Step 1: Try to find by businessId
            Query templateQuery = new Query()
                    .addCriteria(Criteria.where("businessId").is(businessId))
                    .addCriteria(Criteria.where("eventType").is(eventType))
                    .addCriteria(Criteria.where("language").is(language))
                    .addCriteria(Criteria.where("channelType").is(channel))
                    .addCriteria(Criteria.where("recipientType").is(recipientType))
                    .addCriteria(Criteria.where("status").is(TemplateStatus.ACTIVE));

            NotificationTemplate template = mongoTemplate.findOne(templateQuery, NotificationTemplate.class);

            if (template != null) {
                return template.getTemplateId();
            }

            // Step 2: If not found, try to find by scopeLevel = TENANT
            Query tenantLevelQuery = new Query()
                    .addCriteria(Criteria.where("scopeLevel").is(ScopeLevel.TENANT))
                    .addCriteria(Criteria.where("eventType").is(eventType))
                    .addCriteria(Criteria.where("language").is(language))
                    .addCriteria(Criteria.where("channelType").is(channel))
                    .addCriteria(Criteria.where("recipientType").is(recipientType))
                    .addCriteria(Criteria.where("status").is(TemplateStatus.ACTIVE));

            template = mongoTemplate.findOne(tenantLevelQuery, NotificationTemplate.class);

            if (template != null) {
                log.info("Using TENANT-level fallback template for eventType={}, channel={}, language={} (Step 2: scopeLevel=TENANT match)",
                        eventType, channelType, language);
                return template.getTemplateId();
            }

            // Step 3: If still not found, try to find by tenantId in businessId field
            Query tenantIdQuery = new Query()
                    .addCriteria(Criteria.where("businessId").is(tenantId))
                    .addCriteria(Criteria.where("eventType").is(eventType))
                    .addCriteria(Criteria.where("language").is(language))
                    .addCriteria(Criteria.where("channelType").is(channel))
                    .addCriteria(Criteria.where("recipientType").is(recipientType))
                    .addCriteria(Criteria.where("status").is(TemplateStatus.ACTIVE));

            template = mongoTemplate.findOne(tenantIdQuery, NotificationTemplate.class);

            if (template != null) {
                log.info("Using tenantId as businessId fallback template for eventType={}, channel={}, language={} (Step 3: tenantId as businessId match)",
                        eventType, channelType, language);
                return template.getTemplateId();
            }

            return null;

        } catch (IllegalArgumentException e) {
            log.warn("Invalid channel type: {}", channelType);
            return null;
        } catch (Exception e) {
            log.warn("Error finding template for eventType={}, channelType={}, businessId={}, tenantId={}, language={}: {}",
                    eventType, channelType, businessId, tenantId, language, e.getMessage());
            return null;
        }
    }
}
