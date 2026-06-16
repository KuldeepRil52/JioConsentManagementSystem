package com.jio.consent.service;

import com.jio.consent.client.audit.AuditManager;
import com.jio.consent.client.audit.request.Actor;
import com.jio.consent.client.audit.request.AuditRequest;
import com.jio.consent.client.audit.request.Context;
import com.jio.consent.client.audit.request.Resource;
import com.jio.consent.client.auth.AuthManager;
import com.jio.consent.client.auth.response.CreateSecureCodeResponse;
import com.jio.consent.client.notification.NotificationManager;
import com.jio.consent.constant.Constants;
import com.jio.consent.constant.ErrorCodes;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jio.consent.dto.*;
import com.jio.consent.dto.Request.CreateHandleRequest;
import com.jio.consent.dto.Request.CreateParentalConsentRequest;
import com.jio.consent.dto.Response.ParentalConsentResponse;
import com.jio.consent.dto.Response.ConsentHandleResponse;
import com.jio.consent.dto.Response.GetHandleResponse;
import com.jio.consent.dto.Response.GetParentalConsentHandleResponse;
import com.jio.consent.dto.Response.SearchResponse;
import com.jio.consent.entity.ConsentHandle;
import com.jio.consent.entity.Template;
import com.jio.consent.exception.ConsentException;
import com.jio.consent.repository.ConsentHandleRepository;
import com.jio.consent.repository.ProcessorActivityRepository;
import com.jio.consent.repository.PurposeRepository;
import com.jio.consent.service.signature.RequestResponseSignatureService;
import com.jio.consent.utils.Utils;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.ThreadContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ConsentHandleService {

    ConsentHandleRepository consentHandleRepository;
    TemplateService templateService;
    Utils utils;
    PurposeRepository purposeRepository;
    ProcessorActivityRepository processorActivityRepository;
    NotificationManager notificationManager;
    AuditManager auditManager;
    RequestResponseSignatureService signatureService;
    AuthManager authManager;

    @Autowired
    public ConsentHandleService(ConsentHandleRepository consentHandleRepository,
                                TemplateService templateService,
                                Utils utils,
                                PurposeRepository purposeRepository,
                                ProcessorActivityRepository processorActivityRepository,
                                NotificationManager notificationManager,
                                AuditManager auditManager,
                                RequestResponseSignatureService signatureService,
                                AuthManager authManager) {
        this.consentHandleRepository = consentHandleRepository;
        this.templateService = templateService;
        this.utils = utils;
        this.purposeRepository = purposeRepository;
        this.processorActivityRepository = processorActivityRepository;
        this.notificationManager = notificationManager;
        this.auditManager = auditManager;
        this.signatureService = signatureService;
        this.authManager = authManager;
    }

    @Value("${handles.search.parameters}")
    List<String> handlesSearchParams;

    @Value("${parental.consent.redirect.base.url}")
    private String parentalConsentRedirectBaseUrl;

    @Value("${parental.consent.callback.url}")
    private String parentalConsentCallbackUrl;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public ConsentHandle createConsentHandle(CreateHandleRequest request, Map<String, String> headers) throws ConsentException {
        Template template;
        int resolvedTemplateVersion;
        
        // If templateVersion is provided, fetch the specific template with that version
        // Otherwise, fetch the template with the latest version
        if (request.getTemplateVersion() != null) {
            Map<String, Object> templateSearchParams = Map.of(
                    "templateId", request.getTemplateId(),
                    "version", request.getTemplateVersion()
            );
            SearchResponse<Template> searchTemplates = this.templateService.searchTemplates(templateSearchParams);
            List<Template> templates = searchTemplates.getSearchList();
            if (ObjectUtils.isEmpty(templates) || templates.isEmpty()) {
                throw new ConsentException(ErrorCodes.JCMP3002);
            }
            template = templates.getFirst();
            resolvedTemplateVersion = request.getTemplateVersion();
        } else {
            // Fetch all templates for this templateId and get the latest version
            Map<String, Object> templateSearchParams = Map.of(
                    "templateId", request.getTemplateId()
            );
            SearchResponse<Template> searchTemplates = this.templateService.searchTemplates(templateSearchParams);
            List<Template> templates = searchTemplates.getSearchList();
            if (ObjectUtils.isEmpty(templates) || templates.isEmpty()) {
                throw new ConsentException(ErrorCodes.JCMP3002);
            }
            // Sort by version descending to get the latest version
            template = templates.stream()
                    .max((t1, t2) -> Integer.compare(t1.getVersion(), t2.getVersion()))
                    .orElseThrow(() -> new ConsentException(ErrorCodes.JCMP3002));
            resolvedTemplateVersion = template.getVersion();
        }

        if (template.getStatus().equals(TemplateStatus.DRAFT)) {
            throw new ConsentException(ErrorCodes.JCMP3007);
        }

        Map<String, Object> handleSearchParams = Map.of(
                "templateId", request.getTemplateId(),
                "templateVersion", resolvedTemplateVersion,
                "customerIdentifiers.type", request.getCustomerIdentifiers().getType(),
                "customerIdentifiers.value", request.getCustomerIdentifiers().getValue()
        );

        Map<String, Object> eventPayload = new HashMap<>();
        List<ConsentHandle> isAlreadyExist = this.consentHandleRepository.findConsentHandleByParamsWithIds(handleSearchParams);
        if (!ObjectUtils.isEmpty(isAlreadyExist)
                && !isAlreadyExist.isEmpty()
                && isAlreadyExist.getFirst() != null) {
            ConsentHandle existedConsentHandle = isAlreadyExist.getFirst();
            if (existedConsentHandle.getStatus() != ConsentHandleStatus.PENDING) {
                existedConsentHandle.setStatus(ConsentHandleStatus.PENDING);
            }

            ConsentHandle savedHandle = this.consentHandleRepository.save(existedConsentHandle);
            eventPayload.put("consentHandleId", existedConsentHandle.getConsentHandleId());
            
            // Get consent purpose names from template
            String consentPurpose = getConsentPurposeFromTemplate(template);
            if (consentPurpose != null && !consentPurpose.isEmpty()) {
                eventPayload.put("consentPurpose", consentPurpose);
            }
            
            long notificationStartTime = System.currentTimeMillis();
            log.info("Notification call started for existing handle: {}", existedConsentHandle.getConsentHandleId());
            
            this.notificationManager.initiateConsentHandleNotification(NotificationEvent.CONSENT_REQUEST_PENDING,
                    ThreadContext.get(Constants.TENANT_ID_HEADER),
                    headers.get(Constants.BUSINESS_ID_HEADER),
                    request.getCustomerIdentifiers(),
                    null,
                    eventPayload,
                    LANGUAGE.ENGLISH,
                    existedConsentHandle.getConsentHandleId());
            
            long notificationEndTime = System.currentTimeMillis();
            long notificationTat = notificationEndTime - notificationStartTime;
            log.info("Notification call completed for existing handle: {} | TAT: {}ms", existedConsentHandle.getConsentHandleId(), notificationTat);
            
            // Audit logging for updated consent handle
            this.logConsentHandleAudit(savedHandle, ActionType.UPDATE);
            
            return savedHandle;
        }
        String consentHandleId = UUID.randomUUID().toString();

        ConsentHandle consentHandle = ConsentHandle.builder()
                .consentHandleId(consentHandleId)
                .txnId(headers.get(Constants.TXN_ID))
                .businessId(template.getBusinessId())
                .customerIdentifiers(request.getCustomerIdentifiers())
                .templateId(request.getTemplateId())
                .templateVersion(resolvedTemplateVersion)
                .status(ConsentHandleStatus.PENDING)
                .remarks(request.getRemarks() != null ? request.getRemarks() : ConsentHandleRemarks.DATA_FIDUCIARY)
                .build();

        ConsentHandle savedHandle = this.consentHandleRepository.save(consentHandle);
        eventPayload.put("consentHandleId", consentHandle.getConsentHandleId());
        
        // Get consent purpose names from template
        String consentPurpose = getConsentPurposeFromTemplate(template);
        if (consentPurpose != null && !consentPurpose.isEmpty()) {
            eventPayload.put("consentPurpose", consentPurpose);
        }
        
        long notificationStartTime = System.currentTimeMillis();
        log.info("Notification call started for new handle: {}", consentHandleId);
        
        this.notificationManager.initiateConsentHandleNotification(NotificationEvent.CONSENT_REQUEST_PENDING,
                ThreadContext.get(Constants.TENANT_ID_HEADER),
                headers.get(Constants.BUSINESS_ID_HEADER),
                request.getCustomerIdentifiers(),
                null,
                eventPayload,
                LANGUAGE.ENGLISH,
                consentHandleId);
        
        long notificationEndTime = System.currentTimeMillis();
        long notificationTat = notificationEndTime - notificationStartTime;
        log.info("Notification call completed for new handle: {} | TAT: {}ms", consentHandleId, notificationTat);

        // Audit logging for created consent handle
        this.logConsentHandleAudit(savedHandle, ActionType.CREATE);

        return savedHandle;
    }

    public GetHandleResponse getConsentHandleById(String consentHandleId) throws ConsentException {
        GetHandleResponse response = new GetHandleResponse();
        ConsentHandle consentHandle = this.consentHandleRepository.getByConsentHandleId(consentHandleId);
        if (ObjectUtils.isEmpty(consentHandle)) {
            throw new ConsentException(ErrorCodes.JCMP3001);
        }
        Map<String, Object> templateSearchParams = Map.of(
                "templateId", consentHandle.getTemplateId(),
                "version", consentHandle.getTemplateVersion()
        );
        SearchResponse<Template> searchTemplates = this.templateService.searchTemplates(templateSearchParams);
        List<Template> templates = searchTemplates.getSearchList();
        if (ObjectUtils.isEmpty(templates) || templates.isEmpty()) {
            throw new ConsentException(ErrorCodes.JCMP3002);
        }
        Template template = templates.getFirst();

        List<Preference> templatePreferences = template.getPreferences();
        if (!ObjectUtils.isEmpty(templatePreferences)) {
            List<String> purposeIds = templatePreferences.stream()
                    .filter(p -> !ObjectUtils.isEmpty(p.getPurposeIds()))
                    .flatMap(p -> p.getPurposeIds().stream())
                    .distinct()
                    .collect(java.util.stream.Collectors.toList());

            List<String> processorActivityIds = templatePreferences.stream()
                    .filter(p -> !ObjectUtils.isEmpty(p.getProcessorActivityIds()))
                    .flatMap(p -> p.getProcessorActivityIds().stream())
                    .distinct()
                    .collect(java.util.stream.Collectors.toList());

            var purposes = ObjectUtils.isEmpty(purposeIds) ? List.<com.jio.consent.entity.Purpose>of() : this.purposeRepository.findByPurposeIds(purposeIds);
            var processorActivities = ObjectUtils.isEmpty(processorActivityIds) ? List.<com.jio.consent.entity.ProcessorActivity>of() : this.processorActivityRepository.findByProcessorActivityIds(processorActivityIds);

            var purposeById = purposes.stream().collect(java.util.stream.Collectors.toMap(com.jio.consent.entity.Purpose::getPurposeId, p -> p));
            var activityById = processorActivities.stream().collect(java.util.stream.Collectors.toMap(com.jio.consent.entity.ProcessorActivity::getProcessorActivityId, a -> a));

            List<HandlePreference> handlePreferences = templatePreferences.stream().map(pref -> {
                List<PurposeDetails> purposeDetails = ObjectUtils.isEmpty(pref.getPurposeIds()) ? java.util.List.of() : pref.getPurposeIds().stream()
                        .map(pid -> PurposeDetails.builder().purposeId(pid).purposeInfo(purposeById.get(pid)).build())
                        .collect(java.util.stream.Collectors.toList());

                List<ProcessorActivityDetails> processorDetails = ObjectUtils.isEmpty(pref.getProcessorActivityIds()) ? java.util.List.of() : pref.getProcessorActivityIds().stream()
                        .map(aid -> ProcessorActivityDetails.builder().processorActivityId(aid).processActivityInfo(activityById.get(aid)).build())
                        .collect(java.util.stream.Collectors.toList());

                return HandlePreference.builder()
                        .preferenceId(pref.getPreferenceId())
                        .purposeList(purposeDetails)
                        .isMandatory(pref.isMandatory())
                        .autoRenew(pref.isAutoRenew())
                        .preferenceValidity(pref.getPreferenceValidity())
                        .startDate(pref.getStartDate())
                        .endDate(pref.getEndDate())
                        .processorActivityList(processorDetails)
                        .preferenceStatus(pref.getPreferenceStatus())
                        .build();
            }).collect(java.util.stream.Collectors.toList());

            response.setPreferences(handlePreferences);
        }

        response.setConsentHandleId(consentHandle.getConsentHandleId());
        response.setTemplateId(template.getTemplateId());
        response.setTemplateName(template.getTemplateName());
        response.setTemplateVersion(template.getVersion());
        response.setBusinessId(consentHandle.getBusinessId());
        response.setMultilingual(template.getMultilingual());
        response.setCustomerIdentifiers(consentHandle.getCustomerIdentifiers());
        response.setStatus(consentHandle.getStatus());
        response.setRemarks(consentHandle.getRemarks());
        response.setUiConfig(template.getUiConfig());
        response.setDocumentMeta(template.getDocumentMeta());
        return response;
    }

    public GetParentalConsentHandleResponse getParentalConsentHandleById(String consentHandleId) throws ConsentException {
        GetParentalConsentHandleResponse response = new GetParentalConsentHandleResponse();
        ConsentHandle consentHandle = this.consentHandleRepository.getByConsentHandleId(consentHandleId);
        if (ObjectUtils.isEmpty(consentHandle)) {
            throw new ConsentException(ErrorCodes.JCMP3001);
        }
        Map<String, Object> templateSearchParams = Map.of(
                "templateId", consentHandle.getTemplateId(),
                "version", consentHandle.getTemplateVersion()
        );
        SearchResponse<Template> searchTemplates = this.templateService.searchTemplates(templateSearchParams);
        List<Template> templates = searchTemplates.getSearchList();
        if (ObjectUtils.isEmpty(templates) || templates.isEmpty()) {
            throw new ConsentException(ErrorCodes.JCMP3002);
        }
        Template template = templates.getFirst();

        List<Preference> templatePreferences = template.getPreferences();
        if (!ObjectUtils.isEmpty(templatePreferences)) {
            List<String> purposeIds = templatePreferences.stream()
                    .filter(p -> !ObjectUtils.isEmpty(p.getPurposeIds()))
                    .flatMap(p -> p.getPurposeIds().stream())
                    .distinct()
                    .collect(java.util.stream.Collectors.toList());

            List<String> processorActivityIds = templatePreferences.stream()
                    .filter(p -> !ObjectUtils.isEmpty(p.getProcessorActivityIds()))
                    .flatMap(p -> p.getProcessorActivityIds().stream())
                    .distinct()
                    .collect(java.util.stream.Collectors.toList());

            var purposes = ObjectUtils.isEmpty(purposeIds) ? List.<com.jio.consent.entity.Purpose>of() : this.purposeRepository.findByPurposeIds(purposeIds);
            var processorActivities = ObjectUtils.isEmpty(processorActivityIds) ? List.<com.jio.consent.entity.ProcessorActivity>of() : this.processorActivityRepository.findByProcessorActivityIds(processorActivityIds);

            var purposeById = purposes.stream().collect(java.util.stream.Collectors.toMap(com.jio.consent.entity.Purpose::getPurposeId, p -> p));
            var activityById = processorActivities.stream().collect(java.util.stream.Collectors.toMap(com.jio.consent.entity.ProcessorActivity::getProcessorActivityId, a -> a));

            List<HandlePreference> handlePreferences = templatePreferences.stream().map(pref -> {
                List<PurposeDetails> purposeDetails = ObjectUtils.isEmpty(pref.getPurposeIds()) ? java.util.List.of() : pref.getPurposeIds().stream()
                        .map(pid -> PurposeDetails.builder().purposeId(pid).purposeInfo(purposeById.get(pid)).build())
                        .collect(java.util.stream.Collectors.toList());

                List<ProcessorActivityDetails> processorDetails = ObjectUtils.isEmpty(pref.getProcessorActivityIds()) ? java.util.List.of() : pref.getProcessorActivityIds().stream()
                        .map(aid -> ProcessorActivityDetails.builder().processorActivityId(aid).processActivityInfo(activityById.get(aid)).build())
                        .collect(java.util.stream.Collectors.toList());

                return HandlePreference.builder()
                        .preferenceId(pref.getPreferenceId())
                        .purposeList(purposeDetails)
                        .isMandatory(pref.isMandatory())
                        .autoRenew(pref.isAutoRenew())
                        .preferenceValidity(pref.getPreferenceValidity())
                        .startDate(pref.getStartDate())
                        .endDate(pref.getEndDate())
                        .processorActivityList(processorDetails)
                        .preferenceStatus(pref.getPreferenceStatus())
                        .build();
            }).collect(java.util.stream.Collectors.toList());

            response.setPreferences(handlePreferences);
        }

        response.setConsentHandleId(consentHandle.getConsentHandleId());
        response.setTemplateId(template.getTemplateId());
        response.setTemplateName(template.getTemplateName());
        response.setTemplateVersion(template.getVersion());
        response.setBusinessId(consentHandle.getBusinessId());
        response.setMultilingual(template.getMultilingual());
        response.setCustomerIdentifiers(consentHandle.getCustomerIdentifiers());
        response.setStatus(consentHandle.getStatus());
        response.setRemarks(consentHandle.getRemarks());
        response.setUiConfig(template.getUiConfig());
        response.setDocumentMeta(template.getDocumentMeta());

        // Set parental consent fields
        response.setIsParental(consentHandle.getIsParental());
        response.setParentIdentity(consentHandle.getParentIdentity());
        response.setParentIdentityType(consentHandle.getParentIdentityType());
        response.setParentName(consentHandle.getParentName());

        return response;
    }

    public SearchResponse<ConsentHandle> searchHandlesByParams(Map<String, Object> reqParams) throws ConsentException {
        Map<String, Object> searchParams = this.utils.filterRequestParam(reqParams, handlesSearchParams);
        List<ConsentHandle> mongoResponse = this.consentHandleRepository.findConsentHandleByParams(searchParams);

        if (org.apache.commons.lang3.ObjectUtils.isEmpty(mongoResponse)) {
            throw new ConsentException(ErrorCodes.JCMP3001);
        }

        return SearchResponse.<ConsentHandle>builder()
                .searchList(mongoResponse)
                .build();
    }

    public long count() {
        return this.consentHandleRepository.count();
    }

    /**
     * Get consent purpose names from template by extracting unique purposeIds
     * and fetching purpose names from database, then concatenating them into a string
     *
     * @param template The template containing preferences with purposeIds
     * @return Concatenated string of purpose names, or empty string if error occurs
     */
    private String getConsentPurposeFromTemplate(Template template) {
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
            log.error("Error fetching consent purpose names from template: {}", e.getMessage(), e);
            return "";
        }
    }

    /**
     * Modular function to log consent handle audit events
     * Can be used in both create and update consent handle flows
     *
     * @param consentHandle The consent handle entity to audit
     * @param actionType The action type (CREATE, UPDATE, GRANTED)
     */
    public void logConsentHandleAudit(ConsentHandle consentHandle, ActionType actionType) {
        try {
            Actor actor = Actor.builder()
                    .id(consentHandle.getCustomerIdentifiers().getValue())
                    .role(Constants.DATA_PRINCIPLE)
                    .type(consentHandle.getCustomerIdentifiers().getType().name())
                    .build();

            Resource resource = Resource.builder()
                    .type(Constants.CONSENT_HANDLE_ID)
                    .id(consentHandle.getConsentHandleId())
                    .build();

            Context context = Context.builder()
                    .ipAddress(ThreadContext.get(Constants.SOURCE_IP) != null && !ThreadContext.get(Constants.SOURCE_IP).equals("-") 
                            ? ThreadContext.get(Constants.SOURCE_IP) : null)
                    .txnId(ThreadContext.get(Constants.TXN_ID_THREAD_CONTEXT) != null && !ThreadContext.get(Constants.TXN_ID_THREAD_CONTEXT).equals("-") 
                            ? ThreadContext.get(Constants.TXN_ID_THREAD_CONTEXT) : null)
                    .build();

            Map<String, Object> extra = new HashMap<>();
            // Add consent handle POJO in the extra field under the "data" key
            extra.put(Constants.DATA, consentHandle);

            AuditRequest auditRequest = AuditRequest.builder()
                    .actor(actor)
                    .businessId(consentHandle.getBusinessId())
                    .group(Constants.CONSENT_GROUP)
                    .component(AuditComponent.CONSENT_REQUEST)
                    .actionType(actionType)
                    .resource(resource)
                    .initiator(Constants.DATA_FIDUCIARY)
                    .context(context)
                    .extra(extra)
                    .build();

            String tenantId = ThreadContext.get(Constants.TENANT_ID_HEADER);
            this.auditManager.logAudit(auditRequest, tenantId);
        } catch (Exception e) {
            log.error("Audit logging failed for consent handle id: {}, action: {}, error: {}", 
                    consentHandle.getConsentHandleId(), actionType, e.getMessage(), e);
        }
    }

    /**
     * Validates the JWS signature from the request header
     * 
     * @param signature The JWS signature from x-jws-signature header
     * @param headers Request headers map
     * @param request The create handle request
     * @throws ConsentException if signature validation fails
     */
    public void validateSignature(String signature, Map<String, String> headers, CreateHandleRequest request) throws ConsentException {
        log.debug("Validating JWS signature for consent handle creation");
        
        if (signature == null || signature.trim().isEmpty()) {
            throw new ConsentException(ErrorCodes.JCMP1038);
        }
        
        try {
            signatureService.verifyRequest(request, headers);
        } catch (Exception e) {
            log.error("Signature validation failed: {}", e.getMessage(), e);
            if (e instanceof ConsentException) {
                throw (ConsentException) e;
            }
            throw new ConsentException(ErrorCodes.JCMP3018);
        }
    }

    /**
     * Signs the response and returns the JWS signature
     * 
     * @param response The consent handle response to sign
     * @param headers Request headers map
     * @return JWS signature string to be added to response header
     */
    public String signResponse(ConsentHandleResponse response, Map<String, String> headers) {
        log.debug("Signing response for consent handle creation");
        
        try {
            String tenantId = ThreadContext.get(Constants.TENANT_ID_HEADER);
            if (tenantId == null || tenantId.isEmpty()) {
                log.warn("Tenant ID not found in ThreadContext, skipping response signing");
                return null;
            }
            
            return signatureService.signResponse(tenantId, response);
        } catch (Exception e) {
            log.error("Response signing failed: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * Create a parental consent request
     * 
     * @param request The parental consent request containing consent handle id and parent details
     * @param tenantId The tenant ID from request header
     * @return ParentalConsentResponse containing success message
     * @throws ConsentException if consent handle not found or any other error occurs
     */
    public ParentalConsentResponse createParentalConsentRequest(CreateParentalConsentRequest request, String tenantId) throws ConsentException {
        log.info("Creating parental consent request for consentHandleId: {}", request.getConsentHandleId());

        // Step 1: Fetch consent handle by ID and make a copy of the data
        ConsentHandle existingConsentHandle = this.consentHandleRepository.getByConsentHandleId(request.getConsentHandleId());
        if (ObjectUtils.isEmpty(existingConsentHandle)) {
            log.error("Consent handle not found for id: {}", request.getConsentHandleId());
            throw new ConsentException(ErrorCodes.JCMP3001);
        }

        // Store the data from existing consent handle
        String businessId = existingConsentHandle.getBusinessId();
        String txnId = existingConsentHandle.getTxnId();
        String templateId = existingConsentHandle.getTemplateId();
        int templateVersion = existingConsentHandle.getTemplateVersion();
        CustomerIdentifiers customerIdentifiers = existingConsentHandle.getCustomerIdentifiers();
        String consentId = existingConsentHandle.getConsentId();
        ConsentHandleStatus status = existingConsentHandle.getStatus();
        ConsentHandleRemarks remarks = existingConsentHandle.getRemarks();

        // Delete the existing consent handle from DB based on objectId
        this.consentHandleRepository.deleteById(existingConsentHandle.getId());
        log.info("Deleted existing consent handle with id: {}", request.getConsentHandleId());

        // Step 2: Call createSecureCode API
        CreateSecureCodeResponse secureCodeResponse = this.authManager.createSecureCode(
                tenantId,
                businessId,
                request.getParentIdentity(),
                request.getParentIdentityType()
        );
        log.info("Secure code created successfully for consentHandleId: {}, secureCode: {}", 
                request.getConsentHandleId(), secureCodeResponse.getSecureCode());

        // Step 3: Create new consent handle with parental fields and old customer identifiers
        String newConsentHandleId = UUID.randomUUID().toString();
        ConsentHandle newConsentHandle = ConsentHandle.builder()
                .consentHandleId(newConsentHandleId)
                .businessId(businessId)
                .txnId(txnId)
                .templateId(templateId)
                .templateVersion(templateVersion)
                .customerIdentifiers(customerIdentifiers)
                .consentId(consentId)
                .status(status)
                .remarks(remarks)
                .isParental(request.getIsParental())
                .parentIdentity(request.getParentIdentity())
                .parentIdentityType(request.getParentIdentityType())
                .parentName(request.getParentName())
                .build();

        ConsentHandle savedHandle = this.consentHandleRepository.save(newConsentHandle);
        log.info("New consent handle created with parental details, new id: {}", savedHandle.getConsentHandleId());

        // Audit logging for created consent handle
        this.logConsentHandleAudit(savedHandle, ActionType.CREATE);

        // Step 4: Create redirect URL with base64 encoded details
        String redirectUrl = createParentalConsentRedirectUrl(
                tenantId,
                businessId,
                newConsentHandleId,
                secureCodeResponse.getSecureCode(),
                request.getRedirectUri()
        );
        log.info("Redirect URL created for parental consent: {}", redirectUrl);

        // Step 5: Trigger parental consent notification
        triggerParentalConsentNotification(
                tenantId,
                businessId,
                request.getParentIdentity(),
                request.getParentIdentityType(),
                request.getParentName(),
                customerIdentifiers.getValue(),
                newConsentHandleId,
                redirectUrl
        );

        return ParentalConsentResponse.builder()
                .message("Parental consent request created successfully")
                .consentHandleId(newConsentHandleId)
                .redirectUrl(redirectUrl)
                .build();
    }

    /**
     * Triggers parental consent notification to parent
     *
     * @param tenantId Tenant ID
     * @param businessId Business ID
     * @param parentIdentity Parent identity value (e.g., mobile number)
     * @param parentIdentityType Parent identity type (e.g., MOBILE)
     * @param parentName Parent name
     * @param minorName Minor's name (from old consent handle customer identifier value)
     * @param consentHandleId New Consent Handle ID
     * @param reviewUrl Redirect URL for parent to review consent
     */
    private void triggerParentalConsentNotification(String tenantId, String businessId,
                                                     String parentIdentity, String parentIdentityType,
                                                     String parentName, String minorName,
                                                     String consentHandleId, String reviewUrl) {
        try {
            // Create customer identifiers for parent
            CustomerIdentifiers parentCustomerIdentifiers = CustomerIdentifiers.builder()
                    .type(IdentityType.valueOf(parentIdentityType))
                    .value(parentIdentity)
                    .build();

            // Create event payload
            Map<String, Object> eventPayload = new HashMap<>();
            eventPayload.put("parentName", parentName);
            eventPayload.put("minorName", minorName);
            eventPayload.put("consentHandle", consentHandleId);
            eventPayload.put("reviewUrl", reviewUrl);

            // Trigger notification
            this.notificationManager.initiateConsentHandleNotification(
                    NotificationEvent.PARENTAL_CONSENT,
                    tenantId,
                    businessId,
                    parentCustomerIdentifiers,
                    null,
                    eventPayload,
                    LANGUAGE.ENGLISH,
                    consentHandleId
            );

            log.info("Parental consent notification triggered for consentHandleId: {}, parentIdentity: {}", 
                    consentHandleId, parentIdentity);
        } catch (Exception e) {
            log.error("Failed to trigger parental consent notification for consentHandleId: {}, error: {}", 
                    consentHandleId, e.getMessage(), e);
            // Don't throw exception - notification failure should not fail the main flow
        }
    }

    /**
     * Creates a redirect URL with base64 encoded details for parental consent
     *
     * @param tenantId Tenant ID
     * @param businessId Business ID
     * @param consentHandleId New Consent Handle ID
     * @param secCode Secure code from auth service
     * @param redirectUri Redirect URI for callback from request body
     * @return Complete redirect URL with base64 encoded details parameter
     */
    private String createParentalConsentRedirectUrl(String tenantId, String businessId, 
                                                     String consentHandleId, String secCode, String redirectUri) {
        try {
            ParentalConsentRedirectDetails details = ParentalConsentRedirectDetails.builder()
                    .tenantId(tenantId)
                    .businessId(businessId)
                    .consentHandleId(consentHandleId)
                    .callBackUrl(redirectUri)
                    .secCode(secCode)
                    .build();

            String jsonDetails = objectMapper.writeValueAsString(details);
            String base64EncodedDetails = Base64.getEncoder().encodeToString(jsonDetails.getBytes());

            // Construct the redirect URL
            String baseUrl = parentalConsentRedirectBaseUrl.endsWith("/") 
                    ? parentalConsentRedirectBaseUrl.substring(0, parentalConsentRedirectBaseUrl.length() - 1) 
                    : parentalConsentRedirectBaseUrl;
            
            return baseUrl + "?details=" + base64EncodedDetails;
        } catch (JsonProcessingException e) {
            log.error("Error creating redirect URL for parental consent: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to create redirect URL for parental consent", e);
        }
    }
}
