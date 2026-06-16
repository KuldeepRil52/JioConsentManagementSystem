package com.jio.digigov.notification.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;

/**
 * InfoSec compliant audit logger for the DPDP Notification Module.
 *
 * Provides standardized audit logging functionality that meets InfoSec requirements:
 * - Logs Source IP, Username, Timestamp, Status, and Activity in a single line
 * - Supports different log formats (standard, compact, structured)
 * - Integrates with existing logging framework (SLF4J)
 * - Handles sensitive data masking
 * - Provides context-aware logging for different operations
 *
 * @author Notification Service Team
 * @since 1.0.0
 */
@Component
@Slf4j
public class AuditLogger {

    private static final String AUDIT_MARKER = "AUDIT";

    /**
     * Log audit entry using standard format.
     *
     * @param entry the audit log entry
     */
    public void logAudit(AuditLogEntry entry) {
        log.info("{} | {}", AUDIT_MARKER, entry.toLogString());
    }

    /**
     * Log audit entry using compact format for high-volume scenarios.
     *
     * @param entry the audit log entry
     */
    public void logAuditCompact(AuditLogEntry entry) {
        log.info("{} | {}", AUDIT_MARKER, entry.toCompactLogString());
    }

    /**
     * Log successful operation audit.
     *
     * @param sourceIp source IP address
     * @param username username or system identifier
     * @param sessionId session or correlation ID
     * @param activity activity description
     */
    public void logSuccess(String sourceIp, String username, String sessionId, String activity) {
        AuditLogEntry entry = AuditLogEntry.success(sourceIp, username, sessionId, activity);
        logAudit(entry);
    }

    /**
     * Log failed operation audit.
     *
     * @param sourceIp source IP address
     * @param username username or system identifier
     * @param sessionId session or correlation ID
     * @param activity activity description
     */
    public void logFailure(String sourceIp, String username, String sessionId, String activity) {
        AuditLogEntry entry = AuditLogEntry.failure(sourceIp, username, sessionId, activity);
        logAudit(entry);
    }

    /**
     * Log API request audit from HTTP request context.
     *
     * @param request HTTP servlet request
     * @param operation operation performed
     * @param status operation status
     */
    public void logApiRequest(HttpServletRequest request, String operation, String status) {
        String sourceIp = extractClientIP(request);
        String username = extractUsername(request);
        String sessionId = extractSessionId(request);

        AuditLogEntry entry = AuditLogEntry.create(sourceIp, username, sessionId, status, operation);
        logAudit(entry);
    }

    /**
     * Log API request with additional context.
     *
     * @param request HTTP servlet request
     * @param operation operation performed
     * @param status operation status
     * @param tenantId tenant identifier
     * @param businessId business identifier
     */
    public void logApiRequestWithContext(HttpServletRequest request, String operation, String status,
                                       String tenantId, String businessId) {
        String sourceIp = extractClientIP(request);
        String username = extractUsername(request);
        String sessionId = extractSessionId(request);

        AuditLogEntry entry = AuditLogEntry.create(sourceIp, username, sessionId, status, operation)
                .withTenant(tenantId)
                .withBusiness(businessId);
        logAudit(entry);
    }

    /**
     * Log Kafka message processing audit.
     *
     * @param correlationId correlation ID from Kafka headers
     * @param topic Kafka topic
     * @param operation operation performed
     * @param status operation status
     */
    public void logKafkaProcessing(String correlationId, String topic, String operation, String status) {
        AuditLogEntry entry = AuditLogEntry.kafka(correlationId, operation, status)
                .withContext("Topic: " + topic);
        logAudit(entry);
    }

    /**
     * Log Kafka message processing with additional metadata.
     *
     * @param headers Kafka headers map
     * @param topic Kafka topic
     * @param operation operation performed
     * @param status operation status
     * @param tenantId tenant identifier
     */
    public void logKafkaProcessingWithHeaders(Map<String, Object> headers, String topic,
                                            String operation, String status, String tenantId) {
        String correlationId = getHeaderValue(headers, "X-Correlation-ID");
        String sourceIp = getHeaderValue(headers, "X-Source-IP", "KAFKA-BROKER");
        String username = getHeaderValue(headers, "X-Username", "KAFKA-CONSUMER");

        AuditLogEntry entry = AuditLogEntry.create(sourceIp, username, correlationId, status, operation)
                .withTenant(tenantId)
                .withContext("Topic: " + topic);
        logAudit(entry);
    }

    /**
     * Log system operation audit.
     *
     * @param operation system operation performed
     * @param status operation status
     */
    public void logSystemOperation(String operation, String status) {
        AuditLogEntry entry = AuditLogEntry.system(operation, status);
        logAudit(entry);
    }

    /**
     * Log authentication/authorization audit.
     *
     * @param sourceIp source IP address
     * @param username username attempting authentication
     * @param operation authentication operation (LOGIN, LOGOUT, ACCESS_DENIED, etc.)
     * @param status operation status
     */
    public void logAuthentication(String sourceIp, String username, String operation, String status) {
        AuditLogEntry entry = AuditLogEntry.create(sourceIp, username, "AUTH-" + System.currentTimeMillis(),
                status, operation)
                .withContext("Authentication");
        logAudit(entry);
    }

