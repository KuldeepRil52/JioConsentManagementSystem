package com.jio.digigov.notification.service.impl;

import com.jio.digigov.notification.config.MultiTenantMongoConfig;
import com.jio.digigov.notification.enums.NotificationType;
import com.jio.digigov.notification.enums.ProviderType;
import com.jio.digigov.notification.enums.ScopeLevel;
import com.jio.digigov.notification.enums.TemplateStatus;
import com.jio.digigov.notification.entity.EmailTemplate;
import com.jio.digigov.notification.entity.NotificationConfig;
import com.jio.digigov.notification.entity.SMSTemplate;
import com.jio.digigov.notification.dto.digigov.ApproveTemplateRequestDto;
import com.jio.digigov.notification.dto.digigov.ApproveTemplateResponseDto;
import com.jio.digigov.notification.dto.digigov.OnboardTemplateRequestDto;
import com.jio.digigov.notification.dto.digigov.OnboardTemplateResponseDto;
import com.jio.digigov.notification.dto.request.EmailTemplateRequestDto;
import com.jio.digigov.notification.dto.request.SMSTemplateRequestDto;
import com.jio.digigov.notification.dto.response.TemplateCreationResponseDto;
import com.jio.digigov.notification.exception.ConfigurationNotFoundException;
import com.jio.digigov.notification.exception.DigiGovClientException;
import com.jio.digigov.notification.repository.NotificationConfigRepository;
import com.jio.digigov.notification.service.AuditService;
import com.jio.digigov.notification.service.DigiGovClientService;
import com.jio.digigov.notification.service.LegacyTemplateService;
import com.jio.digigov.notification.util.TenantContextHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Legacy Service for template management with complete DigiGov onboarding flow
 * Flow: Get Config → Generate Token → Onboard → Save DB → Approve → Update Status
 * Note: This is legacy - use unified template service for new implementations
 */
@Slf4j
@Service("legacyTemplateService")
@RequiredArgsConstructor
public class LegacyTemplateServiceImpl implements LegacyTemplateService {

    private final NotificationConfigRepository notificationConfigRepository;
    private final DigiGovClientService digiGovClientService;
    private final MultiTenantMongoConfig mongoConfig;
    private final AuditService auditService;
    // Note: We use MongoTemplate instead of repositories for multi-tenant operations

    /**
     * Create SMS template with complete onboarding flow
     * 1. Get configuration for tenant/business
     * 2. Call DigiGov onboard API
     * 3. Save template with PENDING status
     * 4. Call DigiGov approve API
     * 5. Update template status
     */
    @Transactional
    @Override
    public TemplateCreationResponseDto createSMSTemplate(
            SMSTemplateRequestDto request,
            String tenantId,
            String businessId,
            ScopeLevel scopeLevel,
            NotificationType notificationType,
            String transactionId) {

        OnboardTemplateRequestDto onboardRequest = buildSMSOnboardRequest(request);
        return createTemplate(onboardRequest, tenantId, businessId, scopeLevel, notificationType, transactionId, "SMS", "sms");
    }

    /**
     * Create Email template with complete onboarding flow
     */
    @Transactional
    @Override
    public TemplateCreationResponseDto createEmailTemplate(
            EmailTemplateRequestDto request,
            String tenantId,
            String businessId,
            ScopeLevel scopeLevel,
            NotificationType notificationType,
            String transactionId) {

        OnboardTemplateRequestDto onboardRequest = buildEmailOnboardRequest(request);
        return createTemplate(onboardRequest, tenantId, businessId, scopeLevel, notificationType, transactionId, "EMAIL", "email");
    }

