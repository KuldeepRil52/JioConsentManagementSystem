package com.jio.partnerportal.service;

import com.jio.partnerportal.client.audit.AuditManager;
import com.jio.partnerportal.client.audit.request.Actor;
import com.jio.partnerportal.client.audit.request.AuditRequest;
import com.jio.partnerportal.client.audit.request.Context;
import com.jio.partnerportal.client.audit.request.Resource;
import com.jio.partnerportal.client.notification.NotificationManager;
import com.jio.partnerportal.client.notification.request.TriggerEventRequest;
import com.jio.partnerportal.dto.ScopeLevel;
import com.jio.partnerportal.constant.Constants;
import com.jio.partnerportal.constant.ErrorCodes;
import com.jio.partnerportal.dto.ActionType;
import com.jio.partnerportal.dto.AuditComponent;
import com.jio.partnerportal.dto.IdentityType;
import com.jio.partnerportal.dto.request.TenantOtpRequest;
import com.jio.partnerportal.dto.request.UserRequest;
import com.jio.partnerportal.dto.response.ProfileResponse;
import com.jio.partnerportal.dto.response.UserResponse;
import com.jio.partnerportal.entity.*;
import com.jio.partnerportal.exception.BodyValidationException;
import com.jio.partnerportal.exception.PartnerPortalException;
import com.jio.partnerportal.multitenancy.TenantMongoTemplateProvider;

import com.jio.partnerportal.repository.LegalEntityRepository;
import com.jio.partnerportal.repository.TenantRepository;
import com.jio.partnerportal.util.*;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.SignedJWT;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.ThreadContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.web.server.ResponseStatusException;


import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static com.jio.partnerportal.util.AuthUtility.loadRSAKey;

/**
 *
 *
 * @author Kirte.Bhatt
 *
 */
@Slf4j
@Service
//@RequiredArgsConstructor
public class UserService {

    RestUtility restUtility;
    Environment env;
    TenantRepository tenantRepository;
    TenantMongoTemplateProvider tenantMongoTemplateProvider;
    LegalEntityRepository legalEntityRepository;
    AuditManager auditManager;
    Validation validation;
    NotificationManager notificationManager;

    @Value("${security.2fa.enabled:false}")
    private boolean twoFactorAuthEnabled;

    @Value("${portal.url:https://partnerportal.example.com}")
    private String portalUrl;

    @Autowired
    public UserService(RestUtility restUtility,
                       Environment env,
                       TenantRepository tenantRepository,
                       TenantMongoTemplateProvider tenantMongoTemplateProvider,
                       LegalEntityRepository legalEntityRepository,
                       AuditManager auditManager,
                       Validation validation,
                       NotificationManager notificationManager) {
        this.restUtility = restUtility;
        this.env = env;
        this.tenantMongoTemplateProvider = tenantMongoTemplateProvider;
        this.tenantRepository = tenantRepository;
        this.legalEntityRepository = legalEntityRepository;
        this.auditManager = auditManager;
        this.validation = validation;
        this.notificationManager = notificationManager;
    }

