package com.example.scanner.dto.request;

import com.example.scanner.dto.Actor;
import com.example.scanner.dto.Resource;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditRequest {

    private String auditId;
    private String tenantId;
    private String businessId;
    private Actor actor;
    private String group;
    private String component;
    private String actionType;
    private String initiator;
    private Resource resource;
    private String payloadHash;
    private Map<String, Object> context;
    private Map<String, Object> extra;
    private String status;
    private String timestamp;
}