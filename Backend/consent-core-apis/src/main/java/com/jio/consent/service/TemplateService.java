package com.jio.consent.service;

import com.jio.consent.client.audit.AuditManager;
import com.jio.consent.client.audit.request.Actor;
import com.jio.consent.client.audit.request.AuditRequest;
import com.jio.consent.client.audit.request.Context;
import com.jio.consent.client.audit.request.Resource;
import com.jio.consent.client.notification.NotificationManager;
import com.jio.consent.constant.Constants;
import com.jio.consent.constant.ErrorCodes;
import com.jio.consent.dto.*;
import com.jio.consent.dto.Request.CreateTemplateRequest;
import com.jio.consent.dto.Request.UpdateTemplateRequest;
import com.jio.consent.dto.Response.SearchResponse;
import com.jio.consent.entity.BusinessApplication;
import com.jio.consent.entity.ConsentHandle;
import com.jio.consent.entity.Document;
import com.jio.consent.entity.Template;
import com.jio.consent.exception.ConsentException;
import com.jio.consent.repository.BusinessApplicationRepository;
import com.jio.consent.repository.ConsentHandleRepository;
import com.jio.consent.repository.DocumentRepository;
import com.jio.consent.repository.ProcessorActivityRepository;
import com.jio.consent.repository.PurposeRepository;
import com.jio.consent.repository.TemplateRepository;
import com.jio.consent.utils.Utils;
import com.jio.consent.utils.Validation;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.logging.log4j.ThreadContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
public class TemplateService {

    TemplateRepository templateRepository;
    DocumentRepository documentRepository;
    Utils utils;
    ConsentHandleRepository consentHandleRepository;
    NotificationManager notificationManager;
    AuditManager auditManager;
    Validation validation;
    BusinessApplicationRepository businessApplicationRepository;
    PurposeRepository purposeRepository;
    ProcessorActivityRepository processorActivityRepository;

    @Value("${template.handle.creation.batch.size}")
    private int batchSize;

    @Autowired
    public TemplateService(TemplateRepository templateRepository,
                           DocumentRepository documentRepository,
                           Utils utils,
                           ConsentHandleRepository consentHandleRepository,
                           NotificationManager notificationManager,
                           AuditManager auditManager,
                           Validation validation,
                           BusinessApplicationRepository businessApplicationRepository,
                           PurposeRepository purposeRepository,
                           ProcessorActivityRepository processorActivityRepository) {
        this.templateRepository = templateRepository;
        this.documentRepository = documentRepository;
        this.utils = utils;
        this.consentHandleRepository = consentHandleRepository;
        this.notificationManager = notificationManager;
        this.auditManager = auditManager;
        this.validation = validation;
        this.businessApplicationRepository = businessApplicationRepository;
        this.purposeRepository = purposeRepository;
        this.processorActivityRepository = processorActivityRepository;
    }

    @Value("${templates.search.parameters}")
    List<String> templateSearchParams;

