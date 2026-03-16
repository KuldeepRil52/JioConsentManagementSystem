package com.jio.digigov.notification.service.template.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jio.digigov.notification.dto.request.template.CreateTemplateRequestDto;
import com.jio.digigov.notification.dto.request.template.UpdateTemplateRequestDto;
import com.jio.digigov.notification.dto.request.template.SmsTemplateDto;
import com.jio.digigov.notification.dto.request.template.EmailTemplateDto;
import com.jio.digigov.notification.dto.request.template.TemplateFilterRequestDto;
import com.jio.digigov.notification.dto.response.common.CountResponseDto;
import com.jio.digigov.notification.dto.response.common.PagedResponseDto;
import com.jio.digigov.notification.dto.response.common.ResponseMetadata;
import com.jio.digigov.notification.dto.response.template.CreateTemplateResponseDto;
import com.jio.digigov.notification.dto.response.template.UpdateTemplateResponseDto;
import com.jio.digigov.notification.dto.response.template.TemplateResponseDto;
import com.jio.digigov.notification.entity.NotificationConfig;
import com.jio.digigov.notification.entity.template.NotificationTemplate;
import com.jio.digigov.notification.enums.NotificationType;
import com.jio.digigov.notification.enums.ProviderType;
import com.jio.digigov.notification.enums.ScopeLevel;
import com.jio.digigov.notification.enums.NotificationChannel;
import com.jio.digigov.notification.enums.TemplateStatus;
import com.jio.digigov.notification.exception.BusinessException;
import com.jio.digigov.notification.exception.ConfigurationNotFoundException;
import com.jio.digigov.notification.exception.ValidationException;
import com.jio.digigov.notification.service.DigiGovClientService;
import com.jio.digigov.notification.service.template.TemplateService;
import com.jio.digigov.notification.service.provider.NotificationProviderFactory;
import com.jio.digigov.notification.service.provider.NotificationProviderService;
import com.jio.digigov.notification.dto.provider.ProviderTemplateRequest;
import com.jio.digigov.notification.dto.provider.ProviderTemplateResponse;
import com.jio.digigov.notification.config.MultiTenantMongoConfig;
import com.jio.digigov.notification.util.MongoTemplateProvider;
import com.jio.digigov.notification.dto.digigov.OnboardTemplateRequestDto;
import com.jio.digigov.notification.dto.digigov.OnboardTemplateResponseDto;
import com.jio.digigov.notification.dto.digigov.ApproveTemplateRequestDto;
import com.jio.digigov.notification.dto.digigov.ApproveTemplateResponseDto;
import com.jio.digigov.notification.mapper.TemplateMapper;
import com.jio.digigov.notification.exception.TemplateNotFoundException;
import com.jio.digigov.notification.util.TenantContextHolder;
import com.jio.digigov.notification.repository.NotificationConfigRepository;
import com.jio.digigov.notification.entity.TenantMasterListConfig;
import com.jio.digigov.notification.enums.EventType;
import com.jio.digigov.notification.service.audit.AuditEventService;
import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service("templateService")
@RequiredArgsConstructor
public class TemplateServiceImpl implements TemplateService {

    private final DigiGovClientService digiGovClientService;
    private final NotificationProviderFactory providerFactory;
    private final MultiTenantMongoConfig mongoConfig;
    private final MongoTemplateProvider mongoTemplateProvider;
    private final ObjectMapper objectMapper;
    private final TemplateMapper templateMapper;
    private final NotificationConfigRepository notificationConfigRepository;
    private final AuditEventService auditEventService;

    @Override
    @Transactional
    public CreateTemplateResponseDto createTemplate(CreateTemplateRequestDto request,
                                               String tenantId,
                                               String businessId,
                                               ScopeLevel scopeLevel,
                                               NotificationType type,
                                               String transactionId,
                                               HttpServletRequest httpRequest) {

        log.info("Creating unified template for tenant: {}, business: {}, eventType: {}",
                tenantId, businessId, request.getEventType());

        setupTenantContext(tenantId, businessId);

        TemplateCreationContext context = prepareTemplateCreationContext(request, businessId, tenantId,
                scopeLevel, type);

        try {
            NotificationTemplate savedTemplate = processTemplateCreation(context, transactionId);

            // Audit template creation
            auditEventService.auditTemplateOperation(
                    savedTemplate.getTemplateId(),
                    savedTemplate.getStatus().toString(),
                    tenantId,
                    businessId,
                    transactionId,
                    httpRequest
            );

            return buildCreateResponse(savedTemplate, transactionId);

        } catch (Exception e) {
            // Audit failed template creation
            auditEventService.auditTemplateOperation(
                    context.template.getTemplateId() != null ? context.template.getTemplateId() : "FAILED",
                    "FAILED",
                    tenantId,
                    businessId,
                    transactionId,
                    httpRequest
            );
            return handleTemplateCreationFailure(context.template, request.getEventType(), tenantId, e);
        }
    }

    private void setupTenantContext(String tenantId, String businessId) {
        TenantContextHolder.setTenant(tenantId);
        TenantContextHolder.setBusinessId(businessId);
    }

    private TemplateCreationContext prepareTemplateCreationContext(CreateTemplateRequestDto request,
                                                                 String businessId, String tenantId,
                                                                 ScopeLevel scopeLevel, NotificationType type) {
        // Get configuration with 3-level fallback
        NotificationConfig config = notificationConfigRepository.findWithFallback(businessId, tenantId)
                .orElseThrow(() -> new ConfigurationNotFoundException("Configuration not found for businessId: " + businessId));
        String language = determineLanguage(request.getLanguage(), config);

        // Determine channel type for validation
        NotificationChannel channelType;
        if (request.getSmsTemplate() != null && request.getEmailTemplate() == null) {
            channelType = NotificationChannel.SMS;
        } else if (request.getEmailTemplate() != null && request.getSmsTemplate() == null) {
            channelType = NotificationChannel.EMAIL;
        } else {
            throw new ValidationException("Exactly one channel (SMS or EMAIL) must be provided");
        }

        // Determine provider type (use from request or default to DIGIGOV)
        ProviderType providerType = request.getProviderType() != null ?
                request.getProviderType() : ProviderType.DIGIGOV;

        String recipientType = request.getRecipientType() != null ? request.getRecipientType() : "DATA_PRINCIPAL";
        validateNoDuplicateTemplate(tenantId, businessId, request.getEventType(), language, type,
                channelType, recipientType, providerType, null);

        OnboardTemplateRequestDto digiGovRequest = buildOnboardTemplateRequest(request);
        NotificationTemplate template = buildTemplateEntity(request, tenantId, businessId,
                                                           scopeLevel, type, language, providerType);

        return new TemplateCreationContext(config, digiGovRequest, template);
    }

    private NotificationTemplate processTemplateCreation(TemplateCreationContext context, String transactionId) {
        // Use provider-based template creation
        ProviderType providerType = context.template.getProviderType();

        if (providerType == ProviderType.SMTP) {
            return processSmtpTemplateCreation(context, transactionId);
        } else {
            return processDigiGovTemplateCreation(context, transactionId);
        }
    }

    private NotificationTemplate processDigiGovTemplateCreation(TemplateCreationContext context, String transactionId) {
        // Original DigiGov flow
        OnboardTemplateResponseDto digiGovResponse = digiGovClientService.onboardTemplate(
            context.digiGovRequest, context.config, context.template.getType(), transactionId);

        processDigiGovResponse(context.template, digiGovResponse);

        if (context.template.getStatus() == TemplateStatus.ACTIVE) {
            approveTemplateIfActive(context.template, transactionId);
        }

        NotificationTemplate savedTemplate = saveTemplateToDatabase(context.template);

        // Update master list mappings after successful template creation
        updateMasterListMappingsAfterSave(savedTemplate);

        return savedTemplate;
    }

    private NotificationTemplate processSmtpTemplateCreation(TemplateCreationContext context, String transactionId) {
        log.info("Processing SMTP template creation for eventType: {}", context.template.getEventType());

        try {
            // Get NotificationConfig for SMTP
            MongoTemplate tenantMongoTemplate = mongoTemplateProvider.getTemplate(TenantContextHolder.getTenant());
            NotificationConfig notificationConfig = tenantMongoTemplate.findOne(
                    Query.query(Criteria.where("businessId").is(context.template.getBusinessId())),
                    NotificationConfig.class,
                    "notification_configurations"
            );

            if (notificationConfig == null) {
                throw new BusinessException("CONFIG_NOT_FOUND",
                        "NotificationConfig not found for businessId: " + context.template.getBusinessId());
            }

            // Get SMTP provider
            NotificationProviderService provider = providerFactory.getProvider(
                    ProviderType.SMTP,
                    context.template.getChannelType(),
                    notificationConfig
            );

            // Build provider template request
            ProviderTemplateRequest providerRequest = buildProviderTemplateRequest(context, transactionId);

            // Onboard template via provider
            ProviderTemplateResponse providerResponse = provider.onboardTemplate(providerRequest, notificationConfig);

            if (!providerResponse.getSuccess()) {
                context.template.setStatus(TemplateStatus.FAILED);
                throw new BusinessException("SMTP_TEMPLATE_CREATION_FAILED",
                        providerResponse.getErrorDescription());
            }

            // Set template ID from provider
            context.template.setTemplateId(providerResponse.getTemplateId());
            context.template.setStatus(TemplateStatus.ACTIVE); // SMTP doesn't require approval

            log.info("SMTP template created successfully: {}", context.template.getTemplateId());

            NotificationTemplate savedTemplate = saveTemplateToDatabase(context.template);

            // Update master list mappings after successful template creation
            updateMasterListMappingsAfterSave(savedTemplate);

            return savedTemplate;

        } catch (Exception e) {
            log.error("Error creating SMTP template");
            context.template.setStatus(TemplateStatus.FAILED);
            throw new BusinessException("SMTP_TEMPLATE_CREATION_FAILED", e.getMessage());
        }
    }