    /**
     * Log configuration change audit.
     *
     * @param sourceIp source IP address
     * @param username username making the change
     * @param sessionId session ID
     * @param configType type of configuration changed
     * @param configId configuration identifier
     * @param action action performed (CREATE, UPDATE, DELETE)
     * @param status operation status
     */
    public void logConfigurationChange(String sourceIp, String username, String sessionId,
                                     String configType, String configId, String action, String status) {
        String activity = String.format("Configuration %s: %s [%s]", action, configType, configId);
        AuditLogEntry entry = AuditLogEntry.create(sourceIp, username, sessionId, status, activity)
                .withContext("Configuration Management");
        logAudit(entry);
    }

    /**
     * Log data access audit.
     *
     * @param sourceIp source IP address
     * @param username username accessing data
     * @param sessionId session ID
     * @param resourceType type of resource accessed
     * @param resourceId resource identifier
     * @param action action performed (READ, WRITE, DELETE)
     * @param status operation status
     */
    public void logDataAccess(String sourceIp, String username, String sessionId,
                            String resourceType, String resourceId, String action, String status) {
        String activity = String.format("Data %s: %s [%s]", action, resourceType,
                maskSensitiveId(resourceId));
        AuditLogEntry entry = AuditLogEntry.create(sourceIp, username, sessionId, status, activity)
                .withContext("Data Access");
        logAudit(entry);
    }

    /**
     * Log external service integration audit.
     *
     * @param operation integration operation
     * @param serviceName external service name
     * @param status operation status
     * @param durationMs operation duration in milliseconds
     * @param tenantId tenant identifier
     */
    public void logExternalServiceCall(String operation, String serviceName, String status,
                                     Long durationMs, String tenantId) {
        String activity = String.format("External Service Call: %s [%s]", operation, serviceName);
        AuditLogEntry entry = AuditLogEntry.system(activity, status)
                .withTenant(tenantId)
                .withDuration(durationMs)
                .withContext("External Integration");
        logAudit(entry);
    }

    // Utility methods for extracting information from requests and headers

    /**
     * Extract client IP address from HTTP request.
     * Handles X-Forwarded-For, X-Real-IP headers and proxy scenarios.
     *
     * @param request HTTP servlet request
     * @return client IP address
     */
    private String extractClientIP(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty() && !"unknown".equalsIgnoreCase(xForwardedFor)) {
            // X-Forwarded-For can contain multiple IPs, take the first one
            return xForwardedFor.split(",")[0].trim();
        }

        String xRealIP = request.getHeader("X-Real-IP");
        if (xRealIP != null && !xRealIP.isEmpty() && !"unknown".equalsIgnoreCase(xRealIP)) {
            return xRealIP;
        }

        return request.getRemoteAddr();
    }

    /**
     * Extract username from HTTP request.
     * Checks authentication context, custom headers, or defaults to ANONYMOUS.
     *
     * @param request HTTP servlet request
     * @return username or system identifier
     */
    private String extractUsername(HttpServletRequest request) {
        // Check custom username header
        String username = request.getHeader("X-Username");
        if (username != null && !username.isEmpty()) {
            return username;
        }

        // Check authentication principal
        if (request.getUserPrincipal() != null) {
            return request.getUserPrincipal().getName();
        }

        // Check remote user
        if (request.getRemoteUser() != null) {
            return request.getRemoteUser();
        }

        return "ANONYMOUS";
    }

    /**
     * Extract session ID from HTTP request.
     * Checks custom headers, HTTP session, or correlation ID.
     *
     * @param request HTTP servlet request
     * @return session or correlation ID
     */
    private String extractSessionId(HttpServletRequest request) {
        // Check custom session header
        String sessionId = request.getHeader("X-Session-ID");
        if (sessionId != null && !sessionId.isEmpty()) {
            return sessionId;
        }

        // Check correlation ID header
        String correlationId = request.getHeader("X-Correlation-ID");
        if (correlationId != null && !correlationId.isEmpty()) {
            return correlationId;
        }

        // Check HTTP session
        if (request.getSession(false) != null) {
            return request.getSession().getId();
        }

        return "NO-SESSION";
    }

    /**
     * Get header value from Kafka headers map.
     *
     * @param headers Kafka headers map
     * @param headerName header name to retrieve
     * @return header value or null
     */
    private String getHeaderValue(Map<String, Object> headers, String headerName) {
        return getHeaderValue(headers, headerName, null);
    }

    /**
     * Get header value from Kafka headers map with default.
     *
     * @param headers Kafka headers map
     * @param headerName header name to retrieve
     * @param defaultValue default value if header not found
     * @return header value or default
     */
    private String getHeaderValue(Map<String, Object> headers, String headerName, String defaultValue) {
        if (headers == null) {
            return defaultValue;
        }

        Object value = headers.get(headerName);
        if (value == null) {
            return defaultValue;
        }

        if (value instanceof byte[]) {
            return new String((byte[]) value);
        }

        return value.toString();
    }

    /**
     * Mask sensitive parts of an identifier for audit logging.
     * Shows first 2 and last 2 characters, masks the middle.
     *
     * @param id identifier to mask
     * @return masked identifier
     */
    private String maskSensitiveId(String id) {
        if (id == null || id.length() <= 4) {
            return "****";
        }
        return id.substring(0, 2) + "****" + id.substring(id.length() - 2);
    }
}