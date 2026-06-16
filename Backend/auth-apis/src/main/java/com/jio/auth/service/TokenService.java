package com.jio.auth.service;

import com.jio.auth.cache.TokenCache;
import com.jio.auth.constants.ErrorCode;
import com.jio.auth.dto.IntrospectResponse;
import com.jio.auth.dto.JwtResponse;
import com.jio.auth.exception.CustomException;
import com.jio.auth.repository.UserRepository;
import com.jio.auth.token.TokenStore;
import com.jio.auth.utils.KeyProvider;
import io.jsonwebtoken.*;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
public class TokenService {

    @Autowired
    private Wso2TokenManagerService wso2TokenManagerService;

    @Autowired
    private KeyProvider keyProvider;

    @Autowired
    private TokenCache tokenCache;

    @Autowired
    private TokenStore tokenStore;

    @Value("${token.expiry:3600000}")
    private long tokenExpiry;

    @Autowired
    private UserRepository userRepository;

    private static final Logger audit = LoggerFactory.getLogger("AUDIT_LOGGER");

    public JwtResponse generateJwt(Map<String, String> payload, String jsonContent) {
        log.info("generating Jwt iss: {}, tenantId: {}, Businessid: {} and sub: {} ", payload.get("iss"), payload.get("tenantId"), payload.get("businessid"), payload.get("sub"));
        try {
            PrivateKey privateKey = keyProvider.getPrivateKeyFromJson(jsonContent);
            long now = System.currentTimeMillis();
            long exp = now + tokenExpiry;
            UUID  uuid = UUID.randomUUID();
            String id = uuid.toString();
            String token = Jwts.builder()
                    .setClaims(payload)
                    .setIssuedAt(new Date(now))
                    .setExpiration(new Date(exp))
                    .setId(id)
                    .signWith(privateKey, SignatureAlgorithm.RS256)
                    .compact();
            tokenStore.saveActiveToken(payload.get("sub"),id,new Date(exp));
            return new JwtResponse(token,exp);
        } catch (Exception ex) {
            log.error("Error generating JWT Token " + ex.getMessage(), ex);
            throw new CustomException(ErrorCode.INTERNAL_ERROR, "Error generating JWT Token");
        }
    }

    public IntrospectResponse validateToken(String token, String jsonContent) {

        log.info("Validating token: {}", token);
        Claims claims = extractClaims(token, jsonContent);
        log.debug("Checking if token is revoked");
        String jti = claims.getId();
        if (jti == null || jti.trim().isEmpty()) {
            log.error("JWT does not contain a jti claim");
            throw new CustomException(ErrorCode.UNAUTHORIZED, "Missing 'jti' claim in the token");
        }
        if (tokenStore.isTokenRevoked(claims.getId())) {
            log.info("Token {} is revoked", token);
            return new IntrospectResponse.Builder().active(false).build();
        }
        IntrospectResponse response = new IntrospectResponse.Builder()
                .active(true)
                .scope(claims.get("scope", String.class))
                .clientId(claims.get("client_id", String.class))
                .username(claims.get("username", String.class))
                .tokenType(claims.get("token_type", String.class))
                .exp(claims.get("exp") != null ? ((Number) claims.get("exp")).longValue() : null)
                .iat(claims.get("iat") != null ? ((Number) claims.get("iat")).longValue() : null)
                .nbf(claims.get("nbf") != null ? ((Number) claims.get("nbf")).longValue() : null)
                .sub(claims.get("sub", String.class))
                .aud(claims.get("aud", String.class))
                .iss(claims.get("iss", String.class))
                .build();

        log.info("Token {} successfully validated", token);
        return response;
    }

