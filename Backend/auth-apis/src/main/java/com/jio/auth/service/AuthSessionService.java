package com.jio.auth.service;

import com.jio.auth.constants.ErrorCode;
import com.jio.auth.dto.AuthSessionResponse;
import com.jio.auth.dto.IntrospectResponse;
import com.jio.auth.exception.CustomException;
import com.jio.auth.model.AuthSessionManager;
import com.jio.auth.repository.AuthSessionManagerRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthSessionService {

    private final AuthSessionManagerRepository authSessionManagerRepository;
    private final TokenService tokenService;

    private static final long SESSION_LIFETIME_MS = 7 * 24 * 60 * 60 * 1000L;
    private static final long ACCESS_EXPIRY_SECONDS = 3600;
    private static final int MAX_ACTIVE_SESSIONS = 3;


    public AuthSessionResponse createSession(Map<String, String> payload, Map<String, String> headers, String jsonKey) {
        log.info("Creating session with payload: {}", payload);

        String tenantId = payload.get("tenantId");
        String businessId = payload.get("businessId");
        String userId = payload.get("sub");
        String issuer = payload.get("iss");

        if (tenantId == null || businessId == null || userId == null || issuer == null) {
            throw new CustomException(ErrorCode.INVALID_REQUEST, "Missing required fields in payload");
        }

        // Fetch all active sessions
        List<AuthSessionManager> activeSessions =
                authSessionManagerRepository.findActiveSessionsByUser(tenantId, userId);



        // Only active (non-revoked) sessions are counted here
        if (activeSessions.size() >= MAX_ACTIVE_SESSIONS) {
            log.info("Max session limit reached for user {}. Revoking oldest active session.", userId);
            authSessionManagerRepository.revokeOldestActiveSession(tenantId, userId);
        }

        // Generate new refresh token
        String rawRefresh = generateSecureToken();
        String hashedRefresh = BCrypt.hashpw(rawRefresh, BCrypt.gensalt());

        String ipAddress = headers.getOrDefault("X-Forwarded-For", headers.getOrDefault("Remote-Addr", "unknown"));
        String userAgent = headers.getOrDefault("User-Agent", "unknown");
        String deviceFingerprint = headers.getOrDefault("Device-Fingerprint", UUID.randomUUID().toString());

        Date now = new Date();
        Date expiry = new Date(now.getTime() + SESSION_LIFETIME_MS);
        UUID sessionId = UUID.randomUUID();

        AuthSessionManager session = AuthSessionManager.builder()
                .tenantId(tenantId)
                .businessId(businessId)
                .userId(userId)
                .sessionId(sessionId)
                .createdAt(now)
                .lastRotatedAt(now)
                .absoluteExpiry(expiry)
                .accessExpirySeconds((int) ACCESS_EXPIRY_SECONDS)
                .revoked(false)
                .rotationCount(0)
                .refreshTokenHash(hashedRefresh)
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .deviceFingerprint(deviceFingerprint)
                .build();

        Map<String, String> claims = Map.of(
                "tenantId", tenantId,
                "businessId", businessId,
                "iss", issuer,
                "sub", userId,
                "sessionId", sessionId.toString()
        );

        var jwtResponse = tokenService.generateJwt(claims, jsonKey);

        authSessionManagerRepository.saveSession(tenantId, session);

        return AuthSessionResponse.builder()
                .tenantId(tenantId)
                .businessId(businessId)
                .userId(userId)
                .sessionId(sessionId.toString())
                .jwt(jwtResponse.getAccessToken())
                .refreshToken(rawRefresh)
                .expiresAt(new Date(jwtResponse.getExpiry()))
                .build();
    }


    /**
     * Refresh session — rotate refresh token & issue new JWT.
     */
    public AuthSessionResponse refreshSession(Map<String, String> payload, Map<String, String> headers, String jsonKey) {
        String tenantId = payload.get("tenantId");
        String refreshToken = payload.get("refreshToken");
        String issuer = payload.get("iss");

        if (tenantId == null || refreshToken == null) {
            throw new CustomException(ErrorCode.INVALID_REQUEST, "Missing tenantId or refreshToken");
        }

        List<AuthSessionManager> sessions =
                authSessionManagerRepository.findActiveSessionsByUser(tenantId, null);

        AuthSessionManager matched = sessions.stream()
                .filter(s -> BCrypt.checkpw(refreshToken, s.getRefreshTokenHash()))
                .findFirst()
                .orElseThrow(() -> new CustomException(ErrorCode.UNAUTHORIZED, "Invalid refresh token"));

        if (Boolean.TRUE.equals(matched.getRevoked()))
            throw new CustomException(ErrorCode.UNAUTHORIZED, "Session revoked");
        if (new Date().after(matched.getAbsoluteExpiry()))
            throw new CustomException(ErrorCode.UNAUTHORIZED, "Session expired");

        // Rotate refresh token
        String newRaw = generateSecureToken();
        String newHash = BCrypt.hashpw(newRaw, BCrypt.gensalt());
        Date now = new Date();
        Date expiry = new Date(now.getTime() + SESSION_LIFETIME_MS);
        authSessionManagerRepository.updateRefreshToken(
                tenantId,
                matched.getSessionId(),
                newHash,
                matched.getRefreshTokenHash(),
                now,
                matched.getRotationCount() + 1
        );

        // Generate new JWT
        Map<String, String> claims = Map.of(
                "tenantId", matched.getTenantId(),
                "businessId", matched.getBusinessId(),
                "iss", issuer,
                "sub", matched.getUserId(),
                "sessionId", matched.getSessionId().toString()
        );
        var jwtResponse = tokenService.generateJwt(claims, jsonKey);

        return AuthSessionResponse.builder()
                .tenantId(matched.getTenantId())
                .businessId(matched.getBusinessId())
                .userId(matched.getUserId())
                .sessionId(matched.getSessionId().toString())
                .jwt(jwtResponse.getAccessToken())
                .refreshToken(newRaw)
                .expiresAt(new Date(jwtResponse.getExpiry()))
                .build();
    }

    public IntrospectResponse checkSession(String token, String jsonContent) {
        Claims claims = tokenService.extractClaims(token, jsonContent);
        String tenantId = claims.get("tenantId", String.class);
        String businessId = claims.get("businessId", String.class);
        String sub =  claims.get("sub", String.class);
        String iss = claims.get("iss", String.class);
        String sessionId = claims.get("sessionId", String.class);
        IntrospectResponse response = new IntrospectResponse.Builder()
                .active(true)
                .iss(iss)
                .sub(sub)
                .jti(sessionId)
                .build();
        return response;
    }


    private String generateSecureToken() {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