    private ProviderTemplateRequest buildProviderTemplateRequest(TemplateCreationContext context, String transactionId) {
        NotificationTemplate template = context.template;

        ProviderTemplateRequest.ProviderTemplateRequestBuilder builder = ProviderTemplateRequest.builder()
                .eventType(template.getEventType())
                .language(template.getLanguage())
                .notificationType(template.getType())
                .channelType(template.getChannelType())
                .recipientType(template.getRecipientType())
                .tenantId(TenantContextHolder.getTenant())
                .businessId(template.getBusinessId())
                .transactionId(transactionId);

        // Add channel-specific fields
        if (template.getChannelType() == NotificationChannel.SMS && template.getSmsConfig() != null) {
            var smsConfig = template.getSmsConfig();
            builder.smsTemplate(smsConfig.getTemplate())
                    .smsTemplateDetails(smsConfig.getTemplateDetails())
                    .whitelistedNumbers(smsConfig.getWhiteListedNumber())
                    .operatorCountries(smsConfig.getOprCountries())
                    .dltEntityId(smsConfig.getDltEntityId())
                    .dltTemplateId(smsConfig.getDltTemplateId())
                    .smsFrom(smsConfig.getFrom())
                    .smsArgumentsMap(smsConfig.getArgumentsMap());
        } else if (template.getChannelType() == NotificationChannel.EMAIL && template.getEmailConfig() != null) {
            var emailConfig = template.getEmailConfig();
            builder.emailSubject(emailConfig.getTemplateSubject())
                    .emailBody(emailConfig.getTemplateBody())
                    .emailTemplateDetails(emailConfig.getTemplateDetails())
                    .emailFromName(emailConfig.getTemplateFromName())
                    .emailType(emailConfig.getEmailType())
                    .emailTo(emailConfig.getTo())
                    .emailCc(emailConfig.getCc())
                    .emailSubjectArgumentsMap(emailConfig.getArgumentsSubjectMap())
                    .emailBodyArgumentsMap(emailConfig.getArgumentsBodyMap());
        }

        return builder.build();
    }

    private void approveTemplateIfActive(NotificationTemplate template, String transactionId) {
        Map<String, String> stringHeaders = new HashMap<>();
        stringHeaders.put("X-Transaction-Id", transactionId);
        approveTemplate(template, stringHeaders, template.getType(), transactionId);
    }

    private NotificationTemplate saveTemplateToDatabase(NotificationTemplate template) {
        // Manually set timestamps if not already set (workaround for multi-tenant auditing)
        LocalDateTime now = LocalDateTime.now();
        if (template.getCreatedAt() == null) {
            template.setCreatedAt(now);
        }
        if (template.getUpdatedAt() == null) {
            template.setUpdatedAt(now);
        }

        MongoTemplate tenantMongoTemplate = mongoTemplateProvider.getTemplate(TenantContextHolder.getTenant());
        NotificationTemplate savedTemplate = tenantMongoTemplate.save(template);
        log.info("Saved template to tenant-specific database: {} in tenant_db_{}",
                savedTemplate.getTemplateId(), TenantContextHolder.getTenant());
        return savedTemplate;
    }

    private CreateTemplateResponseDto handleTemplateCreationFailure(NotificationTemplate template,
                                                                  String eventType, String tenantId, Exception e) {
        log.error("Failed to create template for eventType: {}", eventType);
        template.setStatus(TemplateStatus.FAILED);

        // Manually set timestamps if not already set
        LocalDateTime now = LocalDateTime.now();
        if (template.getCreatedAt() == null) {
            template.setCreatedAt(now);
        }
        if (template.getUpdatedAt() == null) {
            template.setUpdatedAt(now);
        }

        MongoTemplate tenantMongoTemplate = mongoTemplateProvider.getTemplate(tenantId);
        tenantMongoTemplate.save(template);

        throw new BusinessException("TEMPLATE_CREATION_FAILED", "Template creation failed: " + e.getMessage());
    }

    private static class TemplateCreationContext {
        final NotificationConfig config;
        final OnboardTemplateRequestDto digiGovRequest;
        final NotificationTemplate template;

        TemplateCreationContext(NotificationConfig config, OnboardTemplateRequestDto digiGovRequest,
                              NotificationTemplate template) {
            this.config = config;
            this.digiGovRequest = digiGovRequest;
            this.template = template;
        }
    }
    
    private OnboardTemplateRequestDto buildOnboardTemplateRequest(CreateTemplateRequestDto request) {
        var builder = OnboardTemplateRequestDto.builder();
        
        // Add SMS template if provided
        if (request.getSmsTemplate() != null) {
            OnboardTemplateRequestDto.SmsTemplateDto smsDto = OnboardTemplateRequestDto.SmsTemplateDto.builder()
                .whiteListedNumber(request.getSmsTemplate().getWhiteListedNumber())
                .template(request.getSmsTemplate().getTemplate())
                .templateDetails(request.getSmsTemplate().getTemplateDetails())
                .oprCountries(request.getSmsTemplate().getOprCountries())
                .dltEntityId(request.getSmsTemplate().getDltEntityId())
                .dltTemplateId(request.getSmsTemplate().getDltTemplateId())
                .from(request.getSmsTemplate().getFrom())
                .build();
            builder.smsTemplate(smsDto);
        }
        
        // Add Email template if provided
        if (request.getEmailTemplate() != null) {
            OnboardTemplateRequestDto.EmailTemplateDto emailDto = OnboardTemplateRequestDto.EmailTemplateDto.builder()
                .to(request.getEmailTemplate().getTo())
                .cc(request.getEmailTemplate().getCc())
                .templateDetails(request.getEmailTemplate().getTemplateDetails())
                .templateBody(request.getEmailTemplate().getTemplateBody())
                .templateSubject(request.getEmailTemplate().getTemplateSubject())
                .templateFromName(request.getEmailTemplate().getTemplateFromName())
                .emailType(request.getEmailTemplate().getEmailType())
                .from(request.getEmailTemplate().getFrom())
                .replyTo(request.getEmailTemplate().getReplyTo())
                .build();
            builder.emailTemplate(emailDto);
        }
        
        return builder.build();
    }
    
    private void processDigiGovResponse(NotificationTemplate template, OnboardTemplateResponseDto response) {
        // Process OnboardTemplateResponseDto from DigiGov
        if (response == null || response.getTemplateId() == null || response.getTemplateId().trim().isEmpty()) {
            template.setStatus(TemplateStatus.FAILED);
            log.error("DigiGov template onboarding failed: invalid response - {}", response);
            throw new BusinessException("TEMPLATE_ONBOARD_FAILED", "DigiGov returned invalid response");
        }

        // Validate that we didn't receive a fallback response
        if ("CIRCUIT_BREAKER_FALLBACK".equals(response.getTemplateId())) {
            template.setStatus(TemplateStatus.FAILED);
            log.error("DigiGov template onboarding failed: received fallback response");
            throw new BusinessException("TEMPLATE_ONBOARD_FAILED", "DigiGov service is currently unavailable");
        }

        template.setTemplateId(response.getTemplateId());
        template.setStatus(TemplateStatus.ACTIVE);
        log.info("DigiGov template onboarded successfully: {}", template.getTemplateId());
    }
    
    private void approveTemplate(NotificationTemplate template, Map<String, String> headers,
                               NotificationType type, String transactionId) {
        ApproveTemplateRequestDto approveRequest = ApproveTemplateRequestDto.builder()
            .status("A") // A = Approved
            .templateId(template.getTemplateId())
            .type("all") // Keep as "all" - response structure depends on this
            .build();

        // Get configuration for approval with 3-level fallback
        String tenantId = TenantContextHolder.getTenantId();
        NotificationConfig config = notificationConfigRepository.findWithFallback(template.getBusinessId(), tenantId)
                .orElseThrow(() -> new ConfigurationNotFoundException("Configuration not found for businessId: " + template.getBusinessId()));

        // CRITICAL: Use ADMIN credentials for approval
        ApproveTemplateResponseDto approveResponse = digiGovClientService.approveTemplate(
            approveRequest, config, headers, type, transactionId);

        log.info("Approval response - templateId: {}, channelType: {}, full response: {}",
                template.getTemplateId(), template.getChannelType(), approveResponse);

        // Validate only the channel that was created
        boolean isApproved = approveResponse.isChannelActive(template.getChannelType());

        if (!isApproved) {
            String channelStatus = template.getChannelType() == NotificationChannel.SMS
                ? approveResponse.getSmsStatus()
                : approveResponse.getEmailStatus();

            log.error("Template approval failed for {} channel - status: {}",
                template.getChannelType(), channelStatus);

            throw new BusinessException("TEMPLATE_APPROVAL_FAILED",
                String.format("Template approval failed for %s channel with status: %s",
                    template.getChannelType(), channelStatus));
        }

        log.info("Template approved successfully for {} channel: {}",
            template.getChannelType(), template.getTemplateId());
    }
    
