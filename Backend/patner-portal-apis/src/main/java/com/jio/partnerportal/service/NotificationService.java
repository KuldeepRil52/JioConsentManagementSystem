package com.jio.partnerportal.service;

import com.jio.partnerportal.client.audit.AuditManager;
import com.jio.partnerportal.client.audit.request.Actor;
import com.jio.partnerportal.client.audit.request.AuditRequest;
import com.jio.partnerportal.client.audit.request.Context;
import com.jio.partnerportal.client.audit.request.Resource;
import com.jio.partnerportal.constant.Constants;
import com.jio.partnerportal.dto.ActionType;
import com.jio.partnerportal.dto.AuditComponent;
import com.jio.partnerportal.dto.NetworkType;
import com.jio.partnerportal.dto.NotificationDetails;
import com.jio.partnerportal.dto.ProviderType;
import com.jio.partnerportal.dto.ScopeLevel;
import com.jio.partnerportal.dto.Status;
import com.jio.partnerportal.dto.request.ConfigurationRequest;
import com.jio.partnerportal.dto.response.SearchResponse;
import com.jio.partnerportal.entity.Document;
import com.jio.partnerportal.entity.NotificationConfig;
import com.jio.partnerportal.exception.PartnerPortalException;
import com.jio.partnerportal.repository.DocumentRepository;
import com.jio.partnerportal.repository.NotificationRepository;
import com.jio.partnerportal.util.LogUtil;
import com.jio.partnerportal.util.Utils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.ThreadContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
public class NotificationService {

    NotificationRepository notificationRepository;
    DocumentRepository documentRepository;
    Utils utils;
    AuditManager auditManager;

    @Autowired
    public NotificationService(NotificationRepository notificationRepository, DocumentRepository documentRepository, Utils utils, AuditManager auditManager) {
        this.notificationRepository = notificationRepository;
        this.documentRepository = documentRepository;
        this.utils = utils;
        this.auditManager = auditManager;
    }

    @Value("${notification.search.parameters}")
    List<String> notificationSearchParams;

