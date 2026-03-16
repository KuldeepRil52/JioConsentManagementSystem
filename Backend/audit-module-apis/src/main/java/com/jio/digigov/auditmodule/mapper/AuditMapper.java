package com.jio.digigov.auditmodule.mapper;

import com.jio.digigov.auditmodule.dto.AuditRequest;
import com.jio.digigov.auditmodule.dto.AuditResponse;
import com.jio.digigov.auditmodule.entity.AuditDocument;

import java.time.LocalDateTime;
import java.util.Map;

public class AuditMapper {

    private AuditMapper() {}

    /**
     * Convert AuditRequest DTO to MongoDB entity.
     */
    public static AuditDocument toEntity(AuditRequest req, String tenantId, String businessId, String auditId) {
        AuditDocument auditDocument = new AuditDocument();

        auditDocument.setTenantId(tenantId);
        auditDocument.setBusinessId(businessId);
        auditDocument.setAuditId(auditId);
        auditDocument.setCreatedAt(LocalDateTime.now());
//        auditDocument.setUpdatedAt(LocalDateTime.now());
        auditDocument.setGroup(req.getGroup());
        auditDocument.setComponent(req.getComponent());
        auditDocument.setActionType(req.getActionType());
        auditDocument.setInitiator(req.getInitiator());
        auditDocument.setActor(req.getActor() != null ? Map.of(
                "id", req.getActor().getId(),
                "role", req.getActor().getRole(),
                "type", req.getActor().getType()
        ) : null);
        auditDocument.setResource(req.getResource() != null ? Map.of(
                "type", req.getResource().getType(),
                "id", req.getResource().getId()
        ) : null);
        auditDocument.setStatus("SUCCESS");
        auditDocument.setContext(req.getContext());
        auditDocument.setExtra(req.getExtra());

        return auditDocument;
    }

    /**
     * Convert AuditDocument entity to AuditResponse DTO.
     */
    public static AuditResponse toResponse(AuditDocument doc, String message, String status) {
        return AuditResponse.builder()
                .id(doc.getAuditId())
                .status(status)
                .message(message)
                .build();
    }
}