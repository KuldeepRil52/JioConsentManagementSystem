package com.jio.partnerportal.service;

import com.jio.partnerportal.client.audit.AuditManager;
import com.jio.partnerportal.client.audit.request.Actor;
import com.jio.partnerportal.client.audit.request.AuditRequest;
import com.jio.partnerportal.client.audit.request.Context;
import com.jio.partnerportal.client.audit.request.Resource;
import com.jio.partnerportal.client.notification.NotificationApiManager;
import com.jio.partnerportal.client.notification.request.TriggerEventRequest;
import com.jio.partnerportal.client.notification.request.VerifyOtpRequest;
import com.jio.partnerportal.client.notification.response.SystemTriggerEventResponse;
import com.jio.partnerportal.client.notification.response.VerifyOtpResponse;
import com.jio.partnerportal.constant.ErrorCodes;
import com.jio.partnerportal.dto.ActionType;
import com.jio.partnerportal.dto.AuditComponent;
import com.jio.partnerportal.dto.request.*;
import com.jio.partnerportal.dto.response.*;
import com.jio.partnerportal.util.AuthUtility;
import com.jio.partnerportal.util.LogUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.logging.log4j.ThreadContext;
import com.jio.partnerportal.dto.IdentityType;
import com.jio.partnerportal.entity.AuthSecret;
import com.jio.partnerportal.entity.Otp;
import com.jio.partnerportal.entity.TenantRegistry;
import com.jio.partnerportal.multitenancy.TenantMongoTemplateProvider;
import com.jio.partnerportal.repository.AuthSecretRepository;
import com.jio.partnerportal.repository.OtpRepository;
import com.jio.partnerportal.repository.TenantRepository;
import com.jio.partnerportal.entity.User;
import com.jio.partnerportal.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import com.jio.partnerportal.constant.Constants;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;
import java.time.Instant;
import java.util.*;
import java.util.UUID;

import static com.jio.partnerportal.util.TotpUtils.verifyTOTP;

@Slf4j
@Service
public class AuthService {
    OtpRepository repository;
    TenantRepository tenantRepository;
    TenantMongoTemplateProvider tenantMongoTemplateProvider;
    UserRepository userRepository;
    Environment env;
    AuthSecretRepository authSecretRepository;

    NotificationApiManager notificationApiManager;
    AuthUtility authUtility;
    AuditManager auditManager;

    @Value("${max_retry}")
    private int maxRetry;

    @Value("${security.2fa.enabled:false}")
    private boolean twoFactorAuthEnabled;

    public static final String TEN_DIGIT_MOBILE = "^\\d{10}$";
    public static final String EMAIL_REGEX = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";

    private static final String PAN_REGEX = "^[A-Z]{5}[0-9]{4}[A-Z]{1}$";
    private static final String CLIENT_ID_REGEX = "^CLT(?:[0-9]{2})(?:0[1-9]|1[0-2])[A-Z0-9]{4}[A-F0-9]$";

    @Autowired
    public AuthService(OtpRepository repository, TenantRepository tenantRegistryRepository,
                       TenantMongoTemplateProvider tenantMongoTemplateProvider, UserRepository userRepository, Environment env, NotificationApiManager notificationApiManager, AuthUtility authUtility, AuditManager auditManager, AuthSecretRepository authSecretRepository) {
        this.repository = repository;
        this.tenantRepository = tenantRegistryRepository;
        this.tenantMongoTemplateProvider = tenantMongoTemplateProvider;
        this.userRepository = userRepository;
        this.env = env;
        this.notificationApiManager = notificationApiManager;
        this.authUtility = authUtility;
        this.auditManager = auditManager;
        this.authSecretRepository = authSecretRepository;
    }

