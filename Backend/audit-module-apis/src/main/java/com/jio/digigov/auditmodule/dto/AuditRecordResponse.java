package com.jio.digigov.auditmodule.dto;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * DTO representing audit record response (matches JSON response structure exactly)
 * Includes businessName fetched dynamically.
 */
@Data
public class AuditRecordResponse {
    private String id;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String auditId;
    private String tenantId;
    private String businessId;
    private String businessName; // dynamically fetched from businessId
    private String group;
    private String component;
    private String actionType;
    private String initiator;
    private String status;
    private String timestamp;

    private Map<String, Object> actor;
    private Map<String, Object> resource;
    private Map<String, Object> context;
    private Map<String, Object> extra;

    private String payloadHash;
    private String encryptedReferenceId;
    private String currentChainHash;
}
