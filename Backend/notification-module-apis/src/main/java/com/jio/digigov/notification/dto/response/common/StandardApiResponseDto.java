package com.jio.digigov.notification.dto.response.common;

import com.jio.digigov.notification.enums.JdnmErrorCode;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Standardized API response wrapper for all DPDP Notification Module endpoints.
 *
 * This wrapper provides:
 * - Consistent response structure across all APIs
 * - JDNM error code integration
 * - Timestamp tracking for all responses
 * - Optional metadata for debugging
 * - Success/failure indicators
 * - Correlation ID tracking
 *
 * @param <T> the type of data being returned
 * @author Notification Service Team
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class StandardApiResponseDto<T> {

    /**
     * Indicates if the operation was successful.
     */
    private boolean success;

    /**
     * JDNM error code (e.g., "JDNM0000" for success).
     */
    private String code;

    /**
     * Human-readable message describing the result.
     */
    private String message;

    /**
     * The actual response data (null for error responses).
     */
    private T data;

    /**
     * Timestamp when the response was generated.
     */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    private LocalDateTime timestamp;

    /**
     * Transaction ID for request tracing.
     */
    @com.fasterxml.jackson.annotation.JsonProperty("transactionId")
    private String transactionId;

    /**
     * Path of the API endpoint that generated this response.
     */
    private String path;

    /**
     * Additional metadata for debugging or extra information.
     */
    private Map<String, Object> metadata;

    // Static factory methods for common response types

    /**
     * Create successful response with data.
     *
     * @param data the response data
     * @param <T> the type of data
     * @return successful StandardApiResponseDto
     */
    public static <T> StandardApiResponseDto<T> success(T data) {
        return StandardApiResponseDto.<T>builder()
                .success(true)
                .code(JdnmErrorCode.JDNM0000.getCode())
                .message("Operation completed successfully")
                .data(data)
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * Create successful response with data and custom message.
     *
     * @param data the response data
     * @param message custom success message
     * @param <T> the type of data
     * @return successful StandardApiResponseDto
     */
    public static <T> StandardApiResponseDto<T> success(T data, String message) {
        return StandardApiResponseDto.<T>builder()
                .success(true)
                .code(JdnmErrorCode.JDNM0000.getCode())
                .message(message)
                .data(data)
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * Create successful response without data.
     *
     * @param message success message
     * @param <T> the type parameter
     * @return successful StandardApiResponseDto
     */
    public static <T> StandardApiResponseDto<T> success(String message) {
        return StandardApiResponseDto.<T>builder()
                .success(true)
                .code(JdnmErrorCode.JDNM0000.getCode())
                .message(message)
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * Create error response with JDNM error code.
     *
     * @param errorCode the JDNM error code
     * @param <T> the type parameter
     * @return error StandardApiResponseDto
     */
    public static <T> StandardApiResponseDto<T> error(JdnmErrorCode errorCode) {
        return StandardApiResponseDto.<T>builder()
                .success(false)
                .code(errorCode.getCode())
                .message(errorCode.getMessage())
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * Create error response with JDNM error code and custom message.
     *
     * @param errorCode the JDNM error code
     * @param message custom error message
     * @param <T> the type parameter
     * @return error StandardApiResponseDto
     */
    public static <T> StandardApiResponseDto<T> error(JdnmErrorCode errorCode, String message) {
        return StandardApiResponseDto.<T>builder()
                .success(false)
                .code(errorCode.getCode())
                .message(message)
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * Create error response with JDNM error code, custom message, and metadata.
     *
     * @param errorCode the JDNM error code
     * @param message custom error message
     * @param metadata additional error metadata
     * @param <T> the type parameter
     * @return error StandardApiResponseDto
     */
    public static <T> StandardApiResponseDto<T> error(JdnmErrorCode errorCode, String message, Map<String, Object> metadata) {
        return StandardApiResponseDto.<T>builder()
                .success(false)
                .code(errorCode.getCode())
                .message(message)
                .metadata(metadata)
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * Create error response with string error code, message, and metadata.
     *
     * @param errorCode the error code as string
     * @param message custom error message
     * @param metadata additional error metadata
     * @param <T> the type parameter
     * @return error StandardApiResponseDto
     */
    public static <T> StandardApiResponseDto<T> error(String errorCode, String message, Map<String, Object> metadata) {
        return StandardApiResponseDto.<T>builder()
                .success(false)
                .code(errorCode)
                .message(message)
                .metadata(metadata)
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * Create error response with string error code and message.
     *
     * @param errorCode the error code as string
     * @param message custom error message
     * @param <T> the type parameter
     * @return error StandardApiResponseDto
     */
    public static <T> StandardApiResponseDto<T> error(String errorCode, String message) {
        return StandardApiResponseDto.<T>builder()
                .success(false)
                .code(errorCode)
                .message(message)
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * Create validation error response.
     *
     * @param message validation error message
     * @param <T> the type parameter
     * @return validation error StandardApiResponseDto
     */
    public static <T> StandardApiResponseDto<T> validationError(String message) {
        return error(JdnmErrorCode.JDNM1001, message);
    }

    /**
     * Create validation error response with field details.
     *
     * @param message validation error message
     * @param fieldErrors map of field validation errors
     * @param <T> the type parameter
     * @return validation error StandardApiResponseDto
     */
    public static <T> StandardApiResponseDto<T> validationError(String message, Map<String, Object> fieldErrors) {
        return error(JdnmErrorCode.JDNM1001, message, fieldErrors);
    }

    /**
     * Create not found error response.
     *
     * @param resource the resource that was not found
     * @param <T> the type parameter
     * @return not found error StandardApiResponseDto
     */
    public static <T> StandardApiResponseDto<T> notFound(String resource) {
        return error(JdnmErrorCode.JDNM1002, resource + " not found");
    }

    /**
     * Create unauthorized error response.
     *
     * @param <T> the type parameter
     * @return unauthorized error StandardApiResponseDto
     */
    public static <T> StandardApiResponseDto<T> unauthorized() {
        return error(JdnmErrorCode.JDNM4001, "Authentication required");
    }

    /**
     * Create forbidden error response.
     *
     * @param <T> the type parameter
     * @return forbidden error StandardApiResponseDto
     */
    public static <T> StandardApiResponseDto<T> forbidden() {
        return error(JdnmErrorCode.JDNM4002, "Access denied");
    }

    /**
     * Create internal server error response.
     *
     * @param <T> the type parameter
     * @return internal server error StandardApiResponseDto
     */
    public static <T> StandardApiResponseDto<T> internalServerError() {
        return error(JdnmErrorCode.JDNM5001, "Internal server error occurred");
    }

    /**
     * Create service unavailable error response.
     *
     * @param <T> the type parameter
     * @return service unavailable error StandardApiResponseDto
     */
    public static <T> StandardApiResponseDto<T> serviceUnavailable() {
        return error(JdnmErrorCode.JDNM2006, "Service temporarily unavailable");
    }

    // Builder methods for chaining

    /**
     * Add transaction ID to the response.
     *
     * @param transactionId the transaction ID
     * @return this StandardApiResponseDto for method chaining
     */
    public StandardApiResponseDto<T> withTransactionId(String transactionId) {
        this.transactionId = transactionId;
        return this;
    }

    /**
     * Add metadata to the response.
     *
     * @param metadata additional metadata
     * @return this StandardApiResponseDto for method chaining
     */
    public StandardApiResponseDto<T> withMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
        return this;
    }

    /**
     * Add single metadata entry to the response.
     *
     * @param key metadata key
     * @param value metadata value
     * @return this StandardApiResponseDto for method chaining
     */
    public StandardApiResponseDto<T> withMetadata(String key, Object value) {
        if (this.metadata == null) {
            this.metadata = new java.util.HashMap<>();
        }
        this.metadata.put(key, value);
        return this;
    }

    /**
     * Add path to the response.
     *
     * @param path the API endpoint path
     * @return this StandardApiResponseDto for method chaining
     */
    public StandardApiResponseDto<T> withPath(String path) {
        this.path = path;
        return this;
    }

    /**
     * Check if this response represents an error.
     *
     * @return true if this is an error response
     */
    public boolean isError() {
        return !success;
    }

    /**
     * Check if this response has data.
     *
     * @return true if data is present
     */
    public boolean hasData() {
        return data != null;
    }

    /**
     * Check if this response has metadata.
     *
     * @return true if metadata is present
     */
    public boolean hasMetadata() {
        return metadata != null && !metadata.isEmpty();
    }
}