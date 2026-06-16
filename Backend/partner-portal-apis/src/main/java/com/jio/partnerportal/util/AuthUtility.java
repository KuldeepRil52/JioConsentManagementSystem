package com.jio.partnerportal.util;

import com.jio.partnerportal.client.auth.AuthManager;
import com.jio.partnerportal.client.auth.request.TokenRequest;
import com.jio.partnerportal.client.auth.response.TokenResponse;
import com.jio.partnerportal.client.vault.VaultManager;
import com.jio.partnerportal.constant.Constants;
import com.jio.partnerportal.constant.ErrorCodes;
import com.jio.partnerportal.exception.PartnerPortalException;
import com.jio.partnerportal.repository.TenantRepository;
import com.jio.partnerportal.repository.TenantUserRepository;
import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.SignedJWT;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.ThreadContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Slf4j
@Component
public class AuthUtility {

    TenantUserRepository tenantUserRepository;

    TenantRepository tenantRepository;

    VaultManager vaultManager;

    AuthManager authManager;

    @Autowired
    public AuthUtility(TenantUserRepository tenantUserRepository , TenantRepository tenantRepository, VaultManager vaultManager, AuthManager authManager) {
        this.tenantUserRepository = tenantUserRepository;
        this.tenantRepository = tenantRepository;
        this.vaultManager = vaultManager;
        this.authManager = authManager;
    }

    @Value("${x-session-token.enable:true}")
    private boolean authEnabled;

    public static RSAKey loadRSAKey() throws Exception {
        try (InputStream is = AuthUtility.class.getResourceAsStream("/jwt-set.json")) {
            String json = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            JWK jwk = JWK.parse(json);
            return (RSAKey) jwk;
        }
    }

    public String generateToken(String userId, String tenantId) throws Exception {
        // Create token request
        TokenRequest tokenRequest = TokenRequest.builder()
                .iss("partner-portal")
                .tenantId(tenantId)
                .businessId(tenantId)
                .sub(userId)
                .build();
        
        // Call auth token API
        TokenResponse tokenResponse = authManager.getToken(tenantId, tenantId, tokenRequest);
        
        if (tokenResponse == null || ObjectUtils.isEmpty(tokenResponse.getAccessToken())) {
            log.error("Failed to get token from auth API for userId: {}, tenantId: {}", userId, tenantId);
            throw new PartnerPortalException(ErrorCodes.JCMP5003);
        }
        
        return tokenResponse.getAccessToken();
    }

    public boolean verifyToken(String token) throws Exception {
        SignedJWT signedJWT = SignedJWT.parse(token);
        RSAKey rsaKey = loadRSAKey();

        JWSVerifier verifier = new RSASSAVerifier(rsaKey.toRSAPublicKey());

        if (authEnabled && !signedJWT.verify(verifier)) {
            return false;
        }
        if (authEnabled && signedJWT.getJWTClaimsSet().getExpirationTime().before(new Date())) {
            return false;
        }

        String userId = signedJWT.getJWTClaimsSet().getSubject();
        ThreadContext.put("userId", userId);

        // extract tenantId
        String tenantId = signedJWT.getJWTClaimsSet().getStringClaim("tenantId");

        if (authEnabled && !this.tenantRepository.existsByTenantId(tenantId)) {
            return false ;
        }
        ThreadContext.put(Constants.TENANT_ID_HEADER, tenantId);

        return authEnabled;
    }
}
