package com.example.scanner.service;

import com.example.scanner.client.notification.NotificationManager;
import com.example.scanner.config.MultiTenantMongoConfig;
import com.example.scanner.config.TenantContext;
import com.example.scanner.constants.ErrorCodes;
import com.example.scanner.dto.ConsentDetail;
import com.example.scanner.dto.Preference;
import com.example.scanner.dto.SignableConsent;
import com.example.scanner.dto.request.CreateConsentRequest;
import com.example.scanner.dto.request.DashboardRequest;
import com.example.scanner.dto.request.UpdateConsentRequest;
import com.example.scanner.dto.response.*;
import com.example.scanner.entity.CookieConsent;
import com.example.scanner.entity.CookieConsentHandle;
import com.example.scanner.entity.ConsentTemplate;
import com.example.scanner.entity.ScanResultEntity;
import com.example.scanner.enums.*;
import com.example.scanner.exception.ConsentException;
import com.example.scanner.repository.ConsentHandleRepository;
import com.example.scanner.repository.ConsentRepository;
import com.example.scanner.util.ConsentUtil;
import com.example.scanner.util.InstantTypeAdapter;
import com.example.scanner.util.LocalDateTypeAdapter;
import com.example.scanner.util.TokenUtility;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ConsentService {

    private final ConsentHandleRepository consentHandleRepository;
    private final ConsentTemplateService templateService;
    private final TokenUtility tokenUtility;
    private final ConsentRepository consentRepository;
    private final MultiTenantMongoConfig mongoConfig;
    private final AuditService auditService;
    private final VaultService vaultService;
    private final ObjectMapper objectMapper;
    private final NotificationManager notificationManager;
    private static final String GENESIS_CHAIN = "0000000000000000000000000000000000000000000000000000000000000000";

    public ConsentCreateResponse createConsentByConsentHandleId(CreateConsentRequest request, String tenantId) throws Exception {
        log.info("Processing consent creation for handle: {}, tenant: {}", request.getConsentHandleId(), tenantId);

        // Validate preferences not empty
        if (request.getPreferencesStatus() == null || request.getPreferencesStatus().isEmpty()) {
            throw new ConsentException(ErrorCodes.VALIDATION_ERROR,
                    ErrorCodes.getDescription(ErrorCodes.VALIDATION_ERROR),
                    "Preferences status cannot be empty");
        }

        // Validate consent handle
        CookieConsentHandle consentHandle = validateConsentHandle(request.getConsentHandleId(), tenantId);

        // Check if consent already exists
        CookieConsent existingConsent = consentRepository.existsByTemplateIdAndTemplateVersionAndCustomerIdentifiers(
                consentHandle.getTemplateId(), consentHandle.getTemplateVersion(),
                consentHandle.getCustomerIdentifiers(), tenantId, request.getConsentHandleId());

        if (existingConsent != null) {
            log.info("Consent already exists");
            return ConsentCreateResponse.builder()
                    .consentId(existingConsent.getConsentId())
                    .consentJwtToken(existingConsent.getConsentJwtToken())
                    .message("Consent already exists!")
                    .consentExpiry(existingConsent.getEndDate())
                    .build();
        }

        // Get template
        ConsentTemplate template = getTemplate(consentHandle, tenantId);
        validateAllPreferencesPresent(template.getPreferences(), request.getPreferencesStatus());

        // Check if all preferences rejected
        boolean allNotAccepted = request.getPreferencesStatus().values().stream()
                .allMatch(status -> status == PreferenceStatus.NOTACCEPTED);

        if (allNotAccepted) {
            log.info("All preferences are NOTACCEPTED - marking handle as REJECTED and not creating consent");
            consentHandle.setStatus(ConsentHandleStatus.REJECTED);
            consentHandleRepository.save(consentHandle, tenantId);

            log.info("Updated consent handle {} status to REJECTED", consentHandle.getConsentHandleId());

            return ConsentCreateResponse.builder()
                    .consentId(null)
                    .consentJwtToken(null)
                    .message("All preferences rejected - Consent not created")
                    .consentExpiry(null)
                    .build();
        }

        auditService.logConsentCreationInitiated(tenantId, consentHandle.getBusinessId(), consentHandle.getCustomerIdentifiers().getValue());

        validateMandatoryNotRejected(template.getPreferences(), request.getPreferencesStatus());

        // Process preferences
        List<Preference> processedPreferences = processPreferencesForCreation(
                template.getPreferences(),
                request.getPreferencesStatus()
        );

        // Build consent
        CookieConsent consent = buildNewConsent(consentHandle, template, processedPreferences,
                request.getLanguagePreference());

        // Generate JWT token
        String consentToken = generateConsentToken(consent);
        consent.setConsentJwtToken(consentToken);
        consent.setStaleStatus(StaleStatus.NOT_STALE);

        SignableConsent signableConsent = toSignableConsent(consent);

        // Convert SignableConsent to JSON
        String signableJson;
        try {
            signableJson = objectMapper.writeValueAsString(signableConsent);
            log.debug("Converted SignableConsent to JSON for signing");
        } catch (Exception e) {
            throw new ConsentException(ErrorCodes.INTERNAL_ERROR,
                    ErrorCodes.getDescription(ErrorCodes.INTERNAL_ERROR),
                    "Failed to convert signable consent to JSON: " + e.getMessage());
        }

        // Convert to JSON BEFORE encryption
        String consentJsonString;
        try {
            consentJsonString = objectMapper.writeValueAsString(consent);
            log.debug("Converted CookieConsent to JSON string");
        } catch (Exception e) {
            throw new ConsentException(ErrorCodes.INTERNAL_ERROR,
                    ErrorCodes.getDescription(ErrorCodes.INTERNAL_ERROR),
                    "Failed to convert consent to JSON: " + e.getMessage());
        }

        // Compute payload hash BEFORE encryption
        String consentJsonStringHash = null;
        try {
            if (consentJsonString != null) {
                consentJsonStringHash = ConsentUtil.computeSHA256Hash(consentJsonString);
                consent.setPayloadHash(consentJsonStringHash);
            }
        } catch (Exception e) {
            log.warn("Failed to compute payload hash");
        }

        String chainedHash = ConsentUtil.computeSHA256Hash(GENESIS_CHAIN + consentJsonStringHash);
        consent.setCurrentChainHash(chainedHash);

        // Encrypt (sets encryptionTime, encryptedReferenceId, encryptedString)
        generateConsentEncryption(consent, tenantId);

        String jwsToken = generateConsentSigning(signableJson, tenantId, consent.getBusinessId());

        // Save consent to database
        consentRepository.saveToDatabase(consent, tenantId);

        auditService.logConsentCreated(tenantId, consentHandle.getBusinessId(),
                consentHandle.getCustomerIdentifiers().getValue(), consent.getConsentId());

        Map<String, Object> payload = new HashMap<>();
        payload.put("expiryDate", consent.getEndDate());
        payload.put("consentId", consent.getConsentId());
        payload.put("consentHandleId",consent.getConsentHandleId());
        payload.put("version",consent.getVersion());
        payload.put("templateId",consent.getTemplateId());

        notificationManager.initiateCookieConsentCreatedNotification(NotificationEvent.COOKIE_CONSENT_CREATED,
                tenantId,
                consent.getBusinessId() != null ? consent.getBusinessId() : null,
                consent.getCustomerIdentifiers() != null ? consent.getCustomerIdentifiers() : null,
                payload,
                consent.getLanguagePreferences() != null ? consent.getLanguagePreferences() : LANGUAGE.ENGLISH,
                consent.getConsentId()
        );

        // Mark handle as USED
        consentHandle.setStatus(ConsentHandleStatus.USED);
        consentHandle.setUpdatedAt(Instant.now());
        consentHandleRepository.save(consentHandle, tenantId);


        auditService.logConsentHandleMarkedUsed(tenantId, consentHandle.getBusinessId(), consentHandle.getConsentHandleId(),
        consentHandle.getCustomerIdentifiers().getValue());

        log.info("Successfully created consent: {}", consent.getConsentId());

        return ConsentCreateResponse.builder()
                .consentId(consent.getConsentId())
                .consentJwtToken(consentToken)
                .jwsToken(jwsToken)
                .message("Consent created successfully!")
                .consentExpiry(consent.getEndDate())
                .build();
    }

    @Transactional
    public UpdateConsentResponse updateConsent(String consentId, UpdateConsentRequest updateRequest, String tenantId)
            throws Exception {
        log.info("Processing consent update for consentId: {}, tenant: {}", consentId, tenantId);

        // Validate inputs
        validateUpdateInputs(consentId, updateRequest, tenantId);

        // Get consent handle
        CookieConsentHandle consentHandle = consentHandleRepository.getByConsentHandleId(
                updateRequest.getConsentHandleId(), tenantId);
        if (consentHandle == null) {
            throw new ConsentException(ErrorCodes.CONSENT_HANDLE_NOT_FOUND,
                    ErrorCodes.getDescription(ErrorCodes.CONSENT_HANDLE_NOT_FOUND),
                    "Consent handle not found");
        }

        // Log consent update initiated
        auditService.logConsentUpdateInitiated(tenantId, null, consentId,
                consentHandle.getCustomerIdentifiers().getValue());

        // Get active consent
        CookieConsent activeConsent = findActiveConsentOrThrow(consentId, tenantId);

        // Validate consent can be updated (customer, business, status checks)
        validateConsentCanBeUpdated(activeConsent, consentHandle);

        // Check if already REVOKED or EXPIRED - Cannot update!
        if (activeConsent.getStatus() == Status.REVOKED || activeConsent.getStatus() == Status.EXPIRED) {
            throw new ConsentException(
                    ErrorCodes.CONSENT_CANNOT_UPDATE_REVOKED,
                    ErrorCodes.getDescription(ErrorCodes.CONSENT_CANNOT_UPDATE_REVOKED),
                    "Consent is expired or revoked and cannot be updated"
            );
        }

        // Handle REVOKE Request
        if (updateRequest.getStatus() != null && updateRequest.getStatus() == Status.REVOKED) {
            return handleConsentRevocation(activeConsent, consentHandle, tenantId);
        }

        // Handle NORMAL UPDATE
        return handleNormalConsentUpdate(activeConsent, updateRequest, consentHandle, tenantId);
    }

    /**
     * Handle consent revocation - Creates new version with REVOKED status
     * Only ENCRYPTION happens, NO SIGNING
     */
    private UpdateConsentResponse handleConsentRevocation(CookieConsent activeConsent,
                                                          CookieConsentHandle consentHandle,
                                                          String tenantId) throws Exception {
        log.info("Revoking consent: {}", activeConsent.getConsentId());

        // Store original document ID and previous payload hash
        String originalId = activeConsent.getId();
        String previousPayloadHash = activeConsent.getPayloadHash();

        // Create new version by copying active consent
        CookieConsent newVersion = new CookieConsent(activeConsent);
        newVersion.setId(null);
        newVersion.setVersion(activeConsent.getVersion() + 1);
        newVersion.setStatus(Status.REVOKED);
        newVersion.setConsentStatus(VersionStatus.ACTIVE);
        newVersion.setStaleStatus(StaleStatus.NOT_STALE);
        newVersion.setCreatedAt(Instant.now());
        newVersion.setUpdatedAt(Instant.now());

        // Generate new JWT token
        String consentToken = generateConsentToken(newVersion);
        newVersion.setConsentJwtToken(consentToken);

        // Convert to JSON BEFORE encryption
        String consentJsonString;
        try {
            consentJsonString = objectMapper.writeValueAsString(newVersion);
        } catch (Exception e) {
            throw new ConsentException(ErrorCodes.INTERNAL_ERROR,
                    ErrorCodes.getDescription(ErrorCodes.INTERNAL_ERROR),
                    "Failed to convert consent to JSON: " + e.getMessage());
        }

        String newPayloadHash = null;
        try {
            if (consentJsonString != null) {
                newPayloadHash = ConsentUtil.computeSHA256Hash(consentJsonString);
            }
        } catch (Exception e) {
            log.warn("Failed to compute payload hash:");
        }

        newVersion.setPayloadHash(newPayloadHash);
        String chainedHash = null;
        if (previousPayloadHash != null && newPayloadHash != null) {
            chainedHash = ConsentUtil.computeSHA256Hash(previousPayloadHash + newPayloadHash);
        } else if (newPayloadHash != null) {
            chainedHash = newPayloadHash;
        }
        newVersion.setCurrentChainHash(chainedHash);

        // ENCRYPT
        generateConsentEncryption(newVersion, tenantId);

        // Save new version
        consentRepository.saveToDatabase(newVersion, tenantId);
        auditService.logConsentRevoked(tenantId, null, newVersion.getConsentId(), consentHandle.getCustomerIdentifiers().getValue());

        Map<String, Object> payload = new HashMap<>();
        payload.put("expiryDate", newVersion.getEndDate());
        payload.put("consentId", newVersion.getConsentId());
        payload.put("consentHandleId",newVersion.getConsentHandleId());
        payload.put("version",newVersion.getVersion());
        payload.put("templateId",newVersion.getTemplateId());

        notificationManager.initiateConsentRevokedNotification(NotificationEvent.CONSENT_REVOKED,
                tenantId,
                newVersion.getBusinessId() != null ? newVersion.getBusinessId() : null,
                newVersion.getCustomerIdentifiers() != null ? newVersion.getCustomerIdentifiers() : null,
                payload,
                newVersion.getLanguagePreferences() != null ? newVersion.getLanguagePreferences() : LANGUAGE.ENGLISH,
                newVersion.getConsentId()
        );

        auditService.logNewConsentVersionCreated(tenantId, null, newVersion.getConsentId(),
                consentHandle.getCustomerIdentifiers().getValue());

        notificationManager.initiateNewCookieConsentVersionCreatedNotification(NotificationEvent.NEW_COOKIE_CONSENT_VERSION_CREATED,
                tenantId,
                newVersion.getBusinessId() != null ? newVersion.getBusinessId() : null,
                newVersion.getCustomerIdentifiers() != null ? newVersion.getCustomerIdentifiers() : null,
                payload,
                newVersion.getLanguagePreferences() != null ? newVersion.getLanguagePreferences() : LANGUAGE.ENGLISH,
                newVersion.getConsentId()
        );

        // Mark old version as STALE and UPDATED
        if (originalId != null) {
            Optional<CookieConsent> optionalConsent = consentRepository.findById(originalId, tenantId);
            if (optionalConsent.isPresent()) {
                CookieConsent originalConsent = optionalConsent.get();
                originalConsent.setStaleStatus(StaleStatus.STALE);
                originalConsent.setConsentStatus(VersionStatus.UPDATED);
                originalConsent.setUpdatedAt(Instant.now());
                consentRepository.saveToDatabase(originalConsent, tenantId);
                auditService.logOldConsentVersionMarkedUpdated(tenantId, null, originalConsent.getConsentId(), consentHandle.getCustomerIdentifiers().getValue());
            }
        }

        // Mark handle as USED
        consentHandle.setStatus(ConsentHandleStatus.USED);
        consentHandle.setUpdatedAt(Instant.now());
        consentHandleRepository.save(consentHandle, tenantId);

        auditService.logConsentHandleMarkedUsedAfterUpdate(tenantId, null, consentHandle.getConsentHandleId(),
                consentHandle.getCustomerIdentifiers().getValue());

        log.info("Successfully revoked consent: {}", activeConsent.getConsentId());

        return UpdateConsentResponse.builder()
                .message("Consent revoked successfully")
                .build();
    }

    /**
     * Handle normal consent update - Updates preferences and creates new version
     * ENCRYPTION + SIGNING with SignableConsent
     */
    private UpdateConsentResponse handleNormalConsentUpdate(CookieConsent activeConsent,
                                                            UpdateConsentRequest updateRequest,
                                                            CookieConsentHandle consentHandle,
                                                            String tenantId) throws Exception {
        log.info("Processing normal consent update for consentId: {}", activeConsent.getConsentId());

        // Store original document ID and previous payload hash
        String originalId = activeConsent.getId();
        String previousPayloadHash = activeConsent.getPayloadHash();

        // Get template to validate preferences
        ConsentTemplate template = getTemplate(consentHandle, tenantId);

        // Validate preferences if provided
        if (updateRequest.getPreferencesStatus() != null && !updateRequest.getPreferencesStatus().isEmpty()) {
            validateAllPreferencesPresent(template.getPreferences(), updateRequest.getPreferencesStatus());

            // Check if all preferences rejected
            boolean allNotAccepted = updateRequest.getPreferencesStatus().values().stream()
                    .allMatch(status -> status == PreferenceStatus.NOTACCEPTED);

            if (allNotAccepted) {
                log.info("All preferences NOTACCEPTED in update - marking handle as REJECTED");
                consentHandle.setStatus(ConsentHandleStatus.REJECTED);
                consentHandleRepository.save(consentHandle, tenantId);

                throw new ConsentException(
                        ErrorCodes.VALIDATION_ERROR,
                        ErrorCodes.getDescription(ErrorCodes.VALIDATION_ERROR),
                        "Cannot update consent - all preferences rejected"
                );
            }

            validateMandatoryNotRejected(template.getPreferences(), updateRequest.getPreferencesStatus());
        }

        // Create new version by copying active consent
        CookieConsent newVersion = new CookieConsent(activeConsent);
        newVersion.setId(null);
        newVersion.setVersion(activeConsent.getVersion() + 1);
        newVersion.setConsentStatus(VersionStatus.ACTIVE);
        newVersion.setStaleStatus(StaleStatus.NOT_STALE);
        newVersion.setConsentHandleId(consentHandle.getConsentHandleId());
        newVersion.setTemplateVersion(consentHandle.getTemplateVersion());
        newVersion.setCreatedAt(Instant.now());
        newVersion.setUpdatedAt(Instant.now());

        // Apply updates from request
        if (updateRequest.getLanguagePreference() != null) {
            newVersion.setLanguagePreferences(updateRequest.getLanguagePreference());
        }

        if (updateRequest.getPreferencesStatus() != null && !updateRequest.getPreferencesStatus().isEmpty()) {
            List<Preference> updatedPreferences = processPreferencesForUpdate(
                    template.getPreferences(),
                    updateRequest.getPreferencesStatus()
            );
            newVersion.setPreferences(updatedPreferences);
            newVersion.setEndDate(calculateConsentExpiry(updatedPreferences));
            newVersion.setStatus(determineConsentStatus(updatedPreferences));
        }

        // Generate new JWT token
        String consentToken = generateConsentToken(newVersion);
        newVersion.setConsentJwtToken(consentToken);
        newVersion.setStaleStatus(StaleStatus.NOT_STALE);

        SignableConsent signableConsent = toSignableConsent(newVersion);

        // Convert SignableConsent to JSON for signing
        String signableJson;
        try {
            signableJson = objectMapper.writeValueAsString(signableConsent);
        } catch (Exception e) {
            throw new ConsentException(ErrorCodes.INTERNAL_ERROR,
                    ErrorCodes.getDescription(ErrorCodes.INTERNAL_ERROR),
                    "Failed to convert signable consent to JSON: " + e.getMessage());
        }

        String newPayloadHash = null;
        try {
            if (signableJson != null) {
                newPayloadHash = ConsentUtil.computeSHA256Hash(signableJson);
            }
        } catch (Exception e) {
            log.warn("Failed to compute payload hash. Exception occurred");
        }

        newVersion.setPayloadHash(newPayloadHash);

        // CHAIN HASH: Combine previous hash + new hash
        String chainedHash = null;
        if (previousPayloadHash != null && newPayloadHash != null) {
            chainedHash = ConsentUtil.computeSHA256Hash(previousPayloadHash + newPayloadHash);
        } else if (newPayloadHash != null) {
            chainedHash = newPayloadHash;
        }
        newVersion.setCurrentChainHash(chainedHash);

        // ENCRYPT (sets encryptionTime, encryptedReferenceId, encryptedString)
        generateConsentEncryption(newVersion, tenantId);

        // SIGN using SignableConsent JSON (without encryption fields)
        String jwsToken = generateConsentSigning(signableJson, tenantId, newVersion.getBusinessId());

        // Save new version
        consentRepository.saveToDatabase(newVersion, tenantId);
        auditService.logNewConsentVersionCreated(tenantId, null, newVersion.getConsentId()
        ,consentHandle.getCustomerIdentifiers().getValue());

        Map<String, Object> payload = new HashMap<>();
        payload.put("expiryDate", newVersion.getEndDate());
        payload.put("consentId", newVersion.getConsentId());
        payload.put("consentHandleId",newVersion.getConsentHandleId());
        payload.put("version",newVersion.getVersion());
        payload.put("templateId",newVersion.getTemplateId());

        notificationManager.initiateNewCookieConsentVersionCreatedNotification(NotificationEvent.NEW_COOKIE_CONSENT_VERSION_CREATED,
                tenantId,
                newVersion.getBusinessId() != null ? newVersion.getBusinessId() : null,
                newVersion.getCustomerIdentifiers() != null ? newVersion.getCustomerIdentifiers() : null,
                payload,
                newVersion.getLanguagePreferences() != null ? newVersion.getLanguagePreferences() : LANGUAGE.ENGLISH,
                newVersion.getConsentId()
        );


        // Mark old version as STALE and UPDATED
        if (originalId != null) {
            Optional<CookieConsent> optionalConsent = consentRepository.findById(originalId, tenantId);
            if (optionalConsent.isPresent()) {
                CookieConsent originalConsent = optionalConsent.get();
                originalConsent.setStaleStatus(StaleStatus.STALE);
                originalConsent.setConsentStatus(VersionStatus.UPDATED);
                originalConsent.setUpdatedAt(Instant.now());
                consentRepository.saveToDatabase(originalConsent, tenantId);
                auditService.logOldConsentVersionMarkedUpdated(tenantId, null, originalConsent.getConsentId(), consentHandle.getCustomerIdentifiers().getValue());
            }
        }

        log.info("Successfully updated consent: {} to version: {}", newVersion.getConsentId(), newVersion.getVersion());

        return UpdateConsentResponse.success(
                newVersion.getConsentId(),
                newVersion.getId(),
                newVersion.getVersion(),
                activeConsent.getVersion(),
                consentToken,
                newVersion.getEndDate(),
                jwsToken
        );
    }

    public List<CookieConsent> getConsentHistory(String consentId, String tenantId) throws Exception {
        validateBasicInputs(consentId, tenantId);

        List<CookieConsent> history = consentRepository.findAllVersionsByConsentId(consentId, tenantId);

        if (history.isEmpty()) {
            throw new ConsentException(ErrorCodes.CONSENT_NOT_FOUND,
                    ErrorCodes.getDescription(ErrorCodes.CONSENT_NOT_FOUND),
                    "No consent versions found for consentId: " + consentId);
        }

        log.info("Retrieved versions consent");
        return history;
    }

    public Optional<CookieConsent> getConsentByIdAndVersion(String tenantId, String consentId, Integer version)
            throws Exception {
        validateBasicInputs(consentId, tenantId);

        if (version == null || version <= 0) {
            throw new ConsentException(ErrorCodes.VERSION_NUMBER_INVALID,
                    ErrorCodes.getDescription(ErrorCodes.VERSION_NUMBER_INVALID),
                    "Version must be positive, got: " + version);
        }

        return consentRepository.findByConsentIdAndVersion(consentId, version, tenantId);
    }

    public ConsentTokenValidateResponse validateConsentToken(String consentToken, String jwsToken,
                                                             String tenantId, String businessId) throws Exception {

        String tokenId = consentToken.substring(0, Math.min(20, consentToken.length()));
        SignableConsent signableConsent;
        if (jwsToken != null && !jwsToken.trim().isEmpty()) {
            try {
                log.info("JWS token provided, verifying before consent token validation");
                signableConsent = verifyJwsToken(jwsToken, tenantId, businessId);
                log.info("JWS token verification successful");
            } catch (ConsentException e) {
                log.error("JWS verification failed");
                auditService.logTokenValidationFailed(tenantId, businessId, tokenId);

                return ConsentTokenValidateResponse.builder()
                        .message(e.getUserMessage())
                        .build();
            }
        } else {
            log.warn("JWS token not provided in request header");
            throw new ConsentException(
                    ErrorCodes.CONSENT_JWS_NOT_FOUND,
                    ErrorCodes.getDescription(ErrorCodes.CONSENT_JWS_NOT_FOUND),
                    "Add x-jws-signature header."
            );
        }


        auditService.logTokenVerificationInitiated(tenantId, businessId, tokenId);

        try {
            ConsentTokenValidateResponse response = tokenUtility.verifyConsentToken(consentToken);

            auditService.logTokenSignatureVerified(tenantId, businessId, tokenId);
            auditService.logTokenValidationSuccess(tenantId, businessId, tokenId);

            Map<String, Object> payload = new HashMap<>();
            payload.put("expiryDate", signableConsent.getEndDate());
            payload.put("consentId", signableConsent.getConsentId());
            payload.put("consentHandleId",signableConsent.getConsentHandleId());
            payload.put("version",signableConsent.getVersion());
            payload.put("templateId",signableConsent.getTemplateId());

            notificationManager.initiateTokenValidationSuccessNotification(NotificationEvent.TOKEN_VALIDATION_SUCCESS,
                    tenantId,
                    businessId,
                    signableConsent.getCustomerIdentifiers() != null ? signableConsent.getCustomerIdentifiers() : null,
                    payload,
                    signableConsent.getLanguagePreferences() != null ? signableConsent.getLanguagePreferences() : LANGUAGE.ENGLISH,
                    signableConsent.getConsentHandleId()
            );

            return response;

        } catch (Exception e) {
            auditService.logTokenValidationFailed(tenantId, businessId, tokenId);
            throw e;
        }
    }

    // ============================================
    // VALIDATION & PROCESSING METHODS
    // ============================================

    /**
     * REQUIREMENT 3: Validate ALL template preferences are present in request
     * This is the FIRST validation that should run
     */
    private void validateAllPreferencesPresent(List<Preference> templatePreferences,
                                               Map<String, PreferenceStatus> userChoices) throws ConsentException {

        Set<String> templatePurposes = templatePreferences.stream()
                .map(Preference::getPurpose)
                .collect(Collectors.toSet());

        Set<String> missingPreferences = templatePurposes.stream()
                .filter(category -> !userChoices.containsKey(category))
                .collect(Collectors.toSet());

        if (!missingPreferences.isEmpty()) {
            log.error("Request must contain all template preferences");
            throw new ConsentException(
                    ErrorCodes.INCOMPLETE_PREFERENCES,
                    ErrorCodes.getDescription(ErrorCodes.INCOMPLETE_PREFERENCES),
                    "All template preferences must be provided in request. Missing: " + missingPreferences +
                            ". Template expects " + templatePurposes.size() + " preferences, but received " + userChoices.size()
            );
        }

        Set<String> invalidPurposes = userChoices.keySet().stream()
                .filter(category -> !templatePurposes.contains(category))
                .collect(Collectors.toSet());

        if (!invalidPurposes.isEmpty()) {
            log.error("Invalid purposes provided.");
            throw new ConsentException(
                    ErrorCodes.VALIDATION_ERROR,
                    ErrorCodes.getDescription(ErrorCodes.VALIDATION_ERROR),
                    "Invalid purposes: " + invalidPurposes + ". These do not exist in template."
            );
        }
    }

    /**
     * REQUIREMENT 2: Validate mandatory preferences (isMandatory=true) cannot be NOTACCEPTED
     * This runs AFTER we know all preferences are present, and it's NOT a reject-all case
     */
    private void validateMandatoryNotRejected(List<Preference> templatePreferences,
                                              Map<String, PreferenceStatus> userChoices) throws ConsentException {

        Map<String, Boolean> mandatoryMap = templatePreferences.stream()
                .filter(p -> Boolean.TRUE.equals(p.getIsMandatory()))
                .collect(Collectors.toMap(Preference::getPurpose, Preference::getIsMandatory));

        List<String> rejectedMandatory = userChoices.entrySet().stream()
                .filter(entry -> mandatoryMap.containsKey(entry.getKey()))
                .filter(entry -> entry.getValue() == PreferenceStatus.NOTACCEPTED)
                .map(Map.Entry::getKey)
                .toList();

        if (!rejectedMandatory.isEmpty()) {
            log.error("User attempted to reject mandatory preferences");
            throw new ConsentException(
                    ErrorCodes.MANDATORY_PREFERENCE_REJECTED,
                    ErrorCodes.getDescription(ErrorCodes.MANDATORY_PREFERENCE_REJECTED),
                    "Mandatory preferences cannot be rejected: " + rejectedMandatory + ". These preferences are required for the service."
            );
        }
    }

    /**
     * ✅ Process preferences for CREATION
     * Returns ONLY the preferences user provided (after validation)
     */
    private List<Preference> processPreferencesForCreation(
            List<Preference> templatePreferences,
            Map<String, PreferenceStatus> userChoices) {

        LocalDateTime now = LocalDateTime.now();
        List<Preference> processed = new ArrayList<>();

        for (Preference pref : templatePreferences) {
            if (userChoices.containsKey(pref.getPurpose())) {
                pref.setPreferenceStatus(userChoices.get(pref.getPurpose()));
                pref.setStartDate(now);
                pref.setEndDate(calculatePreferenceEndDate(now, pref.getPreferenceValidity()));
                processed.add(pref);
            }
        }

        return processed;
    }

    /**
     * ✅ Process preferences for UPDATE
     * Returns ALL template preferences, updating only the ones user provided
     */
    private List<Preference> processPreferencesForUpdate(
            List<Preference> templatePreferences,
            Map<String, PreferenceStatus> userChoices) {

        LocalDateTime now = LocalDateTime.now();

        return templatePreferences.stream()
                .map(templatePref -> {
                    Preference pref = new Preference();
                    pref.setPurpose(templatePref.getPurpose());
                    pref.setIsMandatory(templatePref.getIsMandatory());
                    pref.setPreferenceValidity(templatePref.getPreferenceValidity());

                    // Set user's choice
                    if (userChoices.containsKey(pref.getPurpose())) {
                        pref.setPreferenceStatus(userChoices.get(pref.getPurpose()));
                        pref.setStartDate(now);
                        pref.setEndDate(calculatePreferenceEndDate(now, pref.getPreferenceValidity()));
                    }

                    return pref;
                })
                .collect(Collectors.toList());
    }

    /**
     * REQUIREMENT 4: Validate consent handle with explicit PENDING status check
     */
    private CookieConsentHandle validateConsentHandle(String consentHandleId, String tenantId) throws Exception {
        CookieConsentHandle handle = consentHandleRepository.getByConsentHandleId(consentHandleId, tenantId);

        if (handle == null) {
            throw new ConsentException(ErrorCodes.CONSENT_HANDLE_NOT_FOUND,
                    ErrorCodes.getDescription(ErrorCodes.CONSENT_HANDLE_NOT_FOUND),
                    "Consent handle not found: " + consentHandleId);
        }

        // REQUIREMENT 4: Explicit check for PENDING status (must be checked before expiry)
        if (handle.getStatus() != ConsentHandleStatus.PENDING) {
            log.error("Consent handle is not in PENDING status. Current status");
            throw new ConsentException(ErrorCodes.CONSENT_HANDLE_ALREADY_USED,
                    ErrorCodes.getDescription(ErrorCodes.CONSENT_HANDLE_ALREADY_USED),
                    "Consent handle is already used. Current status: " + handle.getStatus());
        }

        // Check if expired (15 minutes check)
        if (handle.isExpired()) {
            throw new ConsentException(ErrorCodes.CONSENT_HANDLE_EXPIRED,
                    ErrorCodes.getDescription(ErrorCodes.CONSENT_HANDLE_EXPIRED),
                    "Consent handle expired: " + consentHandleId);
        }

        return handle;
    }

    private void validateUpdateInputs(String consentId, UpdateConsentRequest request, String tenantId)
            throws ConsentException {
        validateBasicInputs(consentId, tenantId);

        if (request == null || !request.hasUpdates()) {
            throw new ConsentException(ErrorCodes.VALIDATION_ERROR,
                    ErrorCodes.getDescription(ErrorCodes.VALIDATION_ERROR),
                    "Update request must contain at least one update");
        }

        if (request.getConsentHandleId() == null || request.getConsentHandleId().trim().isEmpty()) {
            throw new ConsentException(ErrorCodes.VALIDATION_ERROR,
                    ErrorCodes.getDescription(ErrorCodes.VALIDATION_ERROR),
                    "Consent handle ID required for update");
        }
    }

    private void validateConsentCanBeUpdated(CookieConsent consent, CookieConsentHandle handle) throws ConsentException {
        if (!consent.getCustomerIdentifiers().getValue().equals(handle.getCustomerIdentifiers().getValue())) {
            throw new ConsentException(ErrorCodes.CONSENT_HANDLE_CUSTOMER_MISMATCH,
                    ErrorCodes.getDescription(ErrorCodes.CONSENT_HANDLE_CUSTOMER_MISMATCH),
                    "Customer mismatch between consent and handle");
        }

        if (!consent.getBusinessId().equals(handle.getBusinessId())) {
            throw new ConsentException(ErrorCodes.CONSENT_HANDLE_BUSINESS_MISMATCH,
                    ErrorCodes.getDescription(ErrorCodes.CONSENT_HANDLE_BUSINESS_MISMATCH),
                    "Business mismatch between consent and handle");
        }

        if (consent.getConsentStatus() != VersionStatus.ACTIVE) {
            throw new ConsentException(ErrorCodes.BUSINESS_RULE_VIOLATION,
                    ErrorCodes.getDescription(ErrorCodes.BUSINESS_RULE_VIOLATION),
                    "Only active consents can be updated");
        }

        if (consent.getStatus() == Status.EXPIRED) {
            throw new ConsentException(ErrorCodes.CONSENT_CANNOT_UPDATE_EXPIRED,
                    ErrorCodes.getDescription(ErrorCodes.CONSENT_CANNOT_UPDATE_EXPIRED),
                    "Cannot update expired consent");
        }
    }

    private void validateBasicInputs(String id, String tenantId) throws ConsentException {
        if (tenantId == null || tenantId.trim().isEmpty()) {
            throw new ConsentException(ErrorCodes.VALIDATION_ERROR,
                    ErrorCodes.getDescription(ErrorCodes.VALIDATION_ERROR),
                    "Tenant ID cannot be null or empty");
        }

        if (id == null || id.trim().isEmpty()) {
            throw new ConsentException(ErrorCodes.VALIDATION_ERROR,
                    ErrorCodes.getDescription(ErrorCodes.VALIDATION_ERROR),
                    "Consent ID cannot be null or empty");
        }
    }

    // ============================================
    // HELPER METHODS
    // ============================================

    private ConsentTemplate getTemplate(CookieConsentHandle handle, String tenantId) throws Exception {
        Optional<ConsentTemplate> templateOpt = templateService.getTemplateByIdAndVersion(
                tenantId, handle.getTemplateId(), handle.getTemplateVersion());

        if (templateOpt.isEmpty()) {
            throw new ConsentException(ErrorCodes.TEMPLATE_NOT_FOUND,
                    ErrorCodes.getDescription(ErrorCodes.TEMPLATE_NOT_FOUND),
                    "Template not found: " + handle.getTemplateId());
        }

        return templateOpt.get();
    }

    private CookieConsent findActiveConsentOrThrow(String consentId, String tenantId) throws ConsentException {
        CookieConsent consent = consentRepository.findActiveByConsentId(consentId, tenantId);

        if (consent == null) {
            throw new ConsentException(ErrorCodes.CONSENT_NOT_FOUND,
                    ErrorCodes.getDescription(ErrorCodes.CONSENT_NOT_FOUND),
                    "Active consent not found: " + consentId);
        }

        return consent;
    }

    private CookieConsent buildNewConsent(CookieConsentHandle handle, ConsentTemplate template,
                                          List<Preference> preferences, LANGUAGE languagePreference) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiry = calculateConsentExpiry(preferences);
        Status status = determineConsentStatus(preferences);

        return CookieConsent.builder()
                .consentId(UUID.randomUUID().toString())
                .consentHandleId(handle.getConsentHandleId())
                .businessId(handle.getBusinessId())
                .templateId(handle.getTemplateId())
                .templateVersion(handle.getTemplateVersion())
                .languagePreferences(languagePreference)
                .multilingual(template.getMultilingual())
                .customerIdentifiers(handle.getCustomerIdentifiers())
                .preferences(preferences)
                .status(status)
                .consentStatus(VersionStatus.ACTIVE)
                .version(1)
                .startDate(now)
                .endDate(expiry)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    private LocalDateTime calculatePreferenceEndDate(LocalDateTime start,
                                                     com.example.scanner.dto.Duration validity) {
        if (validity == null) {
            return start.plusYears(1);
        }

        return switch (validity.getUnit()) {
            case YEARS -> start.plusYears(validity.getValue());
            case MONTHS -> start.plusMonths(validity.getValue());
            case DAYS -> start.plusDays(validity.getValue());
        };
    }

    private LocalDateTime calculateConsentExpiry(List<Preference> preferences) {
        return preferences.stream()
                .filter(pref -> pref.getPreferenceStatus() == PreferenceStatus.ACCEPTED)
                .map(Preference::getEndDate)
                .filter(Objects::nonNull)
                .min(LocalDateTime::compareTo)
                .orElse(LocalDateTime.now().plusYears(1));
    }

    private Status determineConsentStatus(List<Preference> preferences) {
        // Check if all preferences have expired
        boolean allExpired = preferences.stream()
                .allMatch(p -> p.getPreferenceStatus() == PreferenceStatus.EXPIRED);

        if (allExpired) {
            return Status.EXPIRED;
        }

        // If consent exists, at least one preference was accepted
        // (All-reject scenario doesn't create consent)
        // Therefore, default status is ACTIVE
        return Status.ACTIVE;
    }

    private String generateConsentToken(CookieConsent consent) throws Exception {
        Gson gson = new GsonBuilder()
                .registerTypeAdapter(LocalDateTime.class, new LocalDateTypeAdapter())
                .registerTypeAdapter(Instant.class, new InstantTypeAdapter())
                .create();

        String json = gson.toJson(consent);
        Date expiry = Date.from(consent.getEndDate().atZone(ZoneId.systemDefault()).toInstant());

        return tokenUtility.generateToken(json, expiry);
    }

    //-----------------------------------Get DASHBOARD data-------------------------------

    public List<DashboardTemplateResponse> getDashboardDataGroupedByTemplate(
            String tenantId, DashboardRequest request) throws ConsentException {

        log.info("Processing dashboard request");

        TenantContext.setCurrentTenant(tenantId);
        MongoTemplate mongoTemplate = mongoConfig.getMongoTemplateForTenant(tenantId);

        try {
            List<ConsentTemplate> templates;

            if (StringUtils.hasText(request.getTemplateID())) {
                Query templateQuery = new Query(
                        Criteria.where("templateId").is(request.getTemplateID())
                );

                if (request.getVersion() != null) {
                    templateQuery.addCriteria(
                            Criteria.where("version").is(request.getVersion())
                    );
                }

                if (StringUtils.hasText(request.getBusinessId())) {
                    templateQuery.addCriteria(
                            Criteria.where("businessId").is(request.getBusinessId())
                    );
                }

                templates = mongoTemplate.find(templateQuery, ConsentTemplate.class);

            } else {
                Query allTemplatesQuery = new Query();
                if (StringUtils.hasText(request.getBusinessId())) {
                    allTemplatesQuery.addCriteria(
                            Criteria.where("businessId").is(request.getBusinessId())
                    );
                }
                templates = mongoTemplate.find(allTemplatesQuery, ConsentTemplate.class);
            }

            // For each template, fetch consents and build response
            List<DashboardTemplateResponse> responses = new ArrayList<>();

            for (ConsentTemplate template : templates) {
                // Apply scanId filter if provided
                if (StringUtils.hasText(request.getScanID()) &&
                        !request.getScanID().equals(template.getScanId())) {
                    continue;
                }

                // Fetch scan results for this template
                ScanResultEntity scanResult = null;
                String scannedSite = null;
                List<String> subDomains = null;

                if (template.getScanId() != null) {
                    Query scanQuery = new Query(
                            Criteria.where("transactionId").is(template.getScanId())
                    );
                    scanResult = mongoTemplate.findOne(scanQuery, ScanResultEntity.class);

                    if (scanResult != null) {
                        scannedSite = scanResult.getUrl();
                        if (scanResult.getCookiesBySubdomain() != null) {
                            subDomains = scanResult.getCookiesBySubdomain().keySet().stream().toList();
                        }
                    }
                }

                // ========== STEP 1: Fetch Consents (Existing Logic) ==========
                Criteria consentCriteria = Criteria.where("templateId").is(template.getTemplateId())
                        .and("templateVersion").is(template.getVersion());

                if (StringUtils.hasText(request.getBusinessId())) {
                    consentCriteria = consentCriteria.and("businessId").is(request.getBusinessId());
                }

                if (request.getStartDate() != null) {
                    consentCriteria.and("startDate").gte(request.getStartDate());
                }

                if (request.getEndDate() != null) {
                    consentCriteria.and("endDate").lte(request.getEndDate());
                }

                Query consentQuery = new Query(consentCriteria);
                consentQuery.with(Sort.by(Sort.Direction.DESC, "createdAt"));

                List<CookieConsent> consents = mongoTemplate.find(consentQuery, CookieConsent.class);

                // Map consents to ConsentDetail objects
                List<ConsentDetail> consentDetails = consents.stream()
                        .map(consent -> mapToConsentDetail(consent, template, mongoTemplate))
                        .filter(Objects::nonNull).toList();

                // ========== STEP 2: NEW - Fetch Handles WITHOUT Consents ==========
                // Get all consent handles for this template that are REJECTED, EXPIRED, or PENDING
                Criteria handleCriteria = Criteria.where("templateId").is(template.getTemplateId())
                        .and("templateVersion").is(template.getVersion())
                        .and("status").in(
                                ConsentHandleStatus.REJECTED,
                                ConsentHandleStatus.REQ_EXPIRED,
                                ConsentHandleStatus.PENDING
                        );

                if (StringUtils.hasText(request.getBusinessId())) {
                    handleCriteria = handleCriteria.and("businessId").is(request.getBusinessId());
                }

                if (request.getStartDate() != null && request.getEndDate() != null) {
                    handleCriteria.and("createdAt")
                            .gte(request.getStartDate().atZone(ZoneId.systemDefault()).toInstant())
                            .lte(request.getEndDate().atZone(ZoneId.systemDefault()).toInstant());
                } else if (request.getStartDate() != null) {
                    handleCriteria.and("createdAt")
                            .gte(request.getStartDate().atZone(ZoneId.systemDefault()).toInstant());
                } else if (request.getEndDate() != null) {
                    handleCriteria.and("createdAt")
                            .lte(request.getEndDate().atZone(ZoneId.systemDefault()).toInstant());
                }

                Query handleQuery = new Query(handleCriteria);
                handleQuery.with(Sort.by(Sort.Direction.DESC, "createdAt"));

                List<CookieConsentHandle> orphanHandles = mongoTemplate.find(handleQuery, CookieConsentHandle.class);

                // Map orphan handles to ConsentDetail objects
                List<ConsentDetail> handleDetails = orphanHandles.stream()
                        .map(handle -> mapHandleToConsentDetail(handle, template))
                        .filter(Objects::nonNull).toList();

                // ========== STEP 3: Combine Both Lists ==========
                List<ConsentDetail> allDetails = new ArrayList<>();
                allDetails.addAll(consentDetails);     // Consents with USED handles
                allDetails.addAll(handleDetails);      // Orphan handles (REJECTED/EXPIRED/PENDING)

                // Sort by creation time (most recent first)
                allDetails.sort((a, b) -> {
                    // Get creation time from consent or handle
                    // Since we don't have createdAt in ConsentDetail, sort by consentVersion desc
                    // Handles will have null version, so they'll come after
                    if (a.getConsentVersion() == null && b.getConsentVersion() == null) return 0;
                    if (a.getConsentVersion() == null) return 1;
                    if (b.getConsentVersion() == null) return -1;
                    return b.getConsentVersion().compareTo(a.getConsentVersion());
                });

                // Build template response
                DashboardTemplateResponse templateResponse = DashboardTemplateResponse.builder()
                        .templateId(template.getTemplateId())
                        .status(template.getTemplateStatus())
                        .scannedSites(scannedSite)
                        .scannedSubDomains(subDomains)
                        .scanId(template.getScanId())
                        .consents(allDetails)  // Contains both consents AND orphan handles
                        .build();

                responses.add(templateResponse);
            }

            return responses;

        } catch (Exception e) {
            log.error("Error fetching dashboard data");
            throw new ConsentException(
                    ErrorCodes.INTERNAL_ERROR,
                    "Failed to fetch dashboard data",
                    e.getMessage()
            );
        } finally {
            TenantContext.clear();
        }
    }

    private ConsentDetail mapHandleToConsentDetail(CookieConsentHandle handle, ConsentTemplate template) {
        try {
            // Get all template preference names
            List<String> templatePreferences = template.getPreferences().stream()
                    .map(Preference::getPurpose)
                    .collect(Collectors.toList());

            return ConsentDetail.builder()
                    .consentID(null)  // No consent created
                    .consentHandle(handle.getConsentHandleId())
                    .templateVersion(handle.getTemplateVersion())
                    .consentVersion(null)  // No consent version
                    .templatePreferences(templatePreferences)
                    .userSelectedPreference(Collections.emptyList())  // User didn't select anything OR rejected all
                    .consentStatus(null)  // No consent = no consent status
                    .consentHandleStatus(handle.getStatus().toString())  // REJECTED/EXPIRED/PENDING
                    .customerIdentifier(handle.getCustomerIdentifiers())
                    .build();

        } catch (Exception e) {
            log.error("Error mapping handle to detail");
            return null;
        }
    }

    private ConsentDetail mapToConsentDetail(CookieConsent consent, ConsentTemplate template,
                                             MongoTemplate mongoTemplate) {
        try {
            // Fetch consent handle
            Query handleQuery = new Query(
                    Criteria.where("consentHandleId").is(consent.getConsentHandleId())
            );
            CookieConsentHandle handle = mongoTemplate.findOne(handleQuery, CookieConsentHandle.class);

            // Get all template preference names
            List<String> templatePreferences = template.getPreferences().stream()
                    .map(Preference::getPurpose)
                    .collect(Collectors.toList());

            List<String> userAccepted = consent.getPreferences().stream()
                    .filter(pref -> pref.getPreferenceStatus() == PreferenceStatus.ACCEPTED)
                    .map(Preference::getPurpose)
                    .collect(Collectors.toList());

            Instant lastUpdated = consent.getUpdatedAt() != null &&
                    consent.getCreatedAt() != null &&
                    consent.getUpdatedAt().isAfter(consent.getCreatedAt())
                    ? consent.getUpdatedAt()
                    : consent.getCreatedAt();

            return ConsentDetail.builder()
                    .consentID(consent.getConsentId())
                    .consentHandle(consent.getConsentHandleId())
                    .templateVersion(consent.getTemplateVersion())
                    .consentVersion(consent.getVersion())
                    .templatePreferences(templatePreferences)
                    .userSelectedPreference(userAccepted)
                    .consentStatus(consent.getStatus().toString())
                    .consentHandleStatus(handle.getStatus().toString())
                    .customerIdentifier(consent.getCustomerIdentifiers())
                    .lastUpdated(lastUpdated)
                    .build();

        } catch (Exception e) {
            log.error("Error mapping consent");
            return null;
        }
    }

    public CheckConsentResponse getConsentStatus(String deviceId, String url, String consentId, String tenantId){
        if (deviceId == null || deviceId.trim().isEmpty()) {
            throw new IllegalArgumentException("Device ID is required");
        }
        if (url == null || url.trim().isEmpty()) {
            throw new IllegalArgumentException("URL is required");
        }
        TenantContext.setCurrentTenant(tenantId);

        try {
            MongoTemplate mongoTemplate = mongoConfig.getMongoTemplateForTenant(TenantContext.getCurrentTenant());
            if (consentId != null && !consentId.isEmpty()) {
                Query handleQuery = new Query(
                        Criteria.where("consentId").is(consentId)
                ).with(Sort.by(Sort.Direction.DESC, "version")).limit(1);

                CookieConsent consent = mongoTemplate.findOne(handleQuery, CookieConsent.class);

                if (consent == null) {
                    return CheckConsentResponse.builder()
                            .consentStatus("No_Record")
                            .consentHandleId("No_Record")
                            .build();
                }

                return CheckConsentResponse.builder()
                        .consentStatus(consent.getStatus().toString())
                        .consentHandleId(consent.getConsentHandleId())
                        .build();
            }
            // Case 2: DeviceId and URL provided - find consent handle first
            Query handleQuery = new Query();
            handleQuery.addCriteria(Criteria.where("customerIdentifiers.value").is(deviceId));
            handleQuery.addCriteria(Criteria.where("url").is(url));
            handleQuery.with(Sort.by(Sort.Direction.DESC, "createdAt")); // Latest first
            handleQuery.limit(1);

            CookieConsentHandle latestHandle = mongoTemplate.findOne(handleQuery, CookieConsentHandle.class);

            if (latestHandle == null) {
                return CheckConsentResponse.builder()
                        .consentStatus("No_Record")
                        .consentHandleId("No_Record")
                        .build();
            }

            if(latestHandle.getStatus().equals(ConsentHandleStatus.USED)){
                Query consentQuery = new Query(Criteria.where("consentHandleId").is(latestHandle.getConsentHandleId()))
                        .with(Sort.by(Sort.Direction.DESC, "version")).limit(1);
                CookieConsent consent = mongoTemplate.findOne(consentQuery, CookieConsent.class);

                if (consent != null) {
                    return CheckConsentResponse.builder()
                            .consentStatus(consent.getStatus().toString())
                            .consentHandleId(latestHandle.getConsentHandleId())
                            .build();
                }
            }

                return CheckConsentResponse.builder()
                        .consentStatus(latestHandle.getStatus().toString())
                        .consentHandleId(latestHandle.getConsentHandleId())
                        .build();

        } catch (IllegalArgumentException e) {
            log.error("Invalid input parameters");
            throw e;
        } catch (Exception e) {
            log.error("Failed to retrieve consent status");
            throw new RuntimeException("Failed to retrieve consent status", e);
        }finally {
            TenantContext.clear();
        }
    }

    private SignableConsent verifyJwsToken(String jwsToken, String tenantId, String businessId) throws ConsentException {
        log.info("Starting JWS token verification");

        // Step 1: Verify token with vault service
        VaultVerifyResponse verifyResponse = verifyTokenWithVault(jwsToken, tenantId, businessId);

        // Step 2: Validate response
        validateVaultResponse(verifyResponse);

        // Step 3: Extract SignableConsent from JWS payload
        SignableConsent jwsConsent = extractSignableConsentFromPayload(verifyResponse.getPayload());

        // Step 4: Fetch consent from database
        CookieConsent dbConsent = fetchConsentFromDatabase(
                jwsConsent.getConsentId(),
                jwsConsent.getVersion(),
                tenantId
        );

        // Step 5: Convert DB consent to SignableConsent
        SignableConsent dbSignableConsent = toSignableConsent(dbConsent);

        // Step 6: Compare SignableConsent objects (NO encryption fields)
        return compareSignableConsents(jwsConsent, dbSignableConsent);
    }

    /**
     * Extract SignableConsent from JWS payload
     */
    private SignableConsent extractSignableConsentFromPayload(Map<String, Object> payload) throws ConsentException {
        try {
            Object dataObject = payload.get("data");

            if (dataObject == null) {
                throw new ConsentException(
                        ErrorCodes.VALIDATION_ERROR,
                        ErrorCodes.getDescription(ErrorCodes.VALIDATION_ERROR),
                        "Missing 'data' field in JWS payload"
                );
            }

            // Parse to SignableConsent (NOT CookieConsent)
            if (dataObject instanceof String jsonString) {
                return objectMapper.readValue(jsonString, SignableConsent.class);
            }

            if (dataObject instanceof Map) {
                return objectMapper.convertValue(dataObject, SignableConsent.class);
            }

            throw new ConsentException(
                    ErrorCodes.VALIDATION_ERROR,
                    ErrorCodes.getDescription(ErrorCodes.VALIDATION_ERROR),
                    "Invalid data field type in JWS payload"
            );

        } catch (JsonProcessingException e) {
            log.error("Failed to parse signable consent JSON ");
            throw new ConsentException(
                    ErrorCodes.VALIDATION_ERROR,
                    ErrorCodes.getDescription(ErrorCodes.VALIDATION_ERROR),
                    "Invalid JSON format in consent data: " + e.getMessage()
            );
        }
    }

    /**
     * Normalize timestamps to millisecond precision
     */
    private SignableConsent compareSignableConsents(SignableConsent jwsConsent, SignableConsent dbConsent)
            throws ConsentException {

        // Normalize timestamps before comparison (truncate to milliseconds)
        jwsConsent = normalizeTimestamps(jwsConsent);
        dbConsent = normalizeTimestamps(dbConsent);

        if (!jwsConsent.equals(dbConsent)) {
            log.error("Signable consent mismatch between JWS and database");
            log.error("JWS Consent: {}", jwsConsent);
            log.error("DB Consent: {}", dbConsent);
            throw new ConsentException(
                    ErrorCodes.VALIDATION_ERROR,
                    ErrorCodes.getDescription(ErrorCodes.VALIDATION_ERROR),
                    "Consent data in JWS token does not match database records"
            );
        }else {
            return dbConsent;
        }
    }

    /**
     * Normalize timestamps to millisecond precision
     */
    private SignableConsent normalizeTimestamps(SignableConsent consent) {
        if (consent.getStartDate() != null) {
            consent.setStartDate(consent.getStartDate().truncatedTo(ChronoUnit.MILLIS));
        }
        if (consent.getEndDate() != null) {
            consent.setEndDate(consent.getEndDate().truncatedTo(ChronoUnit.MILLIS));
        }
        if (consent.getCreatedAt() != null) {
            consent.setCreatedAt(consent.getCreatedAt().truncatedTo(ChronoUnit.MILLIS));
        }
        if (consent.getUpdatedAt() != null) {
            consent.setUpdatedAt(consent.getUpdatedAt().truncatedTo(ChronoUnit.MILLIS));
        }

        // Ensure staleStatus consistency
        if (consent.getStaleStatus() == null) {
            consent.setStaleStatus(StaleStatus.NOT_STALE);
        }

        return consent;
    }

    private VaultVerifyResponse verifyTokenWithVault(String jwsToken, String tenantId, String businessId)
            throws ConsentException {
        try {
            return vaultService.verifyJwtToken(jwsToken, tenantId, businessId);
        } catch (Exception e) {
            log.error("Vault verify API call failed");
            throw new ConsentException(
                    ErrorCodes.EXTERNAL_SERVICE_ERROR,
                    ErrorCodes.getDescription(ErrorCodes.EXTERNAL_SERVICE_ERROR),
                    "Failed to verify JWS token with vault service: " + e.getMessage()
            );
        }
    }

    private void validateVaultResponse(VaultVerifyResponse response) throws ConsentException {
        if (!response.isValid()) {
            log.error("JWS token signature validation failed");
            throw new ConsentException(
                    ErrorCodes.VALIDATION_ERROR,
                    ErrorCodes.getDescription(ErrorCodes.VALIDATION_ERROR),
                    "JWS token signature is invalid"
            );
        }

        if (response.getPayload() == null || response.getPayload().isEmpty()) {
            log.error("JWS token payload is empty");
            throw new ConsentException(
                    ErrorCodes.VALIDATION_ERROR,
                    ErrorCodes.getDescription(ErrorCodes.VALIDATION_ERROR),
                    "JWS token payload is empty"
            );
        }
    }

    private CookieConsent fetchConsentFromDatabase(String consentId, Integer version, String tenantId)
            throws ConsentException {

        Optional<CookieConsent> consentOpt = consentRepository.findByConsentIdAndVersion(
                consentId, version, tenantId
        );

        return consentOpt.orElseThrow(() -> {
            log.error("Consent not found in database");
            return new ConsentException(
                    ErrorCodes.CONSENT_NOT_FOUND,
                    ErrorCodes.getDescription(ErrorCodes.CONSENT_NOT_FOUND),
                    "No consent found with ID: " + consentId + ", version: " + version
            );
        });
    }

    /**
     * Encrypt consent payload using Vault API
     * Sets: encryptionTime, encryptedReferenceId, encryptedString
     * Returns: encrypted JSON string
     */
    private void generateConsentEncryption(CookieConsent consent, String tenantId) throws ConsentException {
        CookieConsent tempConsent = new CookieConsent(consent);
        Gson gson = new GsonBuilder()
                .registerTypeAdapter(LocalDateTime.class, new LocalDateTypeAdapter())
                .registerTypeAdapter(Instant.class, new InstantTypeAdapter())
                .disableHtmlEscaping()
                .create();

        try {
            String consentJsonString = gson.toJson(tempConsent).replaceAll("\\s+", "");

            // Encrypt the consentJsonString using Vault encryptPayload API
            String businessId = consent.getBusinessId();

            EncryptPayloadResponse encryptResponse = vaultService.encryptPayload(
                    tenantId,
                    businessId,
                    "CookieConsent",
                    "CookieConsent",
                    consentJsonString
            );

            // SET ENCRYPTION TIME from Vault API response
            consent.setEncryptionTime(encryptResponse.getCreatedTimeStamp());

            // Store encryptedReferenceId in the consent entity
            consent.setEncryptedReferenceId(encryptResponse.getReferenceId());
            consent.setEncryptedString(encryptResponse.getEncryptedString());

            log.info("Vault encryption successful for consent: {}", consent.getConsentId());

        } catch (Exception e) {
            throw new ConsentException(ErrorCodes.EXTERNAL_SERVICE_ERROR,
                    ErrorCodes.getDescription(ErrorCodes.EXTERNAL_SERVICE_ERROR),
                    "Failed to encrypt consent with vault service: " + e.getMessage());
        }
    }

    /**
     * Sign consent JSON using Vault API
     * Returns: JWS token
     */
    private String generateConsentSigning(String consentJsonString, String tenantId, String businessId)
            throws ConsentException {
        try {
            String jwsToken = vaultService.signJsonPayload(consentJsonString, tenantId, businessId);
            log.info("Vault signing successful");
            return jwsToken;
        } catch (Exception e) {
            log.error("Vault signing failed:");
            throw new ConsentException(ErrorCodes.EXTERNAL_SERVICE_ERROR,
                    ErrorCodes.getDescription(ErrorCodes.EXTERNAL_SERVICE_ERROR),
                    "Failed to sign consent with vault service: " + e.getMessage());
        }
    }

    /**
     * Convert CookieConsent to SignableConsent (removes encryption fields)
     */
    private SignableConsent toSignableConsent(CookieConsent consent) {
        return SignableConsent.builder()
                .consentId(consent.getConsentId())
                .consentHandleId(consent.getConsentHandleId())
                .businessId(consent.getBusinessId())
                .templateId(consent.getTemplateId())
                .templateVersion(consent.getTemplateVersion())
                .languagePreferences(consent.getLanguagePreferences())
                .multilingual(consent.getMultilingual())
                .customerIdentifiers(consent.getCustomerIdentifiers())
                .preferences(consent.getPreferences())
                .status(consent.getStatus())
                .consentStatus(consent.getConsentStatus())
                .version(consent.getVersion())
                .startDate(consent.getStartDate())
                .endDate(consent.getEndDate())
                .createdAt(consent.getCreatedAt())
                .updatedAt(consent.getUpdatedAt())
                .staleStatus(consent.getStaleStatus())
                .build();
    }
}