    public UserResponse createUser(String txn,
                                   String tenantId,
                                   String businessId,
                                   String sessionToken,
                                   UserRequest request, HttpServletRequest req) {


        String activity = "Create User";


        validateUserRequest(request);

        MongoTemplate tenantDb = tenantMongoTemplateProvider.getMongoTemplate(ThreadContext.get(Constants.TENANT_ID_HEADER));

        validateIdentity(request, tenantDb,true,null);

        // Validate roleId and businessId
        validateRoleAndBusinessRelations(tenantDb, request, tenantId, businessId);

        // ===== Map request roles =====
        List<User.Role> roles = request.getRoles() == null ? List.of() :
                request.getRoles().stream()
                        .map(r -> User.Role.builder()
                                .roleId(r.getRoleId())
                                .businessId(r.getBusinessId())
                                .build()
                        ).toList();

        // ===== Generate TOTP Secret =====
        String totpSecret = TotpUtils.generateSecret();
        
        // Generate QR code for TOTP (will be used in notification)
        String qrCodeBase64 = null;
        try {
            String accountName = request.getEmail() != null ? request.getEmail() : request.getMobile();
            String issuer = "JCMP_PARTNERPORTAL";
            String totpURI = TotpUtils.generateTOTPProvisioningURI(totpSecret, accountName, issuer);
            qrCodeBase64 = TotpUtils.generateQRCodeBase64(totpURI);
        } catch (Exception e) {
            log.error("Error generating QR code for TOTP", e);
        }

        // ===== Build User entity =====
        User user = User.builder()
                .userId(UUID.randomUUID().toString())
                .username(request.getUsername())
                .email(request.getEmail())
                .mobile(request.getMobile())
                .totpSecret(totpSecret)
                .designation(request.getDesignation())
                .identityType(request.getIdentityType())
                .roles(roles)
                .build();

        // ===== Save user =====
        tenantDb.save(user, Constants.USERS);

        // Send TOTP Notification if 2FA is enabled
        if (twoFactorAuthEnabled) {
            String idType;
            String idValue;

            if (StringUtils.hasText(request.getEmail())) {
                idType = "EMAIL";
                idValue = request.getEmail();
            } else {
                idType = "MOBILE";
                idValue = request.getMobile();
            }

            // ===== Fetch tenant registry to get PAN and clientId =====
            TenantRegistry registry = tenantRepository.findByTenantId(tenantId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, ErrorCodes.JCMP3045));

            String pan = registry.getPan();
            String clientId = registry.getClientId();

            try {
                IdentityType identityType = idType.equals("EMAIL") ? IdentityType.EMAIL : IdentityType.MOBILE;
                notifycreateuser(tenantId, businessId, request.getUsername(), idValue, pan, totpSecret, identityType, clientId);
            } catch (Exception e) {
                log.error("Error initiating tenant onboard notification", e);
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ErrorCodes.JCMP3043);
            }
        }

        this.logUserAudit(user, ActionType.CREATE);
        LogUtil.logActivity(req, activity, "Success: Create User successfully");

        // ===== Build response =====
        return UserResponse.builder()
                .message("User successfully created")
                .userId(user.getUserId())
                .username(user.getUsername())
                .email(user.getEmail())
                .mobile(user.getMobile())
                .totpSecret(user.getTotpSecret())
                .designation(user.getDesignation())
                .identityType(user.getIdentityType())
                .roles(user.getRoles().stream()
                        .map(r -> UserResponse.RoleResponse.builder()
                                .roleId(r.getRoleId())
                                .businessId(r.getBusinessId())
                                .build()
                        ).toList())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();

    }

    public UserResponse updateUserResponse(String txn,
                                           String tenantId,
                                           String businessId,
                                           String sessionToken,
                                           String userId,
                                           UserRequest request, HttpServletRequest req) {
        String activity = "Update User";

        if (userId == null || userId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ErrorCodes.JCMP3015);
        }

        validateUserRequest(request);

        MongoTemplate tenantDb = tenantMongoTemplateProvider.getMongoTemplate(ThreadContext.get(Constants.TENANT_ID_HEADER));

        validateIdentity(request, tenantDb, false , userId);

        // Validate roleId and businessId
        validateRoleAndBusinessRelations(tenantDb, request, tenantId, businessId);

        Query query = new Query(Criteria.where(Constants.USER_ID).is(userId));
        User user = tenantDb.findOne(query, User.class, Constants.USERS);
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, ErrorCodes.JCMP3009);
        }

        // ===== Update only provided fields =====
        if (request.getUsername() != null && !request.getUsername().isBlank()) user.setUsername(request.getUsername());
        if (request.getEmail() != null && !request.getEmail().isBlank()) user.setEmail(request.getEmail());
        if (request.getMobile() != null && !request.getMobile().isBlank()) user.setMobile(request.getMobile());
        if (request.getDesignation() != null && !request.getDesignation().isBlank())
            user.setDesignation(request.getDesignation());
        if (request.getIdentityType() != null && !request.getIdentityType().isBlank())
            user.setIdentityType(request.getIdentityType());

        if (request.getRoles() != null && !request.getRoles().isEmpty()) {
            user.setRoles(
                    request.getRoles().stream()
                            .map(r -> User.Role.builder()
                                    .roleId(r.getRoleId())
                                    .businessId(r.getBusinessId())
                                    .build()
                            ).toList()
            );
        }

        user.setUpdatedAt(LocalDateTime.now());
        tenantDb.save(user, Constants.USERS);

        this.logUserAudit(user, ActionType.UPDATE);
        LogUtil.logActivity(req, activity, "Success: Updated User successfully");
        // ===== Build response =====
        return UserResponse.builder()
                .message("User updated successfully")
                .userId(user.getUserId())
                .username(user.getUsername())
                .email(user.getEmail())
                .mobile(user.getMobile())
                .designation(user.getDesignation())
                .identityType(user.getIdentityType())
                .roles(user.getRoles().stream()
                        .map(r -> UserResponse.RoleResponse.builder()
                                .roleId(r.getRoleId())
                                .businessId(r.getBusinessId())
                                .build()
                        ).toList())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }

    public String deleteUser(String txn,
                             String tenantId,
                             String sessionToken,
                             String userId, HttpServletRequest req) throws PartnerPortalException {
        String activity = "Delete User";

        // ===== Header validation =====
        if (userId == null || userId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ErrorCodes.JCMP3015);
        }

        MongoTemplate tenantDb = tenantMongoTemplateProvider.getMongoTemplate(ThreadContext.get(Constants.TENANT_ID_HEADER));

        // ===== Check if user is the tenant onboarding user =====
        Optional<TenantRegistry> tenantRegistryOpt = tenantRepository.findByTenantId(tenantId);
        if (tenantRegistryOpt.isPresent()) {
            TenantRegistry tenantRegistry = tenantRegistryOpt.get();
            if (tenantRegistry.getOnboardingUserId() != null && tenantRegistry.getOnboardingUserId().equals(userId)) {
                throw new PartnerPortalException(ErrorCodes.JCMP3076);
            }
        }

        // ===== Search for user =====
        Query query = new Query(Criteria.where(Constants.USER_ID).is(userId));
        User user = tenantDb.findOne(query, User.class, Constants.USERS);
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, ErrorCodes.JCMP3009);
        }

        // ===== Delete user =====
        tenantDb.remove(query, User.class, Constants.USERS);
        this.logUserAudit(user, ActionType.DELETE);
        LogUtil.logActivity(req, activity, "Success:Deleted user successfully");

        return "User deleted successfully";
    }

    public UserResponse searchUser(String txn,
                                   String tenantId,
                                   String sessionToken,
                                   String userId,
                                   String email,
                                   String mobile, HttpServletRequest req) {


        String activity = "Search User";

        // ===== Header validation =====
        if (userId == null || userId.isBlank()) throw new IllegalArgumentException(ErrorCodes.JCMP3015);
        if ((email == null || email.isBlank()) && (mobile == null || mobile.isBlank()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ErrorCodes.JCMP3013);

        MongoTemplate tenantDb = tenantMongoTemplateProvider.getMongoTemplate(ThreadContext.get(Constants.TENANT_ID_HEADER));

        // ===== Find user by userId =====
        Query query = new Query(Criteria.where(Constants.USER_ID).is(userId));
        User user = tenantDb.findOne(query, User.class, Constants.USERS);
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, ErrorCodes.JCMP3009);
        }

        // ===== Validate email/mobile if provided =====
        if (email != null && !email.isBlank() && !email.equals(user.getEmail())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Incorrect email for given userId");
        }

        if (mobile != null && !mobile.isBlank() && !mobile.equals(user.getMobile())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Incorrect mobile for given userId");
        }

        LogUtil.logActivity(req, activity, "Success: Search User successfully");

        // ===== Build response =====
        return UserResponse.builder()
                .message("User found successfully")
                .userId(user.getUserId())
                .username(user.getUsername())
                .email(user.getEmail())
                .mobile(user.getMobile())
                .designation(user.getDesignation())
                .identityType(user.getIdentityType())
                .roles(user.getRoles().stream()
                        .map(r -> UserResponse.RoleResponse.builder()
                                .roleId(r.getRoleId())
                                .businessId(r.getBusinessId())
                                .build()
                        ).toList())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }

    public String countUsers(String txn,
                             String tenantId,
                             String sessionToken, HttpServletRequest req) {

        String activity = "Count User";


        MongoTemplate tenantDb = tenantMongoTemplateProvider.getMongoTemplate(ThreadContext.get(Constants.TENANT_ID_HEADER));

        // ===== Count all users =====
        long count = tenantDb.count(new Query(), Constants.USERS);

        LogUtil.logActivity(req, activity, "Success: Count user successfully");
        return count + " users found";
    }

    public List<UserResponse> listUsers(String txn,
                                        String tenantId,
                                        String sessionToken, HttpServletRequest req
    ) {
        String activity = "List User";

        MongoTemplate tenantDb = tenantMongoTemplateProvider.getMongoTemplate(ThreadContext.get(Constants.TENANT_ID_HEADER));

        // ===== Fetch all users =====
        List<User> users = tenantDb.findAll(User.class, Constants.USERS);

        LogUtil.logActivity(req, activity, "Success:List user successfully");

        // ===== Map to UserResponse =====
        return users.stream().map(user -> UserResponse.builder()
                        .message("User listed")
                        .userId(user.getUserId())
                        .username(user.getUsername())
                        .email(user.getEmail())
                        .mobile(user.getMobile())
                        .designation(user.getDesignation())
                        .identityType(user.getIdentityType())
                        .roles(user.getRoles().stream()
                                .map(r -> UserResponse.RoleResponse.builder()
                                        .roleId(r.getRoleId())
                                        .businessId(r.getBusinessId())
                                        .build()
                                ).toList())
                        .createdAt(user.getCreatedAt())
                        .updatedAt(user.getUpdatedAt())
                        .build())
                .toList();
    }


    public ResponseEntity<ProfileResponse> getProfile(String txn, String tenantId, String sessionToken, HttpServletRequest req) {
        String activity = "Get Profile";

        LogUtil.logActivity(req, activity, "Success:Get profile successfully");
        // ===== Validate headers =====
        if (!StringUtils.hasText(txn) || !StringUtils.hasText(tenantId) || !StringUtils.hasText(sessionToken)) {
            return ResponseEntity.badRequest().build();
        }

        // ===== Decode userId from JWT =====
        String userId;
        try {
            String token = sessionToken.substring(7).trim();
            SignedJWT signedJWT = SignedJWT.parse(token);
            // Extract all claims as map
            Map<String, Object> claims = signedJWT.getJWTClaimsSet().getClaims();

            // Extract userId and tenantId from claims map
            userId = claims.get("sub") != null ? claims.get("sub").toString() : null;
            ThreadContext.put("userId", userId);

            List<Map<String, Object>> errs = new ArrayList<>();
            if (ObjectUtils.isEmpty(tenantId) || !this.tenantRepository.existsByTenantId(tenantId)) {
                Map<String, Object> error = this.validation.getHeaderErrorDetails(ErrorCodes.JCMP2001, Constants.TENANT_ID_HEADER);
                errs.add(error);
            }
            if (!ObjectUtils.isEmpty(errs)) {
                throw new BodyValidationException(errs);
            }
            ThreadContext.put(Constants.TENANT_ID_HEADER, tenantId);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        MongoTemplate tenantDb = tenantMongoTemplateProvider.getMongoTemplate(ThreadContext.get(Constants.TENANT_ID_HEADER));

        //Fetch tenant registry for clientId and pan
        Optional<TenantRegistry> tenantRegistryOpt = tenantRepository.findByTenantId(tenantId);
        TenantRegistry tenantRegistry = tenantRegistryOpt.orElse(null);
        
        //Fetch legal entity for tenant name
        Query query = new Query(Criteria.where("legalEntityId").is(tenantId));
        LegalEntity entity = tenantDb.findOne(query, LegalEntity.class, "legal_entities");

        if (entity == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        // ===== Fetch user  =====
        Query userQuery = new Query(Criteria.where(Constants.USER_ID).is(userId));
        User user = tenantDb.findOne(userQuery, User.class, Constants.USERS);

        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        // ===== Collect roleIds =====
        List<String> roleIds = user.getRoles().stream()
                .map(User.Role::getRoleId)
                .toList();

        // ===== Fetch roles with permissions =====
        List<Role> roleEntities = tenantDb.find(
                Query.query(Criteria.where("roleId").in(roleIds)),
                Role.class,
                "roles"
        );

        // ===== Map roles with businessId from User =====
        List<ProfileResponse.RoleInfo> roles = user.getRoles().stream()
                .map(userRole -> {
                    Role roleEntity = roleEntities.stream()
                            .filter(r -> r.getRoleId().equals(userRole.getRoleId()))
                            .findFirst()
                            .orElse(null);

                    if (roleEntity == null) {
                        return null;
                    }

                    return ProfileResponse.RoleInfo.builder()
                            .roleId(roleEntity.getRoleId())
                            .role(roleEntity.getRole())
                            .description(roleEntity.getDescription())
                            .businessId(userRole.getBusinessId())
                            //  Added permissions mapping
                            .permissions(roleEntity.getPermissions() == null ? List.of() :
                                    roleEntity.getPermissions().stream()
                                            .map(p -> ProfileResponse.PermissionInfo.builder()
                                                    .componentId(p.getComponentId())
                                                    .componentName(p.getComponentName())
                                                    .componentUrl(p.getComponentUrl())
                                                    .displayLabel(p.getDisplayLabel())
                                                    .section(p.getSection())
                                                    .access(p.getAccess())
                                                    .build())
                                            .toList())
                            .build();
                })
                .filter(Objects::nonNull)
                .toList();

        // ===== Build Response =====
        ProfileResponse profile = ProfileResponse.builder()
                .userId(user.getUserId())
                .username(user.getUsername())
                .tenantId(entity.getLegalEntityId())
                .spocName(entity.getSpoc().getName())
                .email(user.getEmail())
                .mobile(user.getMobile())
                .totpSecret(user.getTotpSecret())
                .designation(user.getDesignation())
                .identityType(user.getIdentityType())
                .clientId(tenantRegistry != null ? tenantRegistry.getClientId() : null)
                .pan(tenantRegistry != null ? tenantRegistry.getPan() : null)
                .roles(roles)
                .build();

        return ResponseEntity.ok(profile);
    }

    public void validateUserRequest(UserRequest request) {
        // Null check
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ErrorCodes.JCMP3004);
        }

        // Username required
        if (!StringUtils.hasText(request.getUsername())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ErrorCodes.JCMP3050);
        }

        // Designation required
        if (!StringUtils.hasText(request.getDesignation())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ErrorCodes.JCMP3052);
        }

        // Roles required
        if (request.getRoles() == null || request.getRoles().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ErrorCodes.JCMP3051); // At least one role is required
        }

        for (UserRequest.RoleRequest role : request.getRoles()) {
            if (!StringUtils.hasText(role.getRoleId())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ErrorCodes.JCMP3056); // Role ID is required inside roles
            }
            if (!StringUtils.hasText(role.getBusinessId())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ErrorCodes.JCMP3057); // Business ID is required inside roles
            }
        }
    }

    public void validateIdentity(UserRequest request,
                                 MongoTemplate tenantDb,
                                 boolean isCreate,
                                 String userId) {

        if (request.getIdentityType() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ErrorCodes.JCMP3053);
        }

        String identityType = request.getIdentityType().trim().toUpperCase();

        switch (identityType) {

            case "EMAIL":
                if (!StringUtils.hasText(request.getEmail())) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ErrorCodes.JCMP3054);
                }

                Criteria emailCriteria = Criteria.where("email").is(request.getEmail());

                // For update → exclude the same user
                if (!isCreate && StringUtils.hasText(userId)) {
                    emailCriteria = emailCriteria.and("userId").ne(userId);
                }

                Query emailQuery = new Query(emailCriteria);

                if (tenantDb.exists(emailQuery, Constants.USERS)) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ErrorCodes.JCMP3048);
                }
                break;

            case "MOBILE":
                if (!StringUtils.hasText(request.getMobile())) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ErrorCodes.JCMP3055);
                }

                Criteria mobileCriteria = Criteria.where("mobile").is(request.getMobile());

                // For update → exclude same user
                if (!isCreate && StringUtils.hasText(userId)) {
                    mobileCriteria = mobileCriteria.and("userId").ne(userId);
                }

                Query mobileQuery = new Query(mobileCriteria);

                if (tenantDb.exists(mobileQuery, Constants.USERS)) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ErrorCodes.JCMP3047);
                }
                break;

            default:
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ErrorCodes.JCMP3049);
        }
    }

    private void validateRoleAndBusinessRelations(MongoTemplate tenantDb,
                                                  UserRequest request,
                                                  String tenantId,
                                                  String businessId) {

        // Step 1: Extract all roleIds and businessIds
        Set<String> roleIds = request.getRoles().stream()
                .map(UserRequest.RoleRequest::getRoleId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Set<String> businessIds = request.getRoles().stream()
                .map(UserRequest.RoleRequest::getBusinessId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        businessIds.add(businessId);

        // Step 2: Query DB once for each collection
        Set<String> existingRoleIds = tenantDb.find(
                Query.query(Criteria.where("roleId").in(roleIds)),
                Role.class,
                Constants.ROLES
        ).stream().map(Role::getRoleId).collect(Collectors.toSet());

        Set<String> existingBusinessIds = tenantDb.find(
                Query.query(Criteria.where("businessId").in(businessIds)),
                BusinessApplication.class,
                Constants.BUSINESS_APPLICATIONS
        ).stream().map(BusinessApplication::getBusinessId).collect(Collectors.toSet());

        //check if header business-id is valid
        if (!existingBusinessIds.contains(businessId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    ErrorCodes.JCMP3041);
        }

        // Step 3: Validate Existence
        for (String roleId : roleIds) {
            if (!existingRoleIds.contains(roleId)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        ErrorCodes.JCMP3038);
            }
        }

        for (String bId : businessIds) {
            if (!existingBusinessIds.contains(bId)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        ErrorCodes.JCMP3039);
            }
        }

        // Step 4: Determine user level
        boolean isTenantLevel = tenantId.equals(businessId);

        // Step 5: Validate Role + Business Rules
        for (UserRequest.RoleRequest r : request.getRoles()) {
            if (!isTenantLevel) {
                // Business-level: businessId must match header
                if (!r.getBusinessId().equals(businessId)) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            ErrorCodes.JCMP3040);
                }
            }
        }
    }

    //Totp notification
    private void notifycreateuser(String tenantId, String businessId, String username, String idvalue, String pan, String secretkey, IdentityType identityType, String clientId) {
        try {
            // Determine customer identifier value based on identity type
            String customerIdentifierValue = idvalue;
            
            if (customerIdentifierValue == null || customerIdentifierValue.isEmpty()) {
                log.warn("Customer identifier value is null or empty for identity type: {}", identityType);
                return;
            }

            // Build customer identifiers
            TriggerEventRequest.CustomerIdentifiers customerIdentifiers = TriggerEventRequest.CustomerIdentifiers.builder()
                    .type(identityType)
                    .value(customerIdentifierValue)
                    .build();

            // Build event payload (same structure as TenantService)
            Map<String, Object> eventPayload = new HashMap<>();
            eventPayload.put("accountName", pan);
            eventPayload.put("totpKey", secretkey);
            eventPayload.put("name", username);
            
            // Add portalUrl and clientId to event payload
            eventPayload.put("portalUrl", portalUrl);
            eventPayload.put("clientId", clientId);
            
            // Generate and add QR code to event payload
            try {
                String accountName = idvalue;
                String issuer = "JCMP_PARTNERPORTAL";
                String totpURI = TotpUtils.generateTOTPProvisioningURI(secretkey, accountName, issuer);
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

            // Trigger notification event using NotificationManager (same as tenant user)
            notificationManager.triggerEvent(tenantId, businessId, ScopeLevel.TENANT.toString(), triggerEventRequest);

            log.info("TOTP_PIN_DETAILS notification event triggered successfully for tenantId: {}, businessId: {}, username: {}", 
                    tenantId, businessId, username);

        } catch (Exception e) {
            log.error("Error triggering TOTP_PIN_DETAILS notification event for tenantId: {}, businessId: {}",
                    tenantId, businessId, e);
        }
    }

    /**
     * Modular function to log user audit events
     * Can be used in both create and update user flows
     *
     * @param user       The user entity to audit
     * @param actionType The action type (CREATE, UPDATE, DELETE)
     */
    public void logUserAudit(User user, ActionType actionType) {
        try {
            String tenantId = ThreadContext.get(Constants.TENANT_ID_HEADER);
            Actor actor = Actor.builder()
                    .id(user.getUserId())
                    .role(Constants.USER)
                    .type(Constants.USER_ID_TYPE)
                    .build();

            Resource resource = Resource.builder()
                    .type(Constants.USER_ID_CONSTANT)
                    .id(user.getUserId())
                    .build();

            Context context = Context.builder()
                    .ipAddress(ThreadContext.get(Constants.SOURCE_IP) != null && !ThreadContext.get(Constants.SOURCE_IP).equals("-")
                            ? ThreadContext.get(Constants.SOURCE_IP) : null)
                    .txnId(ThreadContext.get(Constants.TXN_ID_THREAD_CONTEXT) != null && !ThreadContext.get(Constants.TXN_ID_THREAD_CONTEXT).equals("-")
                            ? ThreadContext.get(Constants.TXN_ID_THREAD_CONTEXT) : null)
                    .build();

            Map<String, Object> extra = new HashMap<>();
            // Add user POJO in the extra field under the "data" key
            extra.put(Constants.DATA, user);

            AuditRequest auditRequest = AuditRequest.builder()
                    .actor(actor)
                    .businessId(tenantId)
                    .group(Constants.PARTNER_PORTAL_GROUP)
                    .component(AuditComponent.USER)
                    .actionType(actionType)
                    .resource(resource)
                    .initiator(Constants.DATA_FIDUCIARY)
                    .context(context)
                    .extra(extra)
                    .build();

            this.auditManager.logAudit(auditRequest, tenantId);
        } catch (Exception e) {
            log.error("Audit logging failed for user id: {}, action: {}",
                    user.getUserId(), actionType, e);
        }
    }
}