    /**
     * Generic template creation method with complete onboarding flow
     * 1. Get configuration for tenant/business
     * 2. Call DigiGov onboard API
     * 3. Save template with PENDING status
     * 4. Call DigiGov approve API
     * 5. Update template status
     */
    private TemplateCreationResponseDto createTemplate(
            OnboardTemplateRequestDto onboardRequest,
            String tenantId,
            String businessId,
            ScopeLevel scopeLevel,
            NotificationType notificationType,
            String transactionId,
            String templateTypeUpper,
            String templateTypeLower) {

        log.info("Creating {} template for tenantId: {}, businessId: {}, scope: {}, type: {}",
                templateTypeUpper, tenantId, businessId, scopeLevel, notificationType);

        setupTenantContext(tenantId, businessId);
        Map<String, String> headers = buildHeaders(tenantId, businessId, scopeLevel, notificationType);

        try {
            NotificationConfig config = getAndValidateConfiguration(businessId, tenantId, templateTypeUpper);
            OnboardTemplateResponseDto onboardResponse = callOnboardAPI(onboardRequest, config, notificationType, transactionId);
            validateOnboardResponse(onboardResponse);

            Object savedTemplate = saveTemplateToDatabase(onboardRequest, onboardResponse.getTemplateId(),
                    businessId, scopeLevel, notificationType, tenantId, templateTypeUpper, headers);

            TemplateStatus finalStatus = approveTemplate(onboardResponse.getTemplateId(), templateTypeLower,
                    config, headers, notificationType, transactionId);

            updateTemplateStatus(savedTemplate, finalStatus, tenantId, templateTypeUpper, headers);

            TemplateCreationResponseDto response = buildTemplateResponse(savedTemplate, onboardResponse, templateTypeUpper);

            log.info("{} template created successfully: {} with status: {}",
                    templateTypeUpper, onboardResponse.getTemplateId(), finalStatus);

            return response;

        } catch (Exception e) {
            log.error("Failed to create {} template for tenantId: {}, businessId: {}", templateTypeUpper, tenantId, businessId);
            auditService.auditTemplateOperation(null, templateTypeUpper, "DB_OPERATION_FAILED", headers, null,
                    "Database operation failed during " + templateTypeUpper + " template creation: " + e.getMessage());
            throw e;
        } finally {
            TenantContextHolder.clear();
        }
    }

    /**
     * Setup tenant context for MongoDB operations
     */
    private void setupTenantContext(String tenantId, String businessId) {
        TenantContextHolder.setTenant(tenantId);
        TenantContextHolder.setBusinessId(businessId);
    }

    /**
     * Build request headers for audit operations
     */
    private Map<String, String> buildHeaders(String tenantId, String businessId, ScopeLevel scopeLevel, NotificationType notificationType) {
        return Map.of(
                "tenantId", tenantId,
                "businessId", businessId,
                "scopeLevel", scopeLevel.name(),
                "type", notificationType.name()
        );
    }

    /**
     * Get and validate DigiGov configuration
     */
    private NotificationConfig getAndValidateConfiguration(String businessId, String tenantId, String templateType) {
        NotificationConfig config = notificationConfigRepository.findWithFallback(businessId, tenantId)
                .orElseThrow(() -> new ConfigurationNotFoundException("Configuration not found for businessId: " + businessId));

        // Validate DigiGov configuration (legacy service only supports DigiGov)
        if (config.getProviderType() != ProviderType.DIGIGOV) {
            throw new IllegalArgumentException("Legacy template service only supports DigiGov provider, found: " + config.getProviderType());
        }

        if (config.getConfigurationJson() == null) {
            throw new IllegalArgumentException("DigiGov configuration must have configurationJson set");
        }

        log.debug("Using configuration: {} for network type: {} in create{}Template",
                config.getConfigId(),
                config.getConfigurationJson().getNetworkType(),
                templateType);
        return config;
    }

    /**
     * Call DigiGov onboard API
     */
    private OnboardTemplateResponseDto callOnboardAPI(OnboardTemplateRequestDto onboardRequest, NotificationConfig config,
            NotificationType notificationType, String transactionId) {
        return digiGovClientService.onboardTemplate(onboardRequest, config, notificationType, transactionId);
    }

    /**
     * Validate onboard API response
     */
    private void validateOnboardResponse(OnboardTemplateResponseDto onboardResponse) {
        if (onboardResponse.getTemplateId() == null || onboardResponse.getTemplateId().trim().isEmpty()) {
            throw new IllegalStateException("DigiGov onboard API did not return templateId");
        }
    }

