package com.jio.partnerportal.service;

import com.jio.partnerportal.client.audit.AuditManager;
import com.jio.partnerportal.client.audit.request.Actor;
import com.jio.partnerportal.client.audit.request.AuditRequest;
import com.jio.partnerportal.client.audit.request.Context;
import com.jio.partnerportal.client.audit.request.Resource;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.jio.partnerportal.client.notification.NotificationManager;
import com.jio.partnerportal.client.notification.request.TriggerEventRequest;
import com.jio.partnerportal.constant.Constants;
import com.jio.partnerportal.constant.ErrorCodes;
import com.jio.partnerportal.dto.ActionType;
import com.jio.partnerportal.dto.AuditComponent;
import com.jio.partnerportal.dto.ConfigType;
import com.jio.partnerportal.dto.DPODetails;
import com.jio.partnerportal.dto.IdentityType;
import com.jio.partnerportal.dto.Operation;
import com.jio.partnerportal.dto.ScopeLevel;
import com.jio.partnerportal.dto.request.ConfigurationRequest;
import com.jio.partnerportal.dto.response.DpoDashboardResponse;
import com.jio.partnerportal.dto.response.SearchResponse;
import com.jio.partnerportal.entity.DPOConfig;
import com.jio.partnerportal.entity.Role;
import com.jio.partnerportal.entity.TenantRegistry;
import com.jio.partnerportal.entity.User;
import com.jio.partnerportal.exception.PartnerPortalException;
import com.jio.partnerportal.multitenancy.TenantMongoTemplateProvider;
import com.jio.partnerportal.repository.DPORepository;
import com.jio.partnerportal.repository.TenantRepository;
import com.jio.partnerportal.util.*;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.ThreadContext;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Slf4j
@Service
public class DPOService {

    DPORepository dpoRepository;
    Utils utils;
    ConfigHistoryService configHistoryService;
    TenantMongoTemplateProvider tenantMongoTemplateProvider;
    TenantRepository tenantRepository;
    RestUtility restUtility;
    Environment environment;
    AuditManager auditManager;
    NotificationManager notificationManager;

    @Autowired
    DPOService(DPORepository dpoRepository, Utils utils, ConfigHistoryService configHistoryService,
               TenantMongoTemplateProvider tenantMongoTemplateProvider, TenantRepository tenantRepository,
               RestUtility restUtility, Environment environment, AuditManager auditManager,
               NotificationManager notificationManager) {
        this.dpoRepository = dpoRepository;
        this.utils = utils;
        this.configHistoryService = configHistoryService;
        this.tenantMongoTemplateProvider = tenantMongoTemplateProvider;
        this.tenantRepository = tenantRepository;
        this.restUtility = restUtility;
        this.environment = environment;
        this.auditManager = auditManager;
        this.notificationManager = notificationManager;
    }

    @Value("${dpo.search.parameters}")
    List<String> dpoSearchParams;

    @Value("${portal.url:https://partnerportal.example.com}")
    private String portalUrl;

    private static final List<String> CONSENT_EVENTS = List.of(
            "CONSENT_REQUEST_PENDING",
            "CONSENT_RENEWAL_REQUEST",
            "CONSENT_CREATED",
            "CONSENT_UPDATED",
            "CONSENT_WITHDRAWN",
            "CONSENT_EXPIRED",
            "CONSENT_RENEWED",
            "CONSENT_PREFERENCE_EXPIRY"
    );

    private static final List<String> GRIEVANCE_EVENTS = List.of(
            "GRIEVANCE_RAISED",
            "GRIEVANCE_INPROCESS",
            "GRIEVANCE_ESCALATED",
            "GRIEVANCE_RESOLVED",
            "GRIEVANCE_DENIED",
            "GRIEVANCE_CLOSED",
            "GRIEVANCE_STATUS_UPDATED",
            "GRIEVANCE_ESCALATED_L1",
            "GRIEVANCE_ESCALATED_L2"

    );

    public DPOConfig createConfig(Map<String, String> headers, ConfigurationRequest<DPODetails> request, HttpServletRequest req) throws PartnerPortalException, JsonProcessingException {
        String activity = "Create DPO configuration";

        String scopeLevel = headers.get(Constants.SCOPE_LEVEL_HEADER);
        String businessId = headers.get(Constants.BUSINESS_ID_HEADER);
        String tenantId = headers.get(Constants.TENANT_ID_HEADER);
        
        if (ScopeLevel.TENANT.toString().equals(scopeLevel) && this.dpoRepository.existByScopeLevel(scopeLevel)) {
            throw new PartnerPortalException(ErrorCodes.JCMP3002);
        }

        if(this.dpoRepository.existByScopeLevelAndBusinessId(scopeLevel, businessId)) {
            throw new PartnerPortalException(ErrorCodes.JCMP3003);
        }
        
        DPOConfig config = DPOConfig.builder()
                .configId(UUID.randomUUID().toString())
                .businessId(businessId)
                .scopeLevel(scopeLevel)
                .configurationJson(request.getConfigurationJson())
                .build();


        // ===== Create DPO User in users collection =====
        try {
            createDPOUser(tenantId, businessId, request.getConfigurationJson());
        } catch (Exception e) {
            log.error("Error creating DPO user: {}", e.getMessage(), e);
            // Continue execution even if user creation fails
        }

        DPOConfig dpoConfig = this.dpoRepository.save(config);
        this.configHistoryService.createConfigHistoryEntry(dpoConfig, businessId, ConfigType.DPO, Operation.CREATE);
        
        this.logDPOConfigAudit(dpoConfig, ActionType.CREATE);
        LogUtil.logActivity(req, activity, "Success: Create DPO configuration successfully");
        return dpoConfig;
    }

