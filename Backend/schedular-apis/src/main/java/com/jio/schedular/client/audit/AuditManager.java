package com.jio.schedular.client.audit;

import com.jio.schedular.client.audit.request.AuditRequest;

import java.util.Map;

/**
 * High-level audit client used by application code.
 * Implementations should do best-effort delivery (non-blocking optional).
 */
public interface AuditManager {
    /**
     * Send audit payload.
     *
     * @param auditDto   payload map
     * @param tenantId   tenant header value
     * @param businessId business header value (may be null)
     */
    void sendAudit(Map<String, Object> auditDto, String tenantId, String businessId);

    void logAudit(AuditRequest auditRequest, String tenantId);
}