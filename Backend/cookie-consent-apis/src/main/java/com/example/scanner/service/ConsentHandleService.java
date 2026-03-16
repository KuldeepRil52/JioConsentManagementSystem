package com.example.scanner.service;

import com.example.scanner.client.notification.NotificationManager;
import com.example.scanner.config.MultiTenantMongoConfig;
import com.example.scanner.config.TenantContext;
import com.example.scanner.constants.AuditConstants;
import com.example.scanner.constants.Constants;
import com.example.scanner.constants.ErrorCodes;
import com.example.scanner.dto.Preference;
import com.example.scanner.dto.request.CreateHandleCodeRequest;
import com.example.scanner.dto.request.CreateHandleRequest;
import com.example.scanner.dto.response.ConsentHandleResponse;
import com.example.scanner.dto.response.GetHandleResponse;
import com.example.scanner.dto.response.PreferenceWithCookies;
import com.example.scanner.entity.CookieConsentHandle;
import com.example.scanner.entity.ConsentTemplate;
import com.example.scanner.entity.CookieEntity;
import com.example.scanner.entity.ScanResultEntity;
import com.example.scanner.enums.ConsentHandleStatus;
import com.example.scanner.enums.LANGUAGE;
import com.example.scanner.enums.NotificationEvent;
import com.example.scanner.exception.ConsentHandleExpiredException;
import com.example.scanner.exception.ScannerException;
import com.example.scanner.repository.ConsentHandleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;
import com.example.scanner.dto.response.GetConsentHandleAndSecureCodeResponse;
import com.example.scanner.dto.response.SecureCodeApiResponse;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ConsentHandleService {

    private final ConsentHandleRepository consentHandleRepository;
    private final ConsentTemplateService consentTemplateService;
    private final MultiTenantMongoConfig mongoConfig;
    private final AuditService auditService;
    private final NotificationManager notificationManager;
    private final SecureCodeService secureCodeService;

    @Value("${consent.handle.expiry.minutes:15}")
    private int handleExpiryMinutes;

    public ConsentHandleResponse createConsentHandle(String tenantId, CreateHandleRequest request, Map<String, String> headers)
            throws ScannerException {

        if (tenantId == null || tenantId.trim().isEmpty()) {
            throw new ScannerException(ErrorCodes.VALIDATION_ERROR,
                    "Tenant ID is required",
                    "Missing X-Tenant-ID header");
        }

        auditService.logConsentHandleCreationInitiated(tenantId, Constants.BUSINESS_ID_HEADER,"pending");

        TenantContext.setCurrentTenant(tenantId);

        try {
            validateTemplate(tenantId, request.getTemplateId(), request.getTemplateVersion());

            CookieConsentHandle existingHandle = consentHandleRepository.findActiveConsentHandle(
                    request.getCustomerIdentifiers().getValue(),
                    request.getUrl(),
                    request.getTemplateId(),
                    request.getTemplateVersion(),
                    tenantId
            );

            if (existingHandle != null) {
                log.info("Returning existing consent handle");

                return ConsentHandleResponse.builder()
                        .consentHandleId(existingHandle.getConsentHandleId())
                        .message("Existing Consent Handle returned!")
                        .txnId(headers.get(Constants.TXN_ID))
                        .isNewHandle(false)
                        .build();
            }

            String consentHandleId = UUID.randomUUID().toString();

            CookieConsentHandle consentHandle = new CookieConsentHandle(
                    consentHandleId,
                    headers.get(Constants.BUSINESS_ID_HEADER),
                    headers.get(Constants.TXN_ID),
                    request.getTemplateId(),
                    request.getTemplateVersion(),
                    request.getUrl(),
                    request.getCustomerIdentifiers(),
                    ConsentHandleStatus.PENDING,
                    handleExpiryMinutes
            );

            CookieConsentHandle savedHandle = this.consentHandleRepository.save(consentHandle, tenantId);

            // Log handle created
            auditService.logConsentHandleCreated(tenantId, Constants.BUSINESS_ID_HEADER, consentHandle.getCustomerIdentifiers().getValue(), consentHandleId);

            //Callback for CONSENT_HANDLE_CREATED event
            Map<String, Object> payload = new HashMap<>();
            payload.put("expiresAt", savedHandle.getExpiresAt());
            payload.put("consentHandleId", savedHandle.getConsentHandleId());
            payload.put("templateVersion",savedHandle.getTemplateVersion());

            notificationManager.initiateCookieConsentHandleCreatedNotification(NotificationEvent.COOKIE_CONSENT_HANDLE_CREATED,
                    tenantId,
                    savedHandle.getBusinessId() != null ? savedHandle.getBusinessId() : null,
                    savedHandle.getCustomerIdentifiers() != null ? savedHandle.getCustomerIdentifiers() : null,
                    payload,
                    LANGUAGE.ENGLISH,
                    savedHandle.getConsentHandleId()
            );

            log.info("Created new consent handle ");

            return ConsentHandleResponse.builder()
                    .consentHandleId(savedHandle.getConsentHandleId())
                    .message("Consent Handle Created successfully!")
                    .txnId(headers.get(Constants.TXN_ID))
                    .isNewHandle(true)
                    .build();

        } catch (IllegalStateException e) {
            if (e.getMessage().contains("Database") && e.getMessage().contains("does not exist")) {
                log.error("Database does not exist");
                throw new ScannerException(ErrorCodes.VALIDATION_ERROR,
                        "Invalid tenant - database does not exist",
                        "Database 'template_" + tenantId + "' does not exist. Please check the tenant ID.");
            }
            throw e;
        } catch (DataAccessException e) {
            log.error("Database access error while creating consent handle");
            throw new ScannerException(ErrorCodes.INTERNAL_ERROR,
                    "Failed to access database",
                    e.getMessage());
        } catch (Exception e) {
            log.error("Error creating consent handle");
            throw new ScannerException(ErrorCodes.INTERNAL_ERROR,
                    "Failed to create consent handle",
                    e.getMessage());
        } finally {
            TenantContext.clear();
        }
    }

    public GetHandleResponse getConsentHandleById(String consentHandleId, String tenantId)
            throws ScannerException {

        if (tenantId == null || tenantId.trim().isEmpty()) {
            throw new ScannerException(ErrorCodes.VALIDATION_ERROR,
                    "Tenant ID is required",
                    "Missing tenant ID for consent handle retrieval");
        }

        TenantContext.setCurrentTenant(tenantId);
        MongoTemplate mongoTemplate = mongoConfig.getMongoTemplateForTenant(tenantId);

        try {
            // Fetch consent handle
            CookieConsentHandle consentHandle = this.consentHandleRepository.getByConsentHandleId(consentHandleId, tenantId);
            if (ObjectUtils.isEmpty(consentHandle)) {
                log.warn("Consent handle not found");
                throw new ScannerException(ErrorCodes.NOT_FOUND,
                        "Consent handle not found",
                        "No consent handle found with ID: " + consentHandleId);
            }

            validateNotExpired(consentHandle, tenantId);

            // Get template information
            Optional<ConsentTemplate> templateOpt = consentTemplateService.getTemplateByTenantAndTemplateIdAndTemplateVersion(
                    tenantId, consentHandle.getTemplateId(), consentHandle.getTemplateVersion());

            if (templateOpt.isEmpty()) {
                log.warn("Template not found");
                throw new ScannerException(ErrorCodes.NOT_FOUND,
                        "Template not found",
                        "Template not found for consent handle: " + consentHandleId);
            }

            ConsentTemplate template = templateOpt.get();

            // STEP 1: Fetch cookies from scan results
            Map<String, List<CookieEntity>> cookiesByCategory = fetchAndCategorizeCookies(
                    template.getScanId(),
                    mongoTemplate
            );

            // STEP 2: Create PreferenceWithCookies by matching purpose with cookie category
            List<PreferenceWithCookies> preferencesWithCookies = mapCookiesToPreferences(
                    template.getPreferences(),
                    cookiesByCategory
            );

            GetHandleResponse response = GetHandleResponse.builder()
                    .consentHandleId(consentHandle.getConsentHandleId())
                    .templateId(template.getTemplateId())
                    .templateName(template.getTemplateName())
                    .templateVersion(consentHandle.getTemplateVersion())
                    .url(consentHandle.getUrl())
                    .businessId(consentHandle.getBusinessId())
                    .multilingual(template.getMultilingual())
                    .uiConfig(template.getUiConfig())
                    .preferences(preferencesWithCookies)
                    .customerIdentifiers(consentHandle.getCustomerIdentifiers())
                    .status(consentHandle.getStatus())
                    .build();

            log.info("Retrieved consent handle preferences and cookies for tenant");

            return response;

        } finally {
            TenantContext.clear();
        }
    }

    /**
     * Fetch all cookies from scan result and group them by category
     */
    private Map<String, List<CookieEntity>> fetchAndCategorizeCookies(
            String scanId,
            MongoTemplate mongoTemplate) {

        if (scanId == null || scanId.trim().isEmpty()) {
            log.warn("No scanId provided, returning empty cookie map");
            return Collections.emptyMap();
        }

        try {
            // Fetch scan result
            Query query = new Query(Criteria.where("transactionId").is(scanId));
            ScanResultEntity scanResult = mongoTemplate.findOne(query, ScanResultEntity.class);

            if (scanResult == null || scanResult.getCookiesBySubdomain() == null) {
                log.warn("No scan result or cookies found for scanId: {}", scanId);
                return Collections.emptyMap();
            }

            // Flatten all cookies from all subdomains
            List<CookieEntity> allCookies = scanResult.getCookiesBySubdomain().values()
                    .stream()
                    .flatMap(List::stream).toList();

            log.info("Found {} total cookies across all subdomains for scanId: {}",
                    allCookies.size(), scanId);

            // Group cookies by category
            Map<String, List<CookieEntity>> cookiesByCategory = allCookies.stream()
                    .filter(cookie -> cookie.getCategory() != null && !cookie.getCategory().trim().isEmpty())
                    .collect(Collectors.groupingBy(CookieEntity::getCategory));

            log.info("Categorized cookies into {} categories", cookiesByCategory.size());

            return cookiesByCategory;

        } catch (Exception e) {
            log.error("Error fetching cookies for scanId: {}", scanId);
            return Collections.emptyMap();
        }
    }

    /**
     * Map cookies to preferences based on matching purpose with cookie category
     */
    private List<PreferenceWithCookies> mapCookiesToPreferences(
            List<Preference> preferences,
            Map<String, List<CookieEntity>> cookiesByCategory) {

        if (preferences == null || preferences.isEmpty()) {
            log.warn("No preferences to map cookies to");
            return Collections.emptyList();
        }


        return preferences.stream()
                .map(preference -> {
                    String categoryKey = preference.getPurpose();
                    List<CookieEntity> matchingCookies = cookiesByCategory.getOrDefault(
                            categoryKey,
                            Collections.emptyList()
                    );

                    log.debug("Mapped {} cookies to preference category: {}",
                            matchingCookies.size(), categoryKey);

                    return PreferenceWithCookies.from(preference, matchingCookies);
                })
                .collect(Collectors.toList());
    }

    private void validateTemplate(String tenantId, String templateId, int templateVersion) throws ScannerException {
        // Check if template exists using the existing ConsentTemplateService
        Optional<ConsentTemplate> templateOpt = consentTemplateService.getTemplateByTenantAndTemplateIdAndBusinessId(tenantId, templateId, templateVersion);

        if (templateOpt.isEmpty()) {
            throw new ScannerException(ErrorCodes.NOT_FOUND,
                    "Template not found",
                    "Template with ID " + templateId + " and version "+ templateVersion +" with status PUBLISHED and Template Status ACTIVE does not exist for tenant " + tenantId);
        }

        ConsentTemplate template = templateOpt.get();

        // Validate template is published
        if (!"PUBLISHED".equals(template.getStatus().name())) {
            throw new ScannerException(ErrorCodes.INVALID_STATE_ERROR,
                    "Template is not published",
                    "Template " + templateId + " has status: " + template.getStatus());
        }
    }

    private void validateNotExpired(CookieConsentHandle handle, String tenantId) {
        if (handle.isExpired()) {
            handle.setStatus(ConsentHandleStatus.REQ_EXPIRED);
            consentHandleRepository.save(handle, tenantId);

            throw new ConsentHandleExpiredException(
                    ErrorCodes.CONSENT_HANDLE_EXPIRED,
                    "Consent handle has expired",
                    "Consent handle " + handle.getConsentHandleId() + " expired at " + handle.getExpiresAt()
            );
        }
    }

    /**
     * Create consent handle and fetch secure code in one operation
     *
     * @param tenantId    Tenant ID from header
     * @param businessId  Business ID from header
     * @param request     Request containing templateId, templateVersion, and customerIdentifiers
     * @param headers     All request headers
     * @return Combined response with consent handle ID and secure code details
     * @throws ScannerException if any step fails
     */
    public GetConsentHandleAndSecureCodeResponse createConsentHandleAndSecureCode(
            String tenantId,
            String businessId,
            CreateHandleRequest request,
            Map<String, String> headers) throws ScannerException {

        log.info("Starting consent handle and secure code creation for tenant: {}, businessId: {}, templateId: {}",
                tenantId, businessId, request.getTemplateId());

        try {
            // Step 1: Create consent handle internally
            CreateHandleRequest createHandleRequest = CreateHandleRequest.builder()
                    .templateId(request.getTemplateId())
                    .templateVersion(request.getTemplateVersion())
                    .customerIdentifiers(request.getCustomerIdentifiers())
                    .build();

            log.info("Creating consent handle for templateId: {}, version: {}",
                    request.getTemplateId(), request.getTemplateVersion());

            ConsentHandleResponse consentHandleResponse = this.createConsentHandle(tenantId, createHandleRequest, headers);

            log.info("Consent handle created successfully: {}", consentHandleResponse.getConsentHandleId());

            // Step 2: Call external secure code API through SecureCodeService
            String identityValue = request.getCustomerIdentifiers().getValue();
            String identityType = request.getCustomerIdentifiers().getType();

            log.info("Calling secure code service for identity: {}, type: {}", identityValue, identityType);

            SecureCodeApiResponse secureCodeResponse = secureCodeService.createSecureCode(
                    tenantId, businessId, identityValue, identityType);

            log.info("Secure code created successfully: {}", secureCodeResponse.getSecureCode());

            // Step 3: Build combined response
            GetConsentHandleAndSecureCodeResponse response = GetConsentHandleAndSecureCodeResponse.builder()
                    .consentHandleId(consentHandleResponse.getConsentHandleId())
                    .secureCode(secureCodeResponse.getSecureCode())
                    .identity(secureCodeResponse.getIdentity())
                    .expiry(secureCodeResponse.getExpiry())
                    .templateId(request.getTemplateId())
                    .templateVersion(request.getTemplateVersion())
                    .message("Consent handle and secure code created successfully")
                    .build();

            log.info("Combined response created successfully for consentHandleId: {}, secureCode: {}",
                    response.getConsentHandleId(), response.getSecureCode());

            return response;

        } catch (ScannerException e) {
            log.error("Error during consent handle and secure code creation ");
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error during consent handle and secure code creation");
            throw new ScannerException(
                    ErrorCodes.INTERNAL_ERROR,
                    "Something went wrong",
                    "Unexpected error occurred: " + e.getMessage()
            );
        }
    }

    public GetConsentHandleAndSecureCodeResponse createConsentHandleAndSecureCodeNew(
            String tenantId,
            String businessId,
            CreateHandleCodeRequest request,
            Map<String, String> headers) throws ScannerException {

        log.info("Starting consent handle and secure code creation with latest template for tenant: {}, businessId: {}, templateId: {}",
                tenantId, businessId, request.getTemplateId());

        // ============ STEP 1: FETCH LATEST PUBLISHED TEMPLATE ============
        Optional<ConsentTemplate> latestTemplateOpt = consentTemplateService.getLatestPublishedTemplate(
                tenantId, request.getTemplateId());

        if (latestTemplateOpt.isEmpty()) {
            log.error("No published template found for templateId: {}", request.getTemplateId());
            throw new ScannerException(ErrorCodes.NOT_FOUND,
                    "Template not found",
                    "No published and active template found with ID " + request.getTemplateId() + " for tenant " + tenantId);
        }

        ConsentTemplate latestTemplate = latestTemplateOpt.get();
        int templateVersion = latestTemplate.getVersion();

        log.info("Latest published template found: templateId: {}, version: {}",
                request.getTemplateId(), templateVersion);

        // ============ STEP 2: VALIDATE INPUTS ============
        if (tenantId == null || tenantId.trim().isEmpty()) {
            throw new ScannerException(ErrorCodes.VALIDATION_ERROR,
                    "Tenant ID is required",
                    "Missing X-Tenant-ID header");
        }

        // Audit log: Handle creation initiated
        auditService.logConsentHandleCreationInitiated(tenantId, businessId, "pending");

        TenantContext.setCurrentTenant(tenantId);

        try {

            // ============ STEP 3: CHECK FOR EXISTING ACTIVE HANDLE ============
            CookieConsentHandle existingHandle = consentHandleRepository.findActiveConsentHandle(
                    request.getCustomerIdentifiers().getValue(),
                    request.getUrl(),
                    request.getTemplateId(),
                    templateVersion,
                    tenantId
            );

            String consentHandleId;
            CookieConsentHandle savedHandle;

            if (existingHandle != null) {
                log.info("Found existing active consent handle: {}", existingHandle.getConsentHandleId());
                consentHandleId = existingHandle.getConsentHandleId();
                savedHandle = existingHandle;

            } else {
                // ============ STEP 5: CREATE NEW CONSENT HANDLE ============
                consentHandleId = UUID.randomUUID().toString();

                CookieConsentHandle consentHandle = new CookieConsentHandle(
                        consentHandleId,
                        businessId,
                        headers.get(Constants.TXN_ID),
                        request.getTemplateId(),
                        templateVersion,
                        request.getUrl(),
                        request.getCustomerIdentifiers(),
                        ConsentHandleStatus.PENDING,
                        handleExpiryMinutes
                );

                savedHandle = this.consentHandleRepository.save(consentHandle, tenantId);

                log.info("Created new consent handle: {}", consentHandleId);

                // Audit log: Handle created
                auditService.logConsentHandleCreated(tenantId, businessId,
                        consentHandle.getCustomerIdentifiers().getValue(), consentHandleId);

                // Notification: Consent handle created
                Map<String, Object> payload = new HashMap<>();
                payload.put("expiresAt", savedHandle.getExpiresAt());
                payload.put("consentHandleId", savedHandle.getConsentHandleId());
                payload.put("templateVersion", savedHandle.getTemplateVersion());

                notificationManager.initiateCookieConsentHandleCreatedNotification(
                        NotificationEvent.COOKIE_CONSENT_HANDLE_CREATED,
                        tenantId,
                        savedHandle.getBusinessId() != null ? savedHandle.getBusinessId() : null,
                        savedHandle.getCustomerIdentifiers() != null ? savedHandle.getCustomerIdentifiers() : null,
                        payload,
                        LANGUAGE.ENGLISH,
                        savedHandle.getConsentHandleId()
                );
            }

            // ============ STEP 6: CALL SECURE CODE API ============
            String identityValue = request.getCustomerIdentifiers().getValue();
            String identityType = request.getCustomerIdentifiers().getType();

            log.info("Calling secure code service for identity: {}, type: {}", identityValue, identityType);

            SecureCodeApiResponse secureCodeResponse = secureCodeService.createSecureCode(
                    tenantId, businessId, identityValue, identityType);

            log.info("Secure code created successfully: {}", secureCodeResponse.getSecureCode());

            // ============ STEP 7: BUILD AND RETURN COMBINED RESPONSE ============
            GetConsentHandleAndSecureCodeResponse response = GetConsentHandleAndSecureCodeResponse.builder()
                    .consentHandleId(savedHandle.getConsentHandleId())
                    .secureCode(secureCodeResponse.getSecureCode())
                    .identity(secureCodeResponse.getIdentity())
                    .expiry(secureCodeResponse.getExpiry())
                    .templateId(request.getTemplateId())
                    .templateVersion(templateVersion)
                    .message("Consent handle and secure code created successfully")
                    .build();

            log.info("Combined response created successfully for consentHandleId: {}, secureCode: {}, templateVersion: {}",
                    response.getConsentHandleId(), response.getSecureCode(), templateVersion);

            return response;

        } catch (ScannerException e) {
            log.error("Error during consent handle and secure code creation");
            throw e;
        } catch (IllegalStateException e) {
            if (e.getMessage().contains("Database") && e.getMessage().contains("does not exist")) {
                log.error("Database does not exist for tenant: {}", tenantId);
                throw new ScannerException(ErrorCodes.VALIDATION_ERROR,
                        "Invalid tenant - database does not exist",
                        "Database 'template_" + tenantId + "' does not exist. Please check the tenant ID.");
            }
            throw new ScannerException(ErrorCodes.INTERNAL_ERROR,
                    "Something went wrong",
                    e.getMessage());
        } catch (DataAccessException e) {
            log.error("Database access error while creating consent handle");
            throw new ScannerException(ErrorCodes.INTERNAL_ERROR,
                    "Failed to access database",
                    e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error during consent handle and secure code creation");
            throw new ScannerException(
                    ErrorCodes.INTERNAL_ERROR,
                    "Something went wrong",
                    "Unexpected error occurred: " + e.getMessage()
            );
        } finally {
            TenantContext.clear();
        }
    }
}