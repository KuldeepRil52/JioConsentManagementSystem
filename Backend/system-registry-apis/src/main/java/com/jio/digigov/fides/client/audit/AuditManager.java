package com.jio.digigov.fides.client.audit;

import com.jio.digigov.fides.client.audit.request.AuditRequest;
import com.jio.digigov.fides.client.audit.request.AuditRequestDto;

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
    void sendAudit(AuditRequestDto auditDto, String tenantId, String businessId);

    void logAudit(AuditRequest auditRequest, String tenantId);
}