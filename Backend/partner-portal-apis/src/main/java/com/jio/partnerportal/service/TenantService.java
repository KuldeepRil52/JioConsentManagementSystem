package com.jio.partnerportal.service;

import com.jio.partnerportal.client.audit.AuditManager;
import com.jio.partnerportal.client.audit.request.Actor;
import com.jio.partnerportal.client.audit.request.AuditRequest;
import com.jio.partnerportal.client.audit.request.Context;
import com.jio.partnerportal.client.audit.request.Resource;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jio.partnerportal.client.notification.NotificationManager;
import com.jio.partnerportal.client.wso2.Wso2Manager;
import com.jio.partnerportal.client.wso2.request.OnboardBusinessRequest;
import com.jio.partnerportal.client.wso2.response.OnboardBusinessResponse;
import com.jio.partnerportal.client.wso2.response.RegisterTenantResponse;
import com.jio.partnerportal.constant.Constants;
import com.jio.partnerportal.constant.ErrorCodes;
import com.jio.partnerportal.dto.*;
import com.jio.partnerportal.dto.ActionType;
import com.jio.partnerportal.dto.AuditComponent;
import com.jio.partnerportal.dto.DataProcessorSpocDto;
import com.jio.partnerportal.dto.request.OnboardTenantRequest;
import com.jio.partnerportal.dto.response.OnboardTenantResponse;
import com.jio.partnerportal.dto.response.SearchResponse;
import com.jio.partnerportal.entity.*;
import com.jio.partnerportal.entity.Component;
import com.jio.partnerportal.entity.GrievanceType;
import com.jio.partnerportal.exception.PartnerPortalException;
import com.jio.partnerportal.multitenancy.TenantMongoTemplateProvider;
import com.jio.partnerportal.repository.TenantRepository;
import com.jio.partnerportal.client.notification.request.TriggerEventRequest;
import com.jio.partnerportal.util.*;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoDatabase;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.ThreadContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.*;
import java.util.List;

@Slf4j
@Service
public class TenantService {

    TenantRepository tenantRepository;
    TenantMongoTemplateProvider tenantMongoTemplateProvider;
    MongoClient mongoClient;
    Utils utils;
    MongoTemplate mongoTemplate;
    Wso2Manager wso2Manager;
    RestUtility restUtility;
    Environment environment;
    NotificationManager notificationManager;
    AuditManager auditManager;
    AuthUtility authUtility;
    ConsentSigningKeyService consentSigningKeyService;

    @Autowired
    public TenantService(TenantRepository tenantRepository,
                         TenantMongoTemplateProvider tenantMongoTemplateProvider,
                         MongoClient mongoClient,
                         Utils utils,
                         MongoTemplate mongoTemplate,
                         Wso2Manager wso2Manager,
                         RestUtility restUtility,
                         Environment environment,
                         NotificationManager notificationManager,
                         AuditManager auditManager,
                         AuthUtility authUtility,
                         ConsentSigningKeyService consentSigningKeyService) {
        this.tenantRepository = tenantRepository;
        this.tenantMongoTemplateProvider = tenantMongoTemplateProvider;
        this.mongoClient = mongoClient;
        this.utils = utils;
        this.mongoTemplate = mongoTemplate;
        this.wso2Manager = wso2Manager;
        this.restUtility = restUtility;
        this.environment = environment;
        this.notificationManager = notificationManager;
        this.auditManager = auditManager;
        this.authUtility = authUtility;
        this.consentSigningKeyService = consentSigningKeyService;
    }

    @Value("${tenants.search.parameters}")
    List<String> tenantSearchParams;

    @Value("${portal.url:https://partnerportal.example.com}")
    private String portalUrl;

    @Value("${path.to.json.file}")
    String pathToJsonFile;