    /**
     * Save template to database with audit logging
     */
    private Object saveTemplateToDatabase(OnboardTemplateRequestDto onboardRequest, String templateId,
            String businessId, ScopeLevel scopeLevel, NotificationType notificationType, String tenantId,
            String templateType, Map<String, String> headers) {

        auditService.auditTemplateOperation(templateId, templateType, "DB_SAVE_START",
                headers, null, "Starting template persistence to database");

        MongoTemplate tenantMongoTemplate = mongoConfig.getMongoTemplateForTenant(tenantId);
        Object savedTemplate;

        if ("SMS".equals(templateType)) {
            SMSTemplate smsTemplate = createSMSTemplateEntityFromOnboard(onboardRequest, templateId, businessId, scopeLevel, notificationType);
            savedTemplate = tenantMongoTemplate.save(smsTemplate);
        } else {
            EmailTemplate emailTemplate = createEmailTemplateEntityFromOnboard(onboardRequest, templateId, businessId, scopeLevel, notificationType);
            savedTemplate = tenantMongoTemplate.save(emailTemplate);
        }

        log.info("Saved {} template to database: {}", templateType, templateId);

        auditService.auditTemplateOperation(templateId, templateType, "DB_SAVE_SUCCESS",
                headers, Map.of("templateId", templateId, "status", TemplateStatus.PENDING),
                "Template saved to database with PENDING status");

        return savedTemplate;
    }

    /**
     * Update template status with audit logging
     */
    private void updateTemplateStatus(Object template, TemplateStatus finalStatus, String tenantId,
            String templateType, Map<String, String> headers) {

        String templateId = getTemplateId(template);
        TemplateStatus previousStatus = getTemplateStatus(template);

        auditService.auditTemplateOperation(templateId, templateType, "DB_UPDATE_START",
                headers, Map.of("previousStatus", previousStatus, "newStatus", finalStatus),
                "Starting template status update in database");

        setTemplateStatus(template, finalStatus);
        MongoTemplate tenantMongoTemplate = mongoConfig.getMongoTemplateForTenant(tenantId);
        tenantMongoTemplate.save(template);

        auditService.auditTemplateOperation(templateId, templateType, "DB_UPDATE_SUCCESS",
                headers, Map.of("finalStatus", finalStatus), "Template status updated in database to: " + finalStatus);
    }

    /**
     * Build SMS onboard request from SMS template request
     */
    private OnboardTemplateRequestDto buildSMSOnboardRequest(SMSTemplateRequestDto request) {
        return OnboardTemplateRequestDto.builder()
                .smsTemplate(OnboardTemplateRequestDto.SmsTemplateDto.builder()
                        .whiteListedNumber(request.getWhiteListedNumber())
                        .from(request.getFrom())
                        .oprCountries(request.getOprCountries())
                        .dltEntityId(request.getDltEntityId())
                        .dltTemplateId(request.getDltTemplateId())
                        .template(request.getTemplate())
                        .templateDetails(request.getTemplateDetails())
                        .build())
                .build();
    }

    /**
     * Build Email onboard request from Email template request
     */
    private OnboardTemplateRequestDto buildEmailOnboardRequest(EmailTemplateRequestDto request) {
        return OnboardTemplateRequestDto.builder()
                .emailTemplate(OnboardTemplateRequestDto.EmailTemplateDto.builder()
                        .to(request.getTo())
                        .cc(request.getCc())
                        .templateDetails(request.getTemplateDetails())
                        .templateBody(request.getTemplateBody())
                        .templateSubject(request.getTemplateSubject())
                        .templateFromName(request.getTemplateFromName())
                        .emailType(request.getEmailType())
                        .build())
                .build();
    }

    /**
     * Create SMS template entity from onboard request
     */
    private SMSTemplate createSMSTemplateEntityFromOnboard(OnboardTemplateRequestDto onboardRequest, String templateId,
            String businessId, ScopeLevel scopeLevel, NotificationType notificationType) {

        OnboardTemplateRequestDto.SmsTemplateDto smsDto = onboardRequest.getSmsTemplate();
        return SMSTemplate.builder()
                .templateId(templateId)
                .businessId(businessId)
                .scopeLevel(scopeLevel)
                .type(notificationType)
                .dltEntityId(smsDto.getDltEntityId())
                .dltTemplateId(smsDto.getDltTemplateId())
                .from(smsDto.getFrom())
                .oprCountries(smsDto.getOprCountries())
                .template(smsDto.getTemplate())
                .templateDetails(smsDto.getTemplateDetails())
                .whiteListedNumber(smsDto.getWhiteListedNumber())
                .status(TemplateStatus.PENDING)
                .build();
    }

