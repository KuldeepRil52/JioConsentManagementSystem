package com.jio.digigov.notification.service.template;

import java.util.Map;

/**
 * Template service interface for Kafka consumer operations.
 * Provides template resolution and validation for SMS, Email, and Callback notifications.
 */
public interface TemplateService {

    /**
     * Resolves template content with dynamic arguments
     * @param templateId Template identifier
     * @param arguments Template arguments for placeholder substitution
     * @param tenantId Tenant identifier
     * @return Resolved template content
     */
    String resolveTemplate(String templateId, Map<String, Object> arguments, String tenantId);

    /**
     * Validates template existence for event trigger with language fallback
     * @param eventType Event type identifier
     * @param channelType Channel type (SMS, EMAIL)
     * @param tenantId Tenant identifier
     * @param businessId Business identifier
     * @param language Preferred language (with fallback to business default then English)
     * @param recipientType Recipient type (DATA_PRINCIPAL or DATA_PROTECTION_OFFICER)
     * @return Template ID if found, throws exception if not found
     */
    String validateTemplateExists(String eventType, String channelType, String tenantId,
                                String businessId, String language, String recipientType);
}