    /**
     * Method to onboard a tenant.
     *
     * @param onboardTenantRequest The request object containing tenant details.
     */
    public OnboardTenantResponse onboardTenant(OnboardTenantRequest onboardTenantRequest, HttpServletRequest req ) throws Exception {
        String activity = "Onboard Tenant";

        TenantRegistry tenantAdmin = new TenantRegistry();
        String tenantId = UUID.randomUUID().toString();
        tenantAdmin.setTenantId(tenantId);
        tenantAdmin.setPan(onboardTenantRequest.getPan());
        tenantAdmin.setStatus(Status.ACTIVE);

        //generate client id
        String clientId = Utils.generateClientId(onboardTenantRequest.getPan());
        tenantAdmin.setClientId(clientId);

        //create Tenant DB
        String tenantDbName = "tenant_db_" + tenantId;
        Tenant tenant = new Tenant();
        tenant.setTenantId(tenantId);
        MongoTemplate tenantMongoTemplate = this.tenantMongoTemplateProvider.createMongoTemplate(tenantId);
        tenantMongoTemplate.save(tenant);

        // Generate wso2OnboardName: name + " " + random string (total length < 25 characters)
        String wso2OnboardName = Utils.generateWso2OnboardName(onboardTenantRequest.getCompanyName());

        //create Legal Entity
        LegalEntity legalEntity = LegalEntity.builder().legalEntityId(tenantId)
                .companyName(onboardTenantRequest.getCompanyName())
                .logoUrl(onboardTenantRequest.getLogoUrl())
                .spoc(onboardTenantRequest.getSpoc())
                .wso2OnboardName(wso2OnboardName)
                .build();

        //create businsess application
        BusinessApplication businessApplication = BusinessApplication.builder()
                .businessId(tenantId)
                .name(onboardTenantRequest.getCompanyName())
                .description("Default legal entity business.")
                .scopeLevel(ScopeLevel.TENANT.toString())
                .build();

        // Populate components from JSON file
        populateComponents(tenantMongoTemplate);

        // Query for ADMIN_LOGIN component to use in default role
        Query componentQuery = new Query(Criteria.where("componentName").is("ADMIN"));
        Component adminLoginComponent = tenantMongoTemplate.findOne(componentQuery, Component.class, Constants.COMPONENTS);

        if (adminLoginComponent == null) {
            throw new Exception("ADMIN component not found after population");
        }

        // Create role linked to component
        Role.RolePermission defaultPermission = Role.RolePermission.builder()
                .componentId(adminLoginComponent.getComponentId())
                .componentName(adminLoginComponent.getComponentName())
                .componentUrl(adminLoginComponent.getComponentUrl())
                .displayLabel(adminLoginComponent.getDisplayLabel())
                .section(adminLoginComponent.getSection())
                .access(RolesAccess.YES)
                .build();

        String roleId = UUID.randomUUID().toString();
        Role role = Role.builder()
                .roleId(roleId)
                .role(RoleType.LEGAL_ADMIN.toString())
                .description(Constants.LEGAL_ADMIN_DESCRIPTION)
                .permissions(List.of(defaultPermission)) // wrap in a list
                .build();

        // ===== Create DPO Role =====
        // Query for DPO_CONFIGURATION_SETUP component
        Query dpoComponentQuery = new Query(Criteria.where("componentName").is("DATA_PROTECTION_OFFICER"));
        Component dpoComponent = tenantMongoTemplate.findOne(dpoComponentQuery, Component.class, Constants.COMPONENTS);

        if (dpoComponent == null) {
            throw new Exception("DPO_CONFIGURATION_SETUP component not found after population");
        }

        // Create DPO role permission with all actions from the component
        Role.RolePermission dpoPermission = Role.RolePermission.builder()
                .componentId(dpoComponent.getComponentId())
                .componentName(dpoComponent.getComponentName().toString())
                .componentUrl(dpoComponent.getComponentUrl())
                .displayLabel(dpoComponent.getDisplayLabel())
                .section(dpoComponent.getSection())
                .access(RolesAccess.YES)
                .build();

        String dpoRoleId = UUID.randomUUID().toString();
        Role dpoRole = Role.builder()
                .roleId(dpoRoleId)
                .role("DPO")
                .description("Data Protection Officer Role")
                .permissions(List.of(dpoPermission))
                .build();

        User.Role userRole = User.Role.builder()
                .roleId(roleId)
                .businessId(tenantId)
                .build();

        String userId = UUID.randomUUID().toString();
        String totpSecret = TotpUtils.generateSecret();
        
        // Generate QR code for TOTP
        String qrCodeBase64 = null;
        try {
            String accountName = onboardTenantRequest.getSpoc().getEmail() != null 
                    ? onboardTenantRequest.getSpoc().getEmail() 
                    : onboardTenantRequest.getSpoc().getMobile();
            String issuer = "JCMP_PARTNERPORTAL";
            String totpURI = TotpUtils.generateTOTPProvisioningURI(totpSecret, accountName, issuer);
            qrCodeBase64 = TotpUtils.generateQRCodeBase64(totpURI);
        } catch (Exception e) {
            log.error("Error generating QR code for TOTP: {}", e.getMessage(), e);
        }
        
        User user = User.builder()
                .userId(userId)
                .username(onboardTenantRequest.getSpoc().getName())
                .totpSecret(totpSecret)
                .identityType(onboardTenantRequest.getIdentityType().toString())
                .roles(Arrays.asList(userRole))
                .build();

        if (onboardTenantRequest.getIdentityType().equals(IdentityType.EMAIL)) {
            user.setEmail(onboardTenantRequest.getSpoc().getEmail());
        } else if (onboardTenantRequest.getIdentityType().equals(IdentityType.MOBILE)) {
            user.setMobile(onboardTenantRequest.getSpoc().getMobile());
        }

        try {
            tenantMongoTemplate.save(businessApplication);
            RegisterTenantResponse registerTenantResponse = wso2Manager.registerTenant(tenantId, wso2OnboardName);

            if (registerTenantResponse == null || !registerTenantResponse.isSuccess()) {
                throw new PartnerPortalException(ErrorCodes.JCMP3032);
            }
            OnboardBusinessRequest onboardBusinessRequest = OnboardBusinessRequest.builder()
                    .tenantId(tenantId)
                    .businessId(tenantId)
                    .businessName(onboardTenantRequest.getCompanyName())
                    .build();

            OnboardBusinessResponse onboardBusinessResponse = wso2Manager.onboardBusiness(onboardBusinessRequest);

            if (onboardBusinessResponse == null || !onboardBusinessResponse.isSuccess() || onboardBusinessResponse.getData() == null) {
                throw new PartnerPortalException(ErrorCodes.JCMP3033);
            }

            ClientCredentials clientCredentials = ClientCredentials.builder()
                    .businessId(tenantId)
                    .tenantId(tenantId)
                    .businessUniqueId(onboardBusinessResponse.getData().getBusinessUniqueId())
                    .consumerKey(onboardBusinessResponse.getData().getConsumerKey())
                    .consumerSecret(onboardBusinessResponse.getData().getConsumerSecret())
                    .scopeLevel(ScopeLevel.TENANT.toString())
                    .status("ACTIVE")
                    .build();


            this.notificationManager.setupNotificationOnboarding(tenantId, tenantId, ScopeLevel.TENANT.toString());
            tenantMongoTemplate.save(clientCredentials);

            // Generate consent signing key
            try {
                ThreadContext.put(Constants.TENANT_ID_HEADER, tenantId);
                consentSigningKeyService.generateConsentSigningKey(tenantId, tenantId, ScopeLevel.TENANT.toString(), "rsa-2048", req);
            } catch (PartnerPortalException e) {
                log.error("Failed to generate consent signing key for businessId: {}, error: {}", tenantId, e.getMessage(), e);
                throw e;
            }

            tenantMongoTemplate.save(legalEntity);
            tenantMongoTemplate.save(role);
            tenantMongoTemplate.save(dpoRole); // Save DPO role
            tenantMongoTemplate.save(user);

            populatePurposes(tenantMongoTemplate, tenantId);
            populateDataTypes(tenantMongoTemplate, tenantId);
            populateDataProcessors(tenantMongoTemplate, tenantId, onboardTenantRequest.getCompanyName(), onboardTenantRequest.getSpoc(), onboardTenantRequest.getIdentityType(), clientCredentials.getConsumerKey(), clientCredentials.getConsumerSecret());
//            populateProcessorActivities(tenantMongoTemplate, tenantId);
            populateUserDetails(tenantMongoTemplate, tenantId);
            populateUserTypes(tenantMongoTemplate, tenantId);
            populateGrievanceTypes(tenantMongoTemplate, tenantId);
            populateCookieCategories(tenantMongoTemplate, tenantId);
            tenantAdmin.setOnboardingUserId(userId);
            tenantRepository.save(tenantAdmin);
        } catch (Exception e) {
            log.error("Error during tenant onboarding: {}", e.getMessage(), e);
            MongoDatabase tenantDatabase = mongoClient.getDatabase(tenantDbName);
            tenantDatabase.drop();
            throw new Exception(e);
        }

        String xSessionToken = authUtility.generateToken(userId, tenantId);

        OnboardTenantResponse response = OnboardTenantResponse.builder()
                .tenantId(tenantId)
                .clientId(clientId)
                .message("Tenant onboarded successfully.")
                .userDetails(UserDto.builder().userId(userId).build())
                .xSessionToken("Bearer " + xSessionToken)
                .build();

        try {
            initiateTenantOnboardNotification(onboardTenantRequest, tenantId, clientId);
            notifyCreateUser(tenantId, onboardTenantRequest.getSpoc().getName(), 
                    onboardTenantRequest.getSpoc().getEmail(), onboardTenantRequest.getSpoc().getMobile(), 
                    onboardTenantRequest.getPan(), totpSecret, onboardTenantRequest.getIdentityType(), clientId);
        } catch (Exception e) {
            log.error("Error initiating tenant onboard notification: {}", e.getMessage(), e);
        }

        this.logTenantAudit(tenantAdmin, ActionType.ONBOARD);
        LogUtil.logActivity(req, activity, "Success: Onboard Tenant successfully");

        return response;
    }

