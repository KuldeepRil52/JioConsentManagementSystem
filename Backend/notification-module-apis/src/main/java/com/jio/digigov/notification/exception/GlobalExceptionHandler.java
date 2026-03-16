package com.jio.digigov.notification.exception;

import com.jio.digigov.notification.constant.NotificationConstants;
import com.jio.digigov.notification.dto.response.common.StandardApiResponseDto;
import com.jio.digigov.notification.enums.JdnmErrorCode;
import com.jio.digigov.notification.util.AuditLogger;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Global exception handler for standardized error responses.
 *
 * This handler ensures all exceptions are properly mapped to JDNM error codes
 * and wrapped in StandardApiResponseDto format for consistent error handling across the application.
 * It also provides comprehensive audit logging for all error scenarios.
 *
 * @author Notification Service Team
 * @since 1.0.0
 */
@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    private final AuditLogger auditLogger;
    
    /**
     * Handle validation exceptions from @Valid annotations.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<StandardApiResponseDto<Object>> handleValidationException(
            MethodArgumentNotValidException ex, HttpServletRequest request) {

        log.error("Validation Exception: {}", ex.getMessage());

        auditLogger.logApiRequest(request, "VALIDATION_ERROR", "FAILED");

        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("validationErrors", errors);

        StandardApiResponseDto<Object> response = StandardApiResponseDto.error(JdnmErrorCode.JDNM1001, "Validation failed for request", metadata)
                .withTransactionId(request.getHeader(NotificationConstants.HEADER_TRANSACTION_ID));

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /**
     * Handle missing request header exceptions.
     * This is thrown when a required @RequestHeader is missing.
     */
    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<StandardApiResponseDto<Object>> handleMissingRequestHeaderException(
            MissingRequestHeaderException ex, HttpServletRequest request) {

        String headerName = ex.getHeaderName();
        log.warn("Missing required header: {}", headerName);

        auditLogger.logApiRequest(request, "MISSING_HEADER", "FAILED");

        Map<String, Object> metadata = Map.of("missingHeader", headerName);
        String errorMessage = String.format("Missing required header: %s", headerName);

        StandardApiResponseDto<Object> response = StandardApiResponseDto.error(
                JdnmErrorCode.JDNM1001, errorMessage, metadata)
                .withTransactionId(request.getHeader(NotificationConstants.HEADER_TRANSACTION_ID));

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /**
     * Handle constraint violation exceptions.
     * This is thrown when validation constraints on parameters (including headers) fail.
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<StandardApiResponseDto<Object>> handleConstraintViolationException(
            ConstraintViolationException ex, HttpServletRequest request) {

        log.warn("Constraint violation: {}", ex.getMessage());

        auditLogger.logApiRequest(request, "CONSTRAINT_VIOLATION", "FAILED");

        Map<String, String> violations = new HashMap<>();
        for (ConstraintViolation<?> violation : ex.getConstraintViolations()) {
            String propertyPath = violation.getPropertyPath().toString();
            String message = violation.getMessage();
            violations.put(propertyPath, message);
        }

        Map<String, Object> metadata = Map.of("violations", violations);
        String errorMessage = "Validation constraint violation";

        StandardApiResponseDto<Object> response = StandardApiResponseDto.error(
                JdnmErrorCode.JDNM1001, errorMessage, metadata)
                .withTransactionId(request.getHeader(NotificationConstants.HEADER_TRANSACTION_ID));

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /**
     * Handle HTTP message not readable exceptions.
     * This is thrown when JSON parsing fails, including invalid enum values.
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<StandardApiResponseDto<Object>> handleHttpMessageNotReadableException(
            HttpMessageNotReadableException ex, HttpServletRequest request) {

        log.warn("HTTP message not readable: {}", ex.getMessage());

        auditLogger.logApiRequest(request, "INVALID_REQUEST_BODY", "FAILED");

        // Extract a user-friendly error message from the exception
        String errorMessage = extractReadableErrorMessage(ex);

        StandardApiResponseDto<Object> response = StandardApiResponseDto.error(
                JdnmErrorCode.JDNM1001, errorMessage, null)
                .withTransactionId(request.getHeader(NotificationConstants.HEADER_TRANSACTION_ID));

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /**
     * Handle illegal argument exceptions (header validation and other argument errors).
     * Enhanced to provide specific error messages for header-related errors.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<StandardApiResponseDto<Object>> handleIllegalArgumentException(
            IllegalArgumentException ex, HttpServletRequest request) {

        log.warn("Illegal argument: {}", ex.getMessage());

        auditLogger.logApiRequest(request, "ILLEGAL_ARGUMENT", "FAILED");

        String errorMessage = ex.getMessage();
        Map<String, Object> metadata = null;

        // Check if this is a header validation error
        if (errorMessage != null && errorMessage.toLowerCase().contains("header")) {
            // Try to extract header name and provide a better error message
            String headerName = extractHeaderName(errorMessage);
            if (headerName != null) {
                errorMessage = String.format("Missing or invalid required header: %s", headerName);
                metadata = Map.of("headerName", headerName);
            }
        }

        StandardApiResponseDto<Object> response = StandardApiResponseDto.error(
                JdnmErrorCode.JDNM1001, errorMessage, metadata)
                .withTransactionId(request.getHeader(NotificationConstants.HEADER_TRANSACTION_ID));

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }
    
    /**
     * Handle configuration not found
     */
    @ExceptionHandler(ConfigurationNotFoundException.class)
    public ResponseEntity<StandardApiResponseDto<Object>> handleConfigurationNotFoundException(
            ConfigurationNotFoundException ex, HttpServletRequest request) {

        log.error("Configuration not found: {}", ex.getMessage());

        auditLogger.logApiRequest(request, "CONFIGURATION_NOT_FOUND", "FAILED");

        StandardApiResponseDto<Object> response = StandardApiResponseDto.error(JdnmErrorCode.JDNM2001, ex.getMessage(), null)
                .withTransactionId(request.getHeader(NotificationConstants.HEADER_TRANSACTION_ID));

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }
    
    /**
     * Handle token generation errors
     */
    @ExceptionHandler(TokenGenerationException.class)
    public ResponseEntity<StandardApiResponseDto<Object>> handleTokenGenerationException(
            TokenGenerationException ex, HttpServletRequest request) {

        log.error("Token generation error: {}", ex.getMessage());

        auditLogger.logApiRequest(request, "TOKEN_GENERATION_ERROR", "FAILED");

        StandardApiResponseDto<Object> response = StandardApiResponseDto.error(JdnmErrorCode.JDNM3002,
                "Token generation failed: " + ex.getMessage(), null)
                .withTransactionId(request.getHeader(NotificationConstants.HEADER_TRANSACTION_ID));

        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(response);
    }
    
    /**
     * Handle DigiGov client errors
     */
    @ExceptionHandler(DigiGovClientException.class)
    public ResponseEntity<StandardApiResponseDto<Object>> handleDigiGovClientException(
            DigiGovClientException ex, HttpServletRequest request) {

        HttpStatus httpStatus = mapDigiGovStatusCode(ex.getStatusCode());

        log.error("DigiGov client error - operation: {}, statusCode: {}, message: {}",
                ex.getOperation(), ex.getStatusCode(), ex.getMessage());

        auditLogger.logApiRequest(request, "DIGIGOV_CLIENT_ERROR", "FAILED");

        Map<String, Object> metadata = Map.of("operation", ex.getOperation(), "statusCode", ex.getStatusCode());

        StandardApiResponseDto<Object> response = StandardApiResponseDto.error(JdnmErrorCode.JDNM3001,
                String.format("DigiGov %s operation failed: %s", ex.getOperation(), ex.getMessage()),
                metadata)
                .withTransactionId(request.getHeader(NotificationConstants.HEADER_TRANSACTION_ID));

        return ResponseEntity.status(httpStatus).body(response);
    }
    
    /**
     * Handle validation exceptions
     */
    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<StandardApiResponseDto<Object>> handleValidationException(
            ValidationException ex, HttpServletRequest request) {

        log.warn("Validation error: {}", ex.getMessage());

        auditLogger.logApiRequest(request, "VALIDATION_ERROR", "FAILED");

        StandardApiResponseDto<Object> response = StandardApiResponseDto.error(JdnmErrorCode.JDNM1001, ex.getMessage(), null)
                .withTransactionId(request.getHeader(NotificationConstants.HEADER_TRANSACTION_ID));

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }
    
    /**
     * Handle template not found exceptions
     */
    @ExceptionHandler(TemplateNotFoundException.class)
    public ResponseEntity<StandardApiResponseDto<Object>> handleTemplateNotFoundException(
            TemplateNotFoundException ex, HttpServletRequest request) {

        log.warn("Template not found: {}", ex.getMessage());

        auditLogger.logApiRequest(request, "TEMPLATE_NOT_FOUND", "FAILED");

        StandardApiResponseDto<Object> response = StandardApiResponseDto.error(JdnmErrorCode.JDNM2002, ex.getMessage(), null)
                .withTransactionId(request.getHeader(NotificationConstants.HEADER_TRANSACTION_ID));

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    /**
     * Handle onboarding exceptions.
     * These exceptions are thrown during onboarding operations when:
     * - Prerequisites validation fails (JDNM1001)
     * - Job not found (JDNM1002)
     * - Job doesn't belong to business (JDNM1003)
     */
    @ExceptionHandler(OnboardingException.class)
    public ResponseEntity<StandardApiResponseDto<Object>> handleOnboardingException(
            OnboardingException ex, HttpServletRequest request) {

        log.warn("Onboarding error - code: {}, message: {}", ex.getErrorCode(), ex.getMessage());

        auditLogger.logApiRequest(request, "ONBOARDING_ERROR", "FAILED");

        // Map error code to HTTP status
        HttpStatus httpStatus = mapOnboardingErrorCodeToHttpStatus(ex.getErrorCode());

        // Map error code string to JdnmErrorCode enum
        JdnmErrorCode jdnmErrorCode = mapErrorCodeString(ex.getErrorCode());

        StandardApiResponseDto<Object> response = StandardApiResponseDto.error(jdnmErrorCode, ex.getMessage(), null)
                .withTransactionId(request.getHeader(NotificationConstants.HEADER_TRANSACTION_ID));

        return ResponseEntity.status(httpStatus).body(response);
    }

    /**
     * Handle business exceptions
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<StandardApiResponseDto<Object>> handleBusinessException(
            BusinessException ex, HttpServletRequest request) {

        log.warn("Business error - code: {}, message: {}", ex.getErrorCode(), ex.getMessage());

        auditLogger.logApiRequest(request, "BUSINESS_ERROR", "FAILED");

        Map<String, Object> metadata = Map.of("errorCode", ex.getErrorCode());

        StandardApiResponseDto<Object> response = StandardApiResponseDto.error(JdnmErrorCode.JDNM4001, ex.getMessage(), metadata)
                .withTransactionId(request.getHeader(NotificationConstants.HEADER_TRANSACTION_ID));

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }
    
    /**
     * Handle illegal state exceptions
     */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<StandardApiResponseDto<Object>> handleIllegalStateException(
            IllegalStateException ex, HttpServletRequest request) {

        log.error("Illegal state: {}", ex.getMessage());

        auditLogger.logApiRequest(request, "ILLEGAL_STATE", "FAILED");

        StandardApiResponseDto<Object> response = StandardApiResponseDto.error(JdnmErrorCode.JDNM5001,
                "System state error: " + ex.getMessage(), null)
                .withTransactionId(request.getHeader(NotificationConstants.HEADER_TRANSACTION_ID));

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
    
    /**
     * Handle invalid event type exceptions.
     *
     * <p>Thrown when an operation is attempted on an event with an incompatible type.
     * This includes:
     * <ul>
     *   <li>Consent deletion API called with non-consent event</li>
     *   <li>Operations requiring specific event types</li>
     * </ul>
     *
     * Returns 400 Bad Request with detailed error message and metadata.
     */
    @ExceptionHandler(InvalidEventTypeException.class)
    public ResponseEntity<StandardApiResponseDto<Object>> handleInvalidEventTypeException(
            InvalidEventTypeException ex, HttpServletRequest request) {

        log.warn("Invalid event type: {} - Metadata: {}", ex.getMessage(), ex.getMetadata());

        auditLogger.logApiRequest(request, "INVALID_EVENT_TYPE", "FAILED");

        StandardApiResponseDto<Object> response = StandardApiResponseDto.error(
                JdnmErrorCode.JDNM1018,
                ex.getMessage(),
                ex.getMetadata())
                .withTransactionId(request.getHeader(NotificationConstants.HEADER_TRANSACTION_ID));

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /**
     * Handle resource not found exceptions.
     *
     * <p>Thrown when a requested resource cannot be found in the database.
     * This includes:
     * <ul>
     *   <li>Event not found for given eventId and businessId</li>
     *   <li>Notification not found</li>
     *   <li>Other resource lookups that fail</li>
     * </ul>
     *
     * Returns 404 Not Found with detailed error message and metadata.
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<StandardApiResponseDto<Object>> handleResourceNotFoundException(
            ResourceNotFoundException ex, HttpServletRequest request) {

        log.warn("Resource not found: {} - Metadata: {}", ex.getMessage(), ex.getMetadata());

        auditLogger.logApiRequest(request, "RESOURCE_NOT_FOUND", "FAILED");

        // Include metadata if available for better error context
        Map<String, Object> metadata = ex.getMetadata() != null && !ex.getMetadata().isEmpty()
                ? ex.getMetadata()
                : null;

        StandardApiResponseDto<Object> response = StandardApiResponseDto.error(
                JdnmErrorCode.JDNM1017,
                ex.getMessage(),
                metadata)
                .withTransactionId(request.getHeader(NotificationConstants.HEADER_TRANSACTION_ID));

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    /**
     * Handle generic exceptions
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<StandardApiResponseDto<Object>> handleGenericException(
            Exception ex, HttpServletRequest request) {

        log.error("Unexpected error occurred");

        auditLogger.logApiRequest(request, "UNEXPECTED_ERROR", "FAILED");

        StandardApiResponseDto<Object> response = StandardApiResponseDto.error(JdnmErrorCode.JDNM5001,
                "Internal server error occurred", null)
                .withTransactionId(request.getHeader(NotificationConstants.HEADER_TRANSACTION_ID));

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    /**
     * Handle invalid status transition exceptions.
     */
    @ExceptionHandler(InvalidStatusTransitionException.class)
    public ResponseEntity<StandardApiResponseDto<Object>> handleInvalidStatusTransition(
            InvalidStatusTransitionException ex, HttpServletRequest request) {

        log.warn("Invalid status transition: {}", ex.getMessage());

        auditLogger.logApiRequest(request, "INVALID_STATUS_TRANSITION", "FAILED");

        StandardApiResponseDto<Object> response = StandardApiResponseDto.error(JdnmErrorCode.JDNM1016,
                ex.getMessage(), null)
                .withTransactionId(request.getHeader(NotificationConstants.HEADER_TRANSACTION_ID));

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /**
     * Handle unauthorized notification access exceptions.
     */
    @ExceptionHandler(UnauthorizedNotificationAccessException.class)
    public ResponseEntity<StandardApiResponseDto<Object>> handleUnauthorizedAccess(
            UnauthorizedNotificationAccessException ex, HttpServletRequest request) {

        log.warn("Unauthorized notification access: {}", ex.getMessage());

        auditLogger.logApiRequest(request, "UNAUTHORIZED_ACCESS", "FAILED");

        StandardApiResponseDto<Object> response = StandardApiResponseDto.error(JdnmErrorCode.JDNM4011,
                ex.getMessage(), null)
                .withTransactionId(request.getHeader(NotificationConstants.HEADER_TRANSACTION_ID));

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
    }

    /**
     * Handle signature verification exceptions.
     *
     * <p>Thrown when request signature verification fails due to:
     * <ul>
     *   <li>Missing or invalid x-jws-signature header</li>
     *   <li>Signature mismatch (tampered payload)</li>
     *   <li>Certificate not found for entity</li>
     *   <li>Missing required headers for verification</li>
     * </ul>
     *
     * Returns 403 Forbidden with detailed error message and metadata.
     */
    @ExceptionHandler(SignatureVerificationException.class)
    public ResponseEntity<StandardApiResponseDto<Object>> handleSignatureVerificationException(
            SignatureVerificationException ex, HttpServletRequest request) {

        log.error("Signature verification failed: {} - Metadata: {}", ex.getMessage(), ex.getMetadata());

        auditLogger.logApiRequest(request, "SIGNATURE_VERIFICATION_FAILED", "FAILED");

        StandardApiResponseDto<Object> response = StandardApiResponseDto.error(
                ex.getErrorCode(),
                ex.getMessage(),
                ex.getMetadata())
                .withTransactionId(request.getHeader(NotificationConstants.HEADER_TRANSACTION_ID));

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
    }

    /**
     * Handle signature generation exceptions.
     *
     * <p>Thrown when response signature generation fails due to:
     * <ul>
     *   <li>JWK private key not found in database</li>
     *   <li>Invalid JWK key format or missing parameters</li>
     *   <li>JSON serialization failure</li>
     *   <li>Cryptographic operation failure</li>
     * </ul>
     *
     * Returns 500 Internal Server Error with detailed error message.
     */
    @ExceptionHandler(SignatureGenerationException.class)
    public ResponseEntity<StandardApiResponseDto<Object>> handleSignatureGenerationException(
            SignatureGenerationException ex, HttpServletRequest request) {

        log.error("Signature generation failed: {} - Metadata: {}", ex.getMessage(), ex.getMetadata());

        auditLogger.logApiRequest(request, "SIGNATURE_GENERATION_FAILED", "FAILED");

        StandardApiResponseDto<Object> response = StandardApiResponseDto.error(
                ex.getErrorCode(),
                ex.getMessage(),
                ex.getMetadata())
                .withTransactionId(request.getHeader(NotificationConstants.HEADER_TRANSACTION_ID));

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    /**
     * Handle rate limit exceeded exceptions.
     *
     * <p>Thrown when a customer identifier (recipient + business) has made too many
     * requests within the configured time window.
     *
     * <p>Returns 429 Too Many Requests with:
     * <ul>
     *   <li>Retry-After header specifying seconds to wait</li>
     *   <li>Error message indicating rate limit exceeded</li>
     *   <li>Metadata with current count, max requests, and window information</li>
     * </ul>
     *
     * <p>HTTP Headers:
     * <ul>
     *   <li>Retry-After: Number of seconds to wait before retrying</li>
     * </ul>
     */
    @ExceptionHandler(RateLimitExceededException.class)
    public ResponseEntity<StandardApiResponseDto<Object>> handleRateLimitExceededException(
            RateLimitExceededException ex, HttpServletRequest request) {

        log.warn("Rate limit exceeded - recipient: {}, businessId: {}, eventType: {}, currentCount: {}, maxRequests: {}",
                ex.getRecipientValue(), ex.getBusinessId(), ex.getEventType(),
                ex.getCurrentCount(), ex.getMaxRequests());

        auditLogger.logApiRequest(request, "RATE_LIMIT_EXCEEDED", "FAILED");

        // Build metadata with rate limit details
        Map<String, Object> metadata = Map.of(
                "recipientValue", ex.getRecipientValue(),
                "businessId", ex.getBusinessId(),
                "eventType", ex.getEventType(),
                "currentCount", ex.getCurrentCount(),
                "maxRequests", ex.getMaxRequests(),
                "windowMinutes", ex.getWindowMinutes()
        );

        // Create error response
        StandardApiResponseDto<Object> response = StandardApiResponseDto.error(
                JdnmErrorCode.JDNM4002,
                ex.getMessage(),
                metadata)
                .withTransactionId(request.getHeader(NotificationConstants.HEADER_TRANSACTION_ID));

        // Add Retry-After header
        HttpHeaders headers = new HttpHeaders();
        headers.add("Retry-After", String.valueOf(ex.getRetryAfterSeconds()));

        return ResponseEntity
                .status(HttpStatus.TOO_MANY_REQUESTS)
                .headers(headers)
                .body(response);
    }


    /**
     * Map DigiGov status codes to HTTP status codes
     */
    private HttpStatus mapDigiGovStatusCode(int digiGovStatusCode) {
        if (digiGovStatusCode >= 400 && digiGovStatusCode < 500) {
            return HttpStatus.BAD_REQUEST;
        } else if (digiGovStatusCode >= 500) {
            return HttpStatus.BAD_GATEWAY;
        } else {
            return HttpStatus.INTERNAL_SERVER_ERROR;
        }
    }

    /**
     * Extract a user-friendly error message from HttpMessageNotReadableException.
     * Handles various cases like invalid enum values, JSON syntax errors, etc.
     */
    private String extractReadableErrorMessage(HttpMessageNotReadableException ex) {
        Throwable rootCause = ex.getMostSpecificCause();

        // Check if root cause has a meaningful message (e.g., from enum validation)
        if (rootCause instanceof IllegalArgumentException) {
            String message = rootCause.getMessage();
            if (message != null && !message.isEmpty()) {
                return message;
            }
        }

        // For other JSON parse errors, provide a generic but helpful message
        String originalMessage = ex.getMessage();
        if (originalMessage != null) {
            // Try to extract field name from the message
            Pattern fieldPattern = Pattern.compile("through reference chain:.*\\[\"([^\"]+)\"\\]");
            Matcher matcher = fieldPattern.matcher(originalMessage);
            if (matcher.find()) {
                return String.format("Invalid value for field '%s'", matcher.group(1));
            }
        }

        return "Invalid request body: malformed JSON or invalid field values";
    }

    /**
     * Extract header name from error message.
     * Attempts to identify the specific header causing the error.
     */
    private String extractHeaderName(String errorMessage) {
        // Common header patterns to check
        String[] commonHeaders = {
            "X-Tenant-ID", "X-Tenant-Id",
            "X-Business-ID", "X-Business-Id",
            "X-Transaction-ID", "X-Transaction-Id",
            "X-Scope-Level", "X-Type"
        };

        // Check for exact matches with common headers
        for (String header : commonHeaders) {
            if (errorMessage.contains(header)) {
                return header;
            }
        }

        // Try to extract any header name pattern (X-Something or similar)
        Pattern pattern = Pattern.compile("([Xx]-[A-Za-z][A-Za-z0-9-]*[A-Za-z0-9])");
        Matcher matcher = pattern.matcher(errorMessage);
        if (matcher.find()) {
            return matcher.group(1);
        }

        return null;
    }

    /**
     * Map onboarding error code string to HTTP status.
     *
     * @param errorCode the error code from OnboardingException
     * @return the appropriate HTTP status
     */
    private HttpStatus mapOnboardingErrorCodeToHttpStatus(String errorCode) {
        switch (errorCode) {
            case "JDNM1001":  // Prerequisites validation (templates/configs exist)
                return HttpStatus.CONFLICT;
            case "JDNM1002":  // Job not found
                return HttpStatus.NOT_FOUND;
            case "JDNM1003":  // Job doesn't belong to business
                return HttpStatus.FORBIDDEN;
            default:
                return HttpStatus.BAD_REQUEST;
        }
    }

    /**
     * Map error code string to JdnmErrorCode enum.
     * Uses JdnmErrorCode.fromCode() for validation.
     * Falls back to JDNM5001 if code is unknown.
     *
     * @param errorCodeString the error code string (e.g., "JDNM1001")
     * @return the matching JdnmErrorCode enum
     */
    private JdnmErrorCode mapErrorCodeString(String errorCodeString) {
        try {
            return JdnmErrorCode.fromCode(errorCodeString);
        } catch (IllegalArgumentException e) {
            log.warn("Unknown error code '{}', defaulting to JDNM5001", errorCodeString);
            return JdnmErrorCode.JDNM5001;
        }
    }
}