    public Template createTemplate(CreateTemplateRequest request) throws IOException, ConsentException {

        // Validate businessId exists in business collection
        BusinessApplication businessApplication = this.businessApplicationRepository.findByBusinessId(request.getBusinessId());
        if (ObjectUtils.isEmpty(businessApplication)) {
            throw new ConsentException(ErrorCodes.JCMP1043);
        }

        String documentId = UUID.randomUUID().toString();
        String templateId = UUID.randomUUID().toString();

        String documentContentType = request.getPrivacyPolicyDocumentMeta().getContentType();
        String documentName = request.getPrivacyPolicyDocumentMeta().getName();
        Long documentSize = request.getPrivacyPolicyDocumentMeta().getSize();

        this.validation.validateDocument(request.getPrivacyPolicyDocument(), request.getPrivacyPolicyDocumentMeta());

        Document document = Document.builder()
                .documentId(documentId)
                .documentName(documentName)
                .businessId(request.getBusinessId())
                .contentType(documentContentType)
                .status(Status.ACTIVE.toString())
                .documentSize(documentSize)
                .tag(request.getPrivacyPolicyDocumentMeta().getTag())
                .isBase64Document(true)
                .data(request.getPrivacyPolicyDocument())
                .version(1)
                .build();

        DocumentMeta documentMeta = DocumentMeta.builder()
                .documentId(documentId)
                .name(documentName)
                .contentType(documentContentType)
                .size(documentSize)
                .tag(request.getPrivacyPolicyDocumentMeta().getTag())
                .build();

        // Handle logo document from UiConfig if provided
        if (request.getUiConfig() != null && request.getUiConfig().getLogo() != null && request.getUiConfig().getLogoMeta() != null) {
            String logoDocumentId = UUID.randomUUID().toString();

            // this.validation.validateDocument(request.getUiConfig().getLogo(), request.getUiConfig().getLogoMeta());

            Document logoDocument = Document.builder()
                    .documentId(logoDocumentId)
                    .documentName(request.getUiConfig().getLogoMeta().getName())
                    .businessId(request.getBusinessId())
                    .contentType(request.getUiConfig().getLogoMeta().getContentType())
                    .status(Status.ACTIVE.toString())
                    .documentSize(request.getUiConfig().getLogoMeta().getSize())
                    .tag(request.getUiConfig().getLogoMeta().getTag())
                    .isBase64Document(true)
                    .data(request.getUiConfig().getLogo())
                    .version(1)
                    .build();

            // Update logoMeta with the generated documentId
            DocumentMeta logoMeta = DocumentMeta.builder()
                    .documentId(logoDocumentId)
                    .name(request.getUiConfig().getLogoMeta().getName())
                    .contentType(request.getUiConfig().getLogoMeta().getContentType())
                    .size(request.getUiConfig().getLogoMeta().getSize())
                    .tag(request.getUiConfig().getLogoMeta().getTag())
                    .build();

            this.documentRepository.saveDocument(logoDocument);
            
            // Update logoMeta in UiConfig with the generated documentId
            request.getUiConfig().setLogoMeta(logoMeta);
        }
        
        request.getPreferences().forEach(p -> p.setPreferenceId(UUID.randomUUID().toString()));
        if(request.getUiConfig().getTypographySettings()!=null) {
            this.validation.validateTypographySettings(request.getUiConfig().getTypographySettings());
        }
        Template template = Template.builder()
                .templateName(request.getTemplateName())
                .templateId(templateId)
                .multilingual(request.getMultilingual())
                .businessId(request.getBusinessId())
                .uiConfig(request.getUiConfig())
                .documentMeta(documentMeta)
                .preferences(request.getPreferences())
                .status(request.getStatus() == null ? TemplateStatus.DRAFT : request.getStatus())
                .version(1)
                .build();

        try {
            this.documentRepository.saveDocument(document);
            Template savedTemplate = this.templateRepository.saveTemplate(template);
            
            // Audit logging
            ActionType actionType = savedTemplate.getStatus() == TemplateStatus.DRAFT ? ActionType.DRAFT : ActionType.PUBLISHED;
            this.logTemplateAudit(savedTemplate, actionType);
            
            return savedTemplate;
        } catch (Exception e) {
            throw e;
        }
    }


