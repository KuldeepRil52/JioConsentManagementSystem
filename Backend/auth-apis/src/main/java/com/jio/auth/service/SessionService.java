package com.jio.auth.service;

import com.jio.auth.constants.BodyFields;
import com.jio.auth.constants.ErrorCode;
import com.jio.auth.dto.SessionResponseDto;
import com.jio.auth.dto.SessionStatusResponseDto;
import com.jio.auth.dto.TenantSecretCode;
import com.jio.auth.model.UserSession;
import com.jio.auth.repository.AuthSecretRepository;
import com.jio.auth.repository.UserSessionRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import com.jio.auth.exception.CustomException;
import com.jio.auth.constants.HeaderFields;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import java.util.Map;

@Slf4j
@Service
public class SessionService {

    @Autowired
    private Wso2TokenManagerService wso2TokenManagerService;

    @Autowired
    private AuthSecretRepository  authSecretRepository;

    private final TenantVerificationService tenantVerifier;
    private final UserSessionRepository userSessionRepository;

    @Value("${securecode.expiry:10}")
    private int EXPIRY_IN_MIN;

    public SessionService(TenantVerificationService tenantVerifier,
                          UserSessionRepository userSessionRepository) {
        this.tenantVerifier = tenantVerifier;
        this.userSessionRepository = userSessionRepository;
    }

    public SessionResponseDto createSession(Map<String, String> payload, Map<String, String> headers) {

        String tenantId = headers.get(HeaderFields.TENANT_ID_CODE);
        String businessId = headers.get(HeaderFields.BUSINESS_ID_CODE);
        String identity = payload.get(BodyFields.IDENTITY);
        String identityType = payload.get(BodyFields.IDENTITY_TYPE);

        if (!tenantVerifier.verifyTenant(tenantId)) {
            throw new CustomException(ErrorCode.INVALID_TENANT_ID);
        }

        if (!tenantVerifier.verifyBusiness(tenantId, businessId)) {
            throw new CustomException(ErrorCode.INVALID_BUSINESS_ID);
        }

        String accessToken = UUID.randomUUID().toString();


        UserSession session = new UserSession();
        session.setAccessToken(accessToken);
        session.setTenantId(tenantId);
        session.setBusinessId(businessId);
        session.setIdentity(identity);
        session.setIdentityType(identityType);

        userSessionRepository.deleteByIdentity(identity);
        userSessionRepository.save(session);

        SessionResponseDto response = new SessionResponseDto();
        response.setSecureCode(session.getAccessToken());
        response.setIdentity(session.getIdentity());
        response.setExpiry(session.getCreatedAt().plus(Duration.ofMinutes(EXPIRY_IN_MIN)).toEpochMilli());
        log.info("Secure Code for user create successfully. BusinessId: "+businessId +" TenantId: "+tenantId);
        return response;
    }


    public SessionStatusResponseDto checkSession(Map<String, String> headers) {

        String accessToken = headers.get(HeaderFields.ACCESS_TOKEN);
        String tenantId = headers.get(HeaderFields.TENANT_ID);
        String businessId = headers.get(HeaderFields.BUSINESS_ID);
        String identity = headers.get(HeaderFields.IDENTITY);

        UserSession session = userSessionRepository
                .findByAccessTokenAndTenantIdAndBusinessIdAndIdentity(accessToken, tenantId, businessId, identity)
                .orElseThrow(() -> new CustomException(ErrorCode.UNAUTHORIZED));

        // Check expiry
        Instant expiryTime = session.getCreatedAt().plus(Duration.ofMinutes(EXPIRY_IN_MIN));

        if (Instant.now().isAfter(expiryTime)) {
            throw new CustomException(ErrorCode.SESSION_EXPIRED);
        }
        String wso2token = wso2TokenManagerService.getValidAccessToken(session.getTenantId(),  session.getBusinessId());

        SessionStatusResponseDto response = new SessionStatusResponseDto();
        response.setActive(true);
        response.setTenantId(session.getTenantId());
        response.setBusinessId(session.getBusinessId());
        response.setIdentity(session.getIdentity());
        response.setIdentityType(session.getIdentityType());
        response.setAccessToken(wso2token);

        return response;

    }

    public SessionStatusResponseDto checkAccessTokenOnly(Map<String, String> headers) {
        String accessToken = headers.get(HeaderFields.ACCESS_TOKEN);

        UserSession session = userSessionRepository.findByAccessToken(accessToken)
                .orElseThrow(() -> new CustomException(ErrorCode.UNAUTHORIZED));

        Instant expiryTime = session.getCreatedAt().plus(Duration.ofMinutes(EXPIRY_IN_MIN));
        if (Instant.now().isAfter(expiryTime)) {
            throw new CustomException(ErrorCode.SESSION_EXPIRED);
        }
        String wso2token = wso2TokenManagerService.getValidAccessToken(session.getTenantId(),  session.getBusinessId());
        SessionStatusResponseDto response = new SessionStatusResponseDto();
        response.setActive(true);
        response.setTenantId(session.getTenantId());
        response.setBusinessId(session.getBusinessId());
        response.setIdentity(session.getIdentity());
        response.setIdentityType(session.getIdentityType());
        response.setAccessToken(wso2token);
        return response;
    }

    public TenantSecretCode CheckTenantSecretWithIdentityValue(Map<String, String> headers){
        TenantSecretCode response = new TenantSecretCode();
        if(authSecretRepository.existsBySecretCodeAndIdentityValue(headers.get(HeaderFields.SECRET_CODE), headers.get(HeaderFields.IDENTITY_VALUE))){
            response.setActive(true);
            response.setIdentityValue(headers.get(HeaderFields.IDENTITY_VALUE));
        }else{
            throw new CustomException(ErrorCode.UNAUTHORIZED);
        }
        return response;
    }


}