    private String determineLanguage(String requestLanguage, NotificationConfig config) {
        if (requestLanguage != null && !requestLanguage.trim().isEmpty()) {
            return requestLanguage;
        }

        // For DigiGov provider, use defaultLanguage from configurationJson
        if (config.getProviderType() == ProviderType.DIGIGOV &&
            config.getConfigurationJson() != null &&
            config.getConfigurationJson().getDefaultLanguage() != null) {
            return config.getConfigurationJson().getDefaultLanguage();
        }

        // For SMTP or if defaultLanguage not set, use "english" as default
        return "english";
    }
    
    
    private void validateNoDuplicateTemplate(String tenantId, String businessId,
                                           String eventType, String language,
                                           NotificationType type, NotificationChannel channelType,
                                           String recipientType, ProviderType providerType,
                                           String excludeTemplateId) {
        // Use explicit tenant MongoTemplate to query tenant-specific database
        MongoTemplate tenantMongoTemplate = mongoTemplateProvider.getTemplate(tenantId);
        log.info("Validating duplicate: tenantId={}, businessId={}, eventType={}, language={}, type={}, channelType={}, recipientType={}, providerType={}, excludeTemplateId={}",
                tenantId, businessId, eventType, language, type, channelType, recipientType, providerType, excludeTemplateId);

        Criteria criteria = Criteria.where("businessId").is(businessId)
                .and("eventType").is(eventType)
                .and("language").is(language)
                .and("type").is(type)
                .and("channelType").is(channelType)
                .and("recipientType").is(recipientType)
                .and("providerType").is(providerType);

        // Exclude specific templateId if provided (for update operations)
        if (excludeTemplateId != null && !excludeTemplateId.trim().isEmpty()) {
            criteria.and("templateId").ne(excludeTemplateId);
            log.info("Excluding templateId {} from duplicate check (update operation)", excludeTemplateId);
        }

        Query query = new Query().addCriteria(criteria);

        List<NotificationTemplate> existingTemplates =
                tenantMongoTemplate.find(query, NotificationTemplate.class);

        log.info("Duplicate check for eventType: {} in tenant_db_{} - matches found: {}",
                eventType, tenantId, existingTemplates.size());

        if (!existingTemplates.isEmpty()) {
            boolean hasActiveOrPending = existingTemplates.stream()
                    .anyMatch(t -> TemplateStatus.ACTIVE.equals(t.getStatus()) ||
                            TemplateStatus.PENDING.equals(t.getStatus()));
            if (hasActiveOrPending) {
                throw new ValidationException("Template already exists for this configuration");
            }
        }
    }
    
    private NotificationTemplate buildTemplateEntity(CreateTemplateRequestDto request,
                                                   String tenantId, String businessId,
                                                   ScopeLevel scopeLevel, NotificationType type,
                                                   String language, ProviderType providerType) {
        // Determine channel type with proper validation
        NotificationChannel channelType;
        if (request.getSmsTemplate() != null && request.getEmailTemplate() == null) {
            channelType = NotificationChannel.SMS;
        } else if (request.getEmailTemplate() != null && request.getSmsTemplate() == null) {
            channelType = NotificationChannel.EMAIL;
        } else {
            throw new ValidationException("Exactly one channel (SMS or EMAIL) must be provided");
        }

        // Set recipientType with default to DATA_PRINCIPAL for backward compatibility
        String recipientType = request.getRecipientType() != null && !request.getRecipientType().trim().isEmpty()
                ? request.getRecipientType()
                : "DATA_PRINCIPAL";

        NotificationTemplate.NotificationTemplateBuilder builder = NotificationTemplate.builder()
            .businessId(businessId)
            .scopeLevel(scopeLevel)
            .eventType(request.getEventType())
            .language(language)
            .type(type)
            .channelType(channelType)
            .recipientType(recipientType)
            .providerType(providerType)
            .status(TemplateStatus.PENDING)
            .version(1);

        // Set SMS config if provided
        if (request.getSmsTemplate() != null) {
            NotificationTemplate.SmsConfig smsConfig = NotificationTemplate.SmsConfig.builder()
                .whiteListedNumber(request.getSmsTemplate().getWhiteListedNumber())
                .template(request.getSmsTemplate().getTemplate())
                .templateDetails(request.getSmsTemplate().getTemplateDetails())
                .oprCountries(request.getSmsTemplate().getOprCountries())
                .dltEntityId(request.getSmsTemplate().getDltEntityId())
                .dltTemplateId(request.getSmsTemplate().getDltTemplateId())
                .from(request.getSmsTemplate().getFrom())
                .argumentsMap(request.getSmsTemplate().getArgumentsMap())
                .build();
            builder.smsConfig(smsConfig);
        }
        
        // Set Email config if provided
        if (request.getEmailTemplate() != null) {
            NotificationTemplate.EmailConfig emailConfig = NotificationTemplate.EmailConfig.builder()
                .to(request.getEmailTemplate().getTo())
                .cc(request.getEmailTemplate().getCc())
                .templateDetails(request.getEmailTemplate().getTemplateDetails())
                .templateBody(request.getEmailTemplate().getTemplateBody())
                .templateSubject(request.getEmailTemplate().getTemplateSubject())
                .templateFromName(request.getEmailTemplate().getTemplateFromName())
                .emailType(request.getEmailTemplate().getEmailType())
                .from(request.getEmailTemplate().getFrom())
                .replyTo(request.getEmailTemplate().getReplyTo())
                .argumentsSubjectMap(request.getEmailTemplate().getArgumentsSubjectMap())
                .argumentsBodyMap(request.getEmailTemplate().getArgumentsBodyMap())
                .build();
            builder.emailConfig(emailConfig);
        }
        
        return builder.build();
    }
    
