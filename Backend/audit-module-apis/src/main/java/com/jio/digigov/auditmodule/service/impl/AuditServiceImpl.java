package com.jio.digigov.auditmodule.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jio.digigov.auditmodule.config.MultiTenantMongoConfig;
import com.jio.digigov.auditmodule.dto.*;
import com.jio.digigov.auditmodule.entity.AuditDocument;
import com.jio.digigov.auditmodule.mapper.AuditMapper;
import com.jio.digigov.auditmodule.service.AuditService;
import com.jio.digigov.auditmodule.util.AuditChainUtil;
import com.jio.digigov.auditmodule.util.AuditQueryUtils;
import com.jio.digigov.auditmodule.util.TenantContextHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.*;

/**
 * Multi-tenant implementation of {@link AuditService}.
 * Includes blockchain-style hash chain generation for tamper-proof audit logs.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuditServiceImpl implements AuditService {

    private final MultiTenantMongoConfig mongoConfig;
    private final ObjectMapper objectMapper;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final AuditChainUtil auditChainUtil;

    @Value("${kafka.topics.audit}")
    private String auditTopicName;

    @Override
    public AuditResponse createAudit(AuditRequest req, String tenantId, String transactionId) {
        log.info("[AUDIT-CREATE] Incoming request for tenantId={} businessId={} transactionId={}",
                tenantId, req.getBusinessId(), transactionId);

        String auditId = Optional.ofNullable(req.getAuditId()).orElse(UUID.randomUUID().toString());
        req.setAuditId(auditId);

        try {
            // Initialize MongoTemplate for the tenant
            MongoTemplate mongoTemplate = mongoConfig.getMongoTemplateForTenant(tenantId);

            // --- Step 1: Fetch the latest chain from DB ---
            String previousChain = AuditChainUtil.fetchLatestChain(mongoTemplate, AuditDocument.class);

            // --- Step 2: Map to Entity ---
            AuditDocument auditDoc = AuditMapper.toEntity(req, tenantId, req.getBusinessId(), auditId);

            // --- Step 3: Canonicalize + Encode + Hash ---
            this.auditChainUtil.populateChainFields(auditDoc, previousChain, objectMapper, tenantId);

            log.info("auditDoc after chain population: {}", auditDoc);
            // --- Step 4: Publish to Kafka ---
           try {
               kafkaTemplate.send(auditTopicName, auditId, auditDoc);
               log.info("[AUDIT-CREATE] Audit event published to Kafka successfully | auditId={}", auditId);
           } catch (Exception kafkaEx) {
               log.warn("[AUDIT-CREATE] Kafka publish failed | auditId={} reason={}", auditId, kafkaEx.getMessage());
           }

            return AuditResponse.builder()
                    .id(auditId)
                    .status("success")
                    .message("Audit created successfully")
                    .currentChainHash(auditDoc.getCurrentChainHash())
                    .encryptedReferenceId(auditDoc.getEncryptedReferenceId())
                    .build();

        } catch (DataAccessException dae) {
            log.error("[AUDIT-CREATE] Database error for tenantId={} auditId={} cause={}",
                    tenantId, auditId, dae.getMessage(), dae);
            return AuditResponse.builder()
                    .id(auditId)
                    .status("failure")
                    .message("Database error while creating audit: " + dae.getMessage())
                    .build();

        } catch (Exception ex) {
            log.error("[AUDIT-CREATE] Unexpected error for tenantId={} auditId={} error={}",
                    tenantId, auditId, ex.getMessage(), ex);
            return AuditResponse.builder()
                    .id(auditId)
                    .status("failure")
                    .message("Unexpected error while creating audit: " + ex.getMessage())
                    .build();
        }
    }

    /**
     * Fetch audits with filters + pagination.
     */
    @Override
    public PagedResponse<AuditRecordResponse> getAuditsPaged(Map<String, String> params, String businessId, Integer page, Integer size, String sort) {
        String tenantId = params.get("tenantId");

        AuditQueryUtils.validateBusinessId(businessId);
        TenantContextHolder.setTenant(tenantId);
        TenantContextHolder.setBusinessId(businessId);

        // If tenantId and businessId are same → search in all records for that tenant (ignore only businessId filter)
        if (!tenantId.equals(businessId)) {
            log.info("[AUDIT-FETCH] tenantId {} and businessId {} are not same, adding businessId filter", tenantId, businessId);
            params.put("businessId", businessId);
        }

        String tempBusinessName = businessId;
        try {
            Query query = AuditQueryUtils.buildQuery(params);
            MongoTemplate tenantMongoTemplate = mongoConfig.getMongoTemplateForTenant(tenantId);

            // --- Fetch business name from master DB ---
            try {
                Query businessQuery = new Query();
                businessQuery.addCriteria(Criteria.where("businessId").is(businessId));

                BusinessApplicationDto businessApplication = tenantMongoTemplate.findOne(
                        businessQuery,
                        BusinessApplicationDto.class,
                        "business_applications"
                );

                if (businessApplication != null && businessApplication.getName() != null) {
                    tempBusinessName = businessApplication.getName();
                }

                log.info("BusinessApplication found for businessId={} is : {}", businessId, businessApplication);
            } catch (Exception ex) {
                log.warn("[AUDIT-FETCH] Could not fetch businessName for businessId={} reason={}", businessId, ex.getMessage());
            }

            //  Make variable final for use in lambda
            final String businessName = tempBusinessName;

            // --- Sorting ---
            Sort.Direction direction = "desc".equalsIgnoreCase(sort)
                    ? Sort.Direction.DESC
                    : Sort.Direction.ASC;
            query.with(Sort.by(direction, "createdAt"));

            // --- Total count ---
            long total = tenantMongoTemplate.count(query, AuditDocument.class);

            List<AuditDocument> auditDocs;
            PagedResponse.Pagination pagination;

            if (size != null && size > 0) {
                int currentPage = (page != null && page >= 0) ? page : 0;
                query.skip((long) currentPage * size).limit(size);
                auditDocs = tenantMongoTemplate.find(query, AuditDocument.class);
                pagination = new PagedResponse.Pagination(
                        currentPage,
                        size,
                        total,
                        (int) Math.ceil((double) total / size),
                        (currentPage + 1) * size >= total
                );
            } else {
                auditDocs = tenantMongoTemplate.find(query, AuditDocument.class);
                pagination = new PagedResponse.Pagination(0, auditDocs.size(), auditDocs.size(), 1, true);
            }

            // --- Convert to DTOs (all fields mapped) ---
            List<AuditRecordResponse> responseList = auditDocs.stream().map(doc -> {
                AuditRecordResponse dto = new AuditRecordResponse();
                dto.setId(doc.getId() != null ? doc.getId().toString() : null);
                dto.setCreatedAt(doc.getCreatedAt());
                dto.setAuditId(doc.getAuditId());
                dto.setTenantId(tenantId);
                dto.setBusinessId(doc.getBusinessId());
                dto.setBusinessName(businessName);
                dto.setGroup(doc.getGroup());
                dto.setComponent(doc.getComponent());
                dto.setActionType(doc.getActionType());
                dto.setInitiator(doc.getInitiator());
                dto.setStatus(doc.getStatus());
                dto.setActor(doc.getActor());
                dto.setResource(doc.getResource());
                dto.setContext(doc.getContext());
                dto.setExtra(doc.getExtra());
                dto.setPayloadHash(doc.getPayloadHash());
                dto.setEncryptedReferenceId(doc.getEncryptedReferenceId());
                dto.setCurrentChainHash(doc.getCurrentChainHash());
                return dto;
            }).toList();

            return new PagedResponse<>(responseList, pagination);

        } catch (Exception e) {
            log.error("[AUDIT-FETCH] Error fetching audits tenantId={} businessId={} error={}", tenantId, businessId, e.getMessage(), e);
            throw new RuntimeException("Failed to fetch audits: " + e.getMessage(), e);
        } finally {
            TenantContextHolder.clear();
        }
    }

    /**
     * Count audits with filters.
     */
    @Override
    public long countAudits(Map<String, String> params, String businessId) {
        String tenantId = params.get("tenantId");

        AuditQueryUtils.validateBusinessId(businessId);
        TenantContextHolder.setTenant(tenantId);
        TenantContextHolder.setBusinessId(businessId);

        // If tenantId and businessId are same → search in all records for that tenant (ignore only businessId filter)
        if (!tenantId.equals(businessId)) {
            log.info("[AUDIT-FETCH] tenantId {} and businessId {} are not same, adding businessId filter", tenantId, businessId);
            params.put("businessId", businessId);
        }

        try {
            Query query = AuditQueryUtils.buildQuery(params);
            MongoTemplate tenantMongoTemplate = mongoConfig.getMongoTemplateForTenant(tenantId);

            long count = tenantMongoTemplate.count(query, AuditDocument.class);
            log.info("[AUDIT-COUNT] Found {} audit records for tenantId={} businessId={}", count, tenantId, businessId);
            return count;

        } catch (Exception e) {
            log.error("[AUDIT-COUNT] Error counting audits tenantId={} businessId={} error={}",
                    tenantId, businessId, e.getMessage(), e);
            throw new RuntimeException("Failed to count audits: " + e.getMessage(), e);

        } finally {
            TenantContextHolder.clear();
        }
    }

    @Override
    public Map<String, Object> getAuditByReferenceId(String tenantId, String businessId, String referenceId) {

        log.info("[AUDIT-FETCH] Fetching audit for tenantId={} businessId={} referenceId={}",
                tenantId, businessId, referenceId);

        Map<String, Object> response = new HashMap<>();

        AuditQueryUtils.validateBusinessId(businessId);
        TenantContextHolder.setTenant(tenantId);
        TenantContextHolder.setBusinessId(businessId);

        try {
            MongoTemplate tenantMongoTemplate = mongoConfig.getMongoTemplateForTenant(tenantId);

            Query query = new Query();
            query.addCriteria(
                    Criteria.where("businessId").is(businessId)
                            .and("resource.id").is(referenceId)
            );

            query.with(Sort.by(Sort.Direction.DESC, "createdAt"));
            query.limit(1);

            AuditDocument audit = tenantMongoTemplate.findOne(query, AuditDocument.class);

            if (audit == null) {
                log.warn("[AUDIT-FETCH] No audit found for referenceId={}", referenceId);

                // Still return success with audit = null
                response.put("success", true);
                response.put("audit", null);
                response.put("message", "No audit found but continuing process");
                return response;
            }

            response.put("success", true);
            response.put("audit", audit);
            response.put("message", "Audit fetched successfully");
            return response;

        } catch (Exception e) {

            log.error("[AUDIT-FETCH] Error fetching audit tenantId={} businessId={} referenceId={} error={}",
                    tenantId, businessId, referenceId, e.getMessage(), e);

            response.put("success", false);
            response.put("error", e.getMessage());
            return response;

        } finally {
            TenantContextHolder.clear();
        }
    }
}