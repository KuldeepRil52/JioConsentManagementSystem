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
import com.jio.partnerportal.dto.ScopeLevel;
import com.jio.partnerportal.dto.Status;
import com.jio.partnerportal.dto.request.DigiLockerCredentialRequest;
import com.jio.partnerportal.dto.response.SearchResponse;
import com.jio.partnerportal.entity.DigiLockerCredential;
import com.jio.partnerportal.exception.PartnerPortalException;
import com.jio.partnerportal.repository.DigiLockerCredentialRepository;
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
public class DigiLockerCredentialService {

    private DigiLockerCredentialRepository digiLockerCredentialRepository;
    private Utils utils;
    private AuditManager auditManager;

    public DigiLockerCredentialService(DigiLockerCredentialRepository digiLockerCredentialRepository, Utils utils, AuditManager auditManager) {
        this.digiLockerCredentialRepository = digiLockerCredentialRepository;
        this.utils = utils;
        this.auditManager = auditManager;
    }

    @Value("${digilocker.credential.search.parameters}")
    List<String> digiLockerCredentialSearchParams;

    public DigiLockerCredential createCredential(DigiLockerCredentialRequest request, Map<String, String> headers, HttpServletRequest req) throws PartnerPortalException {
        String activity = "Create DigiLocker credential";

        String scopeLevel = headers.get(Constants.SCOPE_LEVEL_HEADER);
        String businessId = headers.get(Constants.BUSINESS_ID_HEADER);
        
        if (ScopeLevel.TENANT.toString().equals(scopeLevel) && this.digiLockerCredentialRepository.existByScopeLevel(scopeLevel)) {
            throw new PartnerPortalException(ErrorCodes.JCMP3002);
        }

        if(this.digiLockerCredentialRepository.existByScopeLevelAndBusinessId(scopeLevel, businessId)) {
            throw new PartnerPortalException(ErrorCodes.JCMP3003);
        }
        
        String credentialId = UUID.randomUUID().toString();
        DigiLockerCredential credential = DigiLockerCredential.builder()
                .credentialId(credentialId)
                .clientId(request.getClientId())
                .clientSecret(request.getClientSecret())
                .redirectUri(request.getRedirectUri())
                .codeVerifier(request.getCodeVerifier())
                .status(Status.ACTIVE.toString())
                .businessId(businessId)
                .scopeLevel(scopeLevel)
                .build();

        this.logDigiLockerCredentialAudit(credential, ActionType.CREATE);
        LogUtil.logActivity(req, activity, "Success: Create DigiLocker credential successfully");
        return this.digiLockerCredentialRepository.save(credential);
    }

    public DigiLockerCredential updateCredential(DigiLockerCredentialRequest request, Map<String, String> headers, String credentialId, HttpServletRequest req) throws PartnerPortalException {
        String activity = "Update DigiLocker credentials";

        DigiLockerCredential credential = this.digiLockerCredentialRepository.findByCredentialId(credentialId);
        if(ObjectUtils.isEmpty(credential)) {
            throw new PartnerPortalException(ErrorCodes.JCMP3001);
        }

        credential.setClientId(request.getClientId());
        credential.setClientSecret(request.getClientSecret());
        credential.setRedirectUri(request.getRedirectUri());
        credential.setCodeVerifier(request.getCodeVerifier());

        this.logDigiLockerCredentialAudit(credential, ActionType.UPDATE);
        LogUtil.logActivity(req, activity, "Success: Update Digilocker credentials successfully");
        return this.digiLockerCredentialRepository.save(credential);
    }

    public SearchResponse<DigiLockerCredential> search(Map<String, String> reqParams, HttpServletRequest req) throws PartnerPortalException {
        String activity = "Search DigiLocker credentials";

        LogUtil.logActivity(req, activity, "Success: Search DigiLocker credentials successfully");
        Map<String, String> searchParams = this.utils.filterRequestParam(reqParams, digiLockerCredentialSearchParams);
        List<DigiLockerCredential> mongoResponse = this.digiLockerCredentialRepository.findCredentialByParams(searchParams);

        if (ObjectUtils.isEmpty(mongoResponse)) {
            throw new PartnerPortalException(ErrorCodes.JCMP3001);
        }

        return SearchResponse.<DigiLockerCredential>builder()
                .searchList(mongoResponse)
                .build();
    }

    public long count() {
        return this.digiLockerCredentialRepository.count();
    }

    /**
     * Modular function to log digiLockerCredential audit events
     * Can be used in both create and update digiLockerCredential flows
     *
     * @param digiLockerCredential The digiLockerCredential entity to audit
     * @param actionType The action type (CREATE, UPDATE)
     */
    public void logDigiLockerCredentialAudit(DigiLockerCredential digiLockerCredential, ActionType actionType) {
        try {
            Actor actor = Actor.builder()
                    .id(ThreadContext.get(Constants.USER_ID_THREAD_CONTEXT))
                    .role(Constants.USER)
                    .type(Constants.USER_ID_TYPE)
                    .build();

            Resource resource = Resource.builder()
                    .type(Constants.CREDENTIAL_ID)
                    .id(digiLockerCredential.getCredentialId())
                    .build();

            Context context = Context.builder()
                    .ipAddress(ThreadContext.get(Constants.SOURCE_IP) != null && !ThreadContext.get(Constants.SOURCE_IP).equals("-")
                            ? ThreadContext.get(Constants.SOURCE_IP) : null)
                    .txnId(ThreadContext.get(Constants.TXN_ID_THREAD_CONTEXT) != null && !ThreadContext.get(Constants.TXN_ID_THREAD_CONTEXT).equals("-") 
                            ? ThreadContext.get(Constants.TXN_ID_THREAD_CONTEXT) : null)
                    .build();

            Map<String, Object> extra = new HashMap<>();
            // Add digiLockerCredential POJO in the extra field under the "data" key
            extra.put(Constants.DATA, digiLockerCredential);

            AuditRequest auditRequest = AuditRequest.builder()
                    .actor(actor)
                    .businessId(digiLockerCredential.getBusinessId())
                    .group(Constants.PARTNER_PORTAL_GROUP)
                    .component(AuditComponent.DIGILOCKER_CREDENTIAL)
                    .actionType(actionType)
                    .resource(resource)
                    .initiator(Constants.DATA_FIDUCIARY)
                    .context(context)
                    .extra(extra)
                    .build();

            String tenantId = ThreadContext.get(Constants.TENANT_ID_HEADER);
            this.auditManager.logAudit(auditRequest, tenantId);
        } catch (Exception e) {
            log.error("Audit logging failed for digiLocker credential id: {}, action: {}, error: {}", 
                    digiLockerCredential.getCredentialId(), actionType, e.getMessage(), e);
        }
    }
}
