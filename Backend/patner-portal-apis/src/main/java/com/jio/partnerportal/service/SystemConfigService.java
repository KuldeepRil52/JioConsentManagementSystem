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
import com.jio.partnerportal.dto.ScopeLevel;
import com.jio.partnerportal.dto.Status;
import com.jio.partnerportal.dto.SystemDetails;
import com.jio.partnerportal.dto.request.ConfigurationRequest;
import com.jio.partnerportal.dto.response.SearchResponse;
import com.jio.partnerportal.entity.DataProcessor;
import com.jio.partnerportal.entity.Document;
import com.jio.partnerportal.entity.SystemConfig;
import com.jio.partnerportal.exception.PartnerPortalException;
import com.jio.partnerportal.repository.DataProcessorRepository;
import com.jio.partnerportal.repository.DocumentRepository;
import com.jio.partnerportal.repository.SystemConfigRepository;
import com.jio.partnerportal.util.LogUtil;
import com.jio.partnerportal.util.Utils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.ThreadContext;
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
public class SystemConfigService {
    SystemConfigRepository systemConfigRepository;
    Utils utils;
    ConfigHistoryService configHistoryService;
    DocumentRepository documentRepository;
    AuditManager auditManager;
    DataProcessorRepository dataProcessorRepository;

    private static final String TARGET_DATA_PROCESSOR_ID = "a1b2c3d4-e5f6-7890-1234-567890abcdef";

    @Autowired
    SystemConfigService(SystemConfigRepository systemConfigRepository, Utils utils, 
                       ConfigHistoryService configHistoryService, DocumentRepository documentRepository, 
                       AuditManager auditManager, DataProcessorRepository dataProcessorRepository) {
        this.systemConfigRepository = systemConfigRepository;
        this.utils = utils;
        this.configHistoryService = configHistoryService;
        this.documentRepository = documentRepository;
        this.auditManager = auditManager;
        this.dataProcessorRepository = dataProcessorRepository;
    }

    @Value("${systemConfig.search.parameters}")
    List<String> systemConfigSearchParams;

    public SystemConfig createConfig(Map<String, String> headers, ConfigurationRequest<SystemDetails> request, HttpServletRequest req) throws PartnerPortalException, JsonProcessingException {
        String activity = "Create system configuration";

        String scopeLevel = headers.get(Constants.SCOPE_LEVEL_HEADER);
        String businessId = headers.get(Constants.BUSINESS_ID_HEADER);
        if (ScopeLevel.TENANT.toString().equals(scopeLevel) && this.systemConfigRepository.existByScopeLevel(scopeLevel)) {
            throw new PartnerPortalException(ErrorCodes.JCMP3002);
        }

        if(this.systemConfigRepository.existByScopeLevelAndBusinessId(scopeLevel, businessId)) {
            throw new PartnerPortalException(ErrorCodes.JCMP3003);
        }

        SystemDetails systemDetails = request.getConfigurationJson();
        
        // Save SSL Certificate to document collection if provided
        if (systemDetails.getSslCertificate() != null && systemDetails.getSslCertificateMeta() != null) {
            String sslCertDocumentId = UUID.randomUUID().toString();
            Document sslCertDocument = Document.builder()
                    .documentId(sslCertDocumentId)
                    .documentName(systemDetails.getSslCertificateMeta().getName())
                    .businessId(businessId)
                    .contentType(systemDetails.getSslCertificateMeta().getContentType())
                    .status(Status.ACTIVE.toString())
                    .documentSize(systemDetails.getSslCertificateMeta().getSize())
                    .tag(systemDetails.getSslCertificateMeta().getTag())
                    .isBase64Document(true)
                    .data(systemDetails.getSslCertificate())
                    .version(1)
                    .build();
            
            documentRepository.saveDocument(sslCertDocument);
            systemDetails.getSslCertificateMeta().setDocumentId(sslCertDocumentId);
            log.info("SSL Certificate saved to document collection with ID: {}", sslCertDocumentId);
        }
        
        // Save Logo to document collection if provided
        if (systemDetails.getLogo() != null && systemDetails.getLogoMeta() != null) {
            String logoDocumentId = UUID.randomUUID().toString();
            Document logoDocument = Document.builder()
                    .documentId(logoDocumentId)
                    .documentName(systemDetails.getLogoMeta().getName())
                    .businessId(businessId)
                    .contentType(systemDetails.getLogoMeta().getContentType())
                    .status(Status.ACTIVE.toString())
                    .documentSize(systemDetails.getLogoMeta().getSize())
                    .tag(systemDetails.getLogoMeta().getTag())
                    .isBase64Document(true)
                    .data(systemDetails.getLogo())
                    .version(1)
                    .build();
            
            documentRepository.saveDocument(logoDocument);
            systemDetails.getLogoMeta().setDocumentId(logoDocumentId);
            log.info("Logo saved to document collection with ID: {}", logoDocumentId);
        }
        
        SystemConfig config = SystemConfig.builder()
                .configId(UUID.randomUUID().toString())
                .businessId(businessId)
                .scopeLevel(scopeLevel)
                .configurationJson(systemDetails)
                .build();

        SystemConfig systemConfig = this.systemConfigRepository.save(config);
        this.configHistoryService.createConfigHistoryEntry(systemConfig, businessId, ConfigType.SYSTEM, Operation.CREATE);
        this.logSystemConfigAudit(systemConfig, ActionType.CREATE);
        
        // Update dataprocessor callback URL if scopeLevel is TENANT and baseUrl is set
        if (ScopeLevel.TENANT.toString().equals(scopeLevel) && systemDetails.getBaseUrl() != null && !systemDetails.getBaseUrl().isEmpty()) {
            updateDataProcessorCallbackUrl(systemDetails.getBaseUrl());
        }
        
        LogUtil.logActivity(req, activity, "Success: Create system configuration successfully");
        return systemConfig;
    }

