package com.jio.digigov.notification.enums;

/**
 * Enumeration of standardized error codes for the DPDP Notification Module.
 *
 * This enum provides a centralized definition of all error codes used throughout
 * the application, ensuring consistency and enabling proper error tracking and
 * monitoring in production environments.
 *
 * Error Code Categories:
 * - Template Configuration Errors: INVALID_*, *_TEMPLATE_NOT_FOUND
 * - Configuration Fetch Errors: *_CONFIG_FETCH_ERROR
 * - Business Logic Errors: BUSINESS_VALIDATION_*
 * - System Errors: SYSTEM_*, INTERNAL_*
 *
 * @author Notification Service Team
 * @version 1.0
 * @since 2025-01-15
 */
public enum ErrorCode {

    // SMS Configuration Errors
    INVALID_DLT_CONFIG("SMS DLT configuration validation failed"),
    SMS_TEMPLATE_NOT_FOUND("SMS template configuration not found"),
    SMS_CONFIG_FETCH_ERROR("Error fetching SMS template configuration"),

    // Email Configuration Errors
    INVALID_EMAIL_TEMPLATE("Email template configuration validation failed"),
    EMAIL_TEMPLATE_NOT_FOUND("Email template configuration not found"),
    EMAIL_CONFIG_FETCH_ERROR("Error fetching Email template configuration"),

    // Callback Configuration Errors
    CALLBACK_TEMPLATE_NOT_FOUND("Callback template configuration not found"),
    CALLBACK_CONFIG_FETCH_ERROR("Error fetching Callback template configuration"),
    CALLBACK_URL_RESOLUTION_ERROR("Error resolving callback URL"),

    // Template Processing Errors
    TEMPLATE_RESOLUTION_ERROR("Template resolution failed"),
    TEMPLATE_VALIDATION_ERROR("Template validation failed"),
    TEMPLATE_ARGUMENT_ERROR("Template argument processing error"),

    // Business Logic Errors
    BUSINESS_VALIDATION_FAILED("Business validation failed"),
    TENANT_CONTEXT_MISSING("Tenant context not found"),
    CONFIGURATION_MISSING("Required configuration missing"),

    // System Errors
    SYSTEM_ERROR("System error occurred"),
    INTERNAL_PROCESSING_ERROR("Internal processing error"),
    DATABASE_ERROR("Database operation error"),
    EXTERNAL_SERVICE_ERROR("External service call failed"),

    // Compliance Errors
    TRAI_COMPLIANCE_ERROR("TRAI compliance validation failed"),
    DLT_VALIDATION_ERROR("DLT validation failed"),
    REGULATORY_COMPLIANCE_ERROR("Regulatory compliance check failed"),

    // Authentication & Authorization Errors
    TOKEN_GENERATION_ERROR("Token generation failed"),
    AUTHENTICATION_ERROR("Authentication failed"),
    AUTHORIZATION_ERROR("Authorization failed"),

    // Kafka & Messaging Errors
    KAFKA_PROCESSING_ERROR("Kafka message processing error"),
    MESSAGE_SERIALIZATION_ERROR("Message serialization error"),
    MESSAGE_DESERIALIZATION_ERROR("Message deserialization error"),

    // Retry & Recovery Errors
    RETRY_EXHAUSTED("Maximum retry attempts exhausted"),
    RECOVERY_FAILED("Error recovery failed"),
    DEADLETTER_PROCESSING_ERROR("Dead letter queue processing error");

    private final String description;

    ErrorCode(String description) {
        this.description = description;
    }

    /**
     * Gets the human-readable description of the error code.
     *
     * @return Error code description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Gets the error code as a string for logging and external systems.
     *
     * @return Error code name
     */
    public String getCode() {
        return this.name();
    }

    /**
     * Creates a formatted error message with the error code and description.
     *
     * @return Formatted error message
     */
    public String getFormattedMessage() {
        return String.format("[%s] %s", this.name(), this.description);
    }
}