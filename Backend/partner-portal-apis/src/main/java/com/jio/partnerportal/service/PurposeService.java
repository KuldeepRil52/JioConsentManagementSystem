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
import com.jio.partnerportal.dto.Status;
import com.jio.partnerportal.dto.request.PurposeRequest;
import com.jio.partnerportal.dto.response.SearchResponse;
import com.jio.partnerportal.entity.Purpose;
import com.jio.partnerportal.exception.PartnerPortalException;
import com.jio.partnerportal.repository.PurposeRepository;
import com.jio.partnerportal.util.LogUtil;
import com.jio.partnerportal.util.Utils;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.ThreadContext;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
public class PurposeService {

    private PurposeRepository purposeRepository;
    private Utils utils;
    AuditManager auditManager;

    public PurposeService(PurposeRepository purposeRepository, Utils utils, AuditManager auditManager) {
        this.purposeRepository = purposeRepository;
        this.utils = utils;
        this.auditManager = auditManager;
    }

    @Value("${purpose.search.parameters}")
    List<String> purposeSearchParams;

    public Purpose createPurpose(PurposeRequest request, Map<String, String> headers, HttpServletRequest req) throws PartnerPortalException {
        String activity = "Create purpose";

        // Check if purpose with the same purposeCode already exists
        Purpose existingPurpose = this.purposeRepository.findByPurposeCode(request.getPurposeCode());
        if (existingPurpose != null) {
            throw new PartnerPortalException(ErrorCodes.JCMP3061);
        }

        // Check if purpose with the same name already exists
        if (this.purposeRepository.existsByPurposeName(request.getPurposeName())) {
            throw new PartnerPortalException(ErrorCodes.JCMP3068);
        }

        String purposeId = UUID.randomUUID().toString();
        Purpose purpose = Purpose.builder().purposeCode(request.getPurposeCode())
                .purposeId(purposeId)
                .purposeName(request.getPurposeName())
                .status(Status.ACTIVE.toString())
                .purposeDescription(request.getPurposeDescription())
                .businessId(headers.get(Constants.BUSINESS_ID_HEADER))
                .scopeType(headers.get(Constants.SCOPE_LEVEL_HEADER))
                .build();

        this.logPurposeAudit(purpose, ActionType.CREATE);
        LogUtil.logActivity(req, activity, "Success: Create purpose successfully");
        return this.purposeRepository.save(purpose);
    }

    public Purpose updatePurpose(PurposeRequest request, Map<String, String> headers, String purposeId, HttpServletRequest req) throws PartnerPortalException {
        String activity = "Update purpose";
        Purpose purpose = this.purposeRepository.findByPurposeId(purposeId);
        if(ObjectUtils.isEmpty(purpose)) {
            throw new PartnerPortalException(ErrorCodes.JCMP3001);
        }

        // Check if another purpose with the same name already exists (excluding current purpose)
        if (this.purposeRepository.existsByPurposeNameExcludingPurposeId(request.getPurposeName(), purposeId)) {
            throw new PartnerPortalException(ErrorCodes.JCMP3069);
        }

        purpose.setPurposeName(request.getPurposeName());
        purpose.setPurposeDescription(request.getPurposeDescription());

        this.logPurposeAudit(purpose, ActionType.UPDATE);
        LogUtil.logActivity(req, activity, "Success: Update purpose successfully");
        return this.purposeRepository.save(purpose);
    }

    public SearchResponse<Purpose> search(Map<String, String> reqParams, HttpServletRequest req) throws PartnerPortalException {
        String activity = "Search purpose";

        Map<String, String> searchParams = this.utils.filterRequestParam(reqParams, purposeSearchParams);
        List<Purpose> mongoResponse = this.purposeRepository.findPurposeByParams(searchParams);

        if (ObjectUtils.isEmpty(mongoResponse)) {
            throw new PartnerPortalException(ErrorCodes.JCMP3001);
        }

        LogUtil.logActivity(req, activity, "Success: Search purpose successfully");
        return SearchResponse.<Purpose>builder()
                .searchList(mongoResponse)
                .build();
    }

    public long count() {
        return this.purposeRepository.count();
    }

    /**
     * Modular function to log purpose audit events
     * Can be used in both create and update purpose flows
     *
     * @param purpose The purpose entity to audit
     * @param actionType The action type (CREATE, UPDATE)
     */
    public void logPurposeAudit(Purpose purpose, ActionType actionType) {
        try {
            Actor actor = Actor.builder()
                    .id(ThreadContext.get(Constants.USER_ID_THREAD_CONTEXT))
                    .role(Constants.USER)
                    .type(Constants.USER_ID_TYPE)
                    .build();

            Resource resource = Resource.builder()
                    .type(Constants.PURPOSE_ID)
                    .id(purpose.getPurposeId())
                    .build();

            Context context = Context.builder()
                    .ipAddress(ThreadContext.get(Constants.SOURCE_IP) != null && !ThreadContext.get(Constants.SOURCE_IP).equals("-")
                            ? ThreadContext.get(Constants.SOURCE_IP) : null)
                    .txnId(ThreadContext.get(Constants.TXN_ID_THREAD_CONTEXT) != null && !ThreadContext.get(Constants.TXN_ID_THREAD_CONTEXT).equals("-") 
                            ? ThreadContext.get(Constants.TXN_ID_THREAD_CONTEXT) : null)
                    .build();

            Map<String, Object> extra = new HashMap<>();
            // Add purpose POJO in the extra field under the "data" key
            extra.put(Constants.DATA, purpose);

            AuditRequest auditRequest = AuditRequest.builder()
                    .actor(actor)
                    .businessId(purpose.getBusinessId())
                    .group(Constants.PARTNER_PORTAL_GROUP)
                    .component(AuditComponent.PURPOSE)
                    .actionType(actionType)
                    .resource(resource)
                    .initiator(Constants.DATA_FIDUCIARY)
                    .context(context)
                    .extra(extra)
                    .build();

            String tenantId = ThreadContext.get(Constants.TENANT_ID_HEADER);
            this.auditManager.logAudit(auditRequest, tenantId);
        } catch (Exception e) {
            log.error("Audit logging failed for purpose id: {}, action: {}, error: {}", 
                    purpose.getPurposeId(), actionType, e.getMessage(), e);
        }
    }
}