package com.jio.digigov.notification.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;

/**
 * Utility for decoding consent JWT tokens to extract Data Principal information.
 *
 * <p>Consent JWT tokens contain customer identifiers in the "sub" claim as a JSON string:</p>
 * <pre>
 * {
 *   "sub": "{\"consentId\":\"...\",\"customerIdentifiers\":{\"type\":\"EMAIL\",\"value\":\"user@example.com\"},...}",
 *   ...
 * }
 * </pre>
 *
 * <p><b>Note:</b> This decoder does NOT verify the JWT signature as the token
 * originates from a trusted source (consent management system).</p>
 *
 * @author Notification Service Team
 * @since 2025-01-20
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ConsentJwtDecoder {

    private final ObjectMapper objectMapper;

    /**
     * Record containing decoded consent information for Data Principal notification.
     *
     * @param consentId The consent identifier
     * @param identifierType The type of customer identifier ("EMAIL" or "MOBILE")
     * @param identifierValue The actual email address or phone number
     * @param fullSubPayload The complete decoded "sub" claim payload for additional context
     */
    public record DecodedConsentInfo(
        String consentId,
        String identifierType,
        String identifierValue,
        Map<String, Object> fullSubPayload
    ) {}

    /**
     * Decodes a consent JWT token to extract Data Principal identifiers.
     *
     * <p>The JWT structure expected:</p>
     * <ul>
     *   <li>Payload.sub = JSON string containing consentId and customerIdentifiers</li>
     *   <li>customerIdentifiers.type = "EMAIL" or "MOBILE"</li>
     *   <li>customerIdentifiers.value = actual email/phone</li>
     * </ul>
     *
     * @param consentJwtToken The consent JWT token from eventPayload
     * @return Optional containing decoded info, empty if token is null/invalid
     */
    @SuppressWarnings("unchecked")
    public Optional<DecodedConsentInfo> decodeConsentJwt(String consentJwtToken) {
        if (consentJwtToken == null || consentJwtToken.isBlank()) {
            log.debug("Consent JWT token is null or blank");
            return Optional.empty();
        }

        try {
            // JWT format: header.payload.signature
            String[] parts = consentJwtToken.split("\\.");
            if (parts.length < 2) {
                log.warn("Invalid JWT format: expected at least 2 parts, got {}", parts.length);
                return Optional.empty();
            }

            // Decode payload (middle part) - use URL decoder for JWT
            String payloadJson = new String(
                Base64.getUrlDecoder().decode(parts[1]),
                StandardCharsets.UTF_8
            );

            // Parse the payload JSON
            Map<String, Object> payload = objectMapper.readValue(
                payloadJson,
                new TypeReference<Map<String, Object>>() {}
            );

            // Extract "sub" claim which contains the consent details as JSON string
            Object subClaim = payload.get("sub");
            if (subClaim == null) {
                log.warn("JWT payload missing 'sub' claim");
                return Optional.empty();
            }

            // Parse the sub claim (it's a JSON string within the payload)
            Map<String, Object> subPayload;
            if (subClaim instanceof String) {
                subPayload = objectMapper.readValue(
                    (String) subClaim,
                    new TypeReference<Map<String, Object>>() {}
                );
            } else if (subClaim instanceof Map) {
                subPayload = (Map<String, Object>) subClaim;
            } else {
                log.warn("Unexpected 'sub' claim type: {}", subClaim.getClass().getName());
                return Optional.empty();
            }

            // Extract consentId
            String consentId = extractStringValue(subPayload, "consentId");
            if (consentId == null) {
                log.warn("Missing consentId in JWT sub payload");
                return Optional.empty();
            }

            // Extract customerIdentifiers
            Object customerIdentifiers = subPayload.get("customerIdentifiers");
            if (!(customerIdentifiers instanceof Map)) {
                log.warn("Missing or invalid customerIdentifiers in JWT sub payload");
                return Optional.empty();
            }

            Map<String, Object> identifiers = (Map<String, Object>) customerIdentifiers;
            String type = extractStringValue(identifiers, "type");
            String value = extractStringValue(identifiers, "value");

            if (type == null || value == null) {
                log.warn("Missing type or value in customerIdentifiers");
                return Optional.empty();
            }

            log.debug("Successfully decoded consent JWT: consentId={}, identifierType={}",
                consentId, type);

            return Optional.of(new DecodedConsentInfo(consentId, type, value, subPayload));

        } catch (Exception e) {
            log.error("Failed to decode consent JWT token: {}", e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Safely extracts a string value from a map.
     *
     * @param map The map to extract from
     * @param key The key to look up
     * @return The string value or null if not found/not a string
     */
    private String extractStringValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return value != null ? value.toString() : null;
    }
}