    public Template updateTemplate(UpdateTemplateRequest request, String templateId) throws IOException, ConsentException {
        Template currentTemplate = this.templateRepository.getByTemplateId(templateId);
        if (ObjectUtils.isEmpty(currentTemplate)) {
            throw new ConsentException(ErrorCodes.JCMP3001);
        }

        for (Preference preference : request.getPreferences()) {
            if (preference.getPreferenceId() == null) {
                preference.setPreferenceId(UUID.randomUUID().toString());
            }
        }

        // Handle version increment based on status transitions
        TemplateStatus currentStatus = currentTemplate.getStatus();
        TemplateStatus requestedStatus = request.getStatus();
        
        // Validate: Cannot convert PUBLISHED or INACTIVE template back to DRAFT
        if (requestedStatus == TemplateStatus.DRAFT && currentStatus != TemplateStatus.DRAFT) {
            throw new ConsentException(ErrorCodes.JCMP3006);
        }

        currentTemplate.setMultilingual(request.getMultilingual());
        currentTemplate.setPreferences(request.getPreferences());
        currentTemplate.setUiConfig(request.getUiConfig());
        
        // Don't increment version if:
        // 1. Current status is DRAFT and requested status is DRAFT
        // 2. Current status is DRAFT and requested status is PUBLISHED
        boolean shouldIncrementVersion = !(currentStatus == TemplateStatus.DRAFT && 
            (requestedStatus == TemplateStatus.DRAFT || requestedStatus == TemplateStatus.PUBLISHED));
        
        if (shouldIncrementVersion) {
            currentTemplate.setVersion(currentTemplate.getVersion() + 1);
            currentTemplate.setId(null);
        }
        
        currentTemplate.setStatus(request.getStatus());

        // Handle privacy policy document update if flag is true
        if (request.isPrivacyPolicyDocumentModified()) {
            this.validation.validateDocument(request.getPrivacyPolicyDocument(), request.getPrivacyPolicyDocumentMeta());
            String documentId = UUID.randomUUID().toString();
            String documentContentType = request.getPrivacyPolicyDocumentMeta().getContentType();
            String documentName = request.getPrivacyPolicyDocumentMeta().getName();
            Long documentSize = request.getPrivacyPolicyDocumentMeta().getSize();

            Document document = Document.builder()
                    .documentId(documentId)
                    .documentName(documentName)
                    .businessId(currentTemplate.getBusinessId())
                    .contentType(documentContentType)
                    .status(Status.ACTIVE.toString())
                    .documentSize(documentSize)
                    .tag(request.getPrivacyPolicyDocumentMeta().getTag())
                    .isBase64Document(true)
                    .data(request.getPrivacyPolicyDocument())
                    .version(1)
                    .build();

            DocumentMeta documentMeta = DocumentMeta.builder()
                    .documentId(documentId)
                    .name(documentName)
                    .contentType(documentContentType)
                    .size(documentSize)
                    .tag(request.getPrivacyPolicyDocumentMeta().getTag())
                    .build();

            // Save the new document
            this.documentRepository.saveDocument(document);
            
            // Update template with new document meta
            currentTemplate.setDocumentMeta(documentMeta);
        }

        // Handle logo update from UiConfig if flag is true
        if (request.isLogoModified() && request.getUiConfig() != null && request.getUiConfig().getLogo() != null && request.getUiConfig().getLogoMeta() != null) {
            String logoDocumentId = UUID.randomUUID().toString();

            // this.validation.validateDocument(request.getUiConfig().getLogo(), request.getUiConfig().getLogoMeta());

            Document logoDocument = Document.builder()
                    .documentId(logoDocumentId)
                    .documentName(request.getUiConfig().getLogoMeta().getName())
                    .businessId(currentTemplate.getBusinessId())
                    .contentType(request.getUiConfig().getLogoMeta().getContentType())
                    .status(Status.ACTIVE.toString())
                    .documentSize(request.getUiConfig().getLogoMeta().getSize())
                    .tag(request.getUiConfig().getLogoMeta().getTag())
                    .isBase64Document(true)
                    .data(request.getUiConfig().getLogo())
                    .version(1)
                    .build();

            // Update logoMeta with the generated documentId
            DocumentMeta logoMeta = DocumentMeta.builder()
                    .documentId(logoDocumentId)
                    .name(request.getUiConfig().getLogoMeta().getName())
                    .contentType(request.getUiConfig().getLogoMeta().getContentType())
                    .size(request.getUiConfig().getLogoMeta().getSize())
                    .tag(request.getUiConfig().getLogoMeta().getTag())
                    .build();

            // Save the new logo document
            this.documentRepository.saveDocument(logoDocument);
            
            // Update logoMeta in UiConfig with the generated documentId
            request.getUiConfig().setLogoMeta(logoMeta);
            
            // Set the updated UiConfig back to the template
            currentTemplate.setUiConfig(request.getUiConfig());
        }
        if(request.getUiConfig()!=null && request.getUiConfig().getTypographySettings()!=null) {
            this.validation.validateTypographySettings(request.getUiConfig().getTypographySettings());
        }

        try {
            Template template = this.templateRepository.saveTemplate(currentTemplate);
            
            // Only create handles for existing consents if version was incremented
            if (shouldIncrementVersion) {
                createHandleForExistingConsents(templateId, currentTemplate.getVersion() - 1);
            }
            
            // Audit logging - if template is already published, action type is UPDATE
            if (currentStatus == TemplateStatus.PUBLISHED) {
                this.logTemplateAudit(template, ActionType.UPDATE);
            } else {
                // For other status transitions, use DRAFT or PUBLISHED based on new status
                ActionType actionType = template.getStatus() == TemplateStatus.DRAFT ? ActionType.DRAFT : ActionType.PUBLISHED;
                this.logTemplateAudit(template, actionType);
            }
            
            return template;
        } catch (Exception e) {
            throw e;
        }
    }