    public DPOConfig updateConfig(String configId, ConfigurationRequest<DPODetails> request, HttpServletRequest req) throws PartnerPortalException, JsonProcessingException {
        String activity = "Update DPO configuration";

        DPOConfig config = this.dpoRepository.findByConfigId(configId);
        if (ObjectUtils.isEmpty(config)) {
            throw new PartnerPortalException(ErrorCodes.JCMP3001);
        }
        config.setConfigurationJson(request.getConfigurationJson());
        DPOConfig dpoConfig = this.dpoRepository.save(config);
        this.configHistoryService.createConfigHistoryEntry(config, config.getBusinessId(), ConfigType.DPO, Operation.UPDATE);

        this.logDPOConfigAudit(dpoConfig, ActionType.UPDATE);
        LogUtil.logActivity(req, activity, "Success: Update DPO configuration successfully");
        return dpoConfig;
    }

    public SearchResponse<DPOConfig> search(Map<String, String> reqParams, HttpServletRequest req) throws PartnerPortalException {
        String activity = "Search DPO configuration";

        Map<String, String> searchParams = this.utils.filterRequestParam(reqParams, dpoSearchParams);
        List<DPOConfig> mongoResponse = this.dpoRepository.findConfigByParams(searchParams);

        if (ObjectUtils.isEmpty(mongoResponse)) {
            throw new PartnerPortalException(ErrorCodes.JCMP3001);
        }

        LogUtil.logActivity(req, activity, "Success: Search DPO configuration successfully");
        return SearchResponse.<DPOConfig>builder()
                .searchList(mongoResponse)
                .build();
    }

    public long count() {
        return this.dpoRepository.count();
    }

    /**
     * Creates a DPO user in the users collection following the same pattern as tenant user creation
     */
    private void createDPOUser(String tenantId, String businessId, DPODetails dpoDetails) {
        log.info("Creating DPO user for tenantId: {}, businessId: {}", tenantId, businessId);
        
        // ===== Validate DPO details =====
        if (dpoDetails == null) {
            log.error("DPO details are null");
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "DPO details are required");
        }
        
