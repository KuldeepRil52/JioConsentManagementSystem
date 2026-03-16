package com.jio.schedular.client.audit;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jio.schedular.client.audit.request.AuditRequest;
import com.jio.schedular.client.audit.response.AuditResponse;
import com.jio.schedular.constant.Constants;
import com.jio.schedular.enums.AuditMetaStatus;
import com.jio.schedular.entity.AuditMeta;
import com.jio.schedular.repository.AuditMetaRepository;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.ThreadContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Component
@Slf4j
public class AuditManagerImpl implements AuditManager {

    private final AuditApiManager apiManager;
    private final AuditMetaRepository auditMetaRepository;

    @Autowired
    public AuditManagerImpl(AuditApiManager apiManager, AuditMetaRepository auditMetaRepository) {
        this.apiManager = apiManager;
        this.auditMetaRepository = auditMetaRepository;
    }

    @Override
    public void sendAudit(Map<String, Object> auditDto, String tenantId, String businessId) {
        try {
            Map<String, String> headers = new HashMap<>();
            if (tenantId != null) headers.put("X-Tenant-Id", tenantId);
            if (businessId != null) headers.put("X-Business-Id", businessId);
            headers.put("X-Transaction-Id", UUID.randomUUID().toString());
            headers.put("Content-Type", "application/json");

            ResponseEntity<String> resp = apiManager.posttriggerEvent(headers, auditDto, String.class);
            if (resp != null && resp.getStatusCode().is2xxSuccessful()) {
                log.debug("Audit posted successfully tenant={} business={} status={}", tenantId, businessId, resp.getStatusCode());
            } else {
                log.warn("Audit API returned non-2xx tenant={} business={} status={}", tenantId, businessId,
                        resp == null ? "null-response" : resp.getStatusCode());
            }
        } catch (Exception ex) {
            log.error("Failed to send audit (tenant={}, business={}): {}", tenantId, businessId, ex.getMessage(), ex);
        }
    }

    @Override
    @Async
    public void logAudit(AuditRequest auditRequest, String tenantId) {
        String txnId = ThreadContext.get(Constants.TXN_ID);
        
        if (tenantId == null || tenantId.equals("-") || tenantId.trim().isEmpty()) {
            log.warn("Tenant ID is null or empty, skipping audit log");
            return;
        }

        // Create AuditMeta entity with PENDING status
        String auditMetaId = UUID.randomUUID().toString();
        AuditMeta auditMeta = AuditMeta.builder()
                .auditMetaId(auditMetaId)
                .businessId(auditRequest.getBusinessId())
                .auditRequest(auditRequest)
                .status(AuditMetaStatus.PENDING)
                .build();

        // Set audit_meta in extra map before calling API
        Map<String, Object> extra = auditRequest.getExtra();
        if (extra == null) {
            extra = new HashMap<>();
            auditRequest.setExtra(extra);
        }
        Map<String, Object> auditMetaMap = new HashMap<>();
        auditMetaMap.put("auditMetaId", auditMetaId);
        extra.put("audit_meta", auditMetaMap);

        // Save AuditMeta with PENDING status
        this.auditMetaRepository.save(auditMeta, tenantId);

        Map<String, String> headers = new HashMap<>();
        headers.put("X-Tenant-ID", tenantId);
        headers.put("X-Transaction-ID", txnId != null && !txnId.equals("-") ? txnId : generateTransactionId());
        headers.put("Content-Type", MediaType.APPLICATION_JSON_VALUE);

        ObjectMapper objectMapper = new ObjectMapper();
        try {
            ResponseEntity<AuditResponse> response = apiManager.posttriggerEvent(headers, auditRequest, AuditResponse.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                auditMeta.setStatus(AuditMetaStatus.SUCCESS);
                auditMeta.setHttpStatus(response.getStatusCode().toString());
                auditMeta.setAuditResponse(response.getBody());
                auditMeta.setAuditId(response.getBody().getId());
                
                log.info("Audit log created successfully - Audit ID: {}, Status: {}", 
                        response.getBody().getId(), response.getBody().getStatus());
            } else {
                auditMeta.setStatus(AuditMetaStatus.FAILED);
                auditMeta.setHttpStatus(response.getStatusCode().toString());
                auditMeta.setErrorMessage(objectMapper.writeValueAsString(response.getBody()));
                
                log.error("Failed to create audit log - HTTP Status: {}, Response: {}", 
                        response.getStatusCode(), objectMapper.writeValueAsString(response.getBody()));
            }
        } catch (RestClientException | JsonProcessingException e) {
            auditMeta.setStatus(AuditMetaStatus.FAILED);
            auditMeta.setErrorMessage(e.getMessage());
            
            log.error("Exception occurred while creating audit log - Error: {}", e.getMessage(), e);
        } finally {
            // Save AuditMeta with updated status and response
            this.auditMetaRepository.save(auditMeta, tenantId);
        }
    }

    private String generateTransactionId() {
        return UUID.randomUUID().toString();
    }
}