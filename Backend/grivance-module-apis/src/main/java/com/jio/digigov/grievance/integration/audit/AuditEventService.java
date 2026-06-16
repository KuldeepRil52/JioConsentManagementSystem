package com.jio.digigov.grievance.integration.audit;

import com.jio.digigov.grievance.entity.Grievance;
import com.jio.digigov.grievance.entity.GrievanceTemplate;
import jakarta.servlet.http.HttpServletRequest;

public interface AuditEventService {

    /**
     * Trigger an audit event for a Grievance action (NEW, INPROCESS, RESOLVED, etc.)
     */
    void triggerAuditEvent(Grievance grievance, String tenantId, String businessId, String transactionId, HttpServletRequest request);

    /**
     * Trigger an audit event for a Grievance Template (DRAFT, PUBLISHED, etc.)
     */
    void triggerAuditEventForTemplate(GrievanceTemplate template, String tenantId, String businessId, String transactionId, HttpServletRequest request);
}