    //Registration OTP init
    public ResponseEntity<OtpInitResponse> initiateOtp(OtpInitRequest request, String txnId, HttpServletRequest req) {

        String activity = "OTP Initiation";
        // ===== Validations =====
        validateRequest(request);

        // ===== Send OTP via notification service =====
        SystemTriggerEventResponse res = sendOtpViaNotificationService(request.getIdValue(), request.getIdType(), txnId);

        // ===== Save or update OTP entity (inline) =====
        Otp otpEntity = repository.findByIdValue(request.getIdValue())
                .orElseGet(() -> {
                    Otp otp = new Otp();
                    otp.setIdValue(request.getIdValue());
                    otp.setIdType(request.getIdType());
                    return otp;
                });

        // update mutable fields
        otpEntity.setTxnId(txnId);
        otpEntity.setOtpTxnId(res.getTransactionId());
        otpEntity.setEventId(res.getEventId());
        otpEntity.setCreatedAt(Instant.now());

        repository.save(otpEntity);

        LogUtil.logActivity(req, activity, "Success: OTP sent successfully");

        return buildSuccess(res.getTransactionId(), "OTP sent successfully");
    }

    //Helper functions for Registration OTP init
    private void validateRequest(OtpInitRequest request) {
        if (!StringUtils.hasText(request.getIdValue()) && request.getIdType() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ErrorCodes.JCMP3031);}
        if (!StringUtils.hasText(request.getIdValue())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ErrorCodes.JCMP3005);
        }
        if (request.getIdType() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ErrorCodes.JCMP3006);
        }
        if (request.getIdType() == IdentityType.MOBILE && !request.getIdValue().matches(TEN_DIGIT_MOBILE)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ErrorCodes.JCMP3007);
        }
        if (request.getIdType() == IdentityType.EMAIL && !request.getIdValue().matches(EMAIL_REGEX)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ErrorCodes.JCMP3008);
        }
    }

    private ResponseEntity<OtpInitResponse> buildSuccess(String txnId, String message) {
        HttpHeaders headers = new HttpHeaders();
        headers.add(Constants.TXN, txnId);
        return ResponseEntity.status(HttpStatus.OK).headers(headers).body(new OtpInitResponse(txnId, message));
    }

    //Registration OTP validate
    public ResponseEntity<OtpValidateResponse> validateOtp(OtpValidateRequest request, String txnId,HttpServletRequest req) {
        String activity = "OTP Validation";
        // ===== Validate incoming request =====
        validateOtpRequest(request);

        // ===== Fetch OTP record from DB =====
        Optional<Otp> optEntity = repository.findByOtpTxnIdAndIdValue(request.getTxnId(), request.getIdValue());
        if (optEntity.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    ErrorCodes.JCMP3010);
        }

        Otp entity = optEntity.get();

        // ===== Retry limit check =====
        if (entity.getRetryCount() >= maxRetry) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                    ErrorCodes.JCMP3011);
        }

        // ===== Call external validation API =====
        ValidateOtpRes validateRes = validateAndHandleOtp(request.getOtp(), request.getTxnId(), entity.getEventId());

        // ===== OTP Failure =====
        if (!Constants.VALIDATED.equalsIgnoreCase(validateRes.getStatus())) {
            entity.setTxnId(txnId);
            entity.setRetryCount(entity.getRetryCount() + 1);
            entity.setValidatedAt(Instant.now());
            repository.save(entity);

            return buildValidateError(validateRes.getMessage(),
                    entity.getRetryCount(), HttpStatus.BAD_REQUEST);
        }
        // ===== OTP Success =====
        repository.delete(entity);
        
        // ===== Create and save AuthSecret =====
        String secretCode = UUID.randomUUID().toString();
        AuthSecret authSecret = new AuthSecret();
        authSecret.setSecretCode(secretCode);
        authSecret.setIdentityValue(request.getIdValue());
        authSecret.setTxnId(request.getTxnId());
        authSecretRepository.save(authSecret);
        
        logAuthAudit(request, ActionType.ONBOARD, request.getTxnId());
        LogUtil.logActivity(req, activity, "Success: OTP validated successfully");
        return buildValidateSuccess("OTP validated successfully", 0, secretCode);
    }

    //Helper functions for Registration OTP validate
    private void validateOtpRequest(OtpValidateRequest request) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ErrorCodes.JCMP3004);
        }

        if ((request.getTxnId() == null || request.getTxnId().isBlank()) &&
                (request.getOtp() == null || request.getOtp().isBlank()) &&
                (request.getIdValue() == null || request.getIdValue().isBlank())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ErrorCodes.JCMP3014);}

        if (request.getOtp() == null || request.getOtp().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ErrorCodes.JCMP3016);
        }
        if (request.getTxnId() == null || request.getTxnId().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ErrorCodes.JCMP3017);
        }
        if (request.getIdValue() == null || request.getIdValue().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ErrorCodes.JCMP3005);
        }
    }

    private ResponseEntity<OtpValidateResponse> buildValidateError(String message, int retryCount, HttpStatus status) {
        OtpValidateResponse response = new OtpValidateResponse();
        response.setStatus(message);
        response.setRetryCount(retryCount);
        return ResponseEntity.status(status).body(response);
    }

    private ResponseEntity<OtpValidateResponse> buildValidateSuccess(String message, int retryCount, String secretCode) {
        OtpValidateResponse response = new OtpValidateResponse();
        response.setStatus(message);
        response.setRetryCount(retryCount);
        response.setSecretCode(secretCode);
        return ResponseEntity.ok(response);
    }

    //Tenant Login OTP init
    public ResponseEntity<TenantOtpResponse> tenantInitOtp(String txnId, TenantOtpRequest request,HttpServletRequest req) {
        String activity = "Tenant OTP Initiation";
        // ===== Validate request =====
        validateTenantOtpRequest(request);

        String pan = request.getPan().trim();
        IdentityType idType = request.getIdType();
        String idValue= request.getIdValue();

        // ===== Detect PAN or Client ID =====
        IdentityValueType valueType = detectPanOrClientId(pan);

        // ===== Find tenant registry =====
        TenantRegistry registry = switch (valueType) {
            case PAN -> tenantRepository.findByPan(pan)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, ErrorCodes.JCMP3018));
            case CLIENT_ID -> tenantRepository.findByClientId(pan)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, ErrorCodes.JCMP3018));
        };

        String tenantId = registry.getTenantId();
        MongoTemplate tenantDb = tenantMongoTemplateProvider.getMongoTemplate(tenantId);

        // ===== Verify user exists =====
        User user = switch (idType) {
            case EMAIL -> tenantDb.findOne(Query.query(Criteria.where("email").is(idValue)), User.class, Constants.USERS);
            case MOBILE -> tenantDb.findOne(Query.query(Criteria.where("mobile").is(idValue)), User.class, Constants.USERS);
        };

        if (user == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, ErrorCodes.JCMP3009);
        }

        // ===== Send OTP via notification service =====
        SystemTriggerEventResponse sendRes = sendOtpViaNotificationService(idValue, idType, txnId);
        String otpTxnId = sendRes.getTransactionId();

        // ===== Save/Update OTP metadata =====
        Otp otpEntity = tenantDb.findOne(
                Query.query(Criteria.where("idValue").is(idValue)), Otp.class, "otp");

        if (otpEntity == null) {
            otpEntity = new Otp();
            otpEntity.setIdValue(idValue);
            otpEntity.setIdType(idType);
        }

        otpEntity.setTxnId(txnId);
        otpEntity.setOtpTxnId(otpTxnId);
        otpEntity.setEventId(sendRes.getEventId());
        otpEntity.setCreatedAt(Instant.now());
        tenantDb.save(otpEntity, "otp");

        // ===== Build Response =====
        TenantOtpResponse response = new TenantOtpResponse();
        response.setTxnId(otpTxnId);
        response.setMessage("OTP sent successfully");
        response.setTwoFactorEnabled(twoFactorAuthEnabled ? "true" : "false");

        LogUtil.logActivity(req, activity, "Success: Tenant OTP sent successfully");

        return ResponseEntity.ok(response);
    }

    //Helper functions for Tenant Login OTP init
    private void validateTenantOtpRequest(TenantOtpRequest request) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ErrorCodes.JCMP3004);
        }

        validateRequiredFields(request);
        validateIdTypeAndFormat(request);
    }

    private void validateRequiredFields(TenantOtpRequest request) {
        if ((request.getPan() == null || request.getPan().isBlank()) &&
                (request.getIdValue() == null || request.getIdValue().isBlank()) &&
                request.getIdType() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    ErrorCodes.JCMP3019);
        }
        if (request.getPan() == null || request.getPan().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ErrorCodes.JCMP3020);
        }
        if (request.getIdValue() == null || request.getIdValue().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ErrorCodes.JCMP3005);
        }
        if (request.getIdType() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ErrorCodes.JCMP3006);
        }
        if (!request.getPan().toUpperCase().matches(PAN_REGEX) &&
                !request.getPan().toUpperCase().matches(CLIENT_ID_REGEX)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ErrorCodes.JCMP3021);
        }
    }

    private void validateIdTypeAndFormat(TenantOtpRequest request) {
        if (request.getIdType() == IdentityType.MOBILE &&
                !request.getIdValue().matches(TEN_DIGIT_MOBILE)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ErrorCodes.JCMP3007);}

        if (request.getIdType() == IdentityType.EMAIL &&
                !request.getIdValue().matches(EMAIL_REGEX)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    ErrorCodes.JCMP3008);
        }
    }

    //Tenant Login OTP validate
    public ResponseEntity<TenantValidateOtpResponse> tenantValidateOtp(String headerTxnId, TenantValidateOtpRequest request,HttpServletRequest req) throws Exception {
        String activity = "Tenant OTP Validation ";
        // ===== Validate request body =====
        validateTenantValidateOtpRequest(request);

        String pan = request.getPan().trim();
        String txnId = request.getTxnId().trim();
        String idValue = request.getIdValue().trim();
        IdentityType idType = request.getIdType();

        // ===== Detect PAN or Client ID =====
        IdentityValueType valueType = detectPanOrClientId(pan);

        // ===== Find tenant registry =====
        TenantRegistry registry = switch (valueType) {
            case PAN -> tenantRepository.findByPan(pan)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, ErrorCodes.JCMP3018));
            case CLIENT_ID -> tenantRepository.findByClientId(pan)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, ErrorCodes.JCMP3018));
        };

        String tenantId = registry.getTenantId();
        MongoTemplate tenantDb = tenantMongoTemplateProvider.getMongoTemplate(tenantId);

        // ===== Fetch user info =====
        User user = tenantDb.findOne(
                Query.query(Criteria.where(idType.name().toLowerCase()).is(idValue)),
                User.class,
                Constants.USERS
        );

        if (user == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, ErrorCodes.JCMP3009);
        }

        // ===== Fetch OTP entry =====
        Otp otpEntity = tenantDb.findOne(
                Query.query(Criteria.where("idValue").is(idValue)),
                Otp.class,
                "otp"
        );

        if (otpEntity == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ErrorCodes.JCMP3022);
        }

        // ===== Check txn match =====
        if (!txnId.equals(otpEntity.getOtpTxnId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ErrorCodes.JCMP3023);
        }

        // ===== Check retry count BEFORE validation =====
        if (otpEntity.getRetryCount() >= 3) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, ErrorCodes.JCMP3011);
        }

