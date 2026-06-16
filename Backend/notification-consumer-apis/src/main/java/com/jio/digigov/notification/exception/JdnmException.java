package com.jio.digigov.notification.exception;

import com.jio.digigov.notification.enums.JdnmErrorCode;
import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

/**
 * Custom exception for JDNM (Java DigiGov Notification Module) errors.
 *
 * This exception provides structured error handling with:
 * - Standardized error codes (JDNM format)
 * - HTTP status mapping
 * - Additional metadata for debugging
 * - Correlation ID tracking
 *
 * @author Notification Service Team
 * @since 1.0.0
 */
@Getter
public class JdnmException extends RuntimeException {

    private final JdnmErrorCode errorCode;
    private final Map<String, Object> metadata;
    private final String correlationId;

    /**
     * Create exception with error code only.
     *
     * @param errorCode the JDNM error code
     */
    public JdnmException(JdnmErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
        this.metadata = new HashMap<>();
        this.correlationId = null;
    }

    /**
     * Create exception with error code and custom message.
     *
     * @param errorCode the JDNM error code
     * @param message custom error message
     */
    public JdnmException(JdnmErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
        this.metadata = new HashMap<>();
        this.correlationId = null;
    }

    /**
     * Create exception with error code, message, and cause.
     *
     * @param errorCode the JDNM error code
     * @param message custom error message
     * @param cause the underlying cause
     */
    public JdnmException(JdnmErrorCode errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.metadata = new HashMap<>();
        this.correlationId = null;
    }

    /**
     * Create exception with error code, message, and metadata.
     *
     * @param errorCode the JDNM error code
     * @param message custom error message
     * @param metadata additional error metadata
     */
    public JdnmException(JdnmErrorCode errorCode, String message, Map<String, Object> metadata) {
        super(message);
        this.errorCode = errorCode;
        this.metadata = metadata != null ? new HashMap<>(metadata) : new HashMap<>();
        this.correlationId = null;
    }

    /**
     * Create exception with full parameters.
     *
     * @param errorCode the JDNM error code
     * @param message custom error message
     * @param metadata additional error metadata
     * @param correlationId correlation ID for tracing
     */
    public JdnmException(JdnmErrorCode errorCode, String message, Map<String, Object> metadata, String correlationId) {
        super(message);
        this.errorCode = errorCode;
        this.metadata = metadata != null ? new HashMap<>(metadata) : new HashMap<>();
        this.correlationId = correlationId;
    }

    /**
     * Create exception with error code, message, cause, and metadata.
     *
     * @param errorCode the JDNM error code
     * @param message custom error message
     * @param cause the underlying cause
     * @param metadata additional error metadata
     */
    public JdnmException(JdnmErrorCode errorCode, String message, Throwable cause, Map<String, Object> metadata) {
        super(message, cause);
        this.errorCode = errorCode;
        this.metadata = metadata != null ? new HashMap<>(metadata) : new HashMap<>();
        this.correlationId = null;
    }

    /**
     * Add metadata to the exception.
     *
     * @param key metadata key
     * @param value metadata value
     * @return this exception for chaining
     */
    public JdnmException addMetadata(String key, Object value) {
        this.metadata.put(key, value);
        return this;
    }

    /**
     * Add multiple metadata entries.
     *
     * @param additionalMetadata metadata to add
     * @return this exception for chaining
     */
    public JdnmException addMetadata(Map<String, Object> additionalMetadata) {
        if (additionalMetadata != null) {
            this.metadata.putAll(additionalMetadata);
        }
        return this;
    }

    /**
     * Get metadata value by key.
     *
     * @param key the metadata key
     * @return the metadata value or null if not found
     */
    public Object getMetadata(String key) {
        return metadata.get(key);
    }

    /**
     * Check if this is a client error.
     *
     * @return true if client error (4xx)
     */
    public boolean isClientError() {
        return errorCode.isClientError();
    }

    /**
     * Check if this is a server error.
     *
     * @return true if server error (5xx)
     */
    public boolean isServerError() {
        return errorCode.isServerError();
    }

    /**
     * Get a detailed error message including metadata.
     *
     * @return detailed error message
     */
    public String getDetailedMessage() {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("[%s] %s", errorCode.getCode(), getMessage()));

        if (correlationId != null) {
            sb.append(String.format(" [CorrelationId: %s]", correlationId));
        }

        if (!metadata.isEmpty()) {
            sb.append(" [Metadata: ").append(metadata).append("]");
        }

        return sb.toString();
    }

    @Override
    public String toString() {
        return String.format("JdnmException{errorCode=%s, message='%s', correlationId='%s', metadata=%s}",
                errorCode.getCode(), getMessage(), correlationId, metadata);
    }

    // Static factory methods for common error scenarios

    /**
     * Create validation error.
     *
     * @param message validation error message
     * @return JdnmException with validation error code
     */
    public static JdnmException validationError(String message) {
        return new JdnmException(JdnmErrorCode.JDNM1001, message);
    }

    /**
     * Create template not found error.
     *
     * @param templateId the template ID that was not found
     * @return JdnmException with template not found error code
     */
    public static JdnmException templateNotFound(String templateId) {
        return new JdnmException(JdnmErrorCode.JDNM1002, "Template not found: " + templateId)
                .addMetadata("templateId", templateId);
    }

    /**
     * Create tenant error.
     *
     * @param tenantId the invalid tenant ID
     * @return JdnmException with tenant error code
     */
    public static JdnmException invalidTenant(String tenantId) {
        return new JdnmException(JdnmErrorCode.JDNM1003, "Invalid tenant: " + tenantId)
                .addMetadata("tenantId", tenantId);
    }

    /**
     * Create external service error.
     *
     * @param serviceName the name of the external service
     * @param message error message
     * @return JdnmException with external service error code
     */
    public static JdnmException externalServiceError(String serviceName, String message) {
        return new JdnmException(JdnmErrorCode.JDNM2001, message)
                .addMetadata("serviceName", serviceName);
    }

    /**
     * Create database error.
     *
     * @param operation the database operation that failed
     * @param message error message
     * @return JdnmException with database error code
     */
    public static JdnmException databaseError(String operation, String message) {
        return new JdnmException(JdnmErrorCode.JDNM3001, message)
                .addMetadata("operation", operation);
    }

    /**
     * Create authentication error.
     *
     * @param message authentication error message
     * @return JdnmException with authentication error code
     */
    public static JdnmException authenticationError(String message) {
        return new JdnmException(JdnmErrorCode.JDNM4001, message);
    }

    /**
     * Create internal server error.
     *
     * @param message error message
     * @param cause the underlying cause
     * @return JdnmException with internal server error code
     */
    public static JdnmException internalServerError(String message, Throwable cause) {
        return new JdnmException(JdnmErrorCode.JDNM5001, message, cause);
    }
}