    /**
     * Create Email template entity from onboard request
     */
    private EmailTemplate createEmailTemplateEntityFromOnboard(OnboardTemplateRequestDto onboardRequest, String templateId,
            String businessId, ScopeLevel scopeLevel, NotificationType notificationType) {

        OnboardTemplateRequestDto.EmailTemplateDto emailDto = onboardRequest.getEmailTemplate();
        return EmailTemplate.builder()
                .templateId(templateId)
                .businessId(businessId)
                .scopeLevel(scopeLevel)
                .type(notificationType)
                .to(emailDto.getTo())
                .cc(emailDto.getCc())
                .templateDetails(emailDto.getTemplateDetails())
                .templateBody(emailDto.getTemplateBody())
                .templateSubject(emailDto.getTemplateSubject())
                .templateFromName(emailDto.getTemplateFromName())
                .emailType(emailDto.getEmailType())
                .status(TemplateStatus.PENDING)
                .build();
    }

    /**
     * Build template response based on template type
     */
    private TemplateCreationResponseDto buildTemplateResponse(Object template, OnboardTemplateResponseDto onboardResponse, String templateType) {
        if ("SMS".equals(templateType)) {
            return buildSMSResponse((SMSTemplate) template, onboardResponse);
        } else {
            return buildEmailResponse((EmailTemplate) template, onboardResponse);
        }
    }

    /**
     * Get template ID from template object
     */
    private String getTemplateId(Object template) {
        if (template instanceof SMSTemplate) {
            return ((SMSTemplate) template).getTemplateId();
        } else if (template instanceof EmailTemplate) {
            return ((EmailTemplate) template).getTemplateId();
        }
        throw new IllegalArgumentException("Unknown template type");
    }

    /**
     * Get template status from template object
     */
    private TemplateStatus getTemplateStatus(Object template) {
        if (template instanceof SMSTemplate) {
            return ((SMSTemplate) template).getStatus();
        } else if (template instanceof EmailTemplate) {
            return ((EmailTemplate) template).getStatus();
        }
        throw new IllegalArgumentException("Unknown template type");
    }

    /**
     * Set template status on template object
     */
    private void setTemplateStatus(Object template, TemplateStatus status) {
        if (template instanceof SMSTemplate) {
            ((SMSTemplate) template).setStatus(status);
        } else if (template instanceof EmailTemplate) {
            ((EmailTemplate) template).setStatus(status);
        } else {
            throw new IllegalArgumentException("Unknown template type");
        }
    }

    /**
     * Approve template in DigiGov
     */
    private TemplateStatus approveTemplate(String templateId, String templateType, NotificationConfig config, Map<String, String> headers, NotificationType notificationType, String transactionId) {
        try {
            ApproveTemplateRequestDto approveRequest = ApproveTemplateRequestDto.builder()
                    .status("A") // A = Approve
                    .templateId(templateId)
                    .type(templateType)
                    .build();

            ApproveTemplateResponseDto approveResponse = digiGovClientService.approveTemplate(approveRequest, config, headers, notificationType, transactionId);
            log.info("DigiGov approve API response for templateId: {} - {}", templateId, approveResponse.getCombinedStatus());

            // Check approval status from response - both emailStatus and smsStatus must be "Active"
            if (approveResponse.isBothActive()) {
                return TemplateStatus.ACTIVE;

            } else if ("Pending".equalsIgnoreCase(approveResponse.getEmailStatus())
                    || "Pending".equalsIgnoreCase(approveResponse.getSmsStatus())) {
                return TemplateStatus.PENDING;

            } else {
                log.warn("Template approval returned unexpected status: {} for templateId: {}",
                        approveResponse.getCombinedStatus(), templateId);
                return TemplateStatus.FAILED;
            }

        } catch (DigiGovClientException e) {
            log.error("DigiGov approve API failed for templateId: {} - {}", templateId, e.getMessage());
            return TemplateStatus.FAILED;

        } catch (Exception e) {
            log.error("Unexpected error during template approval for templateId: {} - {}", templateId, e.getMessage());
            return TemplateStatus.PENDING; // Keep pending for retry
        }
    }

