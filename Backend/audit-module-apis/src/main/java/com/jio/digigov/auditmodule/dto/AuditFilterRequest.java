package com.jio.digigov.auditmodule.dto;

import lombok.Data;

/**
 * DTO for filtering and searching audit logs.
 */
@Data
public class AuditFilterRequest {
    private String businessId;
    private String tenantId;
    private String transactionId;
    private String component;
    private String action;
    private String actorId;
    private String resourceId;
    private String status;
    private String auditId;
}