    public IntrospectResponse validateTokenAndFetchWso2Token(String token, String jsonContent) {
        log.debug("Checking if token is valid");
        log.info("Validating token: {}", token);

        Claims claims = extractClaims(token, jsonContent);
        String tenantId = claims.get("tenantId", String.class);
        String sub = claims.get("sub", String.class);
        String jti =  claims.getId();

        if(!userRepository.existsByUserId(tenantId, sub)){
            log.info("User {} does not exist in Users Collection in tenant db: {}", sub, tenantId);
            throw new CustomException(ErrorCode.UNAUTHORIZED, "User not found");
        }

        log.debug("Getting the token from Wso2 Token Manager Service");
        String wso2token = wso2TokenManagerService.getValidAccessToken(tenantId, tenantId);

        log.debug("Token {} successfully validated claims {}", token, claims);
        IntrospectResponse response = new IntrospectResponse.Builder()
                .active(true)
                .scope(claims.get("scope", String.class))
                .clientId(claims.get("client_id", String.class))
                .username(claims.get("username", String.class))
                .tokenType(claims.get("token_type", String.class))
                .exp(claims.get("exp") != null ? ((Number) claims.get("exp")).longValue() : null)
                .iat(claims.get("iat") != null ? ((Number) claims.get("iat")).longValue() : null)
                .nbf(claims.get("nbf") != null ? ((Number) claims.get("nbf")).longValue() : null)
                .sub(claims.get("sub", String.class))
                .aud(claims.get("aud", String.class))
                .iss(claims.get("iss", String.class))
                .accessToken(wso2token)
                .build();

        return response;
    }

    public void revoke(String token, String jsonContent) {

        log.debug("Checking if token {} is already revoked", token);
        try {
            Claims claims = extractClaims(token, jsonContent);
            String jti = claims.getId();
            Date expiry = claims.getExpiration();
            tokenStore.revokeToken(jti, expiry);
            log.info("Token {} revoked successfully", token);
        } catch (DataAccessException e) {
            log.error("Error revoking token from DB {}: {}", token, e.getMessage());
            throw new CustomException(ErrorCode.DB_ERROR, "Database error while revoking token: " + e.getMessage());
        } catch (Exception e) {
            log.error("Error revoking token {}: {}", token, e.getMessage());
            throw new CustomException(ErrorCode.UNKNOWN_ERROR, "Unexpected error while revoking token: " + e.getMessage());
        }
    }

    public Claims extractClaims(String token, String jsonContent) {
        log.info("Extracting claims from token");
        PublicKey publicKey = keyProvider.getPublicKeyFromJson(jsonContent);

        try {
            return Jwts.parserBuilder()
                    .setSigningKey(publicKey)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

        } catch (ExpiredJwtException ex) {
            log.warn("JWT expired at: {}", ex.getClaims().getExpiration());
            throw new CustomException(ErrorCode.UNAUTHORIZED, "Token has expired. Please reauthenticate.");
        } catch (SignatureException ex) {
            log.error("Invalid JWT signature");
            throw new CustomException(ErrorCode.UNAUTHORIZED, "Invalid token signature.");
        } catch (MalformedJwtException ex) {
            log.error("Malformed JWT: {}", ex.getMessage());
            throw new CustomException(ErrorCode.UNAUTHORIZED, "Malformed token.");
        } catch (Exception ex) {
            log.error("JWT validation failed: {}", ex.getMessage());
            throw new CustomException(ErrorCode.UNAUTHORIZED, "Token validation failed.");
        }
    }

    public HashMap<String, String> extractTenantAndBusinessFromJwt(String jwt) {
        HashMap<String, String> map = new HashMap<>();

        try {
            String[] parts = jwt.split("\\.");
            if (parts.length < 2) return map;

            String payloadJson = new String(java.util.Base64.getUrlDecoder().decode(parts[1]));

            if (payloadJson.contains("\"tenantId\"")) {
                int t1 = payloadJson.indexOf("\"tenantId\"");
                int c1 = payloadJson.indexOf(":", t1);
                int q1 = payloadJson.indexOf("\"", c1 + 1);
                int q2 = payloadJson.indexOf("\"", q1 + 1);
                map.put("tenantId", payloadJson.substring(q1 + 1, q2));
            }

            if (payloadJson.contains("\"businessId\"")) {
                int t1 = payloadJson.indexOf("\"businessId\"");
                int c1 = payloadJson.indexOf(":", t1);
                int q1 = payloadJson.indexOf("\"", c1 + 1);
                int q2 = payloadJson.indexOf("\"", q1 + 1);
                map.put("businessId", payloadJson.substring(q1 + 1, q2));
            }

        } catch (Exception ignored) {}

        return map;
    }



}