    /**
     * Create SMS template entity
     */
    private SMSTemplate createSMSTemplateEntity(
            SMSTemplateRequestDto request,
            String templateId,
            String businessId,
            ScopeLevel scopeLevel,
            NotificationType notificationType) {

        return SMSTemplate.builder()
                .templateId(templateId)
                .businessId(businessId)
                .scopeLevel(scopeLevel)
                .type(notificationType)
                .dltEntityId(request.getDltEntityId())
                .dltTemplateId(request.getDltTemplateId())
                .from(request.getFrom())
                .oprCountries(request.getOprCountries())
                .template(request.getTemplate())
                .templateDetails(request.getTemplateDetails())
                .whiteListedNumber(request.getWhiteListedNumber())
                .status(TemplateStatus.PENDING)
                .build();
    }

    /**
     * Create Email template entity
     */
    private EmailTemplate createEmailTemplateEntity(
            EmailTemplateRequestDto request,
            String templateId,
            String businessId,
            ScopeLevel scopeLevel,
            NotificationType notificationType) {

        return EmailTemplate.builder()
                .templateId(templateId)
                .businessId(businessId)
                .scopeLevel(scopeLevel)
                .type(notificationType)
                .to(request.getTo())
                .cc(request.getCc())
                .templateDetails(request.getTemplateDetails())
                .templateBody(request.getTemplateBody())
                .templateSubject(request.getTemplateSubject())
                .templateFromName(request.getTemplateFromName())
                .emailType(request.getEmailType())
                .status(TemplateStatus.PENDING)
                .build();
    }

    /**
     * Build SMS template creation response
     */
    private TemplateCreationResponseDto buildSMSResponse(SMSTemplate template, OnboardTemplateResponseDto onboardResponse) {
        Map<String, Object> smsTemplateMap = new LinkedHashMap<>();
        smsTemplateMap.put("whiteListedNumber", template.getWhiteListedNumber());
        smsTemplateMap.put("from", template.getFrom());
        smsTemplateMap.put("oprCountries", template.getOprCountries());
        smsTemplateMap.put("dltEntityId", template.getDltEntityId());
        smsTemplateMap.put("dltTemplateId", template.getDltTemplateId());
        smsTemplateMap.put("template", template.getTemplate());
        smsTemplateMap.put("templateDetails", template.getTemplateDetails());

        return TemplateCreationResponseDto.builder()
                .status(template.getStatus().name())
                .templateId(template.getTemplateId())
                .maxDailyLimit(onboardResponse.getMaxDailyLimit())
                .template(TemplateCreationResponseDto.TemplateDetails.builder()
                        .smsTemplate(smsTemplateMap)
                        .build())
                .build();
    }

    /**
     * Build Email template creation response
     */
    private TemplateCreationResponseDto buildEmailResponse(EmailTemplate template, OnboardTemplateResponseDto onboardResponse) {
        Map<String, Object> emailTemplateMap = new LinkedHashMap<>();
        emailTemplateMap.put("to", template.getTo());
        emailTemplateMap.put("cc", template.getCc());
        emailTemplateMap.put("templateDetails", template.getTemplateDetails());
        emailTemplateMap.put("templateBody", template.getTemplateBody());
        emailTemplateMap.put("templateSubject", template.getTemplateSubject());
        emailTemplateMap.put("templateFromName", template.getTemplateFromName());
        emailTemplateMap.put("emailType", template.getEmailType());

        return TemplateCreationResponseDto.builder()
                .status(template.getStatus().name())
                .templateId(template.getTemplateId())
                .maxDailyLimit(onboardResponse.getMaxDailyLimit())
                .template(TemplateCreationResponseDto.TemplateDetails.builder()
                        .emailTemplate(emailTemplateMap)
                        .build())
                .build();
    }
}