package com.jio.digigov.auditmodule.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "audit")
public class AuditDocument extends BaseEntity {

    @Indexed(unique = true)
    private String auditId;

    @Transient
    private String tenantId;
    
    private String businessId;
    private String group;
    private String component;
    private String actionType;
    private String initiator;
    private String status;

    private Map<String, Object> actor;
    private Map<String, Object> resource;
    private Map<String, Object> context;
    private Map<String, Object> extra;

    private String payloadHash;
    private String encryptedReferenceId;

    // Blockchain-style chain fields
    private String currentChainHash;
}