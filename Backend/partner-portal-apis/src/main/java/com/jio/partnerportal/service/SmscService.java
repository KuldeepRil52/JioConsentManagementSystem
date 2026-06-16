package com.jio.partnerportal.service;

import com.jio.partnerportal.client.audit.AuditManager;
import com.jio.partnerportal.client.audit.request.Actor;
import com.jio.partnerportal.client.audit.request.AuditRequest;
import com.jio.partnerportal.client.audit.request.Context;
import com.jio.partnerportal.client.audit.request.Resource;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.jio.partnerportal.constant.Constants;
import com.jio.partnerportal.constant.ErrorCodes;
import com.jio.partnerportal.dto.*;
import com.jio.partnerportal.dto.request.ConfigurationRequest;
import com.jio.partnerportal.dto.response.SearchResponse;
import com.jio.partnerportal.entity.SmscConfig;
import com.jio.partnerportal.exception.PartnerPortalException;
import com.jio.partnerportal.repository.SmscRepository;
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
public class SmscService {

    SmscRepository smscRepository;
    Utils utils;
    ConfigHistoryService configHistoryService;
    AuditManager auditManager;

    @Autowired
    SmscService(SmscRepository smscRepository, Utils utils, ConfigHistoryService configHistoryService, AuditManager auditManager) {
        this.smscRepository = smscRepository;
        this.utils = utils;
        this.configHistoryService = configHistoryService;
        this.auditManager = auditManager;
    }

    @Value("${smsc.search.parameters}")
    List<String> smscSearchParams;

    public SmscConfig createConfig(Map<String, String> headers, ConfigurationRequest<SmscDetails> request, HttpServletRequest req) throws PartnerPortalException, JsonProcessingException {
        String activity = "Create SMSC configuration";

        String scopeLevel = headers.get(Constants.SCOPE_LEVEL_HEADER);
        String businessId = headers.get(Constants.BUSINESS_ID_HEADER);
        if (ScopeLevel.TENANT.toString().equals(scopeLevel) && this.smscRepository.existByScopeLevel(scopeLevel)) {
            throw new PartnerPortalException(ErrorCodes.JCMP3002);
        }

        if(this.smscRepository.existByScopeLevelAndBusinessId(scopeLevel, businessId)) {
            throw new PartnerPortalException(ErrorCodes.JCMP3003);
        }
        SmscConfig config = SmscConfig.builder()
                .configId(UUID.randomUUID().toString())
                .businessId(headers.get(Constants.BUSINESS_ID_HEADER))
                .scopeLevel(headers.get(Constants.SCOPE_LEVEL_HEADER))
                .configurationJson(request.getConfigurationJson())
                .build();

        SmscConfig smscConfig = this.smscRepository.save(config);
        this.configHistoryService.createConfigHistoryEntry(smscConfig, businessId, ConfigType.SMSC, Operation.CREATE);
        this.logSmscConfigAudit(smscConfig, ActionType.CREATE);
        LogUtil.logActivity(req, activity, "Success: Create SMSC configuration successfully");
        return smscConfig;
    }

    public SmscConfig updateConfig(String configId, ConfigurationRequest<SmscDetails> request,HttpServletRequest req) throws PartnerPortalException, JsonProcessingException {
        String activity = "Update SMSC configuration";

        SmscConfig config = this.smscRepository.findByConfigId(configId);
        if (ObjectUtils.isEmpty(config)) {
            throw new PartnerPortalException(ErrorCodes.JCMP3001);
        }
        config.setConfigurationJson(request.getConfigurationJson());
        SmscConfig smscConfig = this.smscRepository.save(config);
        this.configHistoryService.createConfigHistoryEntry(smscConfig, config.getBusinessId(), ConfigType.SMSC, Operation.UPDATE);
        this.logSmscConfigAudit(smscConfig, ActionType.UPDATE);
        LogUtil.logActivity(req, activity, "Success: Update SMSC configuration successfully");
        return smscConfig;
    }

    public SearchResponse<SmscConfig> search(Map<String, String> reqParams, HttpServletRequest req) throws PartnerPortalException {
        String activity = "Search SMSC configurations";

        Map<String, String> searchParams = this.utils.filterRequestParam(reqParams, smscSearchParams);
        List<SmscConfig> mongoResponse = this.smscRepository.findConfigByParams(searchParams);

        if (ObjectUtils.isEmpty(mongoResponse)) {
            throw new PartnerPortalException(ErrorCodes.JCMP3001);
        }

        LogUtil.logActivity(req, activity, "Success: Search SMSC configurations successfully");
        return SearchResponse.<SmscConfig>builder()
                .searchList(mongoResponse)
                .build();
    }

    public long count() {
        return this.smscRepository.count();
    }

    /**
     * Modular function to log smscConfig audit events
     * Can be used in both create and update smscConfig flows
     *
     * @param smscConfig The smscConfig entity to audit
     * @param actionType The action type (CREATE, UPDATE)
     */
    public void logSmscConfigAudit(SmscConfig smscConfig, ActionType actionType) {
        try {
            Actor actor = Actor.builder()
                    .id(ThreadContext.get(Constants.USER_ID_THREAD_CONTEXT))
                    .role(Constants.USER)
                    .type(Constants.USER_ID_TYPE)
                    .build();

            Resource resource = Resource.builder()
                    .type(Constants.CONFIG_ID)
                    .id(smscConfig.getConfigId())
                    .build();

            Context context = Context.builder()
                    .ipAddress(ThreadContext.get(Constants.SOURCE_IP) != null && !ThreadContext.get(Constants.SOURCE_IP).equals("-")
                            ? ThreadContext.get(Constants.SOURCE_IP) : null)
                    .txnId(ThreadContext.get(Constants.TXN_ID_THREAD_CONTEXT) != null && !ThreadContext.get(Constants.TXN_ID_THREAD_CONTEXT).equals("-") 
                            ? ThreadContext.get(Constants.TXN_ID_THREAD_CONTEXT) : null)
                    .build();

            Map<String, Object> extra = new HashMap<>();
            // Add smscConfig POJO in the extra field under the "data" key
            extra.put(Constants.DATA, smscConfig);

            AuditRequest auditRequest = AuditRequest.builder()
                    .actor(actor)
                    .businessId(smscConfig.getBusinessId())
                    .group(Constants.PARTNER_PORTAL_GROUP)
                    .component(AuditComponent.SMSC_CONFIG)
                    .actionType(actionType)
                    .resource(resource)
                    .initiator(Constants.DATA_FIDUCIARY)
                    .context(context)
                    .extra(extra)
                    .build();

            String tenantId = ThreadContext.get(Constants.TENANT_ID_HEADER);
            this.auditManager.logAudit(auditRequest, tenantId);
        } catch (Exception e) {
            log.error("Audit logging failed for smsc config id: {}, action: {}, error: {}", 
                    smscConfig.getConfigId(), actionType, e.getMessage(), e);
        }
    }
}