    @Async
    private void createHandleForExistingConsents(String templateId, int version) throws ConsentException {
        log.info("Starting batch processing for template: {} version: {} with batch size: {}", templateId, version, batchSize);
        
        Map<String, Object> handleSearchParams = Map.of(
                "templateId", templateId,
                "templateVersion", version
        );
        
        int totalProcessed = 0;
        int pageNumber = 0;
        boolean hasMoreRecords = true;
        
        while (hasMoreRecords) {
            Pageable pageable = PageRequest.of(pageNumber, batchSize);
            List<ConsentHandle> existingHandleBatch = this.consentHandleRepository.findConsentHandleByParamsWithPagination(handleSearchParams, pageable);
            
            if (existingHandleBatch.isEmpty()) {
                hasMoreRecords = false;
                log.debug("No more records found for template: {} version: {}", templateId, version);
            } else {
                // Process the batch
                int batchProcessedCount = processHandleBatch(existingHandleBatch, templateId, version);
                totalProcessed += batchProcessedCount;
                
                log.debug("Processed batch {} for template: {} version: {} - Created {} new handles", 
                        pageNumber + 1, templateId, version, batchProcessedCount);
                
                // Check if we got fewer records than batch size (last batch)
                if (existingHandleBatch.size() < batchSize) {
                    hasMoreRecords = false;
                } else {
                    pageNumber++;
                }
            }
        }
        
        log.info("Completed batch processing for template: {} version: {}. Total handles created: {}", 
                templateId, version, totalProcessed);
    }
    
    /**
     * Processes a batch of existing consent handles to create new handles
     * 
     * @param existingHandleBatch List of existing consent handles in the current batch
     * @param templateId The template ID
     * @param version The version number
     * @return Number of new handles created in this batch
     */
    private int processHandleBatch(List<ConsentHandle> existingHandleBatch, String templateId, int version) {
        int batchProcessedCount = 0;
        
        for (ConsentHandle existingHandle : existingHandleBatch) {
            try {
                String newConsentHandleId = UUID.randomUUID().toString();
                ConsentHandle newHandle = ConsentHandle.builder()
                        .consentHandleId(newConsentHandleId)
                        .templateId(templateId)
                        .templateVersion(version + 1)
                        .consentId(existingHandle.getConsentId())
                        .customerIdentifiers(existingHandle.getCustomerIdentifiers())
                        .businessId(existingHandle.getBusinessId())
                        .txnId(existingHandle.getTxnId())
                        .status(ConsentHandleStatus.PENDING)
                        .remarks(ConsentHandleRemarks.TEMPLATE_UPDATE)
                        .build();
                
                this.consentHandleRepository.save(newHandle);
                
                // Trigger notification
                try {
                    Map<String, Object> eventPayload = new HashMap<>();
                    eventPayload.put("consentHandleId", newConsentHandleId);
                    this.notificationManager.triggerConsentEvent(NotificationEvent.CONSENT_RENEWAL_REQUEST,
                            ThreadContext.get(Constants.TENANT_ID_HEADER),
                            existingHandle.getBusinessId(),
                            existingHandle.getCustomerIdentifiers(),
                            null,
                            eventPayload,
                            LANGUAGE.ENGLISH);
                } catch (Exception e) {
                    log.error("Notification event trigger failed for consent handle id: {}, error: {}", 
                            newConsentHandleId, e.getMessage());
                }
                
                batchProcessedCount++;
                
            } catch (Exception e) {
                log.error("Error creating handle for existing consent: {} for template: {} version: {}", 
                        existingHandle.getConsentHandleId(), templateId, version, e);
            }
        }
        
        return batchProcessedCount;
    }

