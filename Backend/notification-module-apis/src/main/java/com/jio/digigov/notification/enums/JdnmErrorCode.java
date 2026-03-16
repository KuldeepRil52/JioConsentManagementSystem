package com.jio.digigov.notification.enums;

import org.springframework.http.HttpStatus;

/**
 * JDNM (Java DigiGov Notification Module) Error Codes.
 *
 * Format: JDNM[SERIES][UNIQUE_ID]
 * Where:
 * - JDNM = Java DigiGov Notification Module
 * - SERIES:
 *   - 1XXX = Validation & Client Errors
 *   - 2XXX = Integration/External Service Errors
 *   - 3XXX = Database/Infrastructure Errors
 *   - 4XXX = Security/Authentication Errors
 *   - 5XXX = Internal/Runtime Errors
 * - UNIQUE_ID = 3-digit specific error identifier
 *
 * @author Notification Service Team
 * @since 1.0.0
 */
public enum JdnmErrorCode {

    // Success
    JDNM0000("JDNM0000", "Operation completed successfully", HttpStatus.OK),

    // 1XXX - Validation & Client Errors
    JDNM1001("JDNM1001", "Invalid request format or missing required fields", HttpStatus.BAD_REQUEST),
    JDNM1002("JDNM1002", "Template not found", HttpStatus.NOT_FOUND),
    JDNM1003("JDNM1003", "Invalid or missing tenant identifier", HttpStatus.BAD_REQUEST),
    JDNM1004("JDNM1004", "Event already exists", HttpStatus.CONFLICT),
    JDNM1005("JDNM1005", "Unsupported notification channel", HttpStatus.BAD_REQUEST),
    JDNM1006("JDNM1006", "Invalid email format", HttpStatus.BAD_REQUEST),
    JDNM1007("JDNM1007", "Invalid phone number format", HttpStatus.BAD_REQUEST),
    JDNM1008("JDNM1008", "Maximum recipients exceeded", HttpStatus.BAD_REQUEST),
    JDNM1009("JDNM1009", "Invalid date range", HttpStatus.BAD_REQUEST),
    JDNM1010("JDNM1010", "Invalid pagination parameters", HttpStatus.BAD_REQUEST),
    JDNM1011("JDNM1011", "Invalid sort parameters", HttpStatus.BAD_REQUEST),
    JDNM1012("JDNM1012", "Template validation failed", HttpStatus.BAD_REQUEST),
    JDNM1013("JDNM1013", "Invalid event configuration", HttpStatus.BAD_REQUEST),
    JDNM1014("JDNM1014", "Invalid customer identifier", HttpStatus.BAD_REQUEST),
    JDNM1015("JDNM1015", "Missing required template arguments", HttpStatus.BAD_REQUEST),
    JDNM1016("JDNM1016", "Invalid status transition", HttpStatus.BAD_REQUEST),
    JDNM1017("JDNM1017", "Resource not found", HttpStatus.NOT_FOUND),
    JDNM1018("JDNM1018", "Invalid event type for the requested operation", HttpStatus.BAD_REQUEST),

    // 2XXX - Integration/External Service Errors
    JDNM2001("JDNM2001", "DigiGov API call failed", HttpStatus.BAD_GATEWAY),
    JDNM2002("JDNM2002", "Failed to send message to Kafka", HttpStatus.SERVICE_UNAVAILABLE),
    JDNM2003("JDNM2003", "OAuth token generation failed", HttpStatus.BAD_GATEWAY),
    JDNM2004("JDNM2004", "External service timeout", HttpStatus.GATEWAY_TIMEOUT),
    JDNM2005("JDNM2005", "Invalid response from external service", HttpStatus.BAD_GATEWAY),
    JDNM2006("JDNM2006", "External service unavailable", HttpStatus.SERVICE_UNAVAILABLE),
    JDNM2007("JDNM2007", "SMS gateway error", HttpStatus.BAD_GATEWAY),
    JDNM2008("JDNM2008", "Email gateway error", HttpStatus.BAD_GATEWAY),
    JDNM2009("JDNM2009", "Callback URL unreachable", HttpStatus.BAD_GATEWAY),
    JDNM2010("JDNM2010", "Rate limit exceeded", HttpStatus.TOO_MANY_REQUESTS),

    // 3XXX - Database/Infrastructure Errors
    JDNM3001("JDNM3001", "MongoDB write operation failed", HttpStatus.INTERNAL_SERVER_ERROR),
    JDNM3002("JDNM3002", "MongoDB read operation failed", HttpStatus.INTERNAL_SERVER_ERROR),
    JDNM3003("JDNM3003", "Cache operation failed", HttpStatus.INTERNAL_SERVER_ERROR),
    JDNM3004("JDNM3004", "Data consistency error", HttpStatus.INTERNAL_SERVER_ERROR),
    JDNM3005("JDNM3005", "Database connection failed", HttpStatus.INTERNAL_SERVER_ERROR),
    JDNM3006("JDNM3006", "Transaction rollback error", HttpStatus.INTERNAL_SERVER_ERROR),
    JDNM3007("JDNM3007", "Index operation failed", HttpStatus.INTERNAL_SERVER_ERROR),
    JDNM3008("JDNM3008", "Database constraint violation", HttpStatus.CONFLICT),
    JDNM3009("JDNM3009", "Tenant database not found", HttpStatus.NOT_FOUND),
    JDNM3010("JDNM3010", "Database migration required", HttpStatus.SERVICE_UNAVAILABLE),

