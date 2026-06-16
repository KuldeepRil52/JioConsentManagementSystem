package com.jio.partnerportal.service;

import com.jio.partnerportal.client.audit.AuditManager;
import com.jio.partnerportal.client.audit.request.Actor;
import com.jio.partnerportal.client.audit.request.AuditRequest;
import com.jio.partnerportal.client.audit.request.Context;
import com.jio.partnerportal.client.audit.request.Resource;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.jio.partnerportal.constant.Constants;
import com.jio.partnerportal.constant.ErrorCodes;
import com.jio.partnerportal.dto.ActionType;
import com.jio.partnerportal.dto.AuditComponent;
import com.jio.partnerportal.dto.CommunicationConfig;
import com.jio.partnerportal.dto.ConfigType;
import com.jio.partnerportal.dto.GrievanceDetails;
import com.jio.partnerportal.dto.Operation;
import com.jio.partnerportal.dto.ScopeLevel;
import com.jio.partnerportal.dto.request.ConfigurationRequest;
import com.jio.partnerportal.dto.response.SearchResponse;
import com.jio.partnerportal.entity.GrievanceConfig;
import com.jio.partnerportal.exception.PartnerPortalException;
import com.jio.partnerportal.repository.GrievanceRepository;
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
public class GrievanceService {

    GrievanceRepository grievanceRepository;
    Utils utils;
    ConfigHistoryService configHistoryService;
    AuditManager auditManager;

    @Autowired
    GrievanceService(GrievanceRepository grievanceRepository, Utils utils, ConfigHistoryService configHistoryService, AuditManager auditManager) {
        this.grievanceRepository = grievanceRepository;
        this.utils = utils;
        this.configHistoryService = configHistoryService;
        this.auditManager = auditManager;
    }

    @Value("${grievance.search.parameters}")
    List<String> grievanceSearchParams;

    public GrievanceConfig createConfig(Map<String, String> headers, ConfigurationRequest<GrievanceDetails> request, HttpServletRequest req) throws PartnerPortalException, JsonProcessingException {
        String activity = "Create grievance configuration";

        String scopeLevel = headers.get(Constants.SCOPE_LEVEL_HEADER);
        String businessId = headers.get(Constants.BUSINESS_ID_HEADER);
        if (ScopeLevel.TENANT.toString().equals(scopeLevel) && this.grievanceRepository.existByScopeLevel(scopeLevel)) {
            throw new PartnerPortalException(ErrorCodes.JCMP3002);
        }

        if(this.grievanceRepository.existByScopeLevelAndBusinessId(scopeLevel, businessId)) {
            throw new PartnerPortalException(ErrorCodes.JCMP3003);
        }
        
        // Set default CommunicationConfig if not provided in request
        GrievanceDetails grievanceDetails = request.getConfigurationJson();
        if (grievanceDetails != null && grievanceDetails.getCommunicationConfig() == null) {
            CommunicationConfig defaultCommunicationConfig = CommunicationConfig.builder()
                    .sms(true)
                    .email(true)
                    .build();
            grievanceDetails.setCommunicationConfig(defaultCommunicationConfig);
        }
        
        GrievanceConfig config = GrievanceConfig.builder()
                .configId(UUID.randomUUID().toString())
                .businessId(businessId)
                .scopeLevel(scopeLevel)
                .configurationJson(grievanceDetails)
                .build();

        GrievanceConfig grievanceConfig = this.grievanceRepository.save(config);
        this.configHistoryService.createConfigHistoryEntry(grievanceConfig, businessId, ConfigType.GRIEVANCE, Operation.CREATE);

        this.logGrievanceConfigAudit(grievanceConfig, ActionType.CREATE);
        LogUtil.logActivity(req, activity, "Success: Create grievance configuration successfully");
        return grievanceConfig;
    }