    public SearchResponse<Template> searchTemplates(Map<String, Object> reqParams) throws ConsentException {
        Map<String, Object> searchParams = this.utils.filterRequestParam(reqParams, templateSearchParams);
        List<Template> mongoResponse = this.templateRepository.findTemplatesByParams(searchParams);

        if (ObjectUtils.isEmpty(mongoResponse)) {
            throw new ConsentException(ErrorCodes.JCMP3001);
        }

        return SearchResponse.<Template>builder()
                .searchList(mongoResponse)
                .build();
    }

    public long count() {
        return this.templateRepository.count();
    }

    /**
     * Modular function to log template audit events
     * Can be used in both create and update template flows
     *
     * @param template The template entity to audit
     * @param actionType The action type (DRAFT, PUBLISHED, UPDATE)
     */
    private void logTemplateAudit(Template template, ActionType actionType) {
        try {
            Actor actor = Actor.builder()
                    .id(template.getBusinessId())
                    .role(Constants.BUSINESS)
                    .type(Constants.BUSINESS_ID)
                    .build();

            Resource resource = Resource.builder()
                    .type(Constants.TEMPLATE_ID)
                    .id(template.getTemplateId())
                    .build();

            Context context = Context.builder()
                    .ipAddress(ThreadContext.get(Constants.SOURCE_IP) != null && !ThreadContext.get(Constants.SOURCE_IP).equals("-") 
                            ? ThreadContext.get(Constants.SOURCE_IP) : null)
                    .txnId(ThreadContext.get(Constants.TXN_ID_THREAD_CONTEXT) != null && !ThreadContext.get(Constants.TXN_ID_THREAD_CONTEXT).equals("-") 
                            ? ThreadContext.get(Constants.TXN_ID_THREAD_CONTEXT) : null)
                    .build();

            Map<String, Object> extra = new HashMap<>();
            // Add template POJO in the extra field under the "data" key
            extra.put(Constants.DATA, template);

            AuditRequest auditRequest = AuditRequest.builder()
                    .actor(actor)
                    .businessId(template.getBusinessId())
                    .group(Constants.CONSENT_GROUP)
                    .component(AuditComponent.TEMPLATE)
                    .actionType(actionType)
                    .resource(resource)
                    .initiator(Constants.DATA_FIDUCIARY)
                    .context(context)
                    .extra(extra)
                    .build();

            String tenantId = ThreadContext.get(Constants.TENANT_ID_HEADER);
            this.auditManager.logAudit(auditRequest, tenantId);
        } catch (Exception e) {
            log.error("Audit logging failed for template id: {}, action: {}, error: {}", 
                    template.getTemplateId(), actionType, e.getMessage(), e);
        }
    }

