package com.jio.digigov.auditmodule.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.Map;

/**
 * DTO for capturing incoming audit event details.
 * Updated for blockchain-style chain generation and canonical serialization.
 */
@Data
@Builder
public class AuditRequest {

    private String auditId;

    private String tenantId;
    @NotNull(message = "businessId is required")
    private String businessId;
    private String transactionId;

    @NotNull(message = "actor is required")
    private Actor actor;
    @NotNull(message = "group is required")
    private String group;
    @NotNull(message = "component is required")
    private String component;
    @NotNull(message = "actionType is required")
    private String actionType;
    @NotNull(message = "initiator is required")
    private String initiator;
    @NotNull(message = "resource is required")
    private Resource resource;
    private String payloadHash;
    @NotNull(message = "context is required")
    private Map<String, Object> context;
    private Map<String, Object> extra;

    // Blockchain related fields (auto-populated by service)
    private String previousChainHash;
    private String currentChainHash;

    @Data
    public static class Actor {
        private String id;
        private String role;
        private String type;
    }

    @Data
    public static class Resource {
        private String type;
        private String id;
    }
}