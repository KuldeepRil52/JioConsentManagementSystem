package com.jio.partnerportal.service;

import com.jio.partnerportal.client.audit.AuditManager;
import com.jio.partnerportal.client.audit.request.Actor;
import com.jio.partnerportal.client.audit.request.AuditRequest;
import com.jio.partnerportal.client.audit.request.Context;
import com.jio.partnerportal.client.audit.request.Resource;
import com.jio.partnerportal.constant.Constants;
import com.jio.partnerportal.constant.ErrorCodes;
import com.jio.partnerportal.dto.ActionType;
import com.jio.partnerportal.dto.AuditComponent;
import com.jio.partnerportal.dto.request.RetentionRequest;
import com.jio.partnerportal.dto.response.RetentionResponse;
import com.jio.partnerportal.entity.RetentionConfig;
import com.jio.partnerportal.multitenancy.TenantMongoTemplateProvider;
import com.jio.partnerportal.util.LogUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.ThreadContext;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class RetentionConfigService {

    private final TenantMongoTemplateProvider tenantMongoTemplateProvider;
    private final AuditManager auditManager;

    /**
     * Create retention config
     */

    public RetentionResponse createRetentionConfig(
            String txn,
            String tenantId,
            String businessId,
            String sessionToken,
            RetentionRequest request,
            HttpServletRequest req) {

        final String activity = "Create Retention Config";

        // === Validate input ===
        if (request == null || request.getRetentions() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ErrorCodes.JCMP3053);
        }

        MongoTemplate tenantDb =
                tenantMongoTemplateProvider.getMongoTemplate(ThreadContext.get(Constants.TENANT_ID_HEADER));

        validateBusinessIdExists(businessId, tenantDb);
        // === Build entity ===
        RetentionRequest.Retentions reqR = request.getRetentions();
        RetentionConfig config = new RetentionConfig();
        config.setRetentionId(UUID.randomUUID().toString());
        config.setBusinessId(businessId);

        RetentionConfig.Retentions entityRet = new RetentionConfig.Retentions();
        entityRet.setConsent_artifact_retention(toEntityValue(reqR.getConsent_artifact_retention()));
        entityRet.setCookie_consent_artifact_retention(toEntityValue(reqR.getCookie_consent_artifact_retention()));
        entityRet.setGrievance_retention(toEntityValue(reqR.getGrievance_retention()));
        entityRet.setLogs_retention(toEntityValue(reqR.getLogs_retention()));
        entityRet.setData_retention(toEntityValue(reqR.getData_retention()));

        config.setRetentions(entityRet);

        // === Save ===
        tenantDb.save(config, Constants.RETENTION_CONFIG);

        logRetentionAudit(config, ActionType.CREATE, config.getRetentionId());
        LogUtil.logActivity(req, activity, "Success: Retention config created successfully");

        // === Build response ===
        return toResponse(config);
    }


    public RetentionResponse updateRetentionConfig(
            String retentionId,
            String txn,
            String tenantId,
            String businessId,
            String sessionToken,
            RetentionRequest request,
            HttpServletRequest req) {

        final String activity = "Update Retention Config";

        // === Validate input ===
        if (!StringUtils.hasText(retentionId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ErrorCodes.JCMP3073); // retentionId missing
        }
        if (request == null || request.getRetentions() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ErrorCodes.JCMP3072); // invalid body
        }

        MongoTemplate tenantDb =
                tenantMongoTemplateProvider.getMongoTemplate(ThreadContext.get(Constants.TENANT_ID_HEADER));

        validateBusinessIdExists(businessId, tenantDb);
        // === Find existing config ===
        Query query = new Query(Criteria.where("retentionId").is(retentionId).and("businessId").is(businessId));

        RetentionConfig existing = tenantDb.findOne(query, RetentionConfig.class, Constants.RETENTION_CONFIG);

        if (existing == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, ErrorCodes.JCMP3074); // Not found
        }

        // === Update fields ===
        RetentionRequest.Retentions reqR = request.getRetentions();
        RetentionConfig.Retentions entityRet = new RetentionConfig.Retentions();

        entityRet.setConsent_artifact_retention(toEntityValue(reqR.getConsent_artifact_retention()));
        entityRet.setCookie_consent_artifact_retention(toEntityValue(reqR.getCookie_consent_artifact_retention()));
        entityRet.setGrievance_retention(toEntityValue(reqR.getGrievance_retention()));
        entityRet.setLogs_retention(toEntityValue(reqR.getLogs_retention()));
        entityRet.setData_retention(toEntityValue(reqR.getData_retention()));

        existing.setRetentions(entityRet);
        existing.setUpdatedAt(LocalDateTime.now());

        // === Save back ===
        tenantDb.save(existing, Constants.RETENTION_CONFIG);

        logRetentionAudit(existing, ActionType.UPDATE, existing.getRetentionId());
        LogUtil.logActivity(req, activity, "Success: Retention config updated successfully");

        return toResponse(existing);
    }

    public RetentionResponse searchRetentionConfig(
            String txn,
            String tenantId,
            String businessId,
            String sessionToken,
            String retentionId,
            HttpServletRequest req
    ) {
        final String activity = "Search Retention Config";

        MongoTemplate tenantDb =
                tenantMongoTemplateProvider.getMongoTemplate(ThreadContext.get(Constants.TENANT_ID_HEADER));

        validateBusinessIdExists(businessId, tenantDb);
        // ==== Fetch by ID or Business ID ====
        Query query = new Query();
        if (StringUtils.hasText(retentionId)) {
            query.addCriteria(Criteria.where("retentionId").is(retentionId));
        } else {
            query.addCriteria(Criteria.where("businessId").is(businessId));
        }

        RetentionConfig config = tenantDb.findOne(query, RetentionConfig.class, Constants.RETENTION_CONFIG);

        if (config == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, ErrorCodes.JCMP3074);
        }

        // Log activity
        LogUtil.logActivity(req, activity, "Success: Retention config fetched successfully");

        // === Build and return response ===
        RetentionResponse response = new RetentionResponse();
        response.setRetentionId(config.getRetentionId());
        response.setBusinessId(config.getBusinessId());
        response.setRetentions(config.getRetentions());
        response.setCreatedAt(config.getCreatedAt() != null ? config.getCreatedAt().toString() : null);
        response.setUpdatedAt(config.getUpdatedAt() != null ? config.getUpdatedAt().toString() : null);

        return response;
    }


