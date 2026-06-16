package com.jio.digigov.notification.util;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Audit log entry data structure for InfoSec compliant logging.
 *
 * Contains all required fields as per InfoSec standards:
 * - Source IP
 * - Username
 * - Timestamp
 * - Status/Result of Operations
 * - Activity/Config done
 * All 5 parameters are present in a single log line.
 *
 * @author Notification Service Team
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLogEntry {

    /**
     * Timestamp of the operation (required).
     */
    private String timestamp;

    /**
     * Source IP address from where the request originated (required).
     */
    private String sourceIp;

    /**
     * Username or system identifier performing the operation (required).
     */
    private String username;

    /**
     * Session ID or correlation ID for tracing (required).
     */
    private String sessionId;

    /**
     * Status or result of the operation (required).
     */
    private String status;

    /**
     * Description of the activity or configuration performed (required).
     */
    private String activity;

    /**
     * Additional context or metadata (optional).
     */
    private String context;

    /**
     * Tenant ID for multi-tenant operations (optional).
     */
    private String tenantId;

    /**
     * Business ID for business-specific operations (optional).
     */
    private String businessId;

    /**
     * Duration of the operation in milliseconds (optional).
     */
    private Long durationMs;

    /**
     * Additional details for debugging (optional).
     */
    private String details;

    /**
     * Create audit log entry with current timestamp.
     *
     * @param sourceIp source IP address
     * @param username username or system identifier
     * @param sessionId session or correlation ID
     * @param status operation status
     * @param activity activity description
     * @return AuditLogEntry with current timestamp
     */
    public static AuditLogEntry create(String sourceIp, String username, String sessionId,
                                     String status, String activity) {
        return AuditLogEntry.builder()
                .timestamp(LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss")))
                .sourceIp(defaultIfNull(sourceIp, "UNKNOWN"))
                .username(defaultIfNull(username, "SYSTEM"))
                .sessionId(defaultIfNull(sessionId, "N/A"))
                .status(defaultIfNull(status, "UNKNOWN"))
                .activity(defaultIfNull(activity, "Operation performed"))
                .build();
    }

    /**
     * Create audit log entry for successful operation.
     *
     * @param sourceIp source IP address
     * @param username username or system identifier
     * @param sessionId session or correlation ID
     * @param activity activity description
     * @return AuditLogEntry with SUCCESS status
     */
    public static AuditLogEntry success(String sourceIp, String username, String sessionId, String activity) {
        return create(sourceIp, username, sessionId, "SUCCESS", activity);
    }

    /**
     * Create audit log entry for failed operation.
     *
     * @param sourceIp source IP address
     * @param username username or system identifier
     * @param sessionId session or correlation ID
     * @param activity activity description
     * @return AuditLogEntry with FAILED status
     */
    public static AuditLogEntry failure(String sourceIp, String username, String sessionId, String activity) {
        return create(sourceIp, username, sessionId, "FAILED", activity);
    }

    /**
     * Create audit log entry for system operations.
     *
     * @param activity activity description
     * @param status operation status
     * @return AuditLogEntry with system defaults
     */
    public static AuditLogEntry system(String activity, String status) {
        String serverIp = ServerIpAddressUtil.getServerIp();
        return create(serverIp, "SYSTEM", "SYSTEM-" + System.currentTimeMillis(), status, activity);
    }

    /**
     * Create audit log entry for Kafka consumer operations.
     *
     * @param correlationId correlation ID from Kafka headers
     * @param activity activity description
     * @param status operation status
     * @return AuditLogEntry for Kafka operations
     */
    public static AuditLogEntry kafka(String correlationId, String activity, String status) {
        return AuditLogEntry.builder()
                .timestamp(LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss")))
                .sourceIp("KAFKA-BROKER")
                .username("KAFKA-CONSUMER")
                .sessionId(defaultIfNull(correlationId, "KAFKA-" + System.currentTimeMillis()))
                .status(defaultIfNull(status, "PROCESSING"))
                .activity(defaultIfNull(activity, "Kafka message processing"))
                .build();
    }

    /**
     * Add tenant context to the audit entry.
     *
     * @param tenantId tenant identifier
     * @return this AuditLogEntry for method chaining
     */
    public AuditLogEntry withTenant(String tenantId) {
        this.tenantId = tenantId;
        return this;
    }

    /**
     * Add business context to the audit entry.
     *
     * @param businessId business identifier
     * @return this AuditLogEntry for method chaining
     */
    public AuditLogEntry withBusiness(String businessId) {
        this.businessId = businessId;
        return this;
    }

    /**
     * Add operation duration to the audit entry.
     *
     * @param durationMs duration in milliseconds
     * @return this AuditLogEntry for method chaining
     */
    public AuditLogEntry withDuration(Long durationMs) {
        this.durationMs = durationMs;
        return this;
    }

    /**
     * Add additional context to the audit entry.
     *
     * @param context additional context information
     * @return this AuditLogEntry for method chaining
     */
    public AuditLogEntry withContext(String context) {
        this.context = context;
        return this;
    }

    /**
     * Add additional details to the audit entry.
     *
     * @param details additional details for debugging
     * @return this AuditLogEntry for method chaining
     */
    public AuditLogEntry withDetails(String details) {
        this.details = details;
        return this;
    }

    /**
     * Get the formatted audit log string as per InfoSec standards.
     * Format: "Timestamp: {timestamp} | SourceIP: {sourceIp} | Username: {username} | " +
     *         "SessionId: {sessionId} | Status: {status} | Activity: {activity}"
     *
     * @return formatted audit log string
     */
    public String toLogString() {
        StringBuilder logBuilder = new StringBuilder();
        logBuilder.append("Timestamp: ").append(timestamp)
                  .append(" | SourceIP: ").append(sourceIp)
                  .append(" | Username: ").append(username)
                  .append(" | SessionId: ").append(sessionId)
                  .append(" | Status: ").append(status)
                  .append(" | Activity: ").append(activity);

        // Add optional fields if present
        if (tenantId != null) {
            logBuilder.append(" | TenantId: ").append(tenantId);
        }
        if (businessId != null) {
            logBuilder.append(" | BusinessId: ").append(businessId);
        }
        if (durationMs != null) {
            logBuilder.append(" | Duration: ").append(durationMs).append("ms");
        }
        if (context != null) {
            logBuilder.append(" | Context: ").append(context);
        }
        if (details != null) {
            logBuilder.append(" | Details: ").append(details);
        }

        return logBuilder.toString();
    }

    /**
     * Get compact audit log string for high-volume scenarios.
     *
     * @return compact audit log string
     */
    public String toCompactLogString() {
        return String.format("TS:%s|IP:%s|USER:%s|SID:%s|STATUS:%s|ACTIVITY:%s",
                timestamp, sourceIp, username, sessionId, status, activity);
    }

    private static String defaultIfNull(String value, String defaultValue) {
        return value != null && !value.trim().isEmpty() ? value : defaultValue;
    }
}