    public SystemConfig updateConfig(String configId, ConfigurationRequest<SystemDetails> request, 
                                     boolean isSslCertificateModified, boolean isLogoModified,HttpServletRequest req) throws PartnerPortalException, JsonProcessingException {
        String activity = "Update system configuration";
        SystemConfig config = this.systemConfigRepository.findByConfigId(configId);
        if(ObjectUtils.isEmpty(config)) {
            throw new PartnerPortalException(ErrorCodes.JCMP3001);
        }
        
        SystemDetails systemDetails = request.getConfigurationJson();
        String businessId = config.getBusinessId();
        String scopeLevel = config.getScopeLevel();
        
        // Check if baseUrl is being updated
        SystemDetails existingSystemDetails = config.getConfigurationJson();
        boolean isBaseUrlUpdated = existingSystemDetails != null && 
                                   systemDetails.getBaseUrl() != null && 
                                   !systemDetails.getBaseUrl().isEmpty() &&
                                   (existingSystemDetails.getBaseUrl() == null || 
                                    !existingSystemDetails.getBaseUrl().equals(systemDetails.getBaseUrl()));
        
        // Handle SSL Certificate modification
        if (isSslCertificateModified && 
            systemDetails.getSslCertificate() != null && 
            systemDetails.getSslCertificateMeta() != null) {
            
            String sslCertDocumentId = UUID.randomUUID().toString();
            Document sslCertDocument = Document.builder()
                    .documentId(sslCertDocumentId)
                    .documentName(systemDetails.getSslCertificateMeta().getName())
                    .businessId(businessId)
                    .contentType(systemDetails.getSslCertificateMeta().getContentType())
                    .status(Status.ACTIVE.toString())
                    .documentSize(systemDetails.getSslCertificateMeta().getSize())
                    .tag(systemDetails.getSslCertificateMeta().getTag())
                    .isBase64Document(true)
                    .data(systemDetails.getSslCertificate())
                    .version(1)
                    .build();
            
            documentRepository.saveDocument(sslCertDocument);
            systemDetails.getSslCertificateMeta().setDocumentId(sslCertDocumentId);
            log.info("Updated SSL Certificate saved to document collection with new ID: {}", sslCertDocumentId);
        }
        
        // Handle Logo modification
        if (isLogoModified && 
            systemDetails.getLogo() != null && 
            systemDetails.getLogoMeta() != null) {
            
            String logoDocumentId = UUID.randomUUID().toString();
            Document logoDocument = Document.builder()
                    .documentId(logoDocumentId)
                    .documentName(systemDetails.getLogoMeta().getName())
                    .businessId(businessId)
                    .contentType(systemDetails.getLogoMeta().getContentType())
                    .status(Status.ACTIVE.toString())
                    .documentSize(systemDetails.getLogoMeta().getSize())
                    .tag(systemDetails.getLogoMeta().getTag())
                    .isBase64Document(true)
                    .data(systemDetails.getLogo())
                    .version(1)
                    .build();
            
            documentRepository.saveDocument(logoDocument);
            systemDetails.getLogoMeta().setDocumentId(logoDocumentId);
            log.info("Updated Logo saved to document collection with new ID: {}", logoDocumentId);
        }
        
        config.setConfigurationJson(systemDetails);
        SystemConfig systemConfig = this.systemConfigRepository.save(config);
        this.configHistoryService.createConfigHistoryEntry(systemConfig, config.getBusinessId(), ConfigType.SYSTEM, Operation.UPDATE);
        this.logSystemConfigAudit(systemConfig, ActionType.UPDATE);
        
        // Update dataprocessor callback URL if scopeLevel is TENANT and baseUrl is updated
        if (ScopeLevel.TENANT.toString().equals(scopeLevel) && isBaseUrlUpdated) {
            updateDataProcessorCallbackUrl(systemDetails.getBaseUrl());
        }
        
        LogUtil.logActivity(req, activity, "Success: Update system configuration successfully");
        return systemConfig;
    }