        if (!StringUtils.hasText(dpoDetails.getEmail()) && !StringUtils.hasText(dpoDetails.getMobile())) {
            log.error("Both email and mobile are missing in DPO details");
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "At least email or mobile is required");
        }
        
        // ===== Get tenant MongoTemplate =====
        MongoTemplate tenantDb = tenantMongoTemplateProvider.getMongoTemplate(tenantId);
        
        // ===== Find DPO role (assuming it's already created) =====
        Query roleQuery = new Query(Criteria.where("role").is("DPO"));
        Role dpoRole = tenantDb.findOne(roleQuery, Role.class, Constants.ROLES);
        
        if (dpoRole == null) {
            log.error("DPO role not found in tenant database");
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "DPO role not found. Please create DPO role first");
        }
        
        log.info("Found DPO role with roleId: {}", dpoRole.getRoleId());
        
        // ===== Build User.Role =====
        User.Role userRole = User.Role.builder()
                .roleId(dpoRole.getRoleId())
                .businessId(businessId)
                .build();

        String totpSecret = TotpUtils.generateSecret();
        
        // Generate QR code for TOTP (will be used in notification)
        String qrCodeBase64 = null;
        try {
            String accountName = dpoDetails.getEmail() != null ? dpoDetails.getEmail() : dpoDetails.getMobile();
            String issuer = "JCMP_PARTNERPORTAL";
            String totpURI = TotpUtils.generateTOTPProvisioningURI(totpSecret, accountName, issuer);
            qrCodeBase64 = TotpUtils.generateQRCodeBase64(totpURI);
        } catch (Exception e) {
            log.error("Error generating QR code for TOTP", e);
        }
        
        // ===== Build User entity with both email and mobile =====
        String userId = UUID.randomUUID().toString();
        User user = User.builder()
                .userId(userId)
                .username(dpoDetails.getName())
                .email(dpoDetails.getEmail())  // Keep email
                .mobile(dpoDetails.getMobile()) // Keep mobile
                .totpSecret(totpSecret)
                .identityType("BOTH") // Both email and mobile
                .roles(Arrays.asList(userRole))
                .designation("DPO")
                .build();
        
        // ===== Save user in tenant database =====
        tenantDb.save(user, Constants.USERS);
        log.info("DPO user created successfully with userId: {}", userId);
        
        // ===== Fetch tenant registry to get PAN and clientId =====
        TenantRegistry registry = tenantRepository.findByTenantId(tenantId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tenant not found"));
        
        String pan = registry.getPan();
        String clientId = registry.getClientId();
        
        // ===== Send notification for both email and mobile =====
        try {
            notifyCreateUser(tenantId, businessId, dpoDetails.getName(), dpoDetails.getEmail(), dpoDetails.getMobile(), pan, totpSecret, clientId);
            // Send DPO_USER_ONBOARDING notification
            notifyDPOUserOnboarding(tenantId, businessId, dpoDetails.getName(), dpoDetails.getEmail(), dpoDetails.getMobile(), pan, clientId);
        } catch (Exception e) {
            log.error("Error sending DPO user notification", e);
            // Don't throw exception, just log it
        }
    }
    
    /**
     * Sends notification to DPO user for both email and mobile (same as tenant user notification)
     */
    private void notifyCreateUser(String tenantId, String businessId, String username, String email, String mobile, String pan, String secretKey, String clientId) {
        try {
            // ===== Send Email notification if available =====
            if (StringUtils.hasText(email)) {
                // Build customer identifiers for email
                TriggerEventRequest.CustomerIdentifiers emailCustomerIdentifiers = TriggerEventRequest.CustomerIdentifiers.builder()
                        .type(IdentityType.EMAIL)
                        .value(email)
                        .build();

                // Build event payload (same structure as TenantService and UserService)
                Map<String, Object> emailEventPayload = new HashMap<>();
                emailEventPayload.put("accountName", pan);
                emailEventPayload.put("totpKey", secretKey);
                emailEventPayload.put("name", username);
                
                // Add portalUrl and clientId to event payload
                emailEventPayload.put("portalUrl", portalUrl);
                emailEventPayload.put("clientId", clientId);
                
                // Generate and add QR code to event payload
                try {
                    String accountName = email;
                    String issuer = "JCMP_PARTNERPORTAL";
                    String totpURI = TotpUtils.generateTOTPProvisioningURI(secretKey, accountName, issuer);
                    String qrCodeBase64 = TotpUtils.generateQRCodeBase64(totpURI);
                    emailEventPayload.put("qrData", qrCodeBase64);
                } catch (Exception e) {
                    log.error("Error generating QR code for TOTP notification", e);
                }

                // Build trigger event request
                TriggerEventRequest emailTriggerEventRequest = TriggerEventRequest.builder()
                        .eventType("TOTP_PIN_DETAILS")
                        .customerIdentifiers(emailCustomerIdentifiers)
                        .eventPayload(emailEventPayload)
                        .build();

                // Trigger notification event using NotificationManager (same as tenant user)
                notificationManager.triggerEvent(tenantId, businessId, ScopeLevel.TENANT.toString(), emailTriggerEventRequest);

                log.info("TOTP_PIN_DETAILS notification event triggered successfully for DPO email - tenantId: {}, businessId: {}, username: {}", 
                        tenantId, businessId, username);
            }
            
            // ===== Send SMS notification if available =====
            if (StringUtils.hasText(mobile)) {
                // Build customer identifiers for mobile
                TriggerEventRequest.CustomerIdentifiers mobileCustomerIdentifiers = TriggerEventRequest.CustomerIdentifiers.builder()
                        .type(IdentityType.MOBILE)
                        .value(mobile)
                        .build();

                // Build event payload (same structure as TenantService and UserService)
                Map<String, Object> mobileEventPayload = new HashMap<>();
                mobileEventPayload.put("accountName", pan);
                mobileEventPayload.put("totpKey", secretKey);
                mobileEventPayload.put("name", username);
                
                // Add portalUrl and clientId to event payload
                mobileEventPayload.put("portalUrl", portalUrl);
                if (clientId != null) {
                    mobileEventPayload.put("clientId", clientId);
                }
                
                // Generate and add QR code to event payload
                try {
                    String accountName = mobile;
                    String issuer = "JCMP_PARTNERPORTAL";
                    String totpURI = TotpUtils.generateTOTPProvisioningURI(secretKey, accountName, issuer);
                    String qrCodeBase64 = TotpUtils.generateQRCodeBase64(totpURI);
                    mobileEventPayload.put("qrData", qrCodeBase64);
                } catch (Exception e) {
                    log.error("Error generating QR code for TOTP notification", e);
                }

                // Build trigger event request
                TriggerEventRequest mobileTriggerEventRequest = TriggerEventRequest.builder()
                        .eventType("TOTP_PIN_DETAILS")
                        .customerIdentifiers(mobileCustomerIdentifiers)
                        .eventPayload(mobileEventPayload)
                        .build();

                // Trigger notification event using NotificationManager (same as tenant user)
                notificationManager.triggerEvent(tenantId, businessId, ScopeLevel.TENANT.toString(), mobileTriggerEventRequest);

                log.info("TOTP_PIN_DETAILS notification event triggered successfully for DPO mobile - tenantId: {}, businessId: {}, username: {}", 
                        tenantId, businessId, username);
            }
            
            // ===== If neither present =====
            if (!StringUtils.hasText(email) && !StringUtils.hasText(mobile)) {
                log.warn("No valid identity provided for DPO 2FA notification");
            }
            
        } catch (Exception e) {
            log.error("Error triggering DPO TOTP_PIN_DETAILS notification events for tenantId: {}, businessId: {}. Error: {}",
                    tenantId, businessId, e.getMessage(), e);
        }
    }

    /**
     * Sends DPO_USER_ONBOARDING notification to DPO user
     */
    private void notifyDPOUserOnboarding(String tenantId, String businessId, String dpoName, String email, String mobile, String pan, String clientId) {
        try {
            // Determine which identity to use (prefer email, fallback to mobile)
            IdentityType identityType = null;
            String customerIdentifierValue = null;
            
            if (StringUtils.hasText(email)) {
                identityType = IdentityType.EMAIL;
                customerIdentifierValue = email;
            } else if (StringUtils.hasText(mobile)) {
                identityType = IdentityType.MOBILE;
                customerIdentifierValue = mobile;
            } else {
                log.warn("No valid identity provided for DPO_USER_ONBOARDING notification");
                return;
            }

            // Build customer identifiers
            TriggerEventRequest.CustomerIdentifiers customerIdentifiers = TriggerEventRequest.CustomerIdentifiers.builder()
                    .type(identityType)
                    .value(customerIdentifierValue)
                    .build();

            // Build event payload for DPO_USER_ONBOARDING
            Map<String, Object> eventPayload = new HashMap<>();
            eventPayload.put("name", dpoName);
            eventPayload.put("userRole", "DPO");
            eventPayload.put("clientId", clientId);
            eventPayload.put("organizationPan", pan);
            eventPayload.put("tenantId", tenantId);

            // Build trigger event request
            TriggerEventRequest triggerEventRequest = TriggerEventRequest.builder()
                    .eventType("DPO_USER_ONBOARDING")
                    .customerIdentifiers(customerIdentifiers)
                    .eventPayload(eventPayload)
                    .build();

            // Trigger notification event using NotificationManager
            notificationManager.triggerEvent(tenantId, businessId, ScopeLevel.TENANT.toString(), triggerEventRequest);

            log.info("DPO_USER_ONBOARDING notification event triggered successfully for tenantId: {}, businessId: {}, DPO: {}", 
                    tenantId, businessId, dpoName);

        } catch (Exception e) {
            log.error("Error triggering DPO_USER_ONBOARDING notification event for tenantId: {}, businessId: {}",
                    tenantId, businessId, e);
        }
    }

    public DpoDashboardResponse getDashboardData(String tenantId, String businessId, LocalDate fromDate, LocalDate toDate, HttpServletRequest req) {

        String activity = "Get DPO Dashboard data";

        MongoTemplate tenantDb = tenantMongoTemplateProvider.getMongoTemplate(ThreadContext.get(Constants.TENANT_ID_HEADER));

        //Determine user level
        boolean isTenantLevel = tenantId.equalsIgnoreCase(businessId);

        Map<String, Integer> cookieCounts = getCookiePreferenceCounts(tenantDb, isTenantLevel, businessId,fromDate,  toDate);
        Map<String, Integer> cookieHandleCounts = getCookieHandleCounts(
                tenantDb, isTenantLevel, businessId, fromDate, toDate);

        Map<String, Integer> slaCounts = getSlaCounts(tenantDb, isTenantLevel, tenantId, businessId,fromDate,  toDate);
        int resolvedCount = slaCounts.getOrDefault("totalResolved", 0);
        int exceededCount = slaCounts.getOrDefault("activeAndExceededSla", 0);

        DpoDashboardResponse response = DpoDashboardResponse.builder()
                // ------------------ PURPOSE ------------------
                .totalPurposeCreated(getCount(tenantDb, "purposes", isTenantLevel, businessId, fromDate, toDate))
                .dataTypes(getCount(tenantDb, "data_types", isTenantLevel, businessId, fromDate,  toDate))
                .processingActivities(getCount(tenantDb, "processor_activities", isTenantLevel, businessId, fromDate,  toDate))
                .dataretentionPeriod(getRetentionFieldValue(tenantDb, isTenantLevel, businessId, fromDate, toDate, "data_retention"))
                .logsretentionPeriod(getRetentionFieldValue(tenantDb, isTenantLevel, businessId, fromDate, toDate, "logs_retention"))
                .consentArtefactsretentionPeriod(getRetentionFieldValue(tenantDb, isTenantLevel, businessId, fromDate, toDate, "consent_artifact_retention"))
                .publishedTemp(getPublishedTemplatesCount(tenantDb, isTenantLevel, businessId,fromDate,  toDate))
                .dataProcessor(getCount(tenantDb, "data_processors", isTenantLevel, businessId, fromDate,  toDate))
                .pendingRenewal(getPendingRenewalCount(tenantDb, isTenantLevel, businessId, fromDate, toDate)
                )

                // ------------------ CONSENTS ------------------
                .totalConsents(getCount(tenantDb, "consents", isTenantLevel, businessId,fromDate,  toDate))
                .activeConsents(getConsentCountByStatus(tenantDb, isTenantLevel, businessId, "ACTIVE",fromDate,  toDate))
                .revokedConsents(getConsentCountByStatus(tenantDb, isTenantLevel, businessId, "WITHDRAWN",fromDate,  toDate))
                .expiredConsents(getConsentCountByStatus(tenantDb, isTenantLevel, businessId, "EXPIRED",fromDate,  toDate))
                .autorenewalConsents(getAutoRenewalCount(tenantDb, isTenantLevel, businessId,fromDate,  toDate))
                // ------------------ COOKIES ------------------
                .cookiesPublished(getCookieCountByStatus(tenantDb, isTenantLevel, businessId, "PUBLISHED",fromDate,  toDate))
                .cookiesDraft(getCookieCountByStatus(tenantDb, isTenantLevel, businessId, "DRAFT",fromDate,  toDate))
                .cookiesInactive(getCookieCountByStatus(tenantDb, isTenantLevel, businessId, "INACTIVE",fromDate,  toDate))

                // ------------------ CONSENT NOTIFICATIONS ------------------
                .notificationSent(
                        getNotificationEventCount(tenantDb, isTenantLevel, businessId, "notification_email", CONSENT_EVENTS, fromDate, toDate) +
                                getNotificationEventCount(tenantDb, isTenantLevel, businessId, "notification_sms", CONSENT_EVENTS, fromDate, toDate)
                )
                .notificationEmail(getNotificationEventCount(tenantDb, isTenantLevel, businessId, "notification_email", CONSENT_EVENTS, fromDate, toDate))
                .notificationSms(getNotificationEventCount(tenantDb, isTenantLevel, businessId, "notification_sms", CONSENT_EVENTS, fromDate, toDate))

//                // ------------------ COOKIES ACTIONS ------------------
                .cookiesAllAccepted(cookieCounts.getOrDefault("cookiesAllAccepted",0))
                .cookiesPartiallyAccepted(cookieCounts.getOrDefault("cookiesPartiallyAccepted",0))
                .cookieExpired(cookieCounts.getOrDefault("cookieExpired",0))
                .cookiesAllRejected(cookieHandleCounts.getOrDefault("cookieAllRejected", 0))
                .cookiesNoAction(cookieHandleCounts.getOrDefault("cookieNoAction", 0))
                .cookiesTotal(
                        cookieCounts.getOrDefault("cookiesAllAccepted", 0)
                                + cookieCounts.getOrDefault("cookiesPartiallyAccepted", 0)
                                + cookieCounts.getOrDefault("cookieExpired", 0)
                                + cookieHandleCounts.getOrDefault("cookieAllRejected", 0)
                                + cookieHandleCounts.getOrDefault("cookieNoAction", 0)
                )

                // ------------------ GRIEVANCES ------------------
                .grievanceTotalRequests(getCount(tenantDb, "grievances", isTenantLevel, businessId,fromDate,  toDate))
                .grievanceResolved(getGrievanceCountByStatus(tenantDb, isTenantLevel, businessId, "RESOLVED",fromDate,  toDate))
                .grievanceInProgress(getGrievanceCountByStatus(tenantDb, isTenantLevel, businessId, "INPROCESS",fromDate,  toDate))
                .grievanceEscalatedL1(getGrievanceCountByStatus(tenantDb, isTenantLevel, businessId, "L1_ESCALATED",fromDate,  toDate))
                .grievanceEscalatedL2(getGrievanceCountByStatus(tenantDb, isTenantLevel, businessId, "L2_ESCALATED",fromDate,  toDate))
                .grievanceNew(getGrievanceCountByStatus(tenantDb, isTenantLevel, businessId, "NEW",fromDate,  toDate))
                .grievanceRejected(0)
                .grievanceEmail(getNotificationEventCount(tenantDb, isTenantLevel, businessId, "notification_email", GRIEVANCE_EVENTS, fromDate, toDate))
                .grievanceSms(getNotificationEventCount(tenantDb, isTenantLevel, businessId, "notification_sms", GRIEVANCE_EVENTS, fromDate, toDate))
                .resolvedSla(resolvedCount)
                .exceededSla(exceededCount)
                .build();

        LogUtil.logActivity(req, activity, "Success: Get DPO Dashboard data successfully");
        return response;
    }

    // Helper: Count depending on user type
    private int getCount(MongoTemplate mongoTemplate, String collection, boolean isTenantLevel, String businessId,LocalDate fromDate, LocalDate toDate) {
        try {
            if (mongoTemplate == null || collection == null) {
                return 0;
            }

            if (isTenantLevel) {
                return count(mongoTemplate, collection,fromDate, toDate);
            } else {
                return countByField(mongoTemplate, collection, "businessId", businessId,fromDate, toDate);
            }
        } catch (Exception e) {
            // Any error → return 0 safely
            return 0;
        }
    }

    private int count(MongoTemplate mongoTemplate, String collectionName,
                      LocalDate fromDate, LocalDate toDate) {
        try {
            Query query = new Query();
            Criteria dateRange = getDateRangeCriteria(fromDate, toDate);
            if (dateRange != null) {
                query.addCriteria(dateRange);
            }

            return (int) mongoTemplate.count(query, collectionName);
        } catch (Exception e) {
            return 0;
        }
    }

    private int countByField(MongoTemplate mongoTemplate, String collectionName, String fieldName, String fieldValue,
                             LocalDate fromDate, LocalDate toDate) {
        try {
            Query query = new Query();
            query.addCriteria(Criteria.where(fieldName).is(fieldValue));

            Criteria dateRange = getDateRangeCriteria(fromDate, toDate);
            if (dateRange != null) {
                query.addCriteria(dateRange);
            }

            return (int) mongoTemplate.count(query, collectionName);
        } catch (Exception e) {
            return 0;
        }
    }
    private int getPublishedTemplatesCount(MongoTemplate mongoTemplate, boolean isTenantLevel, String businessId,
                                           LocalDate fromDate, LocalDate toDate) {
        try {
            if (mongoTemplate == null) {
                return 0;
            }

            Query query = new Query();
            query.addCriteria(Criteria.where("status").is("PUBLISHED"));

            if (!isTenantLevel && businessId != null) {
                query.addCriteria(Criteria.where("businessId").is(businessId));
            }

            // Add date range filter
            Criteria dateRange = getDateRangeCriteria(fromDate, toDate);
            if (dateRange != null) {
                query.addCriteria(dateRange);
            }

            return (int) mongoTemplate.count(query, "templates");
        } catch (Exception e) {
            return 0;
        }
    }

    private String getRetentionFieldValue(MongoTemplate mongoTemplate,
                                          boolean isTenantLevel,
                                          String businessId,
                                          LocalDate fromDate,
                                          LocalDate toDate,
                                          String fieldName) {
        try {
            if (mongoTemplate == null) {
                return "";
            }

            Query query = new Query();

            // Filter by businessId (if applicable)
            if (!isTenantLevel && businessId != null) {
                query.addCriteria(Criteria.where("businessId").is(businessId));
            }

            // Field must exist
            query.addCriteria(Criteria.where("retentions." + fieldName).exists(true));

            // Add date range
            Criteria dateCriteria = getDateRangeCriteria(fromDate, toDate);
            if (dateCriteria != null) {
                query.addCriteria(dateCriteria);
            }

            // Fetch single retention_config document
            Document config = mongoTemplate.findOne(query, Document.class, "retention_config");
            if (config == null) {
                return "";
            }

            // Navigate into retentions -> fieldName
            Document retentions = (Document) config.get("retentions");
            if (retentions == null || !retentions.containsKey(fieldName)) {
                return "";
            }

            Document field = (Document) retentions.get(fieldName);

            Integer value = field.getInteger("value");
            String unit = field.getString("unit");

            if (value == null || unit == null) {
                return "";
            }

            return value + " " + unit; // final output

        } catch (Exception ex) {
            return "";
        }
    }

    private int getPendingRenewalCount(MongoTemplate mongoTemplate,
                                       boolean isTenantLevel,
                                       String businessId,
                                       LocalDate fromDate,
                                       LocalDate toDate) {
        try {
            if (mongoTemplate == null) {
                return 0;
            }

            Query query = new Query();

            // Status = PENDING
            query.addCriteria(Criteria.where("status").is("PENDING"));

            // Business filter only when NOT tenant level
            if (!isTenantLevel && businessId != null) {
                query.addCriteria(Criteria.where("businessId").is(businessId));
            }

            // Date range on createdAt
            Criteria dateCriteria = getDateRangeCriteria(fromDate, toDate);
            if (dateCriteria != null) {
                query.addCriteria(dateCriteria);
            }

            return (int) mongoTemplate.count(query, "consent_handles");

        } catch (Exception e) {
            return 0;
        }
    }

    private int getConsentCountByStatus(MongoTemplate mongoTemplate, boolean isTenantLevel, String businessId, String status,
                                        LocalDate fromDate, LocalDate toDate) {
        try {
            if (mongoTemplate == null) {
                return 0;
            }

            Query query = new Query();
            query.addCriteria(Criteria.where("status").is(status));
            query.addCriteria(Criteria.where("staleStatus").is("NOT_STALE"));

            if (!isTenantLevel && businessId != null) {
                query.addCriteria(Criteria.where("businessId").is(businessId));
            }

            //  Add date range filter
            Criteria dateRange = getDateRangeCriteria(fromDate, toDate);
            if (dateRange != null) {
                query.addCriteria(dateRange);
            }

            return (int) mongoTemplate.count(query, "consents");

        } catch (Exception e) {
            // Any exception → return 0
            return 0;
        }
    }

    private int getGrievanceCountByStatus(MongoTemplate mongoTemplate, boolean isTenantLevel, String businessId, String status,
                                          LocalDate fromDate, LocalDate toDate) {
        try {
            if (mongoTemplate == null) {
                return 0;
            }

            Query query = new Query();
            query.addCriteria(Criteria.where("status").is(status));

            if (!isTenantLevel && businessId != null) {
                query.addCriteria(Criteria.where("businessId").is(businessId));
            }

            // Add date range filter
            Criteria dateRange = getDateRangeCriteria(fromDate, toDate);
            if (dateRange != null) {
                query.addCriteria(dateRange);
            }

            return (int) mongoTemplate.count(query, "grievances");

        } catch (Exception e) {
            // Safe fallback on any error
            return 0;
        }
    }

    private int getNotificationEventCount(MongoTemplate mongoTemplate,
                                          boolean isTenantLevel,
                                          String businessId,
                                          String collectionName,   // notification_email / notification_sms
                                          List<String> eventTypes, // consent or grievance event types
                                          LocalDate fromDate,
                                          LocalDate toDate) {

        try {
            if (mongoTemplate == null) {
                return 0;
            }

            Query query = new Query();

            // Business filter
            if (!isTenantLevel && businessId != null) {
                query.addCriteria(Criteria.where("businessId").is(businessId));
            }

            // Date filter
            Criteria dateCriteria = getDateRange(fromDate, toDate);
            if (dateCriteria != null) {
                query.addCriteria(dateCriteria);
            }

            // Must be SENT
            query.addCriteria(Criteria.where("status").is("SENT"));

            // event_type in list
            query.addCriteria(Criteria.where("eventType").in(eventTypes));

            // Final count
            return (int) mongoTemplate.count(query, collectionName);

        } catch (Exception e) {
            return 0;
        }
    }

    private int getCookieCountByStatus(MongoTemplate mongoTemplate, boolean isTenantLevel, String businessId, String status,
                                       LocalDate fromDate, LocalDate toDate) {
        try {
            if (mongoTemplate == null) {
                return 0;
            }

            Query query = new Query();
            query.addCriteria(Criteria.where("status").is(status));

            if (!isTenantLevel && businessId != null) {
                query.addCriteria(Criteria.where("businessId").is(businessId));
            }

            // Add date range filter
            Criteria dateRange = getDateRangeCriteria(fromDate, toDate);
            if (dateRange != null) {
                query.addCriteria(dateRange);
            }

            return (int) mongoTemplate.count(query, "cookie_consent_templates");

        } catch (Exception e) {
            // Safe fallback
            return 0;
        }
    }


    private Map<String, Integer> getCookiePreferenceCounts(
            MongoTemplate mongoTemplate,
            boolean isTenantLevel,
            String businessId,
            LocalDate fromDate,
            LocalDate toDate) {

        Map<String, Integer> counts = new HashMap<>();

        int allAccepted = 0;
        int partiallyAccepted = 0;
        int expired = 0;

        Query query = new Query();

        if (!isTenantLevel) {
            query.addCriteria(Criteria.where("businessId").is(businessId));
        }

        Criteria dateRange = getDateRangeCriteria(fromDate, toDate);
        if (dateRange != null) {
            query.addCriteria(dateRange);
        }

        List<Document> consents = mongoTemplate.find(query, Document.class, "cookie_consents");

        for (Document consent : consents) {

            String status = consent.getString("status");

            //  EXPIRED → count and skip completely
            if ("EXPIRED".equalsIgnoreCase(status)) {
                expired++;
                continue;   // DO NOT check preferences
            }

            //  ACTIVE → check preferences
            if ("ACTIVE".equalsIgnoreCase(status)) {

                List<Document> prefs = (List<Document>) consent.get("preferences");

                if (prefs == null || prefs.isEmpty()) {
                    // noAction, but as per your rule -> DO NOT COUNT IT
                    continue;
                }

                long accepted = prefs.stream()
                        .filter(p -> "ACCEPTED".equalsIgnoreCase(p.getString("preferenceStatus")))
                        .count();

                if (accepted == prefs.size()) {
                    allAccepted++;
                } else if (accepted > 0) {
                    partiallyAccepted++;
                }
                // else: all rejected → IGNORE completely

            }
        }

        counts.put("cookiesAllAccepted", allAccepted);
        counts.put("cookiesPartiallyAccepted", partiallyAccepted);
        counts.put("cookieExpired", expired);

        return counts;
    }


    private Map<String, Integer> getCookieHandleCounts(MongoTemplate mongoTemplate,
                                                       boolean isTenantLevel,
                                                       String businessId,
                                                       LocalDate fromDate,
                                                       LocalDate toDate) {

        Map<String, Integer> counts = new HashMap<>();

        // Base query
        Query baseQuery = new Query();

        // Business filter
        if (!isTenantLevel && businessId != null) {
            baseQuery.addCriteria(Criteria.where("businessId").is(businessId));
        }

//         Date filter
        Criteria dateRange = getDateRangeCriteria(fromDate, toDate);
        if (dateRange != null) {
            baseQuery.addCriteria(dateRange);
        }

        // Fetch from cookie_consent_handles
        List<Document> handles = mongoTemplate.find(baseQuery, Document.class, "cookie_consent_handles");


        int noAction = 0;
        int allRejected = 0;

        for (Document handle : handles) {
            String status = (String) handle.get("status");

            if (status == null) continue;

            switch (status.toUpperCase()) {

                case "PENDING":
                    noAction++;
                    break;

                case "REJECTED":
                    allRejected++;
                    break;

                default:
                    // ignore other statuses
                    break;
            }
        }

        counts.put("cookieNoAction", noAction);
        counts.put("cookieAllRejected", allRejected);

        return counts;
    }



    public Map<String, Integer> getSlaCounts(MongoTemplate mongoTemplate, boolean isTenantLevel, String tenantId, String businessId,
                                             LocalDate fromDate, LocalDate toDate) {
        Map<String, Integer> result = new HashMap<>();
        // default zeros so we always return something safe
        result.put("totalResolved", 0);
        result.put("activeAndExceededSla", 0);

        try {
            //  Fetch SLA timeline from grievance_configurations collection
            Query configQuery = new Query();
            if (!isTenantLevel) {
                configQuery.addCriteria(Criteria.where("businessId").is(businessId));
            }
            configQuery.fields().include("configurationJson.slaTimeline").include("createdAt");

            Document configDoc = mongoTemplate.findOne(configQuery, Document.class, "grievance_configurations");

            int slaValue = 0;
            String slaUnit = "DAYS"; // Default to days
            // fallback created time - use now so that we don't accidentally treat everything as exceeded
            LocalDateTime created = LocalDateTime.now();

            if (configDoc != null) {
                Object createdDateObj = configDoc.get("createdAt");
                if (createdDateObj instanceof Date dateObj) {
                    created = dateObj.toInstant()
                            .atZone(ZoneId.systemDefault())
                            .toLocalDateTime();
                }

                Object cfgJsonObj = configDoc.get("configurationJson");
                if (cfgJsonObj instanceof Document configurationJson) {
                    Object slaTimelineObj = configurationJson.get("slaTimeline");
                    if (slaTimelineObj instanceof Document slaTimeline) {
                        Object valueObj = slaTimeline.get("value");
                        Object unitObj = slaTimeline.get("unit");
                        if (valueObj != null) {
                            try {
                                slaValue = Integer.parseInt(valueObj.toString());
                            } catch (NumberFormatException e) {
                                slaValue = 0; // default on parse error
                            }
                        }
                        if (unitObj != null) {
                            slaUnit = unitObj.toString().toUpperCase();
                        }
                    }
                }
            } else {
                // No config found -> return zeros (already placed in result)
                return result;
            }

            //  Compute the single SLA cutoff date
            LocalDateTime slaCutoff = getSlaCutoffDate(created, slaValue, slaUnit);
            if (slaCutoff == null) {
                // defensive: if cutoff computation fails, return zeros
                return result;
            }

            // Query all relevant grievances
            Query grievanceQuery = new Query();
            if (!isTenantLevel) {
                grievanceQuery.addCriteria(Criteria.where("businessId").is(businessId));
            }

            // Add date range filter
            Criteria dateRange = getDateRangeCriteria(fromDate, toDate);
            if (dateRange != null) {
                grievanceQuery.addCriteria(dateRange);
            }

            grievanceQuery.fields().include("status").include("createdAt");

            List<Document> grievances = mongoTemplate.find(grievanceQuery, Document.class, "grievances");
            if (grievances == null || grievances.isEmpty()) {
                // no grievances -> already have zeros
                return result;
            }

            int totalResolved = 0;
            int activeAndExceededSla = 0;

            List<String> openStatuses = List.of("INPROCESS", "L1_ESCALATED", "L2_ESCALATED", "NEW");

            for (Document grievance : grievances) {
                if (grievance == null) continue;

                String status = grievance.getString("status");
                Date createdAtDate = grievance.getDate("createdAt");

                if (status == null || createdAtDate == null) {
                    continue;
                }

                if ("RESOLVED".equalsIgnoreCase(status)) {
                    totalResolved++;
                } else if (openStatuses.contains(status.toUpperCase())) {
                    LocalDateTime createdAt = createdAtDate.toInstant()
                            .atZone(ZoneId.systemDefault())
                            .toLocalDateTime();

                    // If createdAt is null for some reason, skip it
                    if (createdAt == null) continue;

                    if (createdAt.isBefore(slaCutoff)) {
                        activeAndExceededSla++;
                    }
                }
            }

            result.put("totalResolved", totalResolved);
            result.put("activeAndExceededSla", activeAndExceededSla);
            return result;

        } catch (Exception ex) {
            // On any unexpected error, return zeros as required
            return result;
        }
    }

    private int getAutoRenewalCount(MongoTemplate mongoTemplate,
                                    boolean isTenantLevel,
                                    String businessId,
                                    LocalDate fromDate,
                                    LocalDate toDate) {
        try {
            if (mongoTemplate == null) {
                return 0;
            }

            Query query = new Query();

            // Business filter
            if (!isTenantLevel && businessId != null) {
                query.addCriteria(Criteria.where("businessId").is(businessId));
            }

            // Date filter on createdAt
            Criteria dateRange = getDateRangeCriteria(fromDate, toDate);
            if (dateRange != null) {
                query.addCriteria(dateRange);
            }

            query.addCriteria(Criteria.where("status").is("ACTIVE"));
            query.addCriteria(Criteria.where("staleStatus").is("NOT_STALE"));

            // Fetch only preferences for performance optimization
            query.fields().include("preferences");

            List<Document> consents = mongoTemplate.find(query, Document.class, "consents");

            int count = 0;

            for (Document consent : consents) {
                List<Document> preferences = (List<Document>) consent.get("preferences");

                if (preferences == null || preferences.isEmpty()) {
                    continue;
                }

                boolean isAutoRenewConsent = false;

                for (Document pref : preferences) {

                    // 1️⃣ autoRenew must be true
                    Boolean autoRenew = pref.getBoolean("autoRenew");
                    if (!Boolean.TRUE.equals(autoRenew)) {
                        continue;
                    }

                    // 2️⃣ startDate + endDate must be valid AND within 30 days
                    Date start = pref.getDate("startDate");
                    Date end = pref.getDate("endDate");
                    if (start == null || end == null) {
                        continue;
                    }

                    LocalDate startDate = start.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
                    LocalDate endDate = end.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();

                    long diffDays = ChronoUnit.DAYS.between(startDate, endDate);

                    if (diffDays > 30) { // Must be within 30 days?
                        continue;
                    }

                    // 3️⃣ preferenceStatus must be ACCEPTED
                    String prefStatus = pref.getString("preferenceStatus");
                    if (!"ACCEPTED".equalsIgnoreCase(prefStatus)) {
                        continue;
                    }

                    // If all 3 conditions match → count the consent once
                    isAutoRenewConsent = true;
                    break;
                }

                if (isAutoRenewConsent) {
                    count++;
                }
            }

            return count;

        } catch (Exception e) {
            return 0;
        }
    }


    // --- Helper Method ---
        private LocalDateTime getSlaCutoffDate (LocalDateTime created,int slaValue, String slaUnit){
            if (slaValue <= 0) {
                return created;
            }
            return switch (slaUnit.toUpperCase()) {
                case "HOURS" -> created.minusHours(slaValue);
                case "MONTHS" -> created.minusMonths(slaValue);
                default -> created.minusDays(slaValue); // Default to DAYS
            };
        }

    //Filter functionality
    private Criteria getDateRangeCriteria(LocalDate fromDate, LocalDate toDate) {
        if (fromDate != null && toDate != null) {
            return Criteria.where("createdAt")
                    .gte(fromDate.atStartOfDay())
                    .lte(toDate.plusDays(1).atStartOfDay());
        }
        return new Criteria(); // empty
    }

    private Criteria getDateRange(LocalDate fromDate, LocalDate toDate) {
        if (fromDate != null && toDate != null) {
            return Criteria.where("created_at")
                    .gte(fromDate.atStartOfDay())
                    .lte(toDate.plusDays(1).atStartOfDay());
        }
        return new Criteria(); // empty
    }

    /**
     * Modular function to log dpoConfig audit events
     * Can be used in both create and update dpoConfig flows
     *
     * @param dpoConfig The dpoConfig entity to audit
     * @param actionType The action type (CREATE, UPDATE)
     */
    public void logDPOConfigAudit(DPOConfig dpoConfig, ActionType actionType) {
        try {
            Actor actor = Actor.builder()
                    .id(ThreadContext.get(Constants.USER_ID_THREAD_CONTEXT))
                    .role(Constants.USER)
                    .type(Constants.USER_ID_TYPE)
                    .build();

            Resource resource = Resource.builder()
                    .type(Constants.CONFIG_ID)
                    .id(dpoConfig.getConfigId())
                    .build();

            Context context = Context.builder()
                    .ipAddress(ThreadContext.get(Constants.SOURCE_IP) != null && !ThreadContext.get(Constants.SOURCE_IP).equals("-")
                            ? ThreadContext.get(Constants.SOURCE_IP) : null)
                    .txnId(ThreadContext.get(Constants.TXN_ID_THREAD_CONTEXT) != null && !ThreadContext.get(Constants.TXN_ID_THREAD_CONTEXT).equals("-") 
                            ? ThreadContext.get(Constants.TXN_ID_THREAD_CONTEXT) : null)
                    .build();

            Map<String, Object> extra = new HashMap<>();
            // Add DPO Config POJO in the extra field under the "data" key
            extra.put(Constants.DATA, dpoConfig);

            AuditRequest auditRequest = AuditRequest.builder()
                    .actor(actor)
                    .businessId(dpoConfig.getBusinessId())
                    .group(Constants.PARTNER_PORTAL_GROUP)
                    .component(AuditComponent.DPO_CONFIG)
                    .actionType(actionType)
                    .resource(resource)
                    .initiator(Constants.DATA_FIDUCIARY)
                    .context(context)
                    .extra(extra)
                    .build();

            String tenantId = ThreadContext.get(Constants.TENANT_ID_HEADER);
            this.auditManager.logAudit(auditRequest, tenantId);
        } catch (Exception e) {
            log.error("Audit logging failed for DPO Config id: {}, action: {}",
                    dpoConfig.getConfigId(), actionType, e);
        }
    }
}