    private CreateTemplateResponseDto buildCreateResponse(NotificationTemplate template, String transactionId) {
        return CreateTemplateResponseDto.builder()
            .templateId(template.getTemplateId())
            .channelType(template.getChannelType())
            .status(template.getStatus().name())
            .eventType(template.getEventType())
            .language(template.getLanguage())
            .createdAt(template.getCreatedAt() != null
                ? template.getCreatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                : LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
            .build();
    }
    
    // Additional methods for other service operations will be implemented next...
    
    @Override
    public PagedResponseDto<TemplateResponseDto> getAllTemplates(TemplateFilterRequestDto request, 
                                                         String tenantId, 
                                                         String businessId, 
                                                         ScopeLevel scopeLevel) {
        
        log.info("Getting all templates for tenant: {}, business: {}, filters: {}", 
                tenantId, businessId, request);
        
        // Set tenant context for multi-tenant database operations
        TenantContextHolder.setTenant(tenantId);
        TenantContextHolder.setBusinessId(businessId);
        
        // Build pageable
        Pageable pageable = buildPageable(request);
        
        // Get templates based on filters
        Page<NotificationTemplate> templatePage = findTemplatesWithFilters(
            tenantId, businessId, request, pageable);
        
        // Convert to response DTOs
        List<TemplateResponseDto> templateResponses = templatePage.getContent()
            .stream()
            .map(templateMapper::toTemplateResponse)
            .collect(Collectors.toList());
        
        // Build pagination info
        PagedResponseDto.PaginationInfo paginationInfo = PagedResponseDto.PaginationInfo.builder()
            .page(templatePage.getNumber() + 1) // Convert 0-based to 1-based
            .pageSize(templatePage.getSize())
            .totalItems(templatePage.getTotalElements())
            .totalPages(templatePage.getTotalPages())
            .hasNext(templatePage.hasNext())
            .hasPrevious(templatePage.hasPrevious())
            .build();

        return PagedResponseDto.<TemplateResponseDto>builder()
            .data(templateResponses)
            .pagination(paginationInfo)
            .build();
    }
    
    @Override
    public TemplateResponseDto getTemplateById(String templateId, 
                                          String tenantId, 
                                          String businessId, 
                                          ScopeLevel scopeLevel) {
        
        log.info("Getting template by ID: {} for tenant: {}, business: {}", 
                templateId, tenantId, businessId);
        
        // Set tenant context for multi-tenant database operations
        TenantContextHolder.setTenant(tenantId);
        TenantContextHolder.setBusinessId(businessId);
        
        // Find template by ID or DigiGov template ID
        NotificationTemplate template = findTemplateByIdOrDigiGovId(templateId, tenantId, businessId);
        
        if (template == null) {
            throw new TemplateNotFoundException("Template not found with ID: " + templateId);
        }
        
        return templateMapper.toTemplateResponse(template);
    }
    
    @Override
    public CountResponseDto getTemplateCount(TemplateFilterRequestDto request, 
                                        String tenantId, 
                                        String businessId, 
                                        ScopeLevel scopeLevel) {
        
        log.info("Getting template count for tenant: {}, business: {}, filters: {}", 
                tenantId, businessId, request);
        
        // Set tenant context for multi-tenant database operations
        TenantContextHolder.setTenant(tenantId);
        TenantContextHolder.setBusinessId(businessId);
        
        // Get total count using tenant MongoTemplate
        MongoTemplate tenantMongoTemplate = mongoTemplateProvider.getTemplate(tenantId);
        Query countQuery =
            new Query()
                .addCriteria(Criteria.where("businessId").is(businessId));
        
        long totalCount = tenantMongoTemplate.count(countQuery, NotificationTemplate.class);
        log.info("Total template count in tenant_db_{}: {}", tenantId, totalCount);
        
        // Build breakdown
        CountResponseDto.CountData.CountBreakdown breakdown = buildCountBreakdown(tenantId, businessId);

        CountResponseDto.CountData data = CountResponseDto.CountData.builder()
            .totalCount(totalCount)
            .breakdown(breakdown)
            .build();

        return CountResponseDto.builder()
            .data(data)
            .build();
    }
    
    @Override
    public TemplateResponseDto getByTemplateId(String templateId,
                                           String tenantId,
                                           String businessId,
                                           ScopeLevel scopeLevel) {

        log.info("Getting template by DigiGov template ID: {} for tenant: {}, business: {}",
                templateId, tenantId, businessId);

        // Set tenant context for multi-tenant database operations
        TenantContextHolder.setTenant(tenantId);
        TenantContextHolder.setBusinessId(businessId);

        try {
            // Use explicit tenant MongoTemplate to query tenant-specific database
            MongoTemplate tenantMongoTemplate = mongoTemplateProvider.getTemplate(tenantId);
            log.info("Searching for DigiGov templateId: {} in tenant_db_{}", templateId, tenantId);

            // Create OR criteria for businessId matching either X-Business-ID or X-Tenant-ID
            Criteria businessIdCriteria = new Criteria().orOperator(
                Criteria.where("businessId").is(businessId),
                Criteria.where("businessId").is(tenantId)
            );

            // Search by DigiGov template ID
            Query templateIdQuery = new Query()
                    .addCriteria(Criteria.where("templateId").is(templateId))
                    .addCriteria(businessIdCriteria);

            NotificationTemplate template = tenantMongoTemplate.findOne(templateIdQuery, NotificationTemplate.class);

            if (template != null) {
                log.info("Found template by DigiGov templateId in tenant_db_{}", tenantId);
                return templateMapper.toTemplateResponse(template);
            } else {
                log.warn("Template not found with DigiGov templateId: {} in tenant_db_{}", templateId, tenantId);
                throw new TemplateNotFoundException("Template not found with DigiGov template ID: " + templateId);
            }

        } finally {
            TenantContextHolder.clear();
        }
    }
    
    @Override
    @Transactional
    public void deleteByTemplateId(String templateId,
                                  String tenantId,
                                  String businessId,
                                  ScopeLevel scopeLevel,
                                  HttpServletRequest httpRequest) {

        log.info("Deleting template by DigiGov template ID: {} for tenant: {}, business: {}",
                templateId, tenantId, businessId);

        // Set tenant context for multi-tenant database operations
        TenantContextHolder.setTenant(tenantId);
        TenantContextHolder.setBusinessId(businessId);

        try {
            // Use explicit tenant MongoTemplate to query tenant-specific database
            MongoTemplate tenantMongoTemplate = mongoTemplateProvider.getTemplate(tenantId);
            log.info("Searching for DigiGov templateId: {} to delete in tenant_db_{}", templateId, tenantId);

            // For DELETE operations, use exact businessId match (only owner can delete)
            Query templateIdQuery = new Query()
                    .addCriteria(Criteria.where("templateId").is(templateId)
                        .and("businessId").is(businessId));

            NotificationTemplate template = tenantMongoTemplate.findOne(templateIdQuery, NotificationTemplate.class);

            if (template != null) {
                log.info("Found template by DigiGov templateId in tenant_db_{}, proceeding with deletion", tenantId);

                // Store eventType before deletion for rebuilding mappings
                String eventType = template.getEventType();

                // Delete the template
                tenantMongoTemplate.remove(templateIdQuery, NotificationTemplate.class);

                log.info("Successfully deleted template with DigiGov templateId: {} from tenant_db_{}",
                        templateId, tenantId);

                // Audit template deletion
                auditEventService.auditTemplateOperation(
                        templateId,
                        "DELETED",
                        tenantId,
                        businessId,
                        null, // No transaction ID for delete
                        httpRequest
                );

                // Rebuild master list mappings for this event after deletion
                rebuildMasterListMappingsForEvent(eventType, businessId, tenantMongoTemplate);
            } else {
                log.warn("Template not found with DigiGov templateId: {} in tenant_db_{}", templateId, tenantId);
                throw new TemplateNotFoundException("Template not found with DigiGov template ID: " + templateId);
            }

        } finally {
            TenantContextHolder.clear();
        }
    }

    @Override
    @Transactional
    public UpdateTemplateResponseDto updateTemplateById(String id,
                                                       UpdateTemplateRequestDto request,
                                                       String tenantId,
                                                       String businessId,
                                                       ScopeLevel scopeLevel,
                                                       NotificationType type,
                                                       String transactionId,
                                                       HttpServletRequest httpRequest) {

        log.info("Updating template by MongoDB ID: {} for tenant: {}, business: {}", id, tenantId, businessId);

        // Set tenant context for multi-tenant database operations
        TenantContextHolder.setTenant(tenantId);
        TenantContextHolder.setBusinessId(businessId);

        try {
            // Find existing template by MongoDB ObjectId
            MongoTemplate tenantMongoTemplate = mongoTemplateProvider.getTemplate(tenantId);

            // Validate and convert String ID to ObjectId
            ObjectId objectId;
            try {
                objectId = new ObjectId(id);
                log.debug("Converted string ID {} to ObjectId: {}", id, objectId);
            } catch (IllegalArgumentException e) {
                log.error("Invalid MongoDB ObjectId format: {}", id);
                throw new ValidationException("Invalid template ID format: " + id);
            }

            // For UPDATE operations, use exact businessId match (only owner can update)
            Query idQuery = new Query()
                    .addCriteria(Criteria.where("_id").is(objectId)
                        .and("businessId").is(businessId));

            NotificationTemplate existingTemplate = tenantMongoTemplate.findOne(idQuery, NotificationTemplate.class);

            if (existingTemplate == null) {
                log.warn("Template not found with MongoDB ID: {} for businessId: {}", id, businessId);
                throw new TemplateNotFoundException("Template not found with ID: " + id + " or access denied");
            }

            log.info("Found template by MongoDB ID, proceeding with update. Old templateId: {}",
                    existingTemplate.getTemplateId());

            // Perform update (delete old, create new)
            return performTemplateUpdate(existingTemplate, request, tenantId, businessId,
                                        scopeLevel, type, transactionId, tenantMongoTemplate, httpRequest);

        } finally {
            TenantContextHolder.clear();
        }
    }

    @Override
    @Transactional
    public UpdateTemplateResponseDto updateTemplateByTemplateId(String templateId,
                                                               UpdateTemplateRequestDto request,
                                                               String tenantId,
                                                               String businessId,
                                                               ScopeLevel scopeLevel,
                                                               NotificationType type,
                                                               String transactionId,
                                                               HttpServletRequest httpRequest) {

        log.info("Updating template by DigiGov template ID: {} for tenant: {}, business: {}",
                templateId, tenantId, businessId);

        // Set tenant context for multi-tenant database operations
        TenantContextHolder.setTenant(tenantId);
        TenantContextHolder.setBusinessId(businessId);

        try {
            // Find existing template by DigiGov template ID
            MongoTemplate tenantMongoTemplate = mongoTemplateProvider.getTemplate(tenantId);

            // For UPDATE operations, use exact businessId match (only owner can update)
            Query templateIdQuery = new Query()
                    .addCriteria(Criteria.where("templateId").is(templateId)
                        .and("businessId").is(businessId));

            NotificationTemplate existingTemplate = tenantMongoTemplate.findOne(templateIdQuery, NotificationTemplate.class);

            if (existingTemplate == null) {
                throw new TemplateNotFoundException("Template not found with DigiGov template ID: " + templateId + " or access denied");
            }

            log.info("Found template by DigiGov templateId, proceeding with update. Old templateId: {}",
                    existingTemplate.getTemplateId());

            // Perform update (delete old, create new)
            return performTemplateUpdate(existingTemplate, request, tenantId, businessId,
                                        scopeLevel, type, transactionId, tenantMongoTemplate, httpRequest);

        } finally {
            TenantContextHolder.clear();
        }
    }

    /**
     * Performs the actual template update: validates, calls DigiGov workflow, updates existing document
     * Preserves MongoDB _id, only updates content and DigiGov templateId
     */
    private UpdateTemplateResponseDto performTemplateUpdate(NotificationTemplate existingTemplate,
                                                           UpdateTemplateRequestDto request,
                                                           String tenantId,
                                                           String businessId,
                                                           ScopeLevel scopeLevel,
                                                           NotificationType type,
                                                           String transactionId,
                                                           MongoTemplate tenantMongoTemplate,
                                                           HttpServletRequest httpRequest) {

        String oldTemplateId = existingTemplate.getTemplateId();
        String mongoId = existingTemplate.getId(); // Preserve MongoDB _id

        // Validate that channel type matches
        NotificationChannel requestChannelType;
        if (request.getSmsTemplate() != null && request.getEmailTemplate() == null) {
            requestChannelType = NotificationChannel.SMS;
        } else if (request.getEmailTemplate() != null && request.getSmsTemplate() == null) {
            requestChannelType = NotificationChannel.EMAIL;
        } else {
            throw new ValidationException("Exactly one channel (SMS or EMAIL) must be provided");
        }

        if (requestChannelType != existingTemplate.getChannelType()) {
            throw new ValidationException("Cannot change channel type. Existing: " +
                    existingTemplate.getChannelType() + ", Requested: " + requestChannelType);
        }

        log.info("Validation passed. Calling DigiGov workflow to get new templateId. MongoDB _id {} will be preserved", mongoId);

        // Merge update request with existing template values (use existing for fields not provided)
        UpdateTemplateRequestDto mergedRequest = mergeWithExistingTemplate(request, existingTemplate);
        log.info("Merged update request with existing template values for fields not provided in request");

        // Build CreateTemplateRequestDto with merged content
        CreateTemplateRequestDto createRequest = buildCreateRequestFromUpdate(existingTemplate, mergedRequest);

        // Prepare for DigiGov workflow - Get configuration with 3-level fallback
        NotificationConfig config = notificationConfigRepository.findWithFallback(businessId, tenantId)
                .orElseThrow(() -> new ConfigurationNotFoundException("Configuration not found for businessId: " + businessId));
        String language = existingTemplate.getLanguage(); // Preserve existing language

        // Validate no duplicate template exists (excluding the old template being updated)
        String recipientType = existingTemplate.getRecipientType();
        ProviderType providerType = existingTemplate.getProviderType() != null ?
                existingTemplate.getProviderType() : ProviderType.DIGIGOV;
        validateNoDuplicateTemplate(tenantId, businessId, existingTemplate.getEventType(), language,
                                   type, existingTemplate.getChannelType(), recipientType, providerType, oldTemplateId);

        // Build DigiGov onboard request with merged values
        OnboardTemplateRequestDto digiGovRequest = buildOnboardTemplateRequest(createRequest);

        try {
            // Step 1: Call DigiGov onboard to get new templateId
            log.info("Calling DigiGov onboard API for update. Old templateId: {}", oldTemplateId);
            OnboardTemplateResponseDto digiGovResponse = digiGovClientService.onboardTemplate(
                digiGovRequest, config, type, transactionId);

            // Validate DigiGov response
            if (digiGovResponse == null || digiGovResponse.getTemplateId() == null ||
                digiGovResponse.getTemplateId().trim().isEmpty()) {
                log.error("DigiGov onboard failed: invalid response - {}", digiGovResponse);
                throw new BusinessException("TEMPLATE_ONBOARD_FAILED", "DigiGov returned invalid response");
            }

            if ("CIRCUIT_BREAKER_FALLBACK".equals(digiGovResponse.getTemplateId())) {
                log.error("DigiGov onboard failed: circuit breaker fallback");
                throw new BusinessException("TEMPLATE_ONBOARD_FAILED", "DigiGov service is currently unavailable");
            }

            String newTemplateId = digiGovResponse.getTemplateId();
            log.info("DigiGov onboard successful. New templateId: {}", newTemplateId);

            // Step 2: Auto-approve the new template
            log.info("Auto-approving new template: {}", newTemplateId);
            Map<String, String> approvalHeaders = new HashMap<>();
            approvalHeaders.put("X-Transaction-Id", transactionId);

            ApproveTemplateRequestDto approveRequest = ApproveTemplateRequestDto.builder()
                .status("A") // A = Approved
                .templateId(newTemplateId)
                .type("all")
                .build();

            ApproveTemplateResponseDto approveResponse = digiGovClientService.approveTemplate(
                approveRequest, config, approvalHeaders, type, transactionId);

            boolean isApproved = approveResponse.isChannelActive(existingTemplate.getChannelType());
            if (!isApproved) {
                String channelStatus = existingTemplate.getChannelType() == NotificationChannel.SMS
                    ? approveResponse.getSmsStatus()
                    : approveResponse.getEmailStatus();
                log.error("Template approval failed for {} channel - status: {}",
                    existingTemplate.getChannelType(), channelStatus);
                throw new BusinessException("TEMPLATE_APPROVAL_FAILED",
                    String.format("Template approval failed for %s channel with status: %s",
                        existingTemplate.getChannelType(), channelStatus));
            }

            log.info("Template approved successfully: {}", newTemplateId);

            // Step 3: Update existing template document with new content and new templateId
            // IMPORTANT: Keep same MongoDB _id, only update fields
            existingTemplate.setTemplateId(newTemplateId);
            existingTemplate.setStatus(TemplateStatus.ACTIVE);

            // Update content fields based on channel type using merged request
            if (mergedRequest.getSmsTemplate() != null) {
                NotificationTemplate.SmsConfig smsConfig = NotificationTemplate.SmsConfig.builder()
                    .whiteListedNumber(mergedRequest.getSmsTemplate().getWhiteListedNumber())
                    .template(mergedRequest.getSmsTemplate().getTemplate())
                    .templateDetails(mergedRequest.getSmsTemplate().getTemplateDetails())
                    .oprCountries(mergedRequest.getSmsTemplate().getOprCountries())
                    .dltEntityId(mergedRequest.getSmsTemplate().getDltEntityId())
                    .dltTemplateId(mergedRequest.getSmsTemplate().getDltTemplateId())
                    .from(mergedRequest.getSmsTemplate().getFrom())
                    .argumentsMap(mergedRequest.getSmsTemplate().getArgumentsMap())
                    .build();
                existingTemplate.setSmsConfig(smsConfig);
            } else if (mergedRequest.getEmailTemplate() != null) {
                NotificationTemplate.EmailConfig emailConfig = NotificationTemplate.EmailConfig.builder()
                    .to(mergedRequest.getEmailTemplate().getTo())
                    .cc(mergedRequest.getEmailTemplate().getCc())
                    .templateDetails(mergedRequest.getEmailTemplate().getTemplateDetails())
                    .templateBody(mergedRequest.getEmailTemplate().getTemplateBody())
                    .templateSubject(mergedRequest.getEmailTemplate().getTemplateSubject())
                    .templateFromName(mergedRequest.getEmailTemplate().getTemplateFromName())
                    .emailType(mergedRequest.getEmailTemplate().getEmailType())
                    .argumentsSubjectMap(mergedRequest.getEmailTemplate().getArgumentsSubjectMap())
                    .argumentsBodyMap(mergedRequest.getEmailTemplate().getArgumentsBodyMap())
                    .build();
                existingTemplate.setEmailConfig(emailConfig);
            }

            // Update timestamp
            existingTemplate.setUpdatedAt(LocalDateTime.now());

            // Save updated template (MongoDB _id remains the same)
            NotificationTemplate updatedTemplate = tenantMongoTemplate.save(existingTemplate);

            log.info("Template updated successfully. MongoDB _id: {} preserved, Old templateId: {}, New templateId: {}",
                    mongoId, oldTemplateId, newTemplateId);

            // Update master list mappings after successful template update
            updateMasterListMappingsAfterSave(updatedTemplate);

            // Audit template update
            auditEventService.auditTemplateOperation(
                    newTemplateId,
                    updatedTemplate.getStatus().toString(),
                    tenantId,
                    businessId,
                    transactionId,
                    httpRequest
            );

            return buildUpdateResponse(oldTemplateId, updatedTemplate);

        } catch (Exception e) {
            log.error("Failed to update template. Old template preserved with templateId: {}", oldTemplateId);
            // Audit failed template update
            auditEventService.auditTemplateOperation(
                    oldTemplateId,
                    "FAILED",
                    tenantId,
                    businessId,
                    transactionId,
                    httpRequest
            );
            throw new BusinessException("TEMPLATE_UPDATE_FAILED",
                    "Template update failed: " + e.getMessage() + ". Old template has been preserved.");
        }
    }

    /**
     * Merges update request with existing template values
     * For any field not provided in request, uses value from existing template
     */
    private UpdateTemplateRequestDto mergeWithExistingTemplate(UpdateTemplateRequestDto request,
                                                               NotificationTemplate existingTemplate) {
        UpdateTemplateRequestDto merged = new UpdateTemplateRequestDto();

        if (existingTemplate.getChannelType() == NotificationChannel.SMS) {
            // Merge SMS template fields
            SmsTemplateDto mergedSms = new SmsTemplateDto();
            SmsTemplateDto requestSms = request.getSmsTemplate();
            NotificationTemplate.SmsConfig existingSms = existingTemplate.getSmsConfig();

            // Use request value if provided, otherwise use existing
            mergedSms.setWhiteListedNumber(
                requestSms != null && requestSms.getWhiteListedNumber() != null
                    ? requestSms.getWhiteListedNumber()
                    : existingSms.getWhiteListedNumber()
            );

            mergedSms.setTemplate(
                requestSms != null && requestSms.getTemplate() != null
                    ? requestSms.getTemplate()
                    : existingSms.getTemplate()
            );

            mergedSms.setTemplateDetails(
                requestSms != null && requestSms.getTemplateDetails() != null
                    ? requestSms.getTemplateDetails()
                    : existingSms.getTemplateDetails()
            );

            mergedSms.setOprCountries(
                requestSms != null && requestSms.getOprCountries() != null
                    ? requestSms.getOprCountries()
                    : existingSms.getOprCountries()
            );

            mergedSms.setDltEntityId(
                requestSms != null && requestSms.getDltEntityId() != null
                    ? requestSms.getDltEntityId()
                    : existingSms.getDltEntityId()
            );

            mergedSms.setDltTemplateId(
                requestSms != null && requestSms.getDltTemplateId() != null
                    ? requestSms.getDltTemplateId()
                    : existingSms.getDltTemplateId()
            );

            mergedSms.setFrom(
                requestSms != null && requestSms.getFrom() != null
                    ? requestSms.getFrom()
                    : existingSms.getFrom()
            );

            mergedSms.setArgumentsMap(
                requestSms != null && requestSms.getArgumentsMap() != null
                    ? requestSms.getArgumentsMap()
                    : existingSms.getArgumentsMap()
            );

            merged.setSmsTemplate(mergedSms);
            log.debug("Merged SMS template - fields from request and existing: template={}, dltEntityId={}, dltTemplateId={}",
                mergedSms.getTemplate() != null ? "provided" : "from-existing",
                mergedSms.getDltEntityId(),
                mergedSms.getDltTemplateId());

        } else if (existingTemplate.getChannelType() == NotificationChannel.EMAIL) {
            // Merge Email template fields
            EmailTemplateDto mergedEmail = new EmailTemplateDto();
            EmailTemplateDto requestEmail = request.getEmailTemplate();
            NotificationTemplate.EmailConfig existingEmail = existingTemplate.getEmailConfig();

            // Use request value if provided, otherwise use existing
            mergedEmail.setTo(
                requestEmail != null && requestEmail.getTo() != null
                    ? requestEmail.getTo()
                    : existingEmail.getTo()
            );

            mergedEmail.setCc(
                requestEmail != null && requestEmail.getCc() != null
                    ? requestEmail.getCc()
                    : existingEmail.getCc()
            );

            mergedEmail.setTemplateDetails(
                requestEmail != null && requestEmail.getTemplateDetails() != null
                    ? requestEmail.getTemplateDetails()
                    : existingEmail.getTemplateDetails()
            );

            mergedEmail.setTemplateBody(
                requestEmail != null && requestEmail.getTemplateBody() != null
                    ? requestEmail.getTemplateBody()
                    : existingEmail.getTemplateBody()
            );

            mergedEmail.setTemplateSubject(
                requestEmail != null && requestEmail.getTemplateSubject() != null
                    ? requestEmail.getTemplateSubject()
                    : existingEmail.getTemplateSubject()
            );

            mergedEmail.setTemplateFromName(
                requestEmail != null && requestEmail.getTemplateFromName() != null
                    ? requestEmail.getTemplateFromName()
                    : existingEmail.getTemplateFromName()
            );

            mergedEmail.setEmailType(
                requestEmail != null && requestEmail.getEmailType() != null
                    ? requestEmail.getEmailType()
                    : existingEmail.getEmailType()
            );

            mergedEmail.setArgumentsSubjectMap(
                requestEmail != null && requestEmail.getArgumentsSubjectMap() != null
                    ? requestEmail.getArgumentsSubjectMap()
                    : existingEmail.getArgumentsSubjectMap()
            );

            mergedEmail.setArgumentsBodyMap(
                requestEmail != null && requestEmail.getArgumentsBodyMap() != null
                    ? requestEmail.getArgumentsBodyMap()
                    : existingEmail.getArgumentsBodyMap()
            );

            merged.setEmailTemplate(mergedEmail);
            log.debug("Merged Email template - fields from request and existing: subject={}, body={}",
                mergedEmail.getTemplateSubject() != null ? "provided" : "from-existing",
                mergedEmail.getTemplateBody() != null ? "provided" : "from-existing");
        }

        return merged;
    }

    /**
     * Builds CreateTemplateRequestDto from existing template and update request
     */
    private CreateTemplateRequestDto buildCreateRequestFromUpdate(NotificationTemplate existingTemplate,
                                                                  UpdateTemplateRequestDto request) {
        CreateTemplateRequestDto createRequest = new CreateTemplateRequestDto();

        // Set immutable identifying fields from existing template
        createRequest.setEventType(existingTemplate.getEventType());
        createRequest.setLanguage(existingTemplate.getLanguage());
        createRequest.setRecipientType(existingTemplate.getRecipientType());

        // Set updated content from request (already merged with existing values)
        createRequest.setSmsTemplate(request.getSmsTemplate());
        createRequest.setEmailTemplate(request.getEmailTemplate());

        return createRequest;
    }

    /**
     * Builds UpdateTemplateResponseDto from old and new template information
     */
    private UpdateTemplateResponseDto buildUpdateResponse(String oldTemplateId, NotificationTemplate savedTemplate) {
        DateTimeFormatter formatter = DateTimeFormatter.ISO_DATE_TIME;

        return UpdateTemplateResponseDto.builder()
                .oldTemplateId(oldTemplateId)
                .newTemplateId(savedTemplate.getTemplateId())
                .channelType(savedTemplate.getChannelType())
                .status(savedTemplate.getStatus().toString())
                .eventType(savedTemplate.getEventType())
                .language(savedTemplate.getLanguage())
                .updatedAt(savedTemplate.getUpdatedAt() != null ? savedTemplate.getUpdatedAt().format(formatter) : null)
                .build();
    }

    // Helper methods for pagination and filtering
    
    private Pageable buildPageable(TemplateFilterRequestDto request) {
        // Parse sort
        String[] sortParts = request.getSort().split(":");
        String sortField = sortParts[0];
        Sort.Direction direction = sortParts.length > 1 && "desc".equalsIgnoreCase(sortParts[1]) 
            ? Sort.Direction.DESC : Sort.Direction.ASC;
        
        Sort sort = Sort.by(direction, sortField);
        
        // Ensure page is 0-based for Spring Data
        int page = Math.max(0, request.getPage() - 1);
        int size = Math.max(1, Math.min(100, request.getPageSize()));
        
        return PageRequest.of(page, size, sort);
    }
    
    private Page<NotificationTemplate> findTemplatesWithFilters(String tenantId, String businessId,
                                                               TemplateFilterRequestDto request, Pageable pageable) {
        MongoTemplate tenantMongoTemplate = mongoTemplateProvider.getTemplate(tenantId);
        log.info("Searching templates in tenant_db_{} with filters: {}", tenantId, request);

        Query query = buildBaseQuery(businessId, tenantId);
        applyFilterCriteria(query, request);
        query.with(pageable);

        List<NotificationTemplate> templates = tenantMongoTemplate.find(query, NotificationTemplate.class);
        long total = executeCountQuery(tenantMongoTemplate, query);

        log.info("Found {} templates in tenant_db_{} with total count: {}", templates.size(), tenantId, total);
        return new org.springframework.data.domain.PageImpl<>(templates, pageable, total);
    }

    private Query buildBaseQuery(String businessId, String tenantId) {
        // Allow templates to be retrieved if businessId matches either X-Business-ID or X-Tenant-ID header
        Criteria businessIdCriteria = new Criteria().orOperator(
            Criteria.where("businessId").is(businessId),
            Criteria.where("businessId").is(tenantId)
        );
        return new Query().addCriteria(businessIdCriteria);
    }

    private void applyFilterCriteria(Query query, TemplateFilterRequestDto request) {
        if (hasSearchFilter(request)) {
            applySearchFilter(query, request.getSearch().trim());
        } else {
            applySpecificFilters(query, request);
        }
    }

    private boolean hasSearchFilter(TemplateFilterRequestDto request) {
        return request.getSearch() != null && !request.getSearch().trim().isEmpty();
    }

    private void applySearchFilter(Query query, String search) {
        Criteria searchCriteria = new Criteria().orOperator(
            Criteria.where("eventType").regex(search, "i"),
            Criteria.where("language").regex(search, "i"),
            Criteria.where("templateId").regex(search, "i")
        );
        query.addCriteria(searchCriteria);
    }

    private void applySpecificFilters(Query query, TemplateFilterRequestDto request) {
        String eventType = parseStringFilter(request.getEventType());
        String language = parseStringFilter(request.getLanguage());
        String channel = parseStringFilter(request.getChannel());
        TemplateStatus status = parseEnumFilter(request.getStatus(), TemplateStatus.class);
        NotificationType type = parseEnumFilter(request.getType(), NotificationType.class);

        if (eventType != null) query.addCriteria(Criteria.where("eventType").is(eventType));
        if (language != null) query.addCriteria(Criteria.where("language").is(language));
        if (status != null) query.addCriteria(Criteria.where("status").is(status));
        if (type != null) query.addCriteria(Criteria.where("type").is(type));

        applyChannelFilter(query, channel);
    }

    private void applyChannelFilter(Query query, String channel) {
        if (channel != null) {
            if (NotificationChannel.SMS.name().equalsIgnoreCase(channel)) {
                query.addCriteria(Criteria.where("smsConfig").ne(null));
            } else if (NotificationChannel.EMAIL.name().equalsIgnoreCase(channel)) {
                query.addCriteria(Criteria.where("emailConfig").ne(null));
            }
        }
    }

    private long executeCountQuery(MongoTemplate tenantMongoTemplate, Query query) {
        Query countQuery = Query.of(query);
        countQuery.limit(0);
        countQuery.skip(0);
        return tenantMongoTemplate.count(countQuery, NotificationTemplate.class);
    }
    
    private String parseStringFilter(String filter) {
        if (filter == null || filter.trim().isEmpty()) {
            return null;
        }
        // For now, take the first value if comma-separated
        return filter.split(",")[0].trim();
    }
    
    private <T extends Enum<T>> T parseEnumFilter(String filter, Class<T> enumClass) {
        if (filter == null || filter.trim().isEmpty()) {
            return null;
        }
        try {
            return Enum.valueOf(enumClass, filter.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
    
    private NotificationTemplate findTemplateByIdOrDigiGovId(String templateId, String tenantId, String businessId) {
        // Use explicit tenant MongoTemplate to query tenant-specific database
        MongoTemplate tenantMongoTemplate = mongoConfig.getMongoTemplateForTenant(tenantId);
        log.info("Searching for template ID: {} in tenant_db_{}", templateId, tenantId);

        // Create OR criteria for businessId matching either X-Business-ID or X-Tenant-ID
        Criteria businessIdCriteria = new Criteria().orOperator(
            Criteria.where("businessId").is(businessId),
            Criteria.where("businessId").is(tenantId)
        );

        // First try by internal MongoDB ID
        try {
            Query idQuery =
                new Query()
                    .addCriteria(Criteria.where("id").is(templateId))
                    .addCriteria(businessIdCriteria);

            NotificationTemplate template = tenantMongoTemplate.findOne(idQuery, NotificationTemplate.class);
            if (template != null) {
                log.info("Found template by internal ID in tenant_db_{}", tenantId);
                return template;
            }
        } catch (Exception e) {
            // Continue to search by DigiGov template ID
        }

        // Then try by DigiGov template ID
        Query templateIdQuery =
            new Query()
                .addCriteria(Criteria.where("templateId").is(templateId))
                .addCriteria(businessIdCriteria);

        NotificationTemplate template = tenantMongoTemplate.findOne(templateIdQuery, NotificationTemplate.class);
        if (template != null) {
            log.info("Found template by DigiGov templateId in tenant_db_{}", tenantId);
        } else {
            log.warn("Template not found with ID: {} in tenant_db_{}", templateId, tenantId);
        }

        return template;
    }
    
    private CountResponseDto.CountData.CountBreakdown buildCountBreakdown(String tenantId, String businessId) {
        MongoTemplate tenantMongoTemplate = mongoConfig.getMongoTemplateForTenant(tenantId);
        log.info("Building count breakdown from tenant_db_{}", tenantId);

        List<NotificationTemplate> allTemplates = fetchAllTemplatesForBusiness(tenantMongoTemplate, businessId);

        Map<String, Integer> byChannel = buildChannelBreakdown(allTemplates);
        Map<String, Integer> byStatus = buildStatusBreakdown(tenantMongoTemplate, businessId);
        Map<String, Integer> byType = buildTypeBreakdown(tenantMongoTemplate, businessId);
        Map<String, Integer> byLanguage = buildLanguageBreakdown(allTemplates);
        Map<String, Integer> byEventType = buildEventTypeBreakdown(allTemplates);

        log.info("Count breakdown completed for tenant_db_{} - byChannel: {}, byStatus: {}, byType: {}",
                tenantId, byChannel.size(), byStatus.size(), byType.size());

        return CountResponseDto.CountData.CountBreakdown.builder()
            .byChannel(byChannel)
            .byStatus(byStatus)
            .byType(byType)
            .byLanguage(byLanguage)
            .byEventType(byEventType)
            .build();
    }

    private List<NotificationTemplate> fetchAllTemplatesForBusiness(MongoTemplate tenantMongoTemplate,
            String businessId) {
        Query allTemplatesQuery = new Query().addCriteria(Criteria.where("businessId").is(businessId));
        return tenantMongoTemplate.find(allTemplatesQuery, NotificationTemplate.class);
    }

    private Map<String, Integer> buildChannelBreakdown(List<NotificationTemplate> allTemplates) {
        Map<String, Integer> byChannel = new HashMap<>();

        long smsCount = allTemplates.stream().filter(t -> t.getSmsConfig() != null).count();
        long emailCount = allTemplates.stream().filter(t -> t.getEmailConfig() != null).count();
        long bothCount = allTemplates.stream()
                .filter(t -> t.getSmsConfig() != null && t.getEmailConfig() != null).count();

        if (smsCount > 0) byChannel.put(NotificationChannel.SMS.name(), (int) (smsCount - bothCount));
        if (emailCount > 0) byChannel.put(NotificationChannel.EMAIL.name(), (int) (emailCount - bothCount));
        if (bothCount > 0) byChannel.put("BOTH", (int) bothCount);

        return byChannel;
    }

    private Map<String, Integer> buildStatusBreakdown(MongoTemplate tenantMongoTemplate, String businessId) {
        Map<String, Integer> byStatus = new HashMap<>();

        Arrays.stream(TemplateStatus.values()).forEach(status -> {
            Query statusQuery = new Query()
                .addCriteria(Criteria.where("businessId").is(businessId)
                    .and("status").is(status));

            long count = tenantMongoTemplate.count(statusQuery, NotificationTemplate.class);
            if (count > 0) byStatus.put(status.name(), (int) count);
        });

        return byStatus;
    }

    private Map<String, Integer> buildTypeBreakdown(MongoTemplate tenantMongoTemplate, String businessId) {
        Map<String, Integer> byType = new HashMap<>();

        Arrays.stream(NotificationType.values()).forEach(type -> {
            Query typeQuery = new Query()
                .addCriteria(Criteria.where("businessId").is(businessId)
                    .and("type").is(type));

            long count = tenantMongoTemplate.count(typeQuery, NotificationTemplate.class);
            if (count > 0) byType.put(type.name(), (int) count);
        });

        return byType;
    }

    private Map<String, Integer> buildLanguageBreakdown(List<NotificationTemplate> allTemplates) {
        Map<String, Integer> byLanguage = new HashMap<>();
        allTemplates.forEach(template -> byLanguage.merge(template.getLanguage(), 1, Integer::sum));
        return byLanguage;
    }

    private Map<String, Integer> buildEventTypeBreakdown(List<NotificationTemplate> allTemplates) {
        Map<String, Integer> byEventType = new HashMap<>();
        allTemplates.forEach(template -> byEventType.merge(template.getEventType(), 1, Integer::sum));
        return byEventType;
    }

    @Override
    public String resolveTemplate(String templateId, Map<String, Object> arguments, String tenantId) {
        log.info("Resolving template ID: {} for tenant: {} with arguments: {}", templateId, tenantId, arguments);

        // Set tenant context for multi-tenant database operations
        TenantContextHolder.setTenant(tenantId);

        try {
            // Use explicit tenant MongoTemplate to query tenant-specific database
            MongoTemplate tenantMongoTemplate = mongoConfig.getMongoTemplateForTenant(tenantId);

            // Search by DigiGov template ID
            Query templateQuery = new Query()
                    .addCriteria(Criteria.where("templateId").is(templateId));

            NotificationTemplate template = tenantMongoTemplate.findOne(templateQuery, NotificationTemplate.class);

            if (template == null) {
                log.warn("Template not found with ID: {} in tenant_db_{}", templateId, tenantId);
                throw new TemplateNotFoundException("Template not found with ID: " + templateId);
            }

            // Get template content with fallback logic
            String content = getTemplateContent(template);
            if (content == null || content.trim().isEmpty()) {
                throw new TemplateNotFoundException("Template content is empty for ID: " + templateId);
            }

            // Perform placeholder replacement with validation
            String resolvedContent = resolvePlaceholders(content, arguments);

            log.info("Template resolved successfully for ID: {}", templateId);
            return resolvedContent;

        } finally {
            TenantContextHolder.clear();
        }
    }

    /**
     * Extracts template content from either SMS or Email configuration.
     */
    private String getTemplateContent(NotificationTemplate template) {
        if (template.getSmsConfig() != null && template.getSmsConfig().getTemplate() != null) {
            return template.getSmsConfig().getTemplate();
        }
        if (template.getEmailConfig() != null && template.getEmailConfig().getTemplateBody() != null) {
            return template.getEmailConfig().getTemplateBody();
        }
        return null;
    }

    /**
     * Resolves placeholders in template content with provided arguments.
     */
    private String resolvePlaceholders(String content, Map<String, Object> arguments) {
        if (arguments == null || arguments.isEmpty()) {
            return content;
        }

        String resolvedContent = content;
        for (Map.Entry<String, Object> entry : arguments.entrySet()) {
            if (entry.getKey() != null) {
                String placeholder = "{{" + entry.getKey() + "}}";
                String value = entry.getValue() != null ? entry.getValue().toString() : "";
                resolvedContent = resolvedContent.replace(placeholder, value);
            }
        }
        return resolvedContent;
    }

    @Override
    public String validateTemplateExists(String eventType, String channelType, String tenantId,
                                       String businessId, String language) {
        log.info("Validating template existence: eventType={}, channelType={}, tenantId={}, businessId={}, language={}",
                eventType, channelType, tenantId, businessId, language);

        TenantContextHolder.setTenant(tenantId);

        try {
            MongoTemplate tenantMongoTemplate = mongoConfig.getMongoTemplateForTenant(tenantId);
            String templateId = findTemplateWithLanguageFallback(eventType, channelType, businessId,
                    language, tenantMongoTemplate);

            if (templateId != null) {
                return templateId;
            }

            throw createTemplateNotFoundException(eventType, channelType, businessId, language);

        } finally {
            TenantContextHolder.clear();
        }
    }

    private String findTemplateWithLanguageFallback(String eventType, String channelType, String businessId,
                                                   String language, MongoTemplate tenantMongoTemplate) {
        List<String> languagesToTry = buildLanguageFallbackList(language, businessId, tenantMongoTemplate);

        for (String lang : languagesToTry) {
            String templateId = findTemplateForLanguage(eventType, channelType, businessId, lang, tenantMongoTemplate);
            if (templateId != null) {
                log.info("Template found: eventType={}, channelType={}, language={}, templateId={}",
                        eventType, channelType, lang, templateId);
                return templateId;
            }
        }
        return null;
    }

    private TemplateNotFoundException createTemplateNotFoundException(String eventType, String channelType,
                                                                    String businessId, String language) {
        String error = String.format("No %s template found for eventType=%s, businessId=%s, language=%s",
                channelType, eventType, businessId, language);
        log.error(error);
        return new TemplateNotFoundException(error);
    }

    private List<String> buildLanguageFallbackList(String requestedLanguage, String businessId,
            MongoTemplate mongoTemplate) {
        List<String> languages = new ArrayList<>();

        addRequestedLanguage(languages, requestedLanguage);
        addBusinessDefaultLanguage(languages, businessId, mongoTemplate);
        addEnglishFallback(languages);

        log.debug("Language fallback order: {}", languages);
        return languages;
    }

    private void addRequestedLanguage(List<String> languages, String requestedLanguage) {
        if (requestedLanguage != null && !requestedLanguage.trim().isEmpty()) {
            languages.add(requestedLanguage.toLowerCase());
        }
    }

    private void addBusinessDefaultLanguage(List<String> languages, String businessId, MongoTemplate mongoTemplate) {
        try {
            String businessDefaultLanguage = getBusinessDefaultLanguage(businessId, mongoTemplate);
            if (businessDefaultLanguage != null && !languages.contains(businessDefaultLanguage.toLowerCase())) {
                languages.add(businessDefaultLanguage.toLowerCase());
            }
        } catch (Exception e) {
            log.warn("Failed to get business default language for businessId={}: {}", businessId, e.getMessage());
        }
    }

    private void addEnglishFallback(List<String> languages) {
        if (!languages.contains("english")) {
            languages.add("english");
        }
    }

    private String getBusinessDefaultLanguage(String businessId, MongoTemplate mongoTemplate) {
        try {
            Query query = new Query(Criteria.where("businessId").is(businessId));
            NotificationConfig config = mongoTemplate.findOne(query, NotificationConfig.class);

            // For DigiGov provider, get defaultLanguage from configurationJson
            if (config != null &&
                config.getProviderType() == ProviderType.DIGIGOV &&
                config.getConfigurationJson() != null &&
                config.getConfigurationJson().getDefaultLanguage() != null) {
                return config.getConfigurationJson().getDefaultLanguage().toLowerCase();
            }
        } catch (Exception e) {
            log.warn("Failed to query business configuration for default language, businessId={}: {}",
                    businessId, e.getMessage());
        }
        return null; // Fallback to null if configuration not found or error occurs
    }

    private String findTemplateForLanguage(String eventType, String channelType, String businessId,
                                         String language, MongoTemplate mongoTemplate) {
        try {
            NotificationChannel channel = NotificationChannel.valueOf(channelType.toUpperCase());

            Query templateQuery = new Query()
                    .addCriteria(Criteria.where("businessId").is(businessId))
                    .addCriteria(Criteria.where("eventType").is(eventType))
                    .addCriteria(Criteria.where("language").is(language))
                    .addCriteria(Criteria.where("channelType").is(channel))
                    .addCriteria(Criteria.where("status").is(TemplateStatus.ACTIVE));

            NotificationTemplate template = mongoTemplate.findOne(templateQuery, NotificationTemplate.class);

            return template != null ? template.getTemplateId() : null;

        } catch (IllegalArgumentException e) {
            log.warn("Invalid channel type: {}", channelType);
            return null;
        } catch (Exception e) {
            log.warn("Error finding template for eventType={}, channelType={}, businessId={}, language={}: {}",
                    eventType, channelType, businessId, language, e.getMessage());
            return null;
        }
    }

    // ==================== Master List Mapping Management ====================

    /**
     * Updates master list mappings after template creation or update.
     * Extracts labels from template arguments and updates event-to-label mappings.
     *
     * @param template The saved template
     */
    private void updateMasterListMappingsAfterSave(NotificationTemplate template) {
        try {
            log.info("Updating master list mappings for template: eventType={}, businessId={}",
                    template.getEventType(), template.getBusinessId());

            // Get tenant-specific MongoTemplate
            MongoTemplate tenantMongoTemplate = mongoTemplateProvider.getTemplate(TenantContextHolder.getTenant());

            // Extract labels from template
            Set<String> labels = extractLabelsFromTemplate(template);

            if (labels.isEmpty()) {
                log.debug("No master labels found in template arguments, skipping mapping update");
                return;
            }

            log.info("Extracted {} labels from template: {}", labels.size(), labels);

            // Get or create TenantMasterListConfig (tenant-level, not business-level)
            TenantMasterListConfig masterListConfig = getOrCreateMasterListConfig(tenantMongoTemplate);

            // Convert eventType string to EventType enum
            EventType eventType = EventType.valueOf(template.getEventType());

            // Update mappings using helper method
            masterListConfig.addEventToLabelMapping(eventType, labels);

            // Save updated master list config
            tenantMongoTemplate.save(masterListConfig);

            log.info("Successfully updated master list mappings for event: {}, labels: {}",
                    template.getEventType(), labels.size());

        } catch (Exception e) {
            log.error("Failed to update master list mappings for template: eventType={}, error: {}",
                    template.getEventType(), e.getMessage(), e);
            // Don't throw exception - mapping update failure shouldn't fail template creation
        }
    }

    /**
     * Rebuilds master list mappings for an event after template deletion.
     * Queries all remaining templates for the event and re-extracts labels.
     *
     * @param eventType Event type to rebuild mappings for
     * @param businessId Business identifier
     * @param tenantMongoTemplate Tenant-specific MongoTemplate
     */
    private void rebuildMasterListMappingsForEvent(String eventType, String businessId,
                                                   MongoTemplate tenantMongoTemplate) {
        try {
            log.info("Rebuilding master list mappings for event: {}, businessId: {}", eventType, businessId);

            // Get TenantMasterListConfig (tenant-level, not business-level)
            TenantMasterListConfig masterListConfig = getOrCreateMasterListConfig(tenantMongoTemplate);

            // Query for all remaining templates for this event
            Query query = new Query()
                    .addCriteria(Criteria.where("businessId").is(businessId))
                    .addCriteria(Criteria.where("eventType").is(eventType));

            List<NotificationTemplate> remainingTemplates = tenantMongoTemplate.find(query, NotificationTemplate.class);

            EventType eventTypeEnum = EventType.valueOf(eventType);

            if (remainingTemplates.isEmpty()) {
                // No templates remain for this event, remove the event mapping entirely
                log.info("No templates remain for event {}, removing event mapping", eventType);
                masterListConfig.removeEventToLabelMapping(eventTypeEnum);
            } else {
                // Extract labels from all remaining templates
                Set<String> allLabels = new HashSet<>();
                for (NotificationTemplate template : remainingTemplates) {
                    Set<String> templateLabels = extractLabelsFromTemplate(template);
                    allLabels.addAll(templateLabels);
                }

                log.info("Found {} remaining templates for event {}, extracted {} unique labels",
                        remainingTemplates.size(), eventType, allLabels.size());

                // Update the event mapping with all labels from remaining templates
                if (!allLabels.isEmpty()) {
                    masterListConfig.addEventToLabelMapping(eventTypeEnum, allLabels);
                } else {
                    // No labels in remaining templates, remove event mapping
                    masterListConfig.removeEventToLabelMapping(eventTypeEnum);
                }
            }

            // Save updated master list config
            tenantMongoTemplate.save(masterListConfig);

            log.info("Successfully rebuilt master list mappings for event: {}", eventType);

        } catch (Exception e) {
            log.error("Failed to rebuild master list mappings for event: {}, error: {}",
                    eventType, e.getMessage(), e);
            // Don't throw exception - mapping update failure shouldn't fail template deletion
        }
    }

    /**
     * Extracts all master labels from a template's argument maps.
     *
     * @param template The template to extract labels from
     * @return Set of master labels (values from argumentsMap)
     */
    private Set<String> extractLabelsFromTemplate(NotificationTemplate template) {
        Set<String> labels = new HashSet<>();

        // Extract from SMS arguments
        if (template.getSmsConfig() != null && template.getSmsConfig().getArgumentsMap() != null) {
            Map<String, String> smsArgs = template.getSmsConfig().getArgumentsMap();
            labels.addAll(smsArgs.values());
            log.debug("Extracted {} labels from SMS arguments", smsArgs.values().size());
        }

        // Extract from Email subject arguments
        if (template.getEmailConfig() != null && template.getEmailConfig().getArgumentsSubjectMap() != null) {
            Map<String, String> emailSubjectArgs = template.getEmailConfig().getArgumentsSubjectMap();
            labels.addAll(emailSubjectArgs.values());
            log.debug("Extracted {} labels from Email subject arguments", emailSubjectArgs.values().size());
        }

        // Extract from Email body arguments
        if (template.getEmailConfig() != null && template.getEmailConfig().getArgumentsBodyMap() != null) {
            Map<String, String> emailBodyArgs = template.getEmailConfig().getArgumentsBodyMap();
            labels.addAll(emailBodyArgs.values());
            log.debug("Extracted {} labels from Email body arguments", emailBodyArgs.values().size());
        }

        // Remove null or empty values
        labels.removeIf(label -> label == null || label.trim().isEmpty());

        return labels;
    }

    /**
     * Gets existing TenantMasterListConfig or creates a new one if it doesn't exist.
     * Note: TenantMasterListConfig is tenant-level (one per tenant), not business-level.
     *
     * @param tenantMongoTemplate Tenant-specific MongoTemplate
     * @return TenantMasterListConfig instance
     */
    private TenantMasterListConfig getOrCreateMasterListConfig(MongoTemplate tenantMongoTemplate) {
        // Try to find existing config (only one per tenant)
        Query query = new Query().addCriteria(Criteria.where("isActive").is(true));

        TenantMasterListConfig config = tenantMongoTemplate.findOne(query, TenantMasterListConfig.class);

        if (config != null) {
            log.debug("Found existing TenantMasterListConfig for tenant: {}", TenantContextHolder.getTenant());
            return config;
        }

        // Create new config if not found
        log.info("Creating new TenantMasterListConfig for tenant: {}", TenantContextHolder.getTenant());
        TenantMasterListConfig newConfig = new TenantMasterListConfig();
        newConfig.setMasterListConfig(new HashMap<>());
        newConfig.setIsActive(true);
        newConfig.setDescription("Auto-generated master list config for dynamic event-to-label mappings");
        newConfig.setVersion(1);
        newConfig.setCreatedAt(LocalDateTime.now());
        newConfig.setUpdatedAt(LocalDateTime.now());

        return newConfig;
    }
}