package com.jio.partnerportal.service;

import com.jio.partnerportal.client.audit.AuditManager;
import com.jio.partnerportal.client.audit.request.Actor;
import com.jio.partnerportal.client.audit.request.AuditRequest;
import com.jio.partnerportal.client.audit.request.Context;
import com.jio.partnerportal.client.audit.request.Resource;
import com.jio.partnerportal.client.wso2.Wso2Manager;
import com.jio.partnerportal.client.wso2.request.OnboardBusinessRequest;
import com.jio.partnerportal.client.wso2.response.OnboardBusinessResponse;
import com.jio.partnerportal.constant.Constants;
import com.jio.partnerportal.constant.ErrorCodes;
import com.jio.partnerportal.dto.ActionType;
import com.jio.partnerportal.dto.AuditComponent;
import com.jio.partnerportal.dto.ScopeLevel;
import com.jio.partnerportal.dto.request.BusinessApplicationRequest;
import com.jio.partnerportal.dto.response.SearchResponse;
import com.jio.partnerportal.entity.BusinessApplication;
import com.jio.partnerportal.entity.ClientCredentials;
import com.jio.partnerportal.exception.PartnerPortalException;
import com.jio.partnerportal.repository.BusinessApplicationRepository;
import com.jio.partnerportal.repository.ClientCredentialsRepository;
import com.jio.partnerportal.util.LogUtil;
import com.jio.partnerportal.util.Utils;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.logging.log4j.ThreadContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
public class BusinessApplicationService {

    BusinessApplicationRepository businessApplicationRepository;
    ClientCredentialsRepository clientCredentialsRepository;
    Wso2Manager wso2Manager;
    Utils utils;
    AuditManager auditManager;
    ConsentSigningKeyService consentSigningKeyService;

    @Autowired
    public BusinessApplicationService(BusinessApplicationRepository businessApplicationRepository,
                                      ClientCredentialsRepository clientCredentialsRepository,
                                      Wso2Manager wso2Manager,
                                      Utils utils, AuditManager auditManager,
                                      ConsentSigningKeyService consentSigningKeyService) {
        this.businessApplicationRepository = businessApplicationRepository;
        this.clientCredentialsRepository = clientCredentialsRepository;
        this.wso2Manager = wso2Manager;
        this.utils = utils;
        this.auditManager = auditManager;
        this.consentSigningKeyService = consentSigningKeyService;
    }

    @Value("${businessApplication.search.parameters}")
    List<String> businessApplicationSearchParams;

    public BusinessApplication createBusinessApplication(BusinessApplicationRequest request, Map<String, String> headers,HttpServletRequest req) throws PartnerPortalException {
        String activity = "Create Business Application";

        String scopeLevel = headers.get(Constants.SCOPE_LEVEL_HEADER);
        if (ScopeLevel.TENANT.toString().equals(scopeLevel) && this.businessApplicationRepository.existByScopeLevel(scopeLevel)) {
            throw new PartnerPortalException(ErrorCodes.JCMP3002);
        }

        // Check if business with the same name already exists
        BusinessApplication existingBusiness = this.businessApplicationRepository.findByName(request.getName());
        if (existingBusiness != null) {
            throw new PartnerPortalException(ErrorCodes.JCMP3059);
        }

        String businessId = UUID.randomUUID().toString();
        String tenantId = headers.get(Constants.TENANT_ID_HEADER);

        // Create and save business application
        BusinessApplication businessApplication = BusinessApplication.builder()
                .name(request.getName())
                .description(request.getDescription())
                .businessId(businessId)
                .scopeLevel(headers.get(Constants.SCOPE_LEVEL_HEADER))
                .build();

        BusinessApplication currentBusinessApplication = this.businessApplicationRepository.save(businessApplication);

        // Call WSO2 to onboard business
        OnboardBusinessRequest onboardBusinessRequest = OnboardBusinessRequest.builder()
                .tenantId(tenantId)
                .businessId(businessId)
                .businessName(request.getName())
                .build();

        OnboardBusinessResponse onboardBusinessResponse = wso2Manager.onboardBusiness(onboardBusinessRequest);

        if (onboardBusinessResponse == null || !onboardBusinessResponse.isSuccess() || onboardBusinessResponse.getData() == null) {
            throw new PartnerPortalException(ErrorCodes.JCMP3033);
        }

        // Create and save client credentials
        ClientCredentials clientCredentials = ClientCredentials.builder()
                .businessId(businessId)
                .tenantId(tenantId)
                .businessUniqueId(onboardBusinessResponse.getData().getBusinessUniqueId())
                .consumerKey(onboardBusinessResponse.getData().getConsumerKey())
                .consumerSecret(onboardBusinessResponse.getData().getConsumerSecret())
                .scopeLevel(scopeLevel)
                .status("ACTIVE")
                .build();

        this.clientCredentialsRepository.save(clientCredentials);

        // Generate consent signing key
        try {
            consentSigningKeyService.generateConsentSigningKey(tenantId, businessId, scopeLevel, "rsa-2048", req);
        } catch (PartnerPortalException e) {
            log.error("Failed to generate consent signing key for businessId: {}, error: {}", businessId, e.getMessage(), e);
            throw e;
        }

        this.logBusinessApplicationAudit(currentBusinessApplication, ActionType.CREATE);
        LogUtil.logActivity(req, activity, "Success: Create Business Application successfully");
        return currentBusinessApplication;
    }

