package com.jio.digigov.notification.enums;

import java.util.Arrays;

/**
 * Enumeration of standardized error messages for the DPDP Notification Module.
 *
 * This enum provides centralized, consistent error messages that can be used
 * throughout the application. Messages are designed to be informative for both
 * developers and system administrators while maintaining security by not exposing
 * sensitive information.
 *
 * Message Categories:
 * - Template Configuration Messages
 * - Compliance and Validation Messages
 * - System and Processing Messages
 * - User-facing Messages
 *
 * @author Notification Service Team
 * @version 1.0
 * @since 2025-01-15
 */
public enum ErrorMessage {

    // SMS Configuration Messages
    INVALID_DLT_CONFIG_MSG("Invalid DLT configuration for templateId: %s. Both DLT Entity ID and Template ID must be provided for TRAI compliance."),
    SMS_TEMPLATE_NOT_FOUND_MSG("SMS template configuration not found for templateId: %s. DLT Entity ID and Template ID are mandatory for TRAI compliance."),
    SMS_CONFIG_FETCH_ERROR_MSG("Failed to fetch SMS template configuration for templateId: %s. DLT configuration is mandatory for SMS delivery compliance."),

    // Email Configuration Messages
    INVALID_EMAIL_TEMPLATE_MSG("Invalid email template configuration for templateId: %s. Both subject and body must be provided for email delivery."),
    EMAIL_TEMPLATE_NOT_FOUND_MSG("Email template configuration not found for templateId: %s. Subject and body templates are mandatory for email delivery."),
    EMAIL_CONFIG_FETCH_ERROR_MSG("Failed to fetch Email template configuration for templateId: %s. Template configuration is mandatory for email delivery."),

    // Callback Configuration Messages
    CALLBACK_TEMPLATE_NOT_FOUND_MSG("Callback template configuration not found for templateId: %s. Callback templates are mandatory for webhook delivery."),
    CALLBACK_CONFIG_FETCH_ERROR_MSG("Failed to fetch Callback template configuration for templateId: %s. Template configuration is mandatory for callback delivery."),
    CALLBACK_URL_RESOLUTION_ERROR_MSG("Failed to resolve callback URL for recipientType: %s and recipientId: %s."),

    // Template Processing Messages
    TEMPLATE_RESOLUTION_ERROR_MSG("Failed to resolve template with templateId: %s for tenantId: %s."),
    TEMPLATE_VALIDATION_ERROR_MSG("Template validation failed for templateId: %s. Template content is invalid or incomplete."),
    TEMPLATE_ARGUMENT_ERROR_MSG("Template argument processing failed for templateId: %s. Invalid or missing template arguments."),

    // Business Logic Messages
    BUSINESS_VALIDATION_FAILED_MSG("Business validation failed for operation: %s with reason: %s"),
    TENANT_CONTEXT_MISSING_MSG("Tenant context not found in current thread. Operation requires valid tenant context."),
    CONFIGURATION_MISSING_MSG("Required configuration missing: %s for operation: %s"),

    // System Messages
    SYSTEM_ERROR_MSG("System error occurred during operation: %s. Please contact system administrator."),
    INTERNAL_PROCESSING_ERROR_MSG("Internal processing error occurred for operation: %s. Request ID: %s"),
    DATABASE_ERROR_MSG("Database operation failed for collection: %s with operation: %s"),
    EXTERNAL_SERVICE_ERROR_MSG("External service call failed for service: %s with endpoint: %s"),

    // Compliance Messages
    TRAI_COMPLIANCE_ERROR_MSG("TRAI compliance validation failed. DLT Entity ID: %s, DLT Template ID: %s"),
    DLT_VALIDATION_ERROR_MSG("DLT validation failed for Entity ID: %s and Template ID: %s. Please ensure valid DLT registration."),
    REGULATORY_COMPLIANCE_ERROR_MSG("Regulatory compliance check failed for operation: %s. Compliance requirement: %s"),

    // Authentication & Authorization Messages
    TOKEN_GENERATION_ERROR_MSG("Token generation failed for businessId: %s. Please check credentials and configuration."),
    AUTHENTICATION_ERROR_MSG("Authentication failed for user: %s. Invalid credentials or expired session."),
    AUTHORIZATION_ERROR_MSG("Authorization failed for user: %s and operation: %s. Insufficient permissions."),

    // Kafka & Messaging Messages
    KAFKA_PROCESSING_ERROR_MSG("Kafka message processing error for topic: %s, partition: %s, offset: %s"),
    MESSAGE_SERIALIZATION_ERROR_MSG("Message serialization error for messageType: %s. Invalid message structure."),
    MESSAGE_DESERIALIZATION_ERROR_MSG("Message deserialization error for messageType: %s. Corrupted or invalid message format."),

    // Retry & Recovery Messages
    RETRY_EXHAUSTED_MSG("Maximum retry attempts (%d) exhausted for operation: %s. Moving to dead letter queue."),
    RECOVERY_FAILED_MSG("Error recovery failed for operation: %s after %d attempts. Manual intervention required."),
    DEADLETTER_PROCESSING_ERROR_MSG("Dead letter queue processing error for messageId: %s. Message: %s");

    private final String messageTemplate;

    ErrorMessage(String messageTemplate) {
        this.messageTemplate = messageTemplate;
    }

    /**
     * Gets the message template for formatting with parameters.
     *
     * @return Message template string
     */
    public String getTemplate() {
        return messageTemplate;
    }

    /**
     * Formats the message with the provided parameters.
     *
     * @param params Parameters to substitute in the message template
     * @return Formatted message string
     */
    public String format(Object... params) {
        try {
            return String.format(messageTemplate, params);
        } catch (Exception e) {
            // Fallback to template if formatting fails
            return messageTemplate + " [Error formatting message with params: " + Arrays.toString(params) + "]";
        }
    }

    /**
     * Gets the raw message template without formatting.
     *
     * @return Raw message template
     */
    public String getMessage() {
        return messageTemplate;
    }
}