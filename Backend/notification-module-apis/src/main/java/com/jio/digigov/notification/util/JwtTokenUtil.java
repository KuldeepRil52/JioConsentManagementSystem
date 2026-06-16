package com.jio.digigov.notification.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * Utility class for JWT token generation and decoding.
 *
 * <p>This utility is used to create JWT tokens containing event payload data
 * for DPO (Data Protection Officer) email notifications. The token embeds all
 * necessary context (eventPayload, tenantId, businessId, transactionId) required
 * for template argument resolution in the email consumer.</p>
 *
 * <p><b>Token Structure:</b></p>
 * <pre>
 * Header: {"alg": "HS256", "typ": "JWT"}
 * Payload: {
 *   "eventPayload": { original trigger request payload },
 *   "tenantId": "tenant123",
 *   "businessId": "business456",
 *   "transactionId": "TXN-ABC123",
 *   "eventType": "GRIEVANCE_RAISED",
 *   "timestamp": "2025-10-24T14:30:00",
 *   "iat": 1729776600
 * }
 * Signature: HMACSHA256(base64UrlEncode(header) + "." + base64UrlEncode(payload), secret)
 * </pre>
 *
 * <p><b>Usage in DPO Email Flow:</b></p>
 * <ol>
 *   <li>TriggerEventService generates JWT token from event payload</li>
 *   <li>Token is added as MASTER_LABEL_DPO_EMAIL_TOKEN argument</li>
 *   <li>EmailNotificationConsumer decodes token to get template arguments</li>
 * </ol>
 *
 * @since 2.0.0
 * @author DPDP Notification Team
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JwtTokenUtil {

    private final ObjectMapper objectMapper;

    @Value("${notification.jwt.secret:dpdp-notification-secret-key-change-in-production}")
    private String jwtSecret;

    @Value("${notification.jwt.issuer:dpdp-notification-service}")
    private String jwtIssuer;

    private static final String HMAC_SHA256 = "HmacSHA256";

    /**
     * Generates a JWT token containing event data for DPO email processing.
     *
     * <p>The generated token includes:</p>
     * <ul>
     *   <li>eventPayload: Original trigger event payload for template resolution</li>
     *   <li>tenantId: Tenant identifier for database access</li>
     *   <li>businessId: Business identifier for data filtering</li>
     *   <li>transactionId: Transaction ID for tracing</li>
     *   <li>timestamp: Token generation timestamp</li>
     *   <li>iat: Issued at time (Unix timestamp)</li>
     * </ul>
     *
     * @param eventPayload Original event payload from trigger request
     * @param tenantId Tenant identifier
     * @param businessId Business identifier
     * @param transactionId Transaction ID for tracing
     * @param eventType Type of event (e.g., GRIEVANCE_RAISED)
     * @return JWT token string (format: header.payload.signature)
     * @throws RuntimeException if token generation fails
     */
    public String generateToken(Map<String, Object> eventPayload,
                                String tenantId,
                                String businessId,
                                String transactionId,
                                String eventType) {
        try {
            log.debug("Generating JWT token for DPO email: eventType={}, tenantId={}, businessId={}",
                    eventType, tenantId, businessId);

            // Create header
            Map<String, String> header = new HashMap<>();
            header.put("alg", "HS256");
            header.put("typ", "JWT");

            // Create payload
            Map<String, Object> payload = new HashMap<>();
            payload.put("eventPayload", eventPayload);
            payload.put("tenantId", tenantId);
            payload.put("businessId", businessId);
            payload.put("transactionId", transactionId);
            payload.put("eventType", eventType);
            payload.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            payload.put("iat", System.currentTimeMillis() / 1000); // Issued at (Unix timestamp)
            payload.put("iss", jwtIssuer); // Issuer

            // Encode header and payload
            String encodedHeader = base64UrlEncode(objectMapper.writeValueAsString(header));
            String encodedPayload = base64UrlEncode(objectMapper.writeValueAsString(payload));

            // Create signature
            String dataToSign = encodedHeader + "." + encodedPayload;
            String signature = createSignature(dataToSign);

            // Combine to form JWT
            String jwt = dataToSign + "." + signature;

            log.debug("JWT token generated successfully: length={}", jwt.length());
            return jwt;

        } catch (Exception e) {
            log.error("Failed to generate JWT token");
            throw new RuntimeException("Failed to generate JWT token for DPO email", e);
        }
    }

    /**
     * Decodes a JWT token and extracts the payload.
     *
     * <p>This method validates the token signature and returns the decoded payload
     * containing eventPayload, tenantId, businessId, transactionId, and other metadata.</p>
     *
     * <p><b>Important:</b> This method verifies the signature to ensure token integrity.</p>
     *
     * @param token JWT token string (format: header.payload.signature)
     * @return Map containing decoded payload data
     * @throws RuntimeException if token is invalid or decoding fails
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> decodeToken(String token) {
        try {
            log.debug("Decoding JWT token: length={}", token.length());

            // Split token into parts
            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                throw new IllegalArgumentException("Invalid JWT token format. Expected 3 parts, got " + parts.length);
            }

            String encodedHeader = parts[0];
            String encodedPayload = parts[1];
            String providedSignature = parts[2];

            // Verify signature
            String dataToSign = encodedHeader + "." + encodedPayload;
            String expectedSignature = createSignature(dataToSign);

            if (!expectedSignature.equals(providedSignature)) {
                log.error("JWT signature verification failed");
                throw new SecurityException("Invalid JWT token signature");
            }

            // Decode payload
            String payloadJson = base64UrlDecode(encodedPayload);
            Map<String, Object> payload = objectMapper.readValue(payloadJson, Map.class);

            log.debug("JWT token decoded successfully: eventType={}, tenantId={}",
                    payload.get("eventType"), payload.get("tenantId"));

            return payload;

        } catch (Exception e) {
            log.error("Failed to decode JWT token");
            throw new RuntimeException("Failed to decode JWT token", e);
        }
    }

    /**
     * Extracts the event payload from a decoded token.
     *
     * @param decodedToken Decoded JWT token payload
     * @return Event payload map
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> extractEventPayload(Map<String, Object> decodedToken) {
        Object eventPayload = decodedToken.get("eventPayload");
        if (eventPayload instanceof Map) {
            return (Map<String, Object>) eventPayload;
        }
        return new HashMap<>();
    }

    /**
     * Extracts a specific field from the decoded token.
     *
     * @param decodedToken Decoded JWT token payload
     * @param fieldName Field name to extract
     * @return Field value as String, or null if not found
     */
    public String extractField(Map<String, Object> decodedToken, String fieldName) {
        Object value = decodedToken.get(fieldName);
        return value != null ? value.toString() : null;
    }

    /**
     * Creates HMAC SHA256 signature for the data.
     *
     * @param data Data to sign
     * @return Base64 URL encoded signature
     * @throws Exception if signature creation fails
     */
    private String createSignature(String data) throws Exception {
        Mac sha256Hmac = Mac.getInstance(HMAC_SHA256);
        SecretKeySpec secretKey = new SecretKeySpec(
                jwtSecret.getBytes(StandardCharsets.UTF_8),
                HMAC_SHA256
        );
        sha256Hmac.init(secretKey);

        byte[] signedBytes = sha256Hmac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        return base64UrlEncode(signedBytes);
    }

    /**
     * Base64 URL encodes a string.
     *
     * @param data String to encode
     * @return Base64 URL encoded string
     */
    private String base64UrlEncode(String data) {
        return base64UrlEncode(data.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Base64 URL encodes byte array.
     *
     * @param data Byte array to encode
     * @return Base64 URL encoded string
     */
    private String base64UrlEncode(byte[] data) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(data);
    }

    /**
     * Base64 URL decodes a string.
     *
     * @param encodedData Base64 URL encoded string
     * @return Decoded string
     */
    private String base64UrlDecode(String encodedData) {
        byte[] decodedBytes = Base64.getUrlDecoder().decode(encodedData);
        return new String(decodedBytes, StandardCharsets.UTF_8);
    }
}