// ---------- Private inline converters ----------

    private RetentionConfig.RetentionValue toEntityValue(RetentionRequest.RetentionItem v) {
        if (v == null) return null;
        RetentionConfig.RetentionValue val = new RetentionConfig.RetentionValue();
        val.setValue(v.getValue());
        val.setUnit(v.getUnit());
        return val;
    }


    private RetentionResponse toResponse(RetentionConfig config) {
        RetentionResponse response = new RetentionResponse();
        response.setRetentionId(config.getRetentionId());
        response.setBusinessId(config.getBusinessId());
        response.setRetentions(config.getRetentions());
        response.setCreatedAt(config.getCreatedAt() != null ? config.getCreatedAt().toString() : null);
        response.setUpdatedAt(config.getUpdatedAt() != null ? config.getUpdatedAt().toString() : null);
        return response;
    }


    public Map<String, Object> deleteRetentionConfig(
            String txn,
            String tenantId,
            String sessionToken,
            String retentionId,
            HttpServletRequest req
    ) {
        final String activity = "Delete Retention Config";
        Map<String, Object> response = new HashMap<>();

        MongoTemplate tenantDb =
                tenantMongoTemplateProvider.getMongoTemplate(ThreadContext.get(Constants.TENANT_ID_HEADER));

        if (!StringUtils.hasText(retentionId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ErrorCodes.JCMP3074);
        }

        Query query = new Query(Criteria.where("retentionId").is(retentionId));
        RetentionConfig existing = tenantDb.findOne(query, RetentionConfig.class, Constants.RETENTION_CONFIG);

        if (existing == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, ErrorCodes.JCMP3074);
        }

        tenantDb.remove(query, Constants.RETENTION_CONFIG);

        logRetentionAudit(existing, ActionType.DELETE, retentionId);
        LogUtil.logActivity(req, activity, "Success: Retention config deleted successfully");

        response.put("message", "Retention config deleted successfully");
        return response;
    }

    private void validateBusinessIdExists(String businessId, MongoTemplate tenantDb) {
        if (!StringUtils.hasText(businessId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ErrorCodes.JCMP3075);
        }

        Query query = new Query(Criteria.where("businessId").is(businessId));
        boolean exists = tenantDb.exists(query, "business_applications");

        if (!exists) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, ErrorCodes.JCMP3039);
        }
    }


    /**
     * Audit function same as your retention audit
     */
    public void logRetentionAudit(Object response, ActionType actionType, String retentionId) {
        try {
            String tenantId = ThreadContext.get(Constants.TENANT_ID_HEADER);
            Actor actor = Actor.builder()
                    .id(ThreadContext.get(Constants.USER_ID_THREAD_CONTEXT))
                    .role(Constants.USER)
                    .type(Constants.USER_ID_TYPE)
                    .build();

            Resource resource = Resource.builder()
                    .type("RETENTION_ID")
                    .id(retentionId)
                    .build();

            Context context = Context.builder()
                    .ipAddress(ThreadContext.get(Constants.SOURCE_IP))
                    .txnId(ThreadContext.get(Constants.TXN_ID_THREAD_CONTEXT))
                    .build();

            Map<String, Object> extra = new HashMap<>();
            extra.put(Constants.DATA, response);

            AuditRequest auditRequest = AuditRequest.builder()
                    .actor(actor)
                    .businessId(tenantId)
                    .group(Constants.PARTNER_PORTAL_GROUP)
                    .component(AuditComponent.RETENTION_CONFIG)
                    .actionType(actionType)
                    .resource(resource)
                    .initiator(Constants.DATA_FIDUCIARY)
                    .context(context)
                    .extra(extra)
                    .build();

            this.auditManager.logAudit(auditRequest, tenantId);

        } catch (Exception e) {
            log.error("Audit logging failed for retentionId {}, action {}, err {}", retentionId, actionType, e.getMessage(), e);
        }
    }
}
