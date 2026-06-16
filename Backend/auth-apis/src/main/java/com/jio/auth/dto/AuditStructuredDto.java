package com.jio.auth.dto;

import lombok.Data;

@Data
public class AuditStructuredDto {

    private String tenantId;
    private String businessId;
    private String group;      // always "AUTH-HANDLER"
    private String component;  // null allowed
    private String action;     // <-- NEW: action = status from AuditDto
    private Context context;
    private Resource resource;
    private Actor actor;

    @Data
    public static class Actor {
        private String id;
        private String role;
        private String type;
    }

    @Data
    public static class Context {
        private String txnId;
        private String ipAddress;
    }

    @Data
    public static class Resource {
        private String id;      // transactionId
        private String type;    // "POST: /oauth2/token"
    }

}