    /**
     * Get template details by templateId
     * Fetches all templates for the given templateId and returns the latest version
     *
     * @param templateId The template ID
     * @return TemplateDetailsResponse with template details and consent purpose names
     * @throws ConsentException if template not found or in DRAFT status
     */
    public Template getTemplateDetailsByTemplateId(String templateId) throws ConsentException {
        // Fetch all templates for this templateId and get the latest version
        Map<String, Object> templateSearchParams = Map.of(
                "templateId", templateId
        );
        SearchResponse<Template> searchTemplates = this.searchTemplates(templateSearchParams);
        List<Template> templates = searchTemplates.getSearchList();
        
        if (ObjectUtils.isEmpty(templates) || templates.isEmpty()) {
            throw new ConsentException(ErrorCodes.JCMP3001);
        }
        
        // Sort by version descending to get the latest version
        Template template = templates.stream()
                .max((t1, t2) -> Integer.compare(t1.getVersion(), t2.getVersion()))
                .orElseThrow(() -> new ConsentException(ErrorCodes.JCMP3001));
        
        // Check if template status is DRAFT
        if (template.getStatus() == TemplateStatus.DRAFT) {
            throw new ConsentException(ErrorCodes.JCMP3007);
        }
        
        return template;
    }

    /**
     * Get consent purpose names from template
     *
     * @param template The template entity
     * @return Comma-separated string of purpose names
     */
    public String getConsentPurposeFromTemplate(Template template) {
        try {
            if (template == null || template.getPreferences() == null || template.getPreferences().isEmpty()) {
                return "";
            }

            // Extract all unique purposeIds from template preferences
            List<String> purposeIds = template.getPreferences().stream()
                    .filter(p -> !ObjectUtils.isEmpty(p.getPurposeIds()))
                    .flatMap(p -> p.getPurposeIds().stream())
                    .distinct()
                    .collect(Collectors.toList());

            if (purposeIds.isEmpty()) {
                return "";
            }

            // Fetch purposes from database
            List<com.jio.consent.entity.Purpose> purposes = this.purposeRepository.findByPurposeIds(purposeIds);

            if (purposes == null || purposes.isEmpty()) {
                return "";
            }

            // Extract purpose names and concatenate them
            String consentPurpose = purposes.stream()
                    .filter(p -> p.getPurposeName() != null && !p.getPurposeName().isEmpty())
                    .map(com.jio.consent.entity.Purpose::getPurposeName)
                    .collect(Collectors.joining(", "));

            return consentPurpose;
        } catch (Exception e) {
            log.error("Error getting consent purpose from template: {}", e.getMessage(), e);
            return "";
        }
    }

    /**
     * Get all Purpose entities from template preferences
     *
     * @param template The template entity
     * @return List of Purpose entities
     */
    public List<com.jio.consent.entity.Purpose> getPurposesFromTemplate(Template template) {
        try {
            if (template == null || template.getPreferences() == null || template.getPreferences().isEmpty()) {
                return List.of();
            }

            // Extract all unique purposeIds from template preferences
            List<String> purposeIds = template.getPreferences().stream()
                    .filter(p -> !ObjectUtils.isEmpty(p.getPurposeIds()))
                    .flatMap(p -> p.getPurposeIds().stream())
                    .distinct()
                    .collect(Collectors.toList());

            if (purposeIds.isEmpty()) {
                return List.of();
            }

            // Fetch purposes from database
            List<com.jio.consent.entity.Purpose> purposes = this.purposeRepository.findByPurposeIds(purposeIds);

            return purposes != null ? purposes : List.of();
        } catch (Exception e) {
            log.error("Error getting purposes from template: {}", e.getMessage(), e);
            return List.of();
        }
    }

    /**
     * Get all ProcessorActivity entities from template preferences
     *
     * @param template The template entity
     * @return List of ProcessorActivity entities
     */
    public List<com.jio.consent.entity.ProcessorActivity> getProcessorActivitiesFromTemplate(Template template) {
        try {
            if (template == null || template.getPreferences() == null || template.getPreferences().isEmpty()) {
                return List.of();
            }

            // Extract all unique processorActivityIds from template preferences
            List<String> processorActivityIds = template.getPreferences().stream()
                    .filter(p -> !ObjectUtils.isEmpty(p.getProcessorActivityIds()))
                    .flatMap(p -> p.getProcessorActivityIds().stream())
                    .distinct()
                    .collect(Collectors.toList());

            if (processorActivityIds.isEmpty()) {
                return List.of();
            }

            // Fetch processor activities from database
            List<com.jio.consent.entity.ProcessorActivity> processorActivities = 
                    this.processorActivityRepository.findByProcessorActivityIds(processorActivityIds);

            return processorActivities != null ? processorActivities : List.of();
        } catch (Exception e) {
            log.error("Error getting processor activities from template: {}", e.getMessage(), e);
            return List.of();
        }
    }