    // 4XXX - Security/Authentication Errors
    JDNM4001("JDNM4001", "Invalid bearer token", HttpStatus.UNAUTHORIZED),
    JDNM4002("JDNM4002", "Unauthorized access attempt", HttpStatus.FORBIDDEN),
    JDNM4003("JDNM4003", "Expired or revoked token", HttpStatus.UNAUTHORIZED),
    JDNM4004("JDNM4004", "Invalid or tampered request signature", HttpStatus.FORBIDDEN),
    JDNM4005("JDNM4005", "Insufficient permissions", HttpStatus.FORBIDDEN),
    JDNM4006("JDNM4006", "Authentication required", HttpStatus.UNAUTHORIZED),
    JDNM4007("JDNM4007", "Security policy violation", HttpStatus.FORBIDDEN),
    JDNM4008("JDNM4008", "IP address not whitelisted", HttpStatus.FORBIDDEN),
    JDNM4009("JDNM4009", "API key invalid or missing", HttpStatus.UNAUTHORIZED),
    JDNM4010("JDNM4010", "Session expired", HttpStatus.UNAUTHORIZED),
    JDNM4011("JDNM4011", "Unauthorized notification access", HttpStatus.FORBIDDEN),
    JDNM4012("JDNM4012", "Missing required signature headers", HttpStatus.FORBIDDEN),

    // 5XXX - Internal/Runtime Errors
    JDNM5001("JDNM5001", "Unexpected server error", HttpStatus.INTERNAL_SERVER_ERROR),
    JDNM5002("JDNM5002", "Null pointer exception", HttpStatus.INTERNAL_SERVER_ERROR),
    JDNM5003("JDNM5003", "JSON parsing failed", HttpStatus.INTERNAL_SERVER_ERROR),
    JDNM5004("JDNM5004", "Unhandled async error", HttpStatus.INTERNAL_SERVER_ERROR),
    JDNM5005("JDNM5005", "Configuration loading error", HttpStatus.INTERNAL_SERVER_ERROR),
    JDNM5006("JDNM5006", "Template rendering error", HttpStatus.INTERNAL_SERVER_ERROR),
    JDNM5007("JDNM5007", "Thread pool exhausted", HttpStatus.SERVICE_UNAVAILABLE),
    JDNM5008("JDNM5008", "Memory limit exceeded", HttpStatus.INTERNAL_SERVER_ERROR),
    JDNM5009("JDNM5009", "Class loading error", HttpStatus.INTERNAL_SERVER_ERROR),
    JDNM5010("JDNM5010", "Resource not available", HttpStatus.INTERNAL_SERVER_ERROR),
    JDNM5011("JDNM5011", "Response signature generation failed", HttpStatus.INTERNAL_SERVER_ERROR),
    JDNM5012("JDNM5012", "Cryptographic operation failed", HttpStatus.INTERNAL_SERVER_ERROR);

    private final String code;
    private final String message;
    private final HttpStatus httpStatus;

    JdnmErrorCode(String code, String message, HttpStatus httpStatus) {
        this.code = code;
        this.message = message;
        this.httpStatus = httpStatus;
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }

    /**
     * Get error code by string code.
     *
     * @param code the error code string
     * @return the JdnmErrorCode enum
     * @throws IllegalArgumentException if code not found
     */
    public static JdnmErrorCode fromCode(String code) {
        for (JdnmErrorCode errorCode : values()) {
            if (errorCode.getCode().equals(code)) {
                return errorCode;
            }
        }
        throw new IllegalArgumentException("Unknown error code: " + code);
    }

    /**
     * Check if error code is a client error (4xx status).
     *
     * @return true if client error
     */
    public boolean isClientError() {
        return httpStatus.is4xxClientError();
    }

    /**
     * Check if error code is a server error (5xx status).
     *
     * @return true if server error
     */
    public boolean isServerError() {
        return httpStatus.is5xxServerError();
    }

    /**
     * Check if error code represents success.
     *
     * @return true if success
     */
    public boolean isSuccess() {
        return httpStatus.is2xxSuccessful();
    }

    /**
     * Get the error series (1XXX, 2XXX, etc.).
     *
     * @return the error series
     */
    public int getSeries() {
        String numericPart = code.substring(4); // Remove "JDNM" prefix
        return Integer.parseInt(numericPart.substring(0, 1)) * 1000;
    }

    @Override
    public String toString() {
        return String.format("%s: %s (HTTP %d)", code, message, httpStatus.value());
    }
}