    private void populatePurposes(MongoTemplate mongoTemplate, String tenantId) throws IOException {
        ObjectMapper mapper = new ObjectMapper();

        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream("purposes.json")) {
            if (inputStream == null) {
                throw new IOException("Resource not found: purposes.json");
            }

            List<Purpose> purposes = mapper.readValue(inputStream, new TypeReference<List<Purpose>>() {
            });

            for (Purpose purpose : purposes) {
                purpose.setPurposeId(UUID.randomUUID().toString());
                purpose.setBusinessId(tenantId);
                mongoTemplate.save(purpose);
            }
        }
    }

    private void populateDataTypes(MongoTemplate mongoTemplate, String tenantId) throws IOException {
        ObjectMapper mapper = new ObjectMapper();

        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream("data-types.json")) {
            if (inputStream == null) {
                throw new IOException("Resource not found: data-types.json");
            }

            List<DataType> dataTypes = mapper.readValue(inputStream, new TypeReference<List<DataType>>() {
            });

            for (DataType dataType : dataTypes) {
                dataType.setBusinessId(tenantId);
                mongoTemplate.save(dataType);
            }
        }
    }

    private void populateDataProcessors(MongoTemplate mongoTemplate, String tenantId, String tenantName, SpocDto tenantSpoc, IdentityType identityType, String consumerKey, String consumerSecret) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream("data-processors.json")) {
            if (inputStream == null) {
                throw new IOException("Resource not found: data-processors.json");
            }

            List<DataProcessor> dataProcessors = mapper.readValue(inputStream, new TypeReference<List<DataProcessor>>() {
            });

            for (DataProcessor dataProcessor : dataProcessors) {
                // Create DataProcessorSpocDto with only name and email (no mobile)
                DataProcessorSpocDto dataProcessorSpoc = new DataProcessorSpocDto();
                dataProcessorSpoc.setName(tenantSpoc.getName());

                // Set email only if identity type is EMAIL
                if (IdentityType.EMAIL.equals(identityType)) {
                    dataProcessorSpoc.setEmail(tenantSpoc.getEmail());
                }

                dataProcessor.setBusinessId(tenantId);
                dataProcessor.setDataProcessorName(tenantName);
                dataProcessor.setSpoc(dataProcessorSpoc);
                dataProcessor.setIdentityType(identityType);
                dataProcessor.setConsumerKey(consumerKey);
                dataProcessor.setConsumerSecret(consumerSecret);
                mongoTemplate.save(dataProcessor);
            }
        }
    }

    private void populateProcessorActivities(MongoTemplate mongoTemplate, String tenantId) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream("processor-activities.json")) {
            if (inputStream == null) {
                throw new IOException("Resource not found: processor-activities.json");
            }

            List<ProcessorActivity> processorActivities = mapper.readValue(inputStream, new TypeReference<List<ProcessorActivity>>() {
            });

            for (ProcessorActivity processorActivity : processorActivities) {
                processorActivity.setBusinessId(tenantId);
                mongoTemplate.save(processorActivity);
            }
        }
    }

    private void populateComponents(MongoTemplate mongoTemplate) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream("components.json")) {
            if (inputStream == null) {
                throw new IOException("Resource not found: components.json");
            }

            List<Component> components = mapper.readValue(inputStream, new TypeReference<List<Component>>() {
            });

            for (Component component : components) {
                mongoTemplate.save(component);
            }
        }
    }

    private void populateUserDetails(MongoTemplate mongoTemplate, String tenantId) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream("user-details.json")) {
            if (inputStream == null) {
                throw new IOException("Resource not found: user-details.json");
            }

            List<UserDetail> userDetails = mapper.readValue(inputStream, new TypeReference<List<UserDetail>>() {
            });

            for (UserDetail userDetail : userDetails) {
                userDetail.setUserDetailId(UUID.randomUUID().toString());
                userDetail.setBusinessId(tenantId);
                userDetail.setScope(ScopeLevel.TENANT);
                mongoTemplate.save(userDetail);
            }
        }
    }

    private void populateUserTypes(MongoTemplate mongoTemplate, String tenantId) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream("user-types.json")) {
            if (inputStream == null) {
                throw new IOException("Resource not found: user-types.json");
            }

            List<UserType> userTypes = mapper.readValue(inputStream, new TypeReference<List<UserType>>() {
            });

            for (UserType userType : userTypes) {
                userType.setUserTypeId(UUID.randomUUID().toString());
                userType.setBusinessId(tenantId);
                userType.setScope(ScopeLevel.TENANT);
                mongoTemplate.save(userType);
            }
        }
    }

    private void populateGrievanceTypes(MongoTemplate mongoTemplate, String tenantId) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream("grievance-types.json")) {
            if (inputStream == null) {
                throw new IOException("Resource not found: grievance-types.json");
            }

            List<GrievanceType> grievanceTypes = mapper.readValue(inputStream, new TypeReference<List<GrievanceType>>() {
            });

            for (GrievanceType grievanceType : grievanceTypes) {
                grievanceType.setGrievanceTypeId(UUID.randomUUID().toString());
                grievanceType.setBusinessId(tenantId);
                grievanceType.setScope(ScopeLevel.TENANT);
                mongoTemplate.save(grievanceType);
            }
        }
    }

    private void populateCookieCategories(MongoTemplate mongoTemplate, String tenantId) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream("cookie-categories.json")) {
            if (inputStream == null) {
                throw new IOException("Resource not found: cookie-categories.json");
            }

            List<CookieCategory> cookieCategories = mapper.readValue(inputStream, new TypeReference<List<CookieCategory>>() {
            });

            for (CookieCategory cookieCategory : cookieCategories) {
                cookieCategory.setCategoryId(UUID.randomUUID().toString());
                cookieCategory.setBusinessId(tenantId);
                cookieCategory.setScope(ScopeLevel.TENANT);
                mongoTemplate.save(cookieCategory);
            }
        }
    }

    public SearchResponse<TenantRegistry> search(Map<String, String> reqParams, HttpServletRequest req) throws PartnerPortalException {
        String activity = "Search Tenant";

        Map<String, String> searchParams = this.utils.filterRequestParam(reqParams, tenantSearchParams);
        Criteria criteria = new Criteria();
        for (Map.Entry<String, String> entry : searchParams.entrySet()) {
            criteria.and(entry.getKey()).is(entry.getValue());
        }

        Query query = new Query(criteria);
        query.fields().exclude("_id");
        List<TenantRegistry> mongoResponse = this.mongoTemplate.find(query, TenantRegistry.class, "tenant_registry");

        if (ObjectUtils.isEmpty(mongoResponse)) {
            throw new PartnerPortalException(ErrorCodes.JCMP3001);
        }

        LogUtil.logActivity(req, activity, "Success: Search Tenant successfully");
        return SearchResponse.<TenantRegistry>builder()
                .searchList(mongoResponse)
                .build();
    }

    public long count() {
        return this.tenantRepository.count();
    }

    private void initiateTenantOnboardNotification(OnboardTenantRequest onboardTenantRequest, String tenantId, String clientId) {
        try {
            // Determine customer identifier value based on identity type
            String customerIdentifierValue = null;
            if (onboardTenantRequest.getIdentityType().equals(IdentityType.EMAIL)) {
                customerIdentifierValue = onboardTenantRequest.getSpoc().getEmail();
                log.info("Sending TENANT_ONBOARDED notification to email: {}", customerIdentifierValue);
            } else if (onboardTenantRequest.getIdentityType().equals(IdentityType.MOBILE)) {
                customerIdentifierValue = onboardTenantRequest.getSpoc().getMobile();
                log.info("Sending TENANT_ONBOARDED notification to mobile: {}", customerIdentifierValue);
            } else {
                log.warn("Unknown identity type: {}", onboardTenantRequest.getIdentityType());
                return;
            }

            if (customerIdentifierValue == null || customerIdentifierValue.isEmpty()) {
                log.warn("Customer identifier value is null or empty for identity type: {}", onboardTenantRequest.getIdentityType());
                return;
            }

            // Build customer identifiers
            TriggerEventRequest.CustomerIdentifiers customerIdentifiers = TriggerEventRequest.CustomerIdentifiers.builder()
                    .type(onboardTenantRequest.getIdentityType())
                    .value(customerIdentifierValue)
                    .build();

            // Build event payload
            Map<String, Object> eventPayload = new HashMap<>();
            eventPayload.put("clientId", clientId);
            eventPayload.put("organizationPan", onboardTenantRequest.getPan());
            eventPayload.put("name", onboardTenantRequest.getSpoc().getName());
            eventPayload.put("businessName", onboardTenantRequest.getCompanyName());

            // Build trigger event request
            TriggerEventRequest triggerEventRequest = TriggerEventRequest.builder()
                    .eventType("ORGANIZATION_ONBOARDING")
                    .customerIdentifiers(customerIdentifiers)
                    .eventPayload(eventPayload)
                    .build();

            // Trigger notification event using NotificationManager (Central - replaces NotificationEvent)
            notificationManager.triggerEventCentral(tenantId, tenantId, clientId, ScopeLevel.TENANT.toString(), triggerEventRequest);

            log.info("TENANT_ONBOARDED notification event triggered successfully for tenantId: {}, clientId: {}", tenantId, clientId);

        } catch (Exception e) {
            log.error("Error triggering TENANT_ONBOARDED notification event for tenantId: {}, clientId: {}. Error: {}",
                    tenantId, clientId, e.getMessage(), e);
        }
    }

    private void notifyCreateUser(String tenantId, String spocName, String email, String mobile, String pan, String secretKey, IdentityType identityType, String clientId) {
        try {
            // Determine customer identifier value based on identity type
            String customerIdentifierValue = null;
            if (identityType != null && identityType.equals(IdentityType.EMAIL) && StringUtils.hasText(email)) {
                customerIdentifierValue = email;
                log.info("Sending TOTP_PIN_DETAILS notification to email: {}", customerIdentifierValue);
            } else if (identityType != null && identityType.equals(IdentityType.MOBILE) && StringUtils.hasText(mobile)) {
                customerIdentifierValue = mobile;
                log.info("Sending TOTP_PIN_DETAILS notification to mobile: {}", customerIdentifierValue);
            } else {
                log.warn("No valid identity provided for TOTP notification. IdentityType: {}, Email: {}, Mobile: {}", 
                        identityType, email, mobile);
                return;
            }

            if (customerIdentifierValue == null || customerIdentifierValue.isEmpty()) {
                log.warn("Customer identifier value is null or empty for identity type: {}", identityType);
                return;
            }

            // Build customer identifiers
            TriggerEventRequest.CustomerIdentifiers customerIdentifiers = TriggerEventRequest.CustomerIdentifiers.builder()
                    .type(identityType)
                    .value(customerIdentifierValue)
                    .build();

            // Build event payload
            Map<String, Object> eventPayload = new HashMap<>();
            eventPayload.put("accountName", pan);
            eventPayload.put("totpKey", secretKey);
            eventPayload.put("name", spocName);
            
            // Add portalUrl and clientId to event payload
            eventPayload.put("portalUrl", portalUrl);
            eventPayload.put("clientId", clientId);

            // Generate and add QR code to event payload
            try {
                String accountName = email != null ? email : mobile;
                String issuer = "JCMP_PARTNERPORTAL";
                String totpURI = TotpUtils.generateTOTPProvisioningURI(secretKey, accountName, issuer);
                String qrCodeBase64 = TotpUtils.generateQRCodeBase64(totpURI);
                eventPayload.put("qrData", qrCodeBase64);
            } catch (Exception e) {
                log.error("Error generating QR code for TOTP notification", e);
            }

            // Build trigger event request
            TriggerEventRequest triggerEventRequest = TriggerEventRequest.builder()
                    .eventType("TOTP_PIN_DETAILS")
                    .customerIdentifiers(customerIdentifiers)
                    .eventPayload(eventPayload)
                    .build();

            // Trigger notification event using NotificationManager (Central - replaces NotificationEvent)
            notificationManager.triggerEventCentral(tenantId, tenantId, clientId, ScopeLevel.TENANT.toString(), triggerEventRequest);

            log.info("TOTP_PIN_DETAILS notification event triggered successfully for tenantId: {}, SPOC: {}", tenantId, spocName);

        } catch (Exception e) {
            log.error("Error triggering TOTP_PIN_DETAILS notification event for tenantId: {}",
                    tenantId, e);
        }
    }

    /**
     * Modular function to log onboardTenant audit events
     * Can be used in both create and update onboardTenant flows
     *
     * @param tenantRegistry The tenantRegistry entity to audit
     * @param actionType The action type (ONBOARD)
     */
    public void logTenantAudit(TenantRegistry tenantRegistry, ActionType actionType) {
        try {
            Actor actor = Actor.builder()
                    .id(tenantRegistry.getTenantId())
                    .role(Constants.USER)
                    .type(Constants.USER_ID_TYPE)
                    .build();

            Resource resource = Resource.builder()
                    .type(Constants.TENANT_ID)
                    .id(tenantRegistry.getTenantId())
                    .build();

            Context context = Context.builder()
                    .ipAddress(ThreadContext.get(Constants.SOURCE_IP) != null && !ThreadContext.get(Constants.SOURCE_IP).equals("-")
                            ? ThreadContext.get(Constants.SOURCE_IP) : null)
                    .txnId(ThreadContext.get(Constants.TXN_ID_THREAD_CONTEXT) != null && !ThreadContext.get(Constants.TXN_ID_THREAD_CONTEXT).equals("-") 
                            ? ThreadContext.get(Constants.TXN_ID_THREAD_CONTEXT) : null)
                    .build();

            Map<String, Object> extra = new HashMap<>();
            // Add tenantRegistry POJO in the extra field under the "data" key
            extra.put(Constants.DATA, tenantRegistry);

            AuditRequest auditRequest = AuditRequest.builder()
                    .actor(actor)
                    .businessId(tenantRegistry.getTenantId())
                    .group(Constants.PARTNER_PORTAL_GROUP)
                    .component(AuditComponent.TENANT)
                    .actionType(actionType)
                    .resource(resource)
                    .initiator(Constants.DATA_FIDUCIARY)
                    .context(context)
                    .extra(extra)
                    .build();

            this.auditManager.logAudit(auditRequest, tenantRegistry.getTenantId());
        } catch (Exception e) {
            log.error("Audit logging failed for tenant id: {}, action: {}",
                    tenantRegistry.getTenantId(), actionType, e);
        }
    }
}