    public GrievanceConfig updateConfig(String configId, ConfigurationRequest<GrievanceDetails> request, HttpServletRequest req) throws PartnerPortalException, JsonProcessingException {
        String activity = "Update grievance configuration";

        GrievanceConfig config = this.grievanceRepository.findByConfigId(configId);
        if (ObjectUtils.isEmpty(config)) {
            throw new PartnerPortalException(ErrorCodes.JCMP3001);
        }
        config.setConfigurationJson(request.getConfigurationJson());
        GrievanceConfig grievanceConfig = this.grievanceRepository.save(config);
        this.configHistoryService.createConfigHistoryEntry(config, config.getBusinessId(), ConfigType.GRIEVANCE, Operation.UPDATE);

        this.logGrievanceConfigAudit(grievanceConfig, ActionType.UPDATE);
        LogUtil.logActivity(req, activity, "Success: Update grievance configuration successfully");
        return grievanceConfig;
    }

    public SearchResponse<GrievanceConfig> search(Map<String, String> reqParams, HttpServletRequest req) throws PartnerPortalException {
        String activity = "Search grievance configuration";

        Map<String, String> searchParams = this.utils.filterRequestParam(reqParams, grievanceSearchParams);
        List<GrievanceConfig> mongoResponse = this.grievanceRepository.findConfigByParams(searchParams);

        if (ObjectUtils.isEmpty(mongoResponse)) {
            throw new PartnerPortalException(ErrorCodes.JCMP3001);
        }

        LogUtil.logActivity(req, activity, "Success: Search grievance configuration successfully");
        return SearchResponse.<GrievanceConfig>builder()
                .searchList(mongoResponse)
                .build();
    }

    public long count() {
        return this.grievanceRepository.count();
    }

    /**
     * Modular function to log grievanceConfig audit events
     * Can be used in both create and update grievanceConfig flows
     *
     * @param grievanceConfig The grievanceConfig entity to audit
     * @param actionType The action type (CREATE, UPDATE)
     */
    public void logGrievanceConfigAudit(GrievanceConfig grievanceConfig, ActionType actionType) {
        try {
            Actor actor = Actor.builder()
                    .id(ThreadContext.get(Constants.USER_ID_THREAD_CONTEXT))
                    .role(Constants.USER)
                    .type(Constants.USER_ID_TYPE)
                    .build();

            Resource resource = Resource.builder()
                    .type(Constants.CONFIG_ID)
                    .id(grievanceConfig.getConfigId())
                    .build();

            Context context = Context.builder()
                    .ipAddress(ThreadContext.get(Constants.SOURCE_IP) != null && !ThreadContext.get(Constants.SOURCE_IP).equals("-")
                            ? ThreadContext.get(Constants.SOURCE_IP) : null)
                    .txnId(ThreadContext.get(Constants.TXN_ID_THREAD_CONTEXT) != null && !ThreadContext.get(Constants.TXN_ID_THREAD_CONTEXT).equals("-") 
                            ? ThreadContext.get(Constants.TXN_ID_THREAD_CONTEXT) : null)
                    .build();

            Map<String, Object> extra = new HashMap<>();
            // Add grievanceConfig POJO in the extra field under the "data" key
            extra.put(Constants.DATA, grievanceConfig);

            AuditRequest auditRequest = AuditRequest.builder()
                    .actor(actor)
                    .businessId(grievanceConfig.getBusinessId())
                    .group(Constants.PARTNER_PORTAL_GROUP)
                    .component(AuditComponent.GRIEVANCE_CONFIG)
                    .actionType(actionType)
                    .resource(resource)
                    .initiator(Constants.DATA_FIDUCIARY)
                    .context(context)
                    .extra(extra)
                    .build();

            String tenantId = ThreadContext.get(Constants.TENANT_ID_HEADER);
            this.auditManager.logAudit(auditRequest, tenantId);
        } catch (Exception e) {
            log.error("Audit logging failed for grievance config id: {}, action: {}, error: {}", 
                    grievanceConfig.getConfigId(), actionType, e.getMessage(), e);
        }
    }
}
