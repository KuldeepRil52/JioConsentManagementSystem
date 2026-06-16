package com.jio.partnerportal.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.jio.partnerportal.client.audit.AuditManager;
import com.jio.partnerportal.client.audit.request.Actor;
import com.jio.partnerportal.client.audit.request.AuditRequest;
import com.jio.partnerportal.client.audit.request.Context;
import com.jio.partnerportal.client.audit.request.Resource;
import com.jio.partnerportal.constant.Constants;
import com.jio.partnerportal.constant.ErrorCodes;
import com.jio.partnerportal.dto.*;
import com.jio.partnerportal.dto.request.ConfigurationRequest;
import com.jio.partnerportal.dto.response.SearchResponse;
import com.jio.partnerportal.entity.ConsentConfig;
import com.jio.partnerportal.exception.PartnerPortalException;
import com.jio.partnerportal.repository.ConsentRepository;
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
import java.util.UUID;

@Slf4j
@Service
public class ConsentService {
    ConsentRepository consentRepository;
    Utils utils;
    ConfigHistoryService configHistoryService;
    AuditManager auditManager;

    @Autowired
    ConsentService(ConsentRepository consentRepository, Utils utils, ConfigHistoryService configHistoryService, AuditManager auditManager) {
        this.consentRepository = consentRepository;
        this.utils = utils;
        this.configHistoryService = configHistoryService;
        this.auditManager = auditManager;
    }

    @Value("${consent.search.parameters}")
    List<String> consentSearchParams;

    public ConsentConfig createConfig(Map<String, String> headers, ConfigurationRequest<ConsentDetails> request, HttpServletRequest req) throws PartnerPortalException, JsonProcessingException {

        String activity = "Create consent configurations";

        String scopeLevel = headers.get(Constants.SCOPE_LEVEL_HEADER);
        String businessId = headers.get(Constants.BUSINESS_ID_HEADER);
        if (ScopeLevel.TENANT.toString().equals(scopeLevel) && this.consentRepository.existByScopeLevel(scopeLevel)) {
            throw new PartnerPortalException(ErrorCodes.JCMP3002);
        }

        if(this.consentRepository.existByScopeLevelAndBusinessId(scopeLevel, businessId)) {
            throw new PartnerPortalException(ErrorCodes.JCMP3003);
        }

        ConsentConfig config = ConsentConfig.builder()
                .configId(UUID.randomUUID().toString())
                .businessId(businessId)
                .scopeLevel(scopeLevel)
                .configurationJson(request.getConfigurationJson())
                .build();

        ConsentConfig consentConfig = this.consentRepository.save(config);
        this.configHistoryService.createConfigHistoryEntry(consentConfig, businessId, ConfigType.CONSENT, Operation.CREATE);

        this.logConsentConfigAudit(consentConfig, ActionType.CREATE);
        LogUtil.logActivity(req, activity, "Success: Create consent configurations successfully");
        return consentConfig;
    }

    public ConsentConfig updateConfig(String configId, ConfigurationRequest<ConsentDetails> request,HttpServletRequest req) throws PartnerPortalException, JsonProcessingException {
        String activity = "Update consent configuration";

        ConsentConfig config = this.consentRepository.findByConfigId(configId);
        if (ObjectUtils.isEmpty(config)) {
            throw new PartnerPortalException(ErrorCodes.JCMP3001);
        }
        config.setConfigurationJson(request.getConfigurationJson());
        ConsentConfig consentConfig = this.consentRepository.save(config);
        this.configHistoryService.createConfigHistoryEntry(consentConfig, config.getBusinessId(), ConfigType.CONSENT, Operation.CREATE);

        this.logConsentConfigAudit(consentConfig, ActionType.UPDATE);
        LogUtil.logActivity(req, activity, "Success: Update consent configuration successfully");
        return consentConfig;
    }

    public SearchResponse<ConsentConfig> search(Map<String, String> reqParams,HttpServletRequest req) throws PartnerPortalException {

        String activity = "Search consent configuration";

        Map<String, String> searchParams = this.utils.filterRequestParam(reqParams, consentSearchParams);
        List<ConsentConfig> mongoResponse = this.consentRepository.findConfigByParams(searchParams);

        if (ObjectUtils.isEmpty(mongoResponse)) {
            throw new PartnerPortalException(ErrorCodes.JCMP3001);
        }

        LogUtil.logActivity(req, activity, "Success: Search consent configuration successfully");
        return SearchResponse.<ConsentConfig>builder()
                .searchList(mongoResponse)
                .build();
    }

    public long count() {
        return this.consentRepository.count();
    }

    /**
     * Modular function to log consent config audit events
     * Can be used in both create and update consent config flows
     *
     * @param consentConfig The consent config entity to audit
     * @param actionType The action type (CREATE, UPDATE)
     */
    public void logConsentConfigAudit(ConsentConfig consentConfig, ActionType actionType) {
        try {
            Actor actor = Actor.builder()
                    .id(ThreadContext.get(Constants.USER_ID_THREAD_CONTEXT))
                    .role(Constants.USER)
                    .type(Constants.USER_ID_TYPE)
                    .build();

            Resource resource = Resource.builder()
                    .type(Constants.CONFIG_ID)
                    .id(consentConfig.getConfigId())
                    .build();

            Context context = Context.builder()
                    .ipAddress(ThreadContext.get(Constants.SOURCE_IP) != null && !ThreadContext.get(Constants.SOURCE_IP).equals("-")
                            ? ThreadContext.get(Constants.SOURCE_IP) : null)
                    .txnId(ThreadContext.get(Constants.TXN_ID_THREAD_CONTEXT) != null && !ThreadContext.get(Constants.TXN_ID_THREAD_CONTEXT).equals("-") 
                            ? ThreadContext.get(Constants.TXN_ID_THREAD_CONTEXT) : null)
                    .build();

            Map<String, Object> extra = new HashMap<>();
            // Add consent config POJO in the extra field under the "data" key
            extra.put(Constants.DATA, consentConfig);

            AuditRequest auditRequest = AuditRequest.builder()
                    .actor(actor)
                    .businessId(consentConfig.getBusinessId())
                    .group(Constants.PARTNER_PORTAL_GROUP)
                    .component(AuditComponent.CONSENT_CONFIG)
                    .actionType(actionType)
                    .resource(resource)
                    .initiator(Constants.DATA_FIDUCIARY)
                    .context(context)
                    .extra(extra)
                    .build();

            String tenantId = ThreadContext.get(Constants.TENANT_ID_HEADER);
            this.auditManager.logAudit(auditRequest, tenantId);
        } catch (Exception e) {
            log.error("Audit logging failed for consent config id: {}, action: {}, error: {}", 
                    consentConfig.getConfigId(), actionType, e.getMessage(), e);
        }
    }
}