//         ===== Local TOTP Validation ONLY if 2FA is enabled =====
        if (twoFactorAuthEnabled) {
            String secret = user.getTotpSecret();
            String totp = request.getTotp();

            if (secret == null || secret.isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ErrorCodes.JCMP3042);
            }

            // ===== Check if TOTP is provided in the request =====
            if (totp == null || totp.isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ErrorCodes.JCMP3044);
            }

            boolean isValid = verifyTOTP(secret, totp, 1); // ±30s window

            if (!isValid) {
                // ===== Invalid TOTP → increment retry count =====
                int retry = otpEntity.getRetryCount() + 1;
                otpEntity.setRetryCount(retry);
                otpEntity.setTxnId(headerTxnId);
                tenantDb.save(otpEntity, "otp");

                return buildErrorResponse("Invalid or expired TOTP", HttpStatus.BAD_REQUEST, retry);
            }

       }

        // ===== Validate OTP with external service =====
        ValidateOtpRes validateRes = validateAndHandleOtp(request.getOtp(), txnId, otpEntity.getEventId());

        if (Constants.VALIDATED.equalsIgnoreCase(validateRes.getStatus())) {
            tenantDb.remove(otpEntity, "otp");
            String xSessionToken = authUtility.generateToken(user.getUserId(),tenantId);
            ThreadContext.put("X-SESSION-TOKEN",Constants.BEARER  + xSessionToken);
            ThreadContext.put("tenant-id", tenantId);

            // ===== Build success response =====
            TenantValidateOtpResponse.UserDetails userDetails = new TenantValidateOtpResponse.UserDetails();
            userDetails.setUserId(user.getUserId());

            TenantValidateOtpResponse successResponse = new TenantValidateOtpResponse();
            successResponse.setStatus("SUCCESSFUL");
            successResponse.setMessage("Tenant OTP validated successfully");
            successResponse.setRetryCount(0);
            successResponse.setUserDetails(userDetails);
            successResponse.setTenantId(tenantId);
            successResponse.setXSessionToken(Constants.BEARER + xSessionToken);

            log.info("OTP validated successfully for txnId: {}", txnId);
            logAuthAudit(request, ActionType.LOGIN, request.getTxnId());
            LogUtil.logActivity(req, activity, "Success: Tenant OTP validated successfully");
            return ResponseEntity.ok(successResponse);

        } else {
           //Invalid OTP → increment retry count
            int retry = otpEntity.getRetryCount() + 1;
            otpEntity.setRetryCount(retry);
            otpEntity.setTxnId(headerTxnId);
            tenantDb.save(otpEntity, "otp");

            LogUtil.logActivity(req, activity, "Failed: Tenant OTP not validated");

            // Use your helper to return structured error
            return buildErrorResponse(validateRes.getMessage(),
                    HttpStatus.BAD_REQUEST,
                    retry);
        }
    }

    //Helper functions for Tenant Login OTP validate
    private void validateTenantValidateOtpRequest(TenantValidateOtpRequest request) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,ErrorCodes.JCMP3004);
        }

        validateRequiredFields(request);
        validateIdTypeAndFormat(request);
    }

    private void validateRequiredFields(TenantValidateOtpRequest request) {
        // If all are missing → throw single combined error
        if (isBlank(request.getTxnId()) &&
                isBlank(request.getPan()) &&
                isBlank(request.getIdValue()) &&
                request.getIdType() == null &&
                isBlank(request.getOtp())) {

            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ErrorCodes.JCMP3024);
        }
        if (request.getPan() == null || request.getPan().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ErrorCodes.JCMP3020);
        }
        if (request.getIdValue() == null || request.getIdValue().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ErrorCodes.JCMP3005);
        }
        if (request.getTxnId() == null || request.getTxnId().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ErrorCodes.JCMP3017);
        }
        if (request.getOtp() == null || request.getOtp().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ErrorCodes.JCMP3016);
        }
        if (request.getIdType() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ErrorCodes.JCMP3006);
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private void validateIdTypeAndFormat(TenantValidateOtpRequest request) {

        if (!request.getPan().toUpperCase().matches(PAN_REGEX) &&
                !request.getPan().toUpperCase().matches(CLIENT_ID_REGEX)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ErrorCodes.JCMP3021);
        }

        if (request.getIdType() == IdentityType.MOBILE &&
                !request.getIdValue().matches(TEN_DIGIT_MOBILE)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,ErrorCodes.JCMP3007);
        }

        if (request.getIdType() == IdentityType.EMAIL &&
                !request.getIdValue().matches(EMAIL_REGEX)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ErrorCodes.JCMP3008);
        }
    }

    private ResponseEntity<TenantValidateOtpResponse> buildErrorResponse(String message, HttpStatus status, int retryCount) {
        TenantValidateOtpResponse errorResponse = new TenantValidateOtpResponse();
        errorResponse.setStatus(Constants.FAILED);
        errorResponse.setMessage(message);
        errorResponse.setRetryCount(retryCount);
        return ResponseEntity.status(status).body(errorResponse);
    }

    private SystemTriggerEventResponse sendOtpViaNotificationService(String idValue, IdentityType idType, String txnId) {
        try {
            // Build request for notification service
            TriggerEventRequest.CustomerIdentifiers customerIdentifiers = TriggerEventRequest.CustomerIdentifiers.builder()
                    .type(idType)
                    .value(idValue)
                    .build();

            TriggerEventRequest request = TriggerEventRequest.builder()
                    .eventType("INIT_OTP")
                    .customerIdentifiers(customerIdentifiers)
                    .build();

            // Prepare headers
            Map<String, String> headers = new HashMap<>();
            headers.put("Content-Type", "application/json");

            // Call notification service
            ResponseEntity<SystemTriggerEventResponse> response = notificationApiManager.postSystemTriggerEvent(
                    headers, request, SystemTriggerEventResponse.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                SystemTriggerEventResponse responseBody = response.getBody();
                if (responseBody != null) {
                    log.info("OTP initiation successful via notification service. eventId={}, transactionId={}, status={}", 
                            responseBody.getEventId(), responseBody.getTransactionId(), responseBody.getStatus());
                    return responseBody;
                } else {
                    log.error("OTP initiation failed via notification service. Response body is null");
                    throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, ErrorCodes.JCMP3025);
                }
            } else {
                log.error("OTP initiation failed via notification service. HTTP Status: {}", response.getStatusCode());
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, ErrorCodes.JCMP3025);
            }
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception ex) {
            log.error("Error while sending OTP via notification service. txnId={}, error={}", txnId, ex.getMessage(), ex);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, ErrorCodes.JCMP3026);
        }
    }

    private ValidateOtpRes validateAndHandleOtp(String otp, String txnId, String eventId) {
        try {
            // Validate eventId is present
            if (eventId == null || eventId.isBlank()) {
                log.error("EventId is missing for OTP validation. txnId={}", txnId);
                ValidateOtpRes errorRes = new ValidateOtpRes();
                errorRes.setStatus(Constants.FAILED);
                errorRes.setMessage("EventId is missing. Please initiate OTP first.");
                return errorRes;
            }

            // Build verify request with plain OTP
            VerifyOtpRequest verifyRequest = VerifyOtpRequest.builder()
                    .eventId(eventId)
                    .txnId(txnId)
                    .encryptedOtpValue(otp)
                    .build();

            // Prepare headers
            Map<String, String> headers = new HashMap<>();
            headers.put("Content-Type", "application/json");

            // Call notification service
            ResponseEntity<VerifyOtpResponse> response = notificationApiManager.postSystemVerifyOtp(
                    headers, verifyRequest, VerifyOtpResponse.class);

            ValidateOtpRes validateRes = new ValidateOtpRes();

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                VerifyOtpResponse responseBody = response.getBody();
                
                // Validate response
                if (responseBody != null && responseBody.getVerified() != null && responseBody.getVerified() 
                        && "SUCCESS".equalsIgnoreCase(responseBody.getStatus())) {
                    validateRes.setStatus(Constants.VALIDATED);
                    validateRes.setMessage(responseBody.getMessage() != null ? responseBody.getMessage() : "OTP verified successfully");
                    log.info("OTP verification successful via notification service. txnId={}, eventId={}", txnId, eventId);
                } else {
                    String status = responseBody != null ? responseBody.getStatus() : "null";
                    String message = responseBody != null && responseBody.getMessage() != null ? responseBody.getMessage() : "OTP verification failed";
                    validateRes.setStatus(Constants.FAILED);
                    validateRes.setMessage(message);
                    log.warn("OTP verification failed via notification service. txnId={}, eventId={}, status={}", 
                            txnId, eventId, status);
                }
            } else {
                log.error("OTP verification failed via notification service. HTTP Status: {}, txnId={}, eventId={}", 
                        response.getStatusCode(), txnId, eventId);
                validateRes.setStatus(Constants.FAILED);
                validateRes.setMessage("OTP verification failed");
            }

            return validateRes;
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception ex) {
            log.error("Error during OTP validation, txnId={}, eventId={}, error={}", txnId, eventId, ex.getMessage(), ex);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, ErrorCodes.JCMP3028);
        }
    }

    // helper function for identifying pan or client id
    private IdentityValueType detectPanOrClientId(String input) {
        input = input.toUpperCase();
        if (input.matches(PAN_REGEX)) return IdentityValueType.PAN;
        if (input.matches(CLIENT_ID_REGEX)) return IdentityValueType.CLIENT_ID;
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ErrorCodes.JCMP3021);
    }

    enum IdentityValueType {
        PAN, CLIENT_ID
    }

    /**
     * Modular function to log data breach report audit events
     * Can be used in both create and update data breach report flows
     *
     * @param request The request entity to audit
     * @param actionType The action type (ONBOARD, LOGIN)
     */
    public void logAuthAudit(Object request, ActionType actionType, String txnId) {
        try {
            String tenantId = ThreadContext.get(Constants.TENANT_ID_HEADER);
            Actor actor = Actor.builder()
                    .id(ThreadContext.get(Constants.USER_ID_THREAD_CONTEXT))
                    .role(Constants.USER)
                    .type(Constants.USER_ID_TYPE)
                    .build();

            Resource resource = Resource.builder()
                    .type(Constants.TXN_ID_THREAD_CONTEXT)
                    .id(txnId)
                    .build();

            Context context = Context.builder()
                    .ipAddress(ThreadContext.get(Constants.SOURCE_IP) != null && !ThreadContext.get(Constants.SOURCE_IP).equals("-")
                            ? ThreadContext.get(Constants.SOURCE_IP) : null)
                    .txnId(ThreadContext.get(Constants.TXN_ID_THREAD_CONTEXT) != null && !ThreadContext.get(Constants.TXN_ID_THREAD_CONTEXT).equals("-") 
                            ? ThreadContext.get(Constants.TXN_ID_THREAD_CONTEXT) : null)
                    .build();

            Map<String, Object> extra = new HashMap<>();
            extra.put(Constants.DATA, request);

            AuditRequest auditRequest = AuditRequest.builder()
                    .actor(actor)
                    .businessId(tenantId)
                    .group(Constants.PARTNER_PORTAL_GROUP)
                    .component(AuditComponent.AUTHENTICATION)
                    .actionType(actionType)
                    .resource(resource)
                    .initiator(Constants.DATA_FIDUCIARY)
                    .context(context)
                    .extra(extra)
                    .build();

            this.auditManager.logAudit(auditRequest, tenantId);
        } catch (Exception e) {
            log.error("Audit logging failed for txn id: {}, action: {}, error: {}",
                    txnId, actionType, e.getMessage(), e);
        }
    }
}