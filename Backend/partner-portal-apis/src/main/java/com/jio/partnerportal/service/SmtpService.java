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
import com.jio.partnerportal.dto.ConfigType;
import com.jio.partnerportal.dto.Operation;
import com.jio.partnerportal.dto.ProviderType;
import com.jio.partnerportal.dto.ScopeLevel;
import com.jio.partnerportal.dto.SmtpDetails;
import com.jio.partnerportal.dto.request.ConfigurationRequest;
import com.jio.partnerportal.dto.response.SearchResponse;
import com.jio.partnerportal.entity.NotificationConfig;
import com.jio.partnerportal.exception.PartnerPortalException;
import com.jio.partnerportal.repository.NotificationRepository;
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
public class SmtpService {

    NotificationRepository notificationRepository;
    Utils utils;
    ConfigHistoryService configHistoryService;
    AuditManager auditManager;

    @Autowired  SmtpService(NotificationRepository notificationRepository, Utils utils, ConfigHistoryService configHistoryService, AuditManager auditManager) {
        this.notificationRepository = notificationRepository;
        this.utils = utils;
        this.configHistoryService = configHistoryService;
        this.auditManager = auditManager;
    }

    @Value("${smtp.search.parameters}")
    List<String> smtpSearchParams;

    public NotificationConfig createConfig(Map<String, String> headers, ConfigurationRequest<SmtpDetails> request, HttpServletRequest req) throws PartnerPortalException, JsonProcessingException {
        String activity = "Create SMTP configuration";

        String scopeLevel = headers.get(Constants.SCOPE_LEVEL_HEADER);
        String businessId = headers.get(Constants.BUSINESS_ID_HEADER);
        String providerTypeStr = headers.get(Constants.PROVIDER_TYPE_HEADER);
        ProviderType providerType = null;
        if (providerTypeStr != null && !providerTypeStr.isEmpty()) {
            try {
                providerType = ProviderType.valueOf(providerTypeStr.toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new PartnerPortalException(ErrorCodes.JCMP3001); // Invalid provider type
            }
        }
        
        // Validation: For TENANT scope level, check if record exists and if smtpDetails is present
        NotificationConfig existingTenantConfig = null;
        if (ScopeLevel.TENANT.toString().equals(scopeLevel) && this.notificationRepository.existByScopeLevel(scopeLevel)) {
            // Find the existing TENANT level record
            List<NotificationConfig> tenantConfigs = this.notificationRepository.findNotificationConfigByParams(Map.of("scopeLevel", scopeLevel));
            if (!tenantConfigs.isEmpty()) {
                existingTenantConfig = tenantConfigs.get(0);
                if (existingTenantConfig.getSmtpDetails() != null) {
                    throw new PartnerPortalException(ErrorCodes.JCMP3002);
                }
            }
        }

        // Validation: For the same businessId, check if record exists and if smtpDetails is present
        NotificationConfig existingConfig = this.notificationRepository.findByBusinessId(businessId);
        if (existingConfig != null) {
            if (existingConfig.getSmtpDetails() != null) {
                throw new PartnerPortalException(ErrorCodes.JCMP3003);
            }
            // Update existing config with SMTP details only
            existingConfig.setSmtpDetails(request.getConfigurationJson());
            if (providerType != null) {
                existingConfig.setProviderType(providerType);
            }
            NotificationConfig updatedConfig = this.notificationRepository.save(existingConfig);
            this.configHistoryService.createConfigHistoryEntry(updatedConfig, businessId, ConfigType.SMTP, Operation.CREATE);
            this.logSmtpConfigAudit(updatedConfig, ActionType.CREATE);
            LogUtil.logActivity(req, activity, "Success: Create SMTP configuration successfully");
            return updatedConfig;
        }
        
        // If TENANT level record exists but smtpDetails is null, update it
        if (existingTenantConfig != null && existingTenantConfig.getSmtpDetails() == null) {
            existingTenantConfig.setSmtpDetails(request.getConfigurationJson());
            if (providerType != null) {
                existingTenantConfig.setProviderType(providerType);
            }
            NotificationConfig updatedConfig = this.notificationRepository.save(existingTenantConfig);
            this.configHistoryService.createConfigHistoryEntry(updatedConfig, businessId, ConfigType.SMTP, Operation.CREATE);
            this.logSmtpConfigAudit(updatedConfig, ActionType.CREATE);
            LogUtil.logActivity(req, activity, "Success: Create SMTP configuration successfully");
            return updatedConfig;
        }
        
        // Create new notification config with only SMTP details
        NotificationConfig config = NotificationConfig.builder()
                .configId(UUID.randomUUID().toString())
                .businessId(businessId)
                .scopeLevel(scopeLevel)
                .providerType(providerType)
                .smtpDetails(request.getConfigurationJson())
                .build();

        NotificationConfig notificationConfig = this.notificationRepository.save(config);
        this.configHistoryService.createConfigHistoryEntry(notificationConfig, businessId, ConfigType.SMTP, Operation.CREATE);
        this.logSmtpConfigAudit(notificationConfig, ActionType.CREATE);
        LogUtil.logActivity(req, activity, "Success: Create SMTP configuration successfully");
        return notificationConfig;
    }

    public NotificationConfig updateConfig(String configId, ConfigurationRequest<SmtpDetails> request, Map<String, String> headers, HttpServletRequest req) throws PartnerPortalException, JsonProcessingException {
        String activity = "Update SMTP configuration";

        NotificationConfig config = this.notificationRepository.findByConfigId(configId);
        if(ObjectUtils.isEmpty(config)) {
            throw new PartnerPortalException(ErrorCodes.JCMP3001);
        }
        
        String providerTypeStr = headers.get(Constants.PROVIDER_TYPE_HEADER);
        ProviderType providerType = null;
        if (providerTypeStr != null && !providerTypeStr.isEmpty()) {
            try {
                providerType = ProviderType.valueOf(providerTypeStr.toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new PartnerPortalException(ErrorCodes.JCMP3001); // Invalid provider type
            }
        }
        
        // Only update SMTP part, preserve other notification details
        config.setSmtpDetails(request.getConfigurationJson());
        if (providerType != null) {
            config.setProviderType(providerType);
        }
        
        NotificationConfig updatedConfig = this.notificationRepository.save(config);
        this.configHistoryService.createConfigHistoryEntry(updatedConfig, config.getBusinessId(), ConfigType.SMTP, Operation.UPDATE);
        this.logSmtpConfigAudit(updatedConfig, ActionType.UPDATE);
        LogUtil.logActivity(req, activity, "Success: Update SMTP configuration successfully");
        return updatedConfig;
    }

    public SearchResponse<NotificationConfig> search(Map<String, String> reqParams, HttpServletRequest req) throws PartnerPortalException {
        String activity="Search SMTP configuration";

        Map<String, String> searchParams = this.utils.filterRequestParam(reqParams, smtpSearchParams);
        List<NotificationConfig> mongoResponse = this.notificationRepository.findNotificationConfigByParams(searchParams);

        if (ObjectUtils.isEmpty(mongoResponse)) {
            throw new PartnerPortalException(ErrorCodes.JCMP3001);
        }

        LogUtil.logActivity(req, activity, "Success: Search SMTP configuration successfully");
        return SearchResponse.<NotificationConfig>builder()
                .searchList(mongoResponse)
                .build();
    }

    public long count() {
        return this.notificationRepository.count();
    }

    /**
     * Modular function to log smtpConfig audit events
     * Can be used in both create and update smtpConfig flows
     *
     * @param notificationConfig The notificationConfig entity to audit
     * @param actionType The action type (CREATE, UPDATE)
     */
    public void logSmtpConfigAudit(NotificationConfig notificationConfig, ActionType actionType) {
        try {
            Actor actor = Actor.builder()
                    .id(ThreadContext.get(Constants.USER_ID_THREAD_CONTEXT))
                    .role(Constants.USER)
                    .type(Constants.USER_ID_TYPE)
                    .build();

            Resource resource = Resource.builder()
                    .type(Constants.CONFIG_ID)
                    .id(notificationConfig.getConfigId())
                    .build();

            Context context = Context.builder()
                    .ipAddress(ThreadContext.get(Constants.SOURCE_IP) != null && !ThreadContext.get(Constants.SOURCE_IP).equals("-")
                            ? ThreadContext.get(Constants.SOURCE_IP) : null)
                    .txnId(ThreadContext.get(Constants.TXN_ID_THREAD_CONTEXT) != null && !ThreadContext.get(Constants.TXN_ID_THREAD_CONTEXT).equals("-") 
                            ? ThreadContext.get(Constants.TXN_ID_THREAD_CONTEXT) : null)
                    .build();

            Map<String, Object> extra = new HashMap<>();
            // Add notificationConfig POJO in the extra field under the "data" key
            extra.put(Constants.DATA, notificationConfig);

            AuditRequest auditRequest = AuditRequest.builder()
                    .actor(actor)
                    .businessId(notificationConfig.getBusinessId())
                    .group(Constants.PARTNER_PORTAL_GROUP)
                    .component(AuditComponent.SMTP_CONFIG)
                    .actionType(actionType)
                    .resource(resource)
                    .initiator(Constants.DATA_FIDUCIARY)
                    .context(context)
                    .extra(extra)
                    .build();

            String tenantId = ThreadContext.get(Constants.TENANT_ID_HEADER);
            this.auditManager.logAudit(auditRequest, tenantId);
        } catch (Exception e) {
            log.error("Audit logging failed for smtp config id: {}, action: {}, error: {}", 
                    notificationConfig.getConfigId(), actionType, e.getMessage(), e);
        }
    }
}