    public NotificationConfig createConfig(Map<String, String> headers, ConfigurationRequest<NotificationDetails> request, HttpServletRequest req) throws PartnerPortalException {
        String activity = "Create notification configuration";

        String scopeLevel = headers.get(com.jio.partnerportal.constant.Constants.SCOPE_LEVEL_HEADER);
        String businessId = headers.get(com.jio.partnerportal.constant.Constants.BUSINESS_ID_HEADER);
        String providerTypeStr = headers.get(com.jio.partnerportal.constant.Constants.PROVIDER_TYPE_HEADER);
        ProviderType providerType = null;
        if (providerTypeStr != null && !providerTypeStr.isEmpty()) {
            try {
                providerType = ProviderType.valueOf(providerTypeStr.toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new PartnerPortalException(com.jio.partnerportal.constant.ErrorCodes.JCMP3001); // Invalid provider type
            }
        }

        // Validation: For TENANT scope level, check if record exists and if configurationJson is present
        NotificationConfig existingTenantConfig = null;
        if (ScopeLevel.TENANT.toString().equals(scopeLevel) && this.notificationRepository.existByScopeLevel(scopeLevel)) {
            // Find the existing TENANT level record
            List<NotificationConfig> tenantConfigs = this.notificationRepository.findNotificationConfigByParams(Map.of("scopeLevel", scopeLevel));
            if (!tenantConfigs.isEmpty()) {
                existingTenantConfig = tenantConfigs.get(0);
                if (existingTenantConfig.getConfigurationJson() != null) {
                    throw new PartnerPortalException(com.jio.partnerportal.constant.ErrorCodes.JCMP3002);
                }
            }
        }

        // Validation: For the same businessId, check if record exists and if configurationJson is present
        NotificationConfig existingConfig = this.notificationRepository.findByBusinessId(businessId);
        
        NotificationDetails notificationDetails = request.getConfigurationJson();

        // Validate network type specific requirements
        // validateNotificationDetails(notificationDetails);

        // Save mutual certificate to document collection if provided
        if (notificationDetails.getMutualCertificate() != null && notificationDetails.getMutualCertificateMeta() != null) {
            String certificateDocumentId = UUID.randomUUID().toString();
            Document certificateDocument = Document.builder()
                    .documentId(certificateDocumentId)
                    .documentName(notificationDetails.getMutualCertificateMeta().getName())
                    .businessId(businessId)
                    .contentType(notificationDetails.getMutualCertificateMeta().getContentType())
                    .status(Status.ACTIVE.toString())
                    .documentSize(notificationDetails.getMutualCertificateMeta().getSize())
                    .tag(notificationDetails.getMutualCertificateMeta().getTag())
                    .isBase64Document(true)
                    .data(notificationDetails.getMutualCertificate())
                    .version(1)
                    .build();

            documentRepository.saveDocument(certificateDocument);
            notificationDetails.getMutualCertificateMeta().setDocumentId(certificateDocumentId);
            log.info("Mutual certificate saved to document collection with ID: {}", certificateDocumentId);
        }

        NotificationConfig notificationConfig;
        if (existingConfig != null) {
            if (existingConfig.getConfigurationJson() != null) {
                throw new PartnerPortalException(com.jio.partnerportal.constant.ErrorCodes.JCMP3003);
            }
            // Update existing config with notification details only
            existingConfig.setConfigurationJson(notificationDetails);
            if (providerType != null) {
                existingConfig.setProviderType(providerType);
            }
            notificationConfig = notificationRepository.save(existingConfig);
        } else if (existingTenantConfig != null && existingTenantConfig.getConfigurationJson() == null) {
            // If TENANT level record exists but configurationJson is null, update it
            existingTenantConfig.setConfigurationJson(notificationDetails);
            if (providerType != null) {
                existingTenantConfig.setProviderType(providerType);
            }
            notificationConfig = notificationRepository.save(existingTenantConfig);
        } else {
            // Create new notification config with only notification details
            NotificationConfig config = NotificationConfig.builder()
                    .configId(UUID.randomUUID().toString())
                    .businessId(businessId)
                    .scopeLevel(scopeLevel)
                    .providerType(providerType)
                    .configurationJson(notificationDetails)
                    .build();
            notificationConfig = notificationRepository.save(config);
        }
        this.logNotificationConfigAudit(notificationConfig, ActionType.CREATE);
        LogUtil.logActivity(req, activity, "Success: Create notification configuration successfully");
        return notificationConfig;
    }

    public NotificationConfig updateConfig(String configId, ConfigurationRequest<NotificationDetails> request, Map<String, String> headers, HttpServletRequest req) throws PartnerPortalException {

        String activity = "Update notification configuration ";

        NotificationConfig config = notificationRepository.findByConfigId(configId);
        if (ObjectUtils.isEmpty(config)) {
            throw new PartnerPortalException(com.jio.partnerportal.constant.ErrorCodes.JCMP3001);
        }

        String providerTypeStr = headers.get(com.jio.partnerportal.constant.Constants.PROVIDER_TYPE_HEADER);
        ProviderType providerType = null;
        if (providerTypeStr != null && !providerTypeStr.isEmpty()) {
            try {
                providerType = ProviderType.valueOf(providerTypeStr.toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new PartnerPortalException(com.jio.partnerportal.constant.ErrorCodes.JCMP3001); // Invalid provider type
            }
        }

        NotificationDetails notificationDetails = request.getConfigurationJson();

        // Validate network type specific requirements
        // validateNotificationDetails(notificationDetails);

        // Save mutual certificate to document collection if provided
        if (notificationDetails.getMutualCertificate() != null && notificationDetails.getMutualCertificateMeta() != null) {
            String certificateDocumentId = UUID.randomUUID().toString();
            Document certificateDocument = Document.builder()
                    .documentId(certificateDocumentId)
                    .documentName(notificationDetails.getMutualCertificateMeta().getName())
                    .businessId(config.getBusinessId())
                    .contentType(notificationDetails.getMutualCertificateMeta().getContentType())
                    .status(Status.ACTIVE.toString())
                    .documentSize(notificationDetails.getMutualCertificateMeta().getSize())
                    .tag(notificationDetails.getMutualCertificateMeta().getTag())
                    .isBase64Document(true)
                    .data(notificationDetails.getMutualCertificate())
                    .version(1)
                    .build();

            documentRepository.saveDocument(certificateDocument);
            notificationDetails.getMutualCertificateMeta().setDocumentId(certificateDocumentId);
            log.info("Mutual certificate saved to document collection with ID: {}", certificateDocumentId);
        }

        // Only update notification details, preserve SMTP details (stored separately)
        config.setConfigurationJson(notificationDetails);
        if (providerType != null) {
            config.setProviderType(providerType);
        }
        NotificationConfig notificationConfig = notificationRepository.save(config);
        this.logNotificationConfigAudit(notificationConfig, ActionType.UPDATE);
        LogUtil.logActivity(req, activity, "Success: Update notification configuration successfully");
        return notificationConfig;
    }

    public SearchResponse<NotificationConfig> search(Map<String, String> reqParams, HttpServletRequest req) throws PartnerPortalException {

        String activity = "Search notification configuration";

        Map<String, String> searchParams = this.utils.filterRequestParam(reqParams, notificationSearchParams);
        List<NotificationConfig> mongoResponse = this.notificationRepository.findNotificationConfigByParams(searchParams);

        if (ObjectUtils.isEmpty(mongoResponse)) {
            throw new PartnerPortalException(com.jio.partnerportal.constant.ErrorCodes.JCMP3001);
        }

        LogUtil.logActivity(req, activity, "Success: Search notification configuration successfully");
        return SearchResponse.<NotificationConfig>builder()
                .searchList(mongoResponse)
                .build();
    }

    public long count() {
        return this.notificationRepository.count();
    }

    /**
     * Validates notification details based on network type
     * - For INTERNET network type: mutualSSL and mutualCertificate are required
     */
    private void validateNotificationDetails(NotificationDetails notificationDetails) {
        // Validate that network type is provided
        if (notificationDetails.getNetworkType() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Network type is required");
        }

        // Validate that baseUrl is provided
        if (!StringUtils.hasText(notificationDetails.getBaseUrl())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Base URL is required");
        }

        // For INTERNET network type, validate mutual SSL requirements
        if (NetworkType.INTERNET.equals(notificationDetails.getNetworkType())) {
            // Check if mutualSSL is enabled
            if (notificationDetails.getMutualSSL() == null || !notificationDetails.getMutualSSL()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Mutual SSL must be enabled for INTERNET network type");
            }

            // Check if certificate is provided when mutualSSL is enabled
            if (!StringUtils.hasText(notificationDetails.getMutualCertificate())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Mutual certificate is required when mutual SSL is enabled for INTERNET network type");
            }

            // Check if certificate metadata is provided
            if (notificationDetails.getMutualCertificateMeta() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Mutual certificate metadata is required when mutual SSL is enabled for INTERNET network type");
            }

            log.info("Validated INTERNET network type with mutual SSL configuration");
        } else if (NetworkType.INTRANET.equals(notificationDetails.getNetworkType())) {
            log.info("Validated INTRANET network type - uses bearer token only");
        }
    }

    /**
     * Modular function to log notificationConfig audit events
     * Can be used in both create and update notificationConfig flows
     *
     * @param notificationConfig The notificationConfig entity to audit
     * @param actionType The action type (CREATE, UPDATE)
     */
    public void logNotificationConfigAudit(NotificationConfig notificationConfig, ActionType actionType) {
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
                    .component(AuditComponent.NOTIFICATION_CONFIG)
                    .actionType(actionType)
                    .resource(resource)
                    .initiator(Constants.DATA_FIDUCIARY)
                    .context(context)
                    .extra(extra)
                    .build();

            String tenantId = ThreadContext.get(Constants.TENANT_ID_HEADER);
            this.auditManager.logAudit(auditRequest, tenantId);
        } catch (Exception e) {
            log.error("Audit logging failed for notification config id: {}, action: {}, error: {}", 
                    notificationConfig.getConfigId(), actionType, e.getMessage(), e);
        }
    }
}