    public SearchResponse<SystemConfig> search(Map<String, String> reqParams, HttpServletRequest req) throws PartnerPortalException {
        String activity = "Search system configurations";
        Map<String, String> searchParams = this.utils.filterRequestParam(reqParams, systemConfigSearchParams);
        List<SystemConfig> mongoResponse = this.systemConfigRepository.findConfigByParams(searchParams);

        if (ObjectUtils.isEmpty(mongoResponse)) {
            throw new PartnerPortalException(ErrorCodes.JCMP3001);
        }
        LogUtil.logActivity(req, activity, "Success: Search system configurations successfully");
        return SearchResponse.<SystemConfig>builder()
                .searchList(mongoResponse)
                .build();
    }

    public long count() {
        return this.systemConfigRepository.count();
    }

    /**
     * Updates the callback URL of the target dataprocessor with the provided baseUrl
     *
     * @param baseUrl The base URL to set as the callback URL
     */
    private void updateDataProcessorCallbackUrl(String baseUrl) {
        try {
            DataProcessor dataProcessor = this.dataProcessorRepository.findByDataProcessorId(TARGET_DATA_PROCESSOR_ID);
            if (dataProcessor != null) {
                dataProcessor.setCallbackUrl(baseUrl);
                this.dataProcessorRepository.save(dataProcessor);
                log.info("Updated callback URL for dataprocessor {} to {}", TARGET_DATA_PROCESSOR_ID, baseUrl);
            } else {
                log.warn("Dataprocessor with ID {} not found. Callback URL not updated.", TARGET_DATA_PROCESSOR_ID);
            }
        } catch (Exception e) {
            log.error("Error updating callback URL for dataprocessor {}: {}", TARGET_DATA_PROCESSOR_ID, e.getMessage(), e);
        }
    }

    /**
     * Modular function to log systemConfig audit events
     * Can be used in both create and update systemConfig flows
     *
     * @param systemConfig The systemConfig entity to audit
     * @param actionType The action type (CREATE, UPDATE)
     */
    public void logSystemConfigAudit(SystemConfig systemConfig, ActionType actionType) {
        try {
            Actor actor = Actor.builder()
                    .id(ThreadContext.get(Constants.USER_ID_THREAD_CONTEXT))
                    .role(Constants.USER)
                    .type(Constants.USER_ID_TYPE)
                    .build();

            Resource resource = Resource.builder()
                    .type(Constants.CONFIG_ID)
                    .id(systemConfig.getConfigId())
                    .build();

            Context context = Context.builder()
                    .ipAddress(ThreadContext.get(Constants.SOURCE_IP) != null && !ThreadContext.get(Constants.SOURCE_IP).equals("-")
                            ? ThreadContext.get(Constants.SOURCE_IP) : null)
                    .txnId(ThreadContext.get(Constants.TXN_ID_THREAD_CONTEXT) != null && !ThreadContext.get(Constants.TXN_ID_THREAD_CONTEXT).equals("-") 
                            ? ThreadContext.get(Constants.TXN_ID_THREAD_CONTEXT) : null)
                    .build();

            Map<String, Object> extra = new HashMap<>();
            // Add systemConfig POJO in the extra field under the "data" key
            extra.put(Constants.DATA, systemConfig);

            AuditRequest auditRequest = AuditRequest.builder()
                    .actor(actor)
                    .businessId(systemConfig.getBusinessId())
                    .group(Constants.PARTNER_PORTAL_GROUP)
                    .component(AuditComponent.SYSTEM_CONFIG)
                    .actionType(actionType)
                    .resource(resource)
                    .initiator(Constants.DATA_FIDUCIARY)
                    .context(context)
                    .extra(extra)
                    .build();

            String tenantId = ThreadContext.get(Constants.TENANT_ID_HEADER);
            this.auditManager.logAudit(auditRequest, tenantId);
        } catch (Exception e) {
            log.error("Audit logging failed for system config id: {}, action: {}, error: {}", 
                    systemConfig.getConfigId(), actionType, e.getMessage(), e);
        }
    }
}
