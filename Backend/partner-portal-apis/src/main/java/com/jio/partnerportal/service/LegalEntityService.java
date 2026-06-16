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
import com.jio.partnerportal.dto.request.LegalEntityUpdateRequest;
import com.jio.partnerportal.dto.response.SearchResponse;
import com.jio.partnerportal.entity.LegalEntity;
import com.jio.partnerportal.exception.PartnerPortalException;
import com.jio.partnerportal.multitenancy.TenantMongoTemplateProvider;
import com.jio.partnerportal.repository.LegalEntityRepository;
import com.jio.partnerportal.util.LogUtil;
import com.jio.partnerportal.util.Utils;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.ThreadContext;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class LegalEntityService {

    LegalEntityRepository legalEntityRepository;
    Utils utils;
    TenantMongoTemplateProvider tenantMongoTemplateProvider;
    AuditManager auditManager;

    @Autowired
    public LegalEntityService(LegalEntityRepository legalEntityRepository, Utils utils, TenantMongoTemplateProvider tenantMongoTemplateProvider, AuditManager auditManager) {
        this.legalEntityRepository = legalEntityRepository;
        this.utils = utils;
        this.tenantMongoTemplateProvider = tenantMongoTemplateProvider;
        this.auditManager = auditManager;
    }

    @Value("${legal-entity.search.parameters}")
    List<String> legalEntitySearchParams;

    public void updateLegalEntity(LegalEntityUpdateRequest request, HttpServletRequest req) throws PartnerPortalException {

        String activity = "Update legal entity";

        LegalEntity currentEntity = this.legalEntityRepository.findLegalEntityByLegalEntityId(request.getLegalEntityId());
        if (ObjectUtils.isEmpty(currentEntity)) {
            throw new PartnerPortalException(ErrorCodes.JCMP3001);
        }

        currentEntity.setCompanyName(request.getCompanyName());
        currentEntity.setLogoUrl(request.getLogoUrl());

        this.logLegalEntityAudit(currentEntity, ActionType.UPDATE);
        LogUtil.logActivity(req, activity, "Success: Update legal entity successfully");
        this.legalEntityRepository.updateLegalEntity(currentEntity);
    }

    public SearchResponse<LegalEntity> search(Map<String, String> reqParams, HttpServletRequest req) throws PartnerPortalException {
        String activity = "Search legal entity";

        Map<String, String> searchParams = this.utils.filterRequestParam(reqParams, legalEntitySearchParams);
        List<LegalEntity> mongoResponse = this.legalEntityRepository.findLegalEntitiesByParams(searchParams);

        if (ObjectUtils.isEmpty(mongoResponse)) {
            throw new PartnerPortalException(ErrorCodes.JCMP3001);
        }

        LogUtil.logActivity(req, activity, "Success: Search legal entity successfully");
        return SearchResponse.<LegalEntity>builder()
                .searchList(mongoResponse)
                .build();
    }

    public long count() {
        return this.legalEntityRepository.count();
    }

    /**
     * Modular function to log legalEntity audit events
     * Can be used in both create and update legalEntity flows
     *
     * @param legalEntity The legalEntity entity to audit
     * @param actionType The action type (UPDATE)
     */
    public void logLegalEntityAudit(LegalEntity legalEntity, ActionType actionType) {
        try {
            String tenantId = ThreadContext.get(Constants.TENANT_ID_HEADER);
            Actor actor = Actor.builder()
                    .id(ThreadContext.get(Constants.USER_ID_THREAD_CONTEXT))
                    .role(Constants.USER)
                    .type(Constants.USER_ID_TYPE)
                    .build();

            Resource resource = Resource.builder()
                    .type(Constants.LEGAL_ENTITY_ID)
                    .id(legalEntity.getLegalEntityId())
                    .build();

            Context context = Context.builder()
                    .ipAddress(ThreadContext.get(Constants.SOURCE_IP) != null && !ThreadContext.get(Constants.SOURCE_IP).equals("-")
                            ? ThreadContext.get(Constants.SOURCE_IP) : null)
                    .txnId(ThreadContext.get(Constants.TXN_ID_THREAD_CONTEXT) != null && !ThreadContext.get(Constants.TXN_ID_THREAD_CONTEXT).equals("-") 
                            ? ThreadContext.get(Constants.TXN_ID_THREAD_CONTEXT) : null)
                    .build();

            Map<String, Object> extra = new HashMap<>();
            // Add legalEntity POJO in the extra field under the "data" key
            extra.put(Constants.DATA, legalEntity);

            AuditRequest auditRequest = AuditRequest.builder()
                    .actor(actor)
                    .businessId(tenantId)
                    .group(Constants.PARTNER_PORTAL_GROUP)
                    .component(AuditComponent.LEGAL_ENTITY)
                    .actionType(actionType)
                    .resource(resource)
                    .initiator(Constants.DATA_FIDUCIARY)
                    .context(context)
                    .extra(extra)
                    .build();

            this.auditManager.logAudit(auditRequest, tenantId);
        } catch (Exception e) {
            log.error("Audit logging failed for legal entity id: {}, action: {}, error: {}", 
                    legalEntity.getLegalEntityId(), actionType, e.getMessage(), e);
        }
    }
}