    public BusinessApplication updateBusinessApplication(BusinessApplicationRequest request, String businessId, HttpServletRequest req) throws PartnerPortalException {
        String activity = "Update Business Application";


        BusinessApplication businessApplication = this.businessApplicationRepository.findBusinessApplicationByApplicationId(businessId);
        if (ObjectUtils.isEmpty(businessApplication)) {
            throw new PartnerPortalException(ErrorCodes.JCMP3001);
        }

        // Check if another business with the same name already exists (excluding current business)
        BusinessApplication existingBusiness = this.businessApplicationRepository.findByNameExcludingBusinessId(
                request.getName(), businessId);
        if (existingBusiness != null) {
            throw new PartnerPortalException(ErrorCodes.JCMP3060);
        }

        businessApplication.setDescription(request.getDescription());
        businessApplication.setName(request.getName());

        this.logBusinessApplicationAudit(businessApplication, ActionType.UPDATE);
        LogUtil.logActivity(req, activity, "Success:Update Business Application successfully");
        return this.businessApplicationRepository.save(businessApplication);
    }

    public SearchResponse<BusinessApplication> search(Map<String, String> reqParams, HttpServletRequest req) throws PartnerPortalException {
        String activity = "Search Business Application";

        Map<String, String> searchParams = this.utils.filterRequestParam(reqParams, businessApplicationSearchParams);
        List<BusinessApplication> mongoResponse = this.businessApplicationRepository.findBusinessApplicationByParams(searchParams);

        if (ObjectUtils.isEmpty(mongoResponse)) {
            throw new PartnerPortalException(ErrorCodes.JCMP3001);
        }

        LogUtil.logActivity(req, activity, "Success:Search Business Application successfully");
        return SearchResponse.<BusinessApplication>builder()
                .searchList(mongoResponse)
                .build();
    }

    public long count() {
        return this.businessApplicationRepository.count();
    }
    
    /**
     * Modular function to log business application audit events
     * Can be used in both create and update business application flows
     *
     * @param businessApplication The business application entity to audit
     * @param actionType The action type (CREATE, UPDATE)
     */
    public void logBusinessApplicationAudit(BusinessApplication businessApplication, ActionType actionType) {
        try {
            Actor actor = Actor.builder()
                    .id(ThreadContext.get(Constants.USER_ID_THREAD_CONTEXT))
                    .role(Constants.USER)
                    .type(Constants.USER_ID_TYPE)
                    .build();

            Resource resource = Resource.builder()
                    .type(Constants.BUSINESS_ID)
                    .id(businessApplication.getBusinessId())
                    .build();

            Context context = Context.builder()
                    .ipAddress(ThreadContext.get(Constants.SOURCE_IP) != null && !ThreadContext.get(Constants.SOURCE_IP).equals("-") 
                            ? ThreadContext.get(Constants.SOURCE_IP) : null)
                    .txnId(ThreadContext.get(Constants.TXN_ID_THREAD_CONTEXT) != null && !ThreadContext.get(Constants.TXN_ID_THREAD_CONTEXT).equals("-") 
                            ? ThreadContext.get(Constants.TXN_ID_THREAD_CONTEXT) : null)
                    .build();

            Map<String, Object> extra = new HashMap<>();
            // Add business application POJO in the extra field under the "data" key
            extra.put(Constants.DATA, businessApplication);

            AuditRequest auditRequest = AuditRequest.builder()
                    .actor(actor)
                    .businessId(businessApplication.getBusinessId())
                    .group(Constants.PARTNER_PORTAL_GROUP)
                    .component(AuditComponent.BUSINESS_APPLICATION)
                    .actionType(actionType)
                    .resource(resource)
                    .initiator(Constants.DATA_FIDUCIARY)
                    .context(context)
                    .extra(extra)
                    .build();

            String tenantId = ThreadContext.get(Constants.TENANT_ID_HEADER);
            this.auditManager.logAudit(auditRequest, tenantId);
        } catch (Exception e) {
            log.error("Audit logging failed for business id: {}, action: {}, error: {}", 
                    businessApplication.getBusinessId(), actionType, e.getMessage(), e);
        }
    }
}