    /**
     * Build enhanced preferences with embedded Purpose and ProcessorActivity entities
     *
     * @param template The template entity
     * @return List of EnhancedPreference with full Purpose and ProcessorActivity objects
     */
    public List<com.jio.consent.dto.Response.EnhancedPreference> buildEnhancedPreferences(Template template) {
        try {
            if (template == null || template.getPreferences() == null || template.getPreferences().isEmpty()) {
                return List.of();
            }

            // Fetch all purposes and processor activities first
            List<com.jio.consent.entity.Purpose> allPurposes = getPurposesFromTemplate(template);
            List<com.jio.consent.entity.ProcessorActivity> allProcessorActivities = getProcessorActivitiesFromTemplate(template);

            // Create maps for quick lookup
            Map<String, com.jio.consent.entity.Purpose> purposeMap = allPurposes.stream()
                    .collect(Collectors.toMap(com.jio.consent.entity.Purpose::getPurposeId, p -> p));
            
            Map<String, com.jio.consent.entity.ProcessorActivity> processorActivityMap = allProcessorActivities.stream()
                    .collect(Collectors.toMap(com.jio.consent.entity.ProcessorActivity::getProcessorActivityId, pa -> pa));

            // Build enhanced preferences
            return template.getPreferences().stream()
                    .map(preference -> {
                        // Get Purpose entities for this preference
                        List<com.jio.consent.entity.Purpose> purposes = List.of();
                        if (!ObjectUtils.isEmpty(preference.getPurposeIds())) {
                            purposes = preference.getPurposeIds().stream()
                                    .map(purposeMap::get)
                                    .filter(p -> p != null)
                                    .collect(Collectors.toList());
                        }

                        // Get ProcessorActivity entities for this preference
                        List<com.jio.consent.entity.ProcessorActivity> processorActivities = List.of();
                        if (!ObjectUtils.isEmpty(preference.getProcessorActivityIds())) {
                            processorActivities = preference.getProcessorActivityIds().stream()
                                    .map(processorActivityMap::get)
                                    .filter(pa -> pa != null)
                                    .collect(Collectors.toList());
                        }

                        return com.jio.consent.dto.Response.EnhancedPreference.builder()
                                .preferenceId(preference.getPreferenceId())
                                .purposeIds(purposes)
                                .isMandatory(preference.isMandatory())
                                .autoRenew(preference.isAutoRenew())
                                .preferenceValidity(preference.getPreferenceValidity())
                                .startDate(preference.getStartDate())
                                .endDate(preference.getEndDate())
                                .processorActivityIds(processorActivities)
                                .preferenceStatus(preference.getPreferenceStatus())
                                .build();
                    })
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Error building enhanced preferences: {}", e.getMessage(), e);
            return List.of();
        }
    }

    /**
     * Get complete Document entity from template's documentMeta
     *
     * @param template The template entity
     * @return Document entity with all fields including data
     */
    public com.jio.consent.entity.Document getDocumentFromTemplate(Template template) {
        try {
            if (template == null || template.getDocumentMeta() == null || 
                ObjectUtils.isEmpty(template.getDocumentMeta().getDocumentId())) {
                return null;
            }

            String documentId = template.getDocumentMeta().getDocumentId();
            com.jio.consent.entity.Document document = this.documentRepository.getDocumentById(documentId);

            return document;
        } catch (Exception e) {
            log.error("Error getting document from template: {}", e.getMessage(), e);
            return null;
        }
    }
}
