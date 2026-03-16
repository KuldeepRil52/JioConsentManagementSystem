package com.jio.auth.dto;

import lombok.Data;

@Data
public class AuditDto {
    private String txnId;
    private String tenantId;
    private String businessId;
    private String transactionId;
    private String ip;
    private String requestUri;
    private String method;
    private String requestPayload;
    private String responsePayload;
    private String actor;
}
