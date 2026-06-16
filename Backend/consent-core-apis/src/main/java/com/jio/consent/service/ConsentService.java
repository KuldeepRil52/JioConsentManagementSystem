package com.jio.consent.service;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.jio.consent.client.audit.AuditManager;
import com.jio.consent.client.audit.request.Actor;
import com.jio.consent.client.audit.request.AuditRequest;
import com.jio.consent.client.audit.request.Context;
import com.jio.consent.client.audit.request.Resource;
import com.jio.consent.client.notification.NotificationManager;
import com.jio.consent.client.registry.RegistryManager;
import com.jio.consent.client.vault.VaultManager;
import com.jio.consent.client.vault.response.EncryptPayloadResponse;
import com.jio.consent.client.vault.response.VerifyResponse;
import com.jio.consent.config.LocalDateTypeAdapter;
import com.jio.consent.constant.Constants;
import com.jio.consent.constant.ErrorCodes;
import com.jio.consent.dto.*;
import com.jio.consent.dto.Request.CreateConsentRequest;
import com.jio.consent.dto.Request.UpdateConsentRequest;
import com.jio.consent.dto.Request.ParentalKycRequest;
import com.jio.consent.dto.Request.ValidateTokenRequest;
import com.jio.consent.dto.Response.ConsentCreateResponse;
import com.jio.consent.dto.Response.ConsentStatusCountResponse;
import com.jio.consent.dto.Response.CountResponse;
import com.jio.consent.dto.Response.SearchResponse;
import com.jio.consent.entity.Consent;
import com.jio.consent.entity.ConsentHandle;
import com.jio.consent.entity.Template;
import com.jio.consent.exception.ConsentException;
import com.jio.consent.multitenancy.TenantMongoTemplateProvider;
import com.jio.consent.repository.ConsentHandleRepository;
import com.jio.consent.repository.ConsentRepository;
import com.jio.consent.repository.ProcessorActivityRepository;
import com.jio.consent.repository.PurposeRepository;
import com.jio.consent.repository.UserSessionRepository;
import com.jio.consent.entity.UserSession;
import com.jio.consent.service.signature.RequestResponseSignatureService;
import com.jio.consent.utils.TokenUtility;
import com.jio.consent.utils.Utils;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.ThreadContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ConsentService {

    private final Object dataHashLock = new Object();

    ConsentHandleRepository consentHandleRepository;
    ConsentHandleService consentHandleService;
    TemplateService templateService;
    TokenUtility tokenUtility;
    ConsentRepository consentRepository;
    PurposeRepository purposeRepository;
    ProcessorActivityRepository processorActivityRepository;
    Utils utils;
    NotificationManager notificationManager;
    AuditManager auditManager;
    VaultManager vaultManager;
    RegistryManager registryManager;
    ParentalKycService parentalKycService;
    TenantMongoTemplateProvider tenantMongoTemplateProvider;
    RequestResponseSignatureService signatureService;
    UserSessionRepository userSessionRepository;

    @Autowired
    public ConsentService(ConsentHandleRepository consentHandleRepository,
                          ConsentHandleService consentHandleService,
                          TemplateService templateService,
                          TokenUtility tokenUtility,
                          ConsentRepository consentRepository,
                          PurposeRepository purposeRepository,
                          ProcessorActivityRepository processorActivityRepository,
                          Utils utils,
                          NotificationManager notificationManager,
                          AuditManager auditManager,
                          VaultManager vaultManager,
                          RegistryManager registryManager,
                          ParentalKycService parentalKycService,
                          TenantMongoTemplateProvider tenantMongoTemplateProvider,
                          RequestResponseSignatureService signatureService,
                          UserSessionRepository userSessionRepository) {
        this.consentHandleRepository = consentHandleRepository;
        this.consentHandleService = consentHandleService;
        this.templateService = templateService;
        this.tokenUtility = tokenUtility;
        this.consentRepository = consentRepository;
        this.purposeRepository = purposeRepository;
        this.processorActivityRepository = processorActivityRepository;
        this.utils = utils;
        this.notificationManager = notificationManager;
        this.auditManager = auditManager;
        this.vaultManager = vaultManager;
        this.registryManager = registryManager;
        this.parentalKycService = parentalKycService;
        this.tenantMongoTemplateProvider = tenantMongoTemplateProvider;
        this.signatureService = signatureService;
        this.userSessionRepository = userSessionRepository;
    }

    @Value("${consent.search.parameters}")
    List<String> consentSearchParams;

    public ConsentCreateResponse createConsentByConsentHandleId(CreateConsentRequest request) throws Exception {
        ConsentHandle currentHandle = this.consentHandleRepository.getByConsentHandleId(request.getConsentHandleId());
        if (ObjectUtils.isEmpty(currentHandle)) {
            throw new ConsentException(ErrorCodes.JCMP3003);
        }

        if (!currentHandle.getStatus().equals(ConsentHandleStatus.PENDING)) {
            throw new ConsentException(ErrorCodes.JCMP3004);
        }

        Consent existingConsent = this.consentRepository.existByTemplateIdAndTemplateVersionAndCustomerIdentifiers(currentHandle.getTemplateId(), currentHandle.getTemplateVersion(), currentHandle.getCustomerIdentifiers());
        Map<String, Object> templateSearchParams = Map.of(
                "templateId", currentHandle.getTemplateId(),
                "version", currentHandle.getTemplateVersion()
        );
        SearchResponse<Template> searchTemplates = this.templateService.searchTemplates(templateSearchParams);
        List<Template> templates = searchTemplates.getSearchList();
        if (ObjectUtils.isEmpty(templates) || templates.isEmpty()) {
            throw new ConsentException(ErrorCodes.JCMP3002);
        }
        Template template = templates.getFirst();

        String consentId;
        Consent consent;

        // If consent already exists, create new entry from existing consent with same consentId
        if (!ObjectUtils.isEmpty(existingConsent)) {
            consentId = existingConsent.getConsentId();
            // Create new consent entry from existing consent (copy constructor)
            consent = new Consent(existingConsent);
            // Set _id to null to create a new record
            consent.setId(null);
        } else {
            consentId = UUID.randomUUID().toString();
            consent = new Consent();
            consent.setConsentId(consentId);
        }
        LocalDateTime now = LocalDateTime.now();
        final LocalDateTime[] consentExpiryList = {now};
        List<Preference> updatedPreferences = new ArrayList<>();
        template.getPreferences().stream()
                .forEach(preference -> {
                    if (request.getPreferencesStatus().containsKey(preference.getPreferenceId())) {
                        preference.setPreferenceStatus(request.getPreferencesStatus().get(preference.getPreferenceId()));
                        preference.setStartDate(now);
                        LocalDateTime endDate = now;
                        if (preference.getPreferenceValidity().getUnit().equals(Period.YEARS)) {
                            endDate = endDate.plusYears(preference.getPreferenceValidity().getValue());
                        } else if (preference.getPreferenceValidity().getUnit().equals(Period.MONTHS)) {
                            endDate = endDate.plusMonths(preference.getPreferenceValidity().getValue());
                        } else {
                            endDate = endDate.plusDays(preference.getPreferenceValidity().getValue());
                        }
                        preference.setEndDate(endDate);
                        if (endDate.isAfter(consentExpiryList[0]) && preference.getPreferenceStatus().equals(PreferenceStatus.ACCEPTED)) {
                            consentExpiryList[0] = endDate;
                        }
                        updatedPreferences.add(preference);
                    } else {
                        preference.setPreferenceStatus(PreferenceStatus.NOTACCEPTED);
                        preference.setStartDate(now);
                        LocalDateTime endDate = now;
                        if (preference.getPreferenceValidity().getUnit().equals(Period.YEARS)) {
                            endDate = endDate.plusYears(preference.getPreferenceValidity().getValue());
                        } else if (preference.getPreferenceValidity().getUnit().equals(Period.MONTHS)) {
                            endDate = endDate.plusMonths(preference.getPreferenceValidity().getValue());
                        } else {
                            endDate = endDate.plusDays(preference.getPreferenceValidity().getValue());
                        }
                        preference.setEndDate(endDate);
                        updatedPreferences.add(preference);
                    }
                });

        boolean hasAcceptedPreferences = updatedPreferences.stream()
                .anyMatch(preference -> PreferenceStatus.ACCEPTED.equals(preference.getPreferenceStatus()));

        LocalDateTime consentExpiry;
        if (!hasAcceptedPreferences) {
            consentExpiry = updatedPreferences.stream()
                    .filter(preference -> preference.getEndDate() != null)
                    .map(Preference::getEndDate)
                    .max(LocalDateTime::compareTo)
                    .orElse(consentExpiryList[0]);
        } else {
            consentExpiry = consentExpiryList[0];
        }

        // Ensure consentId is set
        consent.setConsentId(consentId);
        consent.setConsentHandleId(currentHandle.getConsentHandleId());
        consent.setBusinessId(template.getBusinessId());
        consent.setTemplateId(currentHandle.getTemplateId());
        consent.setTemplateVersion(currentHandle.getTemplateVersion());
        consent.setTemplateName(template.getTemplateName());
        consent.setLanguagePreferences(request.getLanguagePreference());
        consent.setMultilingual(template.getMultilingual());
        consent.setCustomerIdentifiers(currentHandle.getCustomerIdentifiers());
        consent.setStartDate(now);
        consent.setEndDate(consentExpiry);

        // Handle parental consent if required
        if (request.getIsParentalConsent() != null && request.getIsParentalConsent()
                && request.getParentalKYCType() != null && request.getParentalKYCType().equals(ParentalKycType.DIGILOCKER)) {

            log.info("Processing parental consent for handle: {}", currentHandle.getConsentHandleId());

            // Create ParentalKycRequest from request data
            ParentalKycRequest parentalKycRequest = ParentalKycRequest.builder()
                    .code(request.getCode())
                    .state(request.getState())
                    .build();

            // Create headers for parental KYC service
            Map<String, String> headers = new HashMap<>();
            headers.put(Constants.TENANT_ID_HEADER, ThreadContext.get(Constants.TENANT_ID_HEADER));
            headers.put(Constants.BUSINESS_ID_HEADER, currentHandle.getBusinessId());

            try {
                // Call parental KYC service
                Map<String, String> parentalKycResult = this.parentalKycService.createParentalKyc(parentalKycRequest, headers);

                // Set parental KYC data in consent
                consent.setParentalKyc(parentalKycResult.get("parental_kyc"));
                consent.setParentalReferenceId(parentalKycResult.get("parental_reference_id"));
                consent.setIsParentalConsent(true);

                log.info("Parental KYC processed successfully for consent: {}", consentId);
            } catch (Exception e) {
                log.error("Failed to process parental KYC for consent: {}, error: {}", consentId, e.getMessage());
                throw new ConsentException(ErrorCodes.JCMP3008);
            }
        }

        if (!ObjectUtils.isEmpty(updatedPreferences)) {
            List<String> purposeIds = updatedPreferences.stream()
                    .filter(p -> !ObjectUtils.isEmpty(p.getPurposeIds()))
                    .flatMap(p -> p.getPurposeIds().stream())
                    .distinct()
                    .collect(java.util.stream.Collectors.toList());

            List<String> processorActivityIds = updatedPreferences.stream()
                    .filter(p -> !ObjectUtils.isEmpty(p.getProcessorActivityIds()))
                    .flatMap(p -> p.getProcessorActivityIds().stream())
                    .distinct()
                    .collect(java.util.stream.Collectors.toList());

            var purposes = ObjectUtils.isEmpty(purposeIds) ? List.<com.jio.consent.entity.Purpose>of() : this.purposeRepository.findByPurposeIds(purposeIds);
            var processorActivities = ObjectUtils.isEmpty(processorActivityIds) ? List.<com.jio.consent.entity.ProcessorActivity>of() : this.processorActivityRepository.findByProcessorActivityIds(processorActivityIds);

            var purposeById = purposes.stream().collect(java.util.stream.Collectors.toMap(com.jio.consent.entity.Purpose::getPurposeId, p -> p));
            var activityById = processorActivities.stream().collect(java.util.stream.Collectors.toMap(com.jio.consent.entity.ProcessorActivity::getProcessorActivityId, a -> a));

            List<HandlePreference> handlePreferences = updatedPreferences.stream().map(pref -> {
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

            consent.setPreferences(handlePreferences);
        }
        consent.setStatus(determineConsentStatus(consent.getPreferences()));
        // Set staleStatus to NOT_STALE for new consents
        consent.setStaleStatus(StaleStatus.NOT_STALE);

        // Extract processor IDs from processor activities in consent preferences
        List<String> allProcessorIds = (consent.getPreferences() != null ? consent.getPreferences() : List.<HandlePreference>of()).stream()
                .filter(p -> p.getProcessorActivityList() != null)
                .flatMap(p -> p.getProcessorActivityList().stream())
                .filter(pa -> pa.getProcessActivityInfo() != null && pa.getProcessActivityInfo().getProcessorId() != null)
                .map(pa -> pa.getProcessActivityInfo().getProcessorId())
                .distinct()
                .collect(Collectors.toList());

        String consentToken = this.generateConsentJwtToken(consent);

        // Compute SHA-256 hash of consentJsonString and store as payloadHash
        String consentJsonStringHash = null;
        if (consent.getConsentJsonString() != null) {
            consentJsonStringHash = this.utils.computeSHA256Hash(consent.getConsentJsonString());
            consent.setPayloadHash(consentJsonStringHash);
        }

        // Synchronize currentChainHash computation and save to avoid race conditions
        synchronized (dataHashLock) {
            // Get the latest consent by createdAt and compute currentChainHash
            Consent latestConsent = this.consentRepository.findLatestByCreatedAt();
            // Extract previous hash from currentChainHash
            String prevHash = null;
            if (latestConsent != null && latestConsent.getCurrentChainHash() != null) {
                prevHash = latestConsent.getCurrentChainHash();
            }

            // Compute currentChainHash = sha256(prevHash + consentJsonStringHash)
            String currentChainHash;
            if (prevHash != null && consentJsonStringHash != null) {
                currentChainHash = this.utils.computeSHA256Hash(prevHash + consentJsonStringHash);
            } else if (consentJsonStringHash != null) {
                currentChainHash = consentJsonStringHash;
            } else {
                currentChainHash = null;
            }
            consent.setCurrentChainHash(currentChainHash);
            // Save consent within synchronized block to maintain hash chain integrity
            this.consentRepository.save(consent);
        }

        // If consent already exists, mark it as STALE after saving the new consent
        if (!ObjectUtils.isEmpty(existingConsent)) {
            existingConsent.setStaleStatus(StaleStatus.STALE);
            this.consentRepository.save(existingConsent);
        }

        try {
            currentHandle.setStatus(ConsentHandleStatus.APPROVED);
            currentHandle.setConsentId(consent.getConsentId());
            this.consentHandleRepository.save(currentHandle);
        } catch (Exception e) {
            throw e;
        }

        try {
            Map<String, Object> eventPayload = new HashMap<>();
            eventPayload.put("consentId", consent.getConsentId());
            eventPayload.put("consentJwtToken", consentToken);
            this.notificationManager.triggerConsentEvent(NotificationEvent.CONSENT_CREATED,
                    ThreadContext.get(Constants.TENANT_ID_HEADER),
                    consent.getBusinessId(),
                    consent.getCustomerIdentifiers(),
                    allProcessorIds,
                    eventPayload,
                    consent.getLanguagePreferences());
        } catch (Exception e) {
            log.error("CONSENT_CREATED trigger failed for consent id: {}, error: {}", consentId, e.getMessage());
        }

        // Audit logging for consent
        this.logConsentAudit(consent, ActionType.CREATE, consentToken);

        // Audit logging for consent handle when consent is granted
        this.consentHandleService.logConsentHandleAudit(currentHandle, ActionType.GRANTED);

        ConsentCreateResponse response = ConsentCreateResponse.builder()
                .consentId(consent.getConsentId())
                .consentJwtToken(consentToken)
                .message("Consent created Successfully!")
                .consentExpiry(consentExpiry.toString())
                .build();

        return response;
    }


    private ConsentStatus determineConsentStatus(List<HandlePreference> preferences) {
        boolean hasAccept = preferences.stream()
                .anyMatch(preference -> preference.getPreferenceStatus().equals(PreferenceStatus.ACCEPTED));

        boolean allReject = preferences.stream()
                .allMatch(preference -> preference.getPreferenceStatus().equals(PreferenceStatus.NOTACCEPTED));

        boolean allExpired = preferences.stream()
                .allMatch(preference -> preference.getPreferenceStatus().equals(PreferenceStatus.EXPIRED));

        if (hasAccept || allReject) {
            return ConsentStatus.ACTIVE;
        } else if (allExpired) {
            return ConsentStatus.EXPIRED;
        }

        return ConsentStatus.INACTIVE;
    }

    /**
     * Generate JWT token for consent using Vault sign API
     * Also sets the consentJsonString and encryptedReferenceId fields on the consent object
     *
     * @param consent The consent entity to generate token for
     * @return JWT token string
     */
    private String generateConsentJwtToken(Consent consent) {
        Consent tempConsent = new Consent(consent);
        Gson gson = new GsonBuilder()
                .registerTypeAdapter(LocalDateTime.class, new LocalDateTypeAdapter())
                .disableHtmlEscaping()
                .create();
        String consentJsonString = gson.toJson(tempConsent).replaceAll("\\s+", "");

        // Store consentJsonString in the consent entity
        consent.setConsentJsonString(consentJsonString);

        // Encrypt the consentJsonString using Vault encryptPayload API
        String tenantId = ThreadContext.get(Constants.TENANT_ID_HEADER);
        String businessId = consent.getBusinessId();
        try {
            EncryptPayloadResponse encryptResponse = this.vaultManager.encryptPayload(
                    tenantId,
                    businessId,
                    "Consent",
                    "Consent",
                    consentJsonString
            );
            // Store encryptedReferenceId in the consent entity
            consent.setEncryptedReferenceId(encryptResponse.getReferenceId());
        } catch (Exception e) {
            log.error("Failed to encrypt consent payload for consent id: {}, error: {}",
                    consent.getConsentId(), e.getMessage(), e);
            // Continue with JWT generation even if encryption fails
        }

        // Build payload for Vault sign API
        Map<String, Object> payload = new HashMap<>();
        payload.put("sub", consentJsonString);
        payload.put("iss", "JIO CONSENT");
        payload.put("iat", consent.getStartDate().atZone(ZoneId.systemDefault()).toEpochSecond());
        payload.put("exp", consent.getEndDate().atZone(ZoneId.systemDefault()).toEpochSecond());

        return this.vaultManager.sign(tenantId, businessId, payload).getJwt();
    }

    /**
     * Modular function to log consent audit events
     * Can be used in both create and update consent flows
     *
     * @param consent         The consent entity to audit
     * @param actionType      The action type (CREATE, UPDATE, DELETE)
     * @param consentJwtToken The JWT token for the consent (optional)
     */
    private void logConsentAudit(Consent consent, ActionType actionType, String consentJwtToken) {
        try {
            Actor actor = Actor.builder()
                    .id(consent.getCustomerIdentifiers().getValue())
                    .role(Constants.DATA_PRINCIPLE)
                    .type(consent.getCustomerIdentifiers().getType().name())
                    .build();

            Resource resource = Resource.builder()
                    .type(Constants.CONSENT_ID)
                    .id(consent.getConsentId())
                    .build();

            Context context = Context.builder()
                    .ipAddress(ThreadContext.get(Constants.SOURCE_IP) != null && !ThreadContext.get(Constants.SOURCE_IP).equals("-")
                            ? ThreadContext.get(Constants.SOURCE_IP) : null)
                    .txnId(ThreadContext.get(Constants.TXN_ID_THREAD_CONTEXT) != null && !ThreadContext.get(Constants.TXN_ID_THREAD_CONTEXT).equals("-")
                            ? ThreadContext.get(Constants.TXN_ID_THREAD_CONTEXT) : null)
                    .build();

            Map<String, Object> extra = new HashMap<>();
            // Add consent POJO in the extra field under the "data" key
            extra.put(Constants.DATA, consent);
            // Add consentJwtToken to the extra field
            if (consentJwtToken != null) {
                extra.put("consentJwtToken", consentJwtToken);
            }

            AuditRequest auditRequest = AuditRequest.builder()
                    .actor(actor)
                    .businessId(consent.getBusinessId())
                    .group(Constants.CONSENT_GROUP)
                    .component(AuditComponent.CONSENT)
                    .actionType(actionType)
                    .resource(resource)
                    .initiator(Constants.DATA_FIDUCIARY)
                    .context(context)
                    .extra(extra)
                    .build();

            String tenantId = ThreadContext.get(Constants.TENANT_ID_HEADER);
            this.auditManager.logAudit(auditRequest, tenantId);
        } catch (Exception e) {
            log.error("Audit logging failed for consent id: {}, action: {}, error: {}",
                    consent.getConsentId(), actionType, e.getMessage(), e);
        }
    }

    public VerifyResponse validateConsentToken(ValidateTokenRequest request) throws Exception {
        String token = request.getConsentToken();
        
        // Get tenantId from ThreadContext
        String tenantId = ThreadContext.get(Constants.TENANT_ID_HEADER);
        if (tenantId == null || tenantId.isEmpty()) {
            log.error("Tenant ID is missing from ThreadContext");
            throw new ConsentException(ErrorCodes.JCMP3001);
        }

        // Extract businessId from token payload
        // The token payload contains "sub" field with consentJsonString which has businessId
        String businessId = extractBusinessIdFromToken(token);
        if (businessId == null || businessId.isEmpty()) {
            log.error("Unable to extract businessId from token");
            throw new ConsentException(ErrorCodes.JCMP3001);
        }

        log.info("Validating token for tenantId: {}, businessId: {}", tenantId, businessId);
        // Use Vault verify API
        return this.vaultManager.verify(tenantId, businessId, token);
    }

    /**
     * Extract businessId from JWT token payload
     * The token contains "sub" field with consentJsonString which includes businessId
     */
    private String extractBusinessIdFromToken(String token) {
        try {
            // Decode JWT without verification to get payload
            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                log.error("Invalid JWT token format - expected 3 parts, got: {}", parts.length);
                return null;
            }

            // Decode payload (base64url) - handle padding if needed
            String payloadPart = parts[1];
            // Add padding if needed for base64url decoding
            int padding = 4 - (payloadPart.length() % 4);
            if (padding != 4) {
                payloadPart = payloadPart + "=".repeat(padding);
            }
            
            String payload = new String(java.util.Base64.getUrlDecoder().decode(payloadPart));
            log.debug("Decoded JWT payload: {}", payload);
            
            JsonObject jsonObject = JsonParser.parseString(payload).getAsJsonObject();
            
            // Get "sub" field which contains consentJsonString
            if (jsonObject.has("sub")) {
                String consentJsonString = jsonObject.get("sub").getAsString();
                log.debug("Extracted consentJsonString from token: {}", consentJsonString);
                
                // Parse consentJsonString to get businessId
                JsonObject consentJson = JsonParser.parseString(consentJsonString).getAsJsonObject();
                if (consentJson.has("businessId")) {
                    String businessId = consentJson.get("businessId").getAsString();
                    log.debug("Extracted businessId from token: {}", businessId);
                    return businessId;
                } else {
                    log.error("businessId not found in consentJsonString");
                }
            } else {
                log.error("'sub' field not found in JWT payload");
            }
        } catch (IllegalArgumentException e) {
            log.error("Base64 decoding error: {}", e.getMessage(), e);
        } catch (com.google.gson.JsonSyntaxException e) {
            log.error("JSON parsing error: {}", e.getMessage(), e);
        } catch (Exception e) {
            log.error("Error extracting businessId from token: {}", e.getMessage(), e);
        }
        return null;
    }

    public Consent updateConsent(UpdateConsentRequest request, String consentId) throws Exception {
        Consent currentConsent = this.consentRepository.getByConsentId(consentId);
        if (org.apache.commons.lang3.ObjectUtils.isEmpty(currentConsent)) {
            throw new ConsentException(ErrorCodes.JCMP3001);
        }

        // Store the original _id before modifying (to mark original as STALE later)
        org.bson.types.ObjectId originalId = currentConsent.getId();

        // Set _id to null to create a new record instead of updating the existing one
        currentConsent.setId(null);

        LocalDateTime now = LocalDateTime.now();
        final LocalDateTime[] consentExpiryList = {now};
        currentConsent.setLanguagePreferences(request.getLanguagePreferences());
        if (request.getStatus().equals(ConsentStatus.WITHDRAWN)) {
            currentConsent.setStatus(ConsentStatus.WITHDRAWN);
            // Set staleStatus to NOT_STALE for new consent record created during withdrawal
            currentConsent.setStaleStatus(StaleStatus.NOT_STALE);
            String consentToken = this.generateConsentJwtToken(currentConsent);

            // Compute SHA-256 hash of consentJsonString and store as payloadHash
            String consentJsonStringHash = null;
            if (currentConsent.getConsentJsonString() != null) {
                consentJsonStringHash = this.utils.computeSHA256Hash(currentConsent.getConsentJsonString());
                currentConsent.setPayloadHash(consentJsonStringHash);
            }

            // Synchronize currentChainHash computation and save to avoid race conditions
            synchronized (dataHashLock) {
                // Get the latest consent by createdAt and compute currentChainHash
                Consent latestConsent = this.consentRepository.findLatestByCreatedAt();
                // Extract previous hash from currentChainHash
                String prevHash = null;
                if (latestConsent != null && latestConsent.getCurrentChainHash() != null) {
                    prevHash = latestConsent.getCurrentChainHash();
                }

                // Compute currentChainHash = sha256(prevHash + consentJsonStringHash)
                String currentChainHash;
                if (prevHash != null && consentJsonStringHash != null) {
                    currentChainHash = this.utils.computeSHA256Hash(prevHash + consentJsonStringHash);
                } else if (consentJsonStringHash != null) {
                    currentChainHash = consentJsonStringHash;
                } else {
                    currentChainHash = null;
                }
                currentConsent.setCurrentChainHash(currentChainHash);

                // Save new consent record within synchronized block to maintain hash chain integrity
                this.consentRepository.save(currentConsent);
            }

            // Mark original consent as STALE after saving the new consent
            if (originalId != null) {
                Consent originalConsent = this.consentRepository.findById(originalId);
                if (!ObjectUtils.isEmpty(originalConsent)) {
                    originalConsent.setStaleStatus(StaleStatus.STALE);
                    this.consentRepository.save(originalConsent);
                }
            }

            Consent withdrawnConsent = currentConsent;
            try {
                Map<String, Object> eventPayload = new HashMap<>();
                // Extract processor IDs from processor activities in consent preferences
                List<String> allProcessorIds = (currentConsent.getPreferences() != null ? currentConsent.getPreferences() : List.<HandlePreference>of()).stream()
                        .filter(p -> p.getProcessorActivityList() != null)
                        .flatMap(p -> p.getProcessorActivityList().stream())
                        .filter(pa -> pa.getProcessActivityInfo() != null && pa.getProcessActivityInfo().getProcessorId() != null)
                        .map(pa -> pa.getProcessActivityInfo().getProcessorId())
                        .distinct()
                        .collect(Collectors.toList());
                eventPayload.put("consentId", withdrawnConsent.getConsentId());
                eventPayload.put("consentJwtToken", consentToken);
                this.notificationManager.triggerConsentEvent(NotificationEvent.CONSENT_WITHDRAWN,
                        ThreadContext.get(Constants.TENANT_ID_HEADER),
                        withdrawnConsent.getBusinessId(),
                        withdrawnConsent.getCustomerIdentifiers(),
                        allProcessorIds,
                        eventPayload,
                        withdrawnConsent.getLanguagePreferences());
            } catch (Exception e) {
                log.error("CONSENT_WITHDRAWN trigger failed for consent id: {}, error: {}", consentId, e.getMessage());
            }

            // Call Registry API to withdraw consent
            try {
                this.registryManager.withdrawConsent(
                        ThreadContext.get(Constants.TENANT_ID_HEADER),
                        withdrawnConsent.getBusinessId(),
                        withdrawnConsent.getConsentId());
                log.info("Registry withdraw consent API called successfully for consent id: {}", consentId);
            } catch (Exception e) {
                log.error("Registry withdraw consent API failed for consent id: {}, error: {}", consentId, e.getMessage());
            }

            // Audit logging for withdrawn consent
            this.logConsentAudit(withdrawnConsent, ActionType.WITHDRAWN, consentToken);

            return withdrawnConsent;
        }


        currentConsent.getPreferences().stream().forEach(preference -> {
            PreferenceStatus existingStatus = preference.getPreferenceStatus();
            
            if (request.getPreferencesStatus().containsKey(preference.getPreferenceId())) {
                PreferenceStatus requestedStatus = request.getPreferencesStatus().get(preference.getPreferenceId());
                preference.setPreferenceStatus(requestedStatus);
                
                // Handle RENEW status
                if (requestedStatus.equals(PreferenceStatus.RENEW)) {
                    if (existingStatus.equals(PreferenceStatus.ACCEPTED)) {
                        // If existing is ACCEPTED, increase end date only by preference validity
                        LocalDateTime currentEndDate = preference.getEndDate();
                        if (currentEndDate != null) {
                            LocalDateTime newEndDate = currentEndDate;
                            if (preference.getPreferenceValidity().getUnit().equals(Period.YEARS)) {
                                newEndDate = newEndDate.plusYears(preference.getPreferenceValidity().getValue());
                            } else if (preference.getPreferenceValidity().getUnit().equals(Period.MONTHS)) {
                                newEndDate = newEndDate.plusMonths(preference.getPreferenceValidity().getValue());
                            } else {
                                newEndDate = newEndDate.plusDays(preference.getPreferenceValidity().getValue());
                            }
                            preference.setEndDate(newEndDate);
                            if (newEndDate.isAfter(consentExpiryList[0])) {
                                consentExpiryList[0] = newEndDate;
                            }
                        }
                        // Set status back to ACCEPTED after renew
                        preference.setPreferenceStatus(PreferenceStatus.ACCEPTED);
                    } else if (existingStatus.equals(PreferenceStatus.EXPIRED)) {
                        // If existing is EXPIRED, set start as now and end date freshly with preference validity
                        preference.setStartDate(now);
                        LocalDateTime endDate = now;
                        if (preference.getPreferenceValidity().getUnit().equals(Period.YEARS)) {
                            endDate = endDate.plusYears(preference.getPreferenceValidity().getValue());
                        } else if (preference.getPreferenceValidity().getUnit().equals(Period.MONTHS)) {
                            endDate = endDate.plusMonths(preference.getPreferenceValidity().getValue());
                        } else {
                            endDate = endDate.plusDays(preference.getPreferenceValidity().getValue());
                        }
                        preference.setEndDate(endDate);
                        // Set status to ACCEPTED after renew
                        preference.setPreferenceStatus(PreferenceStatus.ACCEPTED);
                        if (endDate.isAfter(consentExpiryList[0])) {
                            consentExpiryList[0] = endDate;
                        }
                    }
                    // If existing is NOT_ACCEPTED, skip the renew (do nothing)
                }
                // Handle ACCEPTED status
                else if (requestedStatus.equals(PreferenceStatus.ACCEPTED)) {
                    if (existingStatus.equals(PreferenceStatus.NOTACCEPTED)) {
                        // If existing is NOT_ACCEPTED, update both startDate and endDate
                        preference.setStartDate(now);
                        LocalDateTime endDate = now;
                        if (preference.getPreferenceValidity().getUnit().equals(Period.YEARS)) {
                            endDate = endDate.plusYears(preference.getPreferenceValidity().getValue());
                        } else if (preference.getPreferenceValidity().getUnit().equals(Period.MONTHS)) {
                            endDate = endDate.plusMonths(preference.getPreferenceValidity().getValue());
                        } else {
                            endDate = endDate.plusDays(preference.getPreferenceValidity().getValue());
                        }
                        preference.setEndDate(endDate);
                        if (endDate.isAfter(consentExpiryList[0])) {
                            consentExpiryList[0] = endDate;
                        }
                    } else if (existingStatus.equals(PreferenceStatus.ACCEPTED) && preference.getStartDate() != null && preference.getStartDate().isBefore(now)) {
                        // If already ACCEPTED and startDate is before now, update dates
                        preference.setStartDate(now);
                        LocalDateTime endDate = now;
                        if (preference.getPreferenceValidity().getUnit().equals(Period.YEARS)) {
                            endDate = endDate.plusYears(preference.getPreferenceValidity().getValue());
                        } else if (preference.getPreferenceValidity().getUnit().equals(Period.MONTHS)) {
                            endDate = endDate.plusMonths(preference.getPreferenceValidity().getValue());
                        } else {
                            endDate = endDate.plusDays(preference.getPreferenceValidity().getValue());
                        }
                        preference.setEndDate(endDate);
                        if (endDate.isAfter(consentExpiryList[0])) {
                            consentExpiryList[0] = endDate;
                        }
                    }
                }
                // Handle NOT_ACCEPTED status
                else if (requestedStatus.equals(PreferenceStatus.NOTACCEPTED)) {
                    // If existing is ACCEPTED, don't update start and end date
                    // Only update status, dates remain unchanged
                }
                // Handle other statuses (EXPIRED, etc.) - update dates if needed
                else {
                    if (preference.getStartDate() == null || preference.getStartDate().isBefore(now)) {
                        preference.setStartDate(now);
                    }
                    LocalDateTime endDate = now;
                    if (preference.getPreferenceValidity().getUnit().equals(Period.YEARS)) {
                        endDate = endDate.plusYears(preference.getPreferenceValidity().getValue());
                    } else if (preference.getPreferenceValidity().getUnit().equals(Period.MONTHS)) {
                        endDate = endDate.plusMonths(preference.getPreferenceValidity().getValue());
                    } else {
                        endDate = endDate.plusDays(preference.getPreferenceValidity().getValue());
                    }
                    preference.setEndDate(endDate);
                }
            } else {
                // Request doesn't contain this preference, mark it as NOT_ACCEPTED
                preference.setPreferenceStatus(PreferenceStatus.NOTACCEPTED);
                if (preference.getStartDate() == null || preference.getStartDate().isBefore(now)) {
                    preference.setStartDate(now);
                }
                LocalDateTime endDate = now;
                if (preference.getPreferenceValidity().getUnit().equals(Period.YEARS)) {
                    endDate = endDate.plusYears(preference.getPreferenceValidity().getValue());
                } else if (preference.getPreferenceValidity().getUnit().equals(Period.MONTHS)) {
                    endDate = endDate.plusMonths(preference.getPreferenceValidity().getValue());
                } else {
                    endDate = endDate.plusDays(preference.getPreferenceValidity().getValue());
                }
                preference.setEndDate(endDate);
            }
        });

        boolean hasAcceptedPreferences = currentConsent.getPreferences().stream()
                .anyMatch(preference -> PreferenceStatus.ACCEPTED.equals(preference.getPreferenceStatus()));

        LocalDateTime consentExpiry;
        if (!hasAcceptedPreferences) {
            consentExpiry = currentConsent.getPreferences().stream()
                    .filter(preference -> preference.getEndDate() != null)
                    .map(HandlePreference::getEndDate)
                    .max(LocalDateTime::compareTo)
                    .orElse(consentExpiryList[0]);
        } else {
            consentExpiry = consentExpiryList[0];
        }

        currentConsent.setLanguagePreferences(request.getLanguagePreferences());
        currentConsent.setEndDate(consentExpiry);
        currentConsent.setStatus(determineConsentStatus(currentConsent.getPreferences()));
        // Set staleStatus to NOT_STALE for new consent record created during update
        currentConsent.setStaleStatus(StaleStatus.NOT_STALE);

        // Extract processor IDs from processor activities in consent preferences
        List<String> allProcessorIds = (currentConsent.getPreferences() != null ? currentConsent.getPreferences() : List.<HandlePreference>of()).stream()
                .filter(p -> p.getProcessorActivityList() != null)
                .flatMap(p -> p.getProcessorActivityList().stream())
                .filter(pa -> pa.getProcessActivityInfo() != null && pa.getProcessActivityInfo().getProcessorId() != null)
                .map(pa -> pa.getProcessActivityInfo().getProcessorId())
                .distinct()
                .collect(Collectors.toList());

        String consentToken = this.generateConsentJwtToken(currentConsent);

        // Compute SHA-256 hash of consentJsonString and store as payloadHash
        String consentJsonStringHash = null;
        if (currentConsent.getConsentJsonString() != null) {
            consentJsonStringHash = this.utils.computeSHA256Hash(currentConsent.getConsentJsonString());
            currentConsent.setPayloadHash(consentJsonStringHash);
        }

        // Synchronize currentChainHash computation and save to avoid race conditions
        synchronized (dataHashLock) {
            // Get the latest consent by createdAt and compute currentChainHash
            Consent latestConsent = this.consentRepository.findLatestByCreatedAt();
            // Extract previous hash from currentChainHash
            String prevHash = null;
            if (latestConsent != null && latestConsent.getCurrentChainHash() != null) {
                prevHash = latestConsent.getCurrentChainHash();
            }

            // Compute currentChainHash = sha256(prevHash + consentJsonStringHash)
            String currentChainHash;
            if (prevHash != null && consentJsonStringHash != null) {
                currentChainHash = this.utils.computeSHA256Hash(prevHash + consentJsonStringHash);
            } else if (consentJsonStringHash != null) {
                currentChainHash = consentJsonStringHash;
            } else {
                currentChainHash = null;
            }
            currentConsent.setCurrentChainHash(currentChainHash);

            // Save new consent record within synchronized block to maintain hash chain integrity
            this.consentRepository.save(currentConsent);
        }

        // Mark original consent as STALE after saving the new consent
        if (originalId != null) {
            Consent originalConsent = this.consentRepository.findById(originalId);
            if (!ObjectUtils.isEmpty(originalConsent)) {
                originalConsent.setStaleStatus(StaleStatus.STALE);
                this.consentRepository.save(originalConsent);
            }
        }

        Consent consent = currentConsent;
        try {
            Map<String, Object> eventPayload = new HashMap<>();
            eventPayload.put("consentId", consent.getConsentId());
            eventPayload.put("consentJwtToken", consentToken);
            this.notificationManager.triggerConsentEvent(NotificationEvent.CONSENT_UPDATED,
                    ThreadContext.get(Constants.TENANT_ID_HEADER),
                    consent.getBusinessId(),
                    consent.getCustomerIdentifiers(),
                    allProcessorIds,
                    eventPayload,
                    consent.getLanguagePreferences());
        } catch (Exception e) {
            log.error("CONSENT_UPDATED trigger failed for consent id: {}, error: {}", consentId, e.getMessage());
        }

        // Audit logging
        this.logConsentAudit(consent, ActionType.UPDATE, consentToken);

        return consent;
    }

    public SearchResponse<Consent> searchConsentsByParams(Map<String, Object> reqParams) throws ConsentException {
        Map<String, Object> searchParams = this.utils.filterRequestParam(reqParams, consentSearchParams);
        List<Consent> mongoResponse = this.consentRepository.findConsentByParams(searchParams);

        if (org.apache.commons.lang3.ObjectUtils.isEmpty(mongoResponse)) {
            throw new ConsentException(ErrorCodes.JCMP3001);
        }

        return SearchResponse.<Consent>builder()
                .searchList(mongoResponse)
                .build();
    }

    public long count() {
        return this.consentRepository.count();
    }

    public CountResponse countByParams(Map<String, Object> reqParams) {
        Map<String, Object> searchParams = this.utils.filterRequestParam(reqParams, consentSearchParams);
        long count = this.consentRepository.countByParams(searchParams);
        return CountResponse.builder().count(count).build();
    }

    public ConsentStatusCountResponse countByStatus(Map<String, Object> reqParams) {
        Map<String, Object> searchParams = this.utils.filterRequestParam(reqParams, consentSearchParams);
        Map<ConsentStatus, Long> statusCounts = this.consentRepository.countByStatus(searchParams);

        long totalCount = statusCounts.values().stream().mapToLong(Long::longValue).sum();

        return ConsentStatusCountResponse.builder()
                .totalCount(totalCount)
                .statusCounts(statusCounts)
                .build();
    }

    /**
     * Verify if the current payloadHash matches the hash of the consentString after fetching
     * This function fetches the consent, recreates the consentJsonString (excluding payloadHash),
     * computes its hash, and compares it with the stored payloadHash
     *
     * @param consentId The consent ID to verify
     * @return true if the payloadHash matches the computed hash, false otherwise
     * @throws ConsentException if consent is not found
     */
    public boolean verifyPayloadHash(String consentId) throws ConsentException {
        Consent consent = this.consentRepository.getActiveByConsentId(consentId);
        if (ObjectUtils.isEmpty(consent)) {
            throw new ConsentException(ErrorCodes.JCMP3001);
        }

        // Recreate consentJsonString from the consent entity (same logic as generateConsentJwtToken)
        // This ensures we compute the hash from the current state of the consent, including encryptedReferenceId
        Consent tempConsent = new Consent(consent);
        Gson gson = new GsonBuilder()
                .registerTypeAdapter(LocalDateTime.class, new LocalDateTypeAdapter())
                .disableHtmlEscaping()
                .create();
        String recreatedConsentJsonString = gson.toJson(tempConsent).replaceAll("\\s+", "");

        // Compute hash of the recreated consentJsonString
        String computedHash = this.utils.computeSHA256Hash(recreatedConsentJsonString);

        // Compare with stored payloadHash
        String storedPayloadHash = consent.getPayloadHash();

        if (storedPayloadHash == null && computedHash == null) {
            return true;
        }

        if (storedPayloadHash == null || computedHash == null) {
            return false;
        }

        return storedPayloadHash.equals(computedHash);
    }

    /**
     * Validates the JWS signature from the request header
     * 
     * @param signature The JWS signature from x-jws-signature header
     * @param headers Request headers map
     * @param request The validate token request
     * @throws ConsentException if signature validation fails
     */
    public void validateSignature(String signature, Map<String, String> headers, ValidateTokenRequest request) throws ConsentException {
        log.debug("Validating JWS signature for consent token validation");
        
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
     * @param response The verify response to sign
     * @param headers Request headers map
     * @return JWS signature string to be added to response header
     */
    public String signResponse(VerifyResponse response, Map<String, String> headers) {
        log.debug("Signing response for consent token validation");
        
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
     * Search consents by identity using secure code authentication.
     * This method validates the X-Secure-Code header, retrieves the user session,
     * validates that the customerIdentifierValue matches the session identity,
     * and searches for consents matching the identity.
     *
     * @param secureCode The secure code from X-Secure-Code header
     * @param customerIdentifierValue The customer identifier value from query parameter
     * @return SearchResponse containing list of consents matching the identity
     * @throws ConsentException if secure code is invalid, session not found, identity mismatch, or no consents found
     */
    public SearchResponse<Consent> searchConsentByIdentity(String secureCode, String customerIdentifierValue) throws ConsentException {
        if (secureCode == null || secureCode.trim().isEmpty()) {
            log.error("X-Secure-Code header is missing or empty");
            throw new ConsentException(ErrorCodes.JCMP6001);
        }

        UserSession userSession = this.userSessionRepository.findByAccessToken(secureCode);
        if (userSession == null) {
            log.error("User session not found for the provided secure code");
            throw new ConsentException(ErrorCodes.JCMP6002);
        }

        String identity = userSession.getIdentity();
        if (identity == null || identity.trim().isEmpty()) {
            log.error("Identity not found in user session");
            throw new ConsentException(ErrorCodes.JCMP6002);
        }

        if (customerIdentifierValue == null || customerIdentifierValue.trim().isEmpty()) {
            log.error("customerIdentifiers.value query parameter is missing or empty");
            throw new ConsentException(ErrorCodes.JCMP1034);
        }

        if (!identity.equals(customerIdentifierValue.trim())) {
            log.error("Identity mismatch: session identity [{}] does not match requested customerIdentifiers.value [{}]", 
                    identity, customerIdentifierValue);
            throw new ConsentException(ErrorCodes.JCMP6003);
        }

        log.info("Searching consents for identity: {}", identity);

        Map<String, Object> searchParams = new HashMap<>();
        searchParams.put("customerIdentifiers.value", identity);

        List<Consent> mongoResponse = this.consentRepository.findConsentByParams(searchParams);

        if (org.apache.commons.lang3.ObjectUtils.isEmpty(mongoResponse)) {
            throw new ConsentException(ErrorCodes.JCMP3001);
        }

        return SearchResponse.<Consent>builder()
                .searchList(mongoResponse)
                .build();
    }
}
