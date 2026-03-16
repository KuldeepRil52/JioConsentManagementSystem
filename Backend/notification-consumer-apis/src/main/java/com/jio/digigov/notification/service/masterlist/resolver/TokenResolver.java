package com.jio.digigov.notification.service.masterlist.resolver;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTDecodeException;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.Claim;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.JWTVerifier;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jio.digigov.notification.dto.request.event.TriggerEventRequestDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Resolver for extracting values from JWT tokens in the event payload.
 *
 * This resolver decrypts JWT tokens found in the TriggerEventRequestDto eventPayload
 * and extracts claim values using dot notation for nested claim navigation.
 * The JWT token is expected to be in the "eventPayload.token" field.
 *
 * Features:
 * - JWT token validation using existing jwt.callback.secret
 * - Nested claim navigation with dot notation
 * - Support for various claim types (string, number, boolean, object)
 * - Comprehensive error handling and logging
 *
 * Examples:
 * - path: "claims.consentId" -> extracts consentId claim
 * - path: "claims.user.profile.name" -> extracts nested user profile name
 * - path: "claims.permissions[0]" -> extracts first permission (if supported)
 *
 * @author Notification Service Team
 * @version 1.0
 * @since 2025-01-15
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class TokenResolver {

    private final ObjectMapper objectMapper;

    @Value("${jwt.callback.secret}")
    private String jwtSecret;

    private static final String TOKEN_FIELD_PATH = "eventPayload.token";

    /**
     * Extracts a value from the JWT token claims using the specified path.
     *
     * @param request the trigger event request containing the token
     * @param path the dot-notation path to the claim value (e.g., "claims.consentId")
     * @return the extracted claim value as a string, or null if not found
     * @throws TokenResolutionException if token extraction or parsing fails
     */
    public String extractValue(TriggerEventRequestDto request, String path) {
        if (request == null) {
            throw new TokenResolutionException("TriggerEventRequestDto is null");
        }

        if (path == null || path.trim().isEmpty()) {
            throw new TokenResolutionException("Path is null or empty");
        }

        try {
            log.debug("Extracting value from JWT token path: {}", path);

            // Step 1: Extract token from request payload
            String token = extractTokenFromPayload(request);
            if (token == null) {
                throw new TokenResolutionException("JWT token not found in eventPayload.token");
            }

            // Step 2: Decode and validate JWT token
            DecodedJWT decodedJWT = decodeAndValidateToken(token);

            // Step 3: Extract value from claims using path navigation
            Object claimValue = navigateClaimsPath(decodedJWT, path);

            if (claimValue == null) {
                log.debug("Claim value not found at path: {}", path);
                return null;
            }

            String stringValue = convertClaimToString(claimValue);
            log.debug("Successfully extracted value from JWT path '{}': '{}'", path, stringValue);
            return stringValue;

        } catch (TokenResolutionException e) {
            throw e; // Re-throw our custom exceptions
        } catch (Exception e) {
            log.error("Unexpected error extracting value from JWT path '{}': {}", path, e.getMessage());
            throw new TokenResolutionException("Unexpected error extracting JWT claim: " + path, e);
        }
    }

    /**
     * Extracts the JWT token from the eventPayload.token field.
     *
     * @param request the trigger event request
     * @return the JWT token string, or null if not found
     */
    private String extractTokenFromPayload(TriggerEventRequestDto request) {
        if (request.getEventPayload() == null) {
            log.debug("eventPayload is null, cannot extract token");
            return null;
        }

        Object tokenObj = request.getEventPayload().get("token");
        if (tokenObj == null) {
            log.debug("token field not found in eventPayload");
            return null;
        }

        if (!(tokenObj instanceof String)) {
            log.warn("token field in eventPayload is not a string: {}", tokenObj.getClass().getSimpleName());
            return null;
        }

        String token = (String) tokenObj;
        if (token.trim().isEmpty()) {
            log.debug("token field in eventPayload is empty");
            return null;
        }

        return token;
    }

    /**
     * Decodes and validates the JWT token.
     *
     * @param token the JWT token string
     * @return the decoded JWT
     * @throws TokenResolutionException if token is invalid
     */
    private DecodedJWT decodeAndValidateToken(String token) {
        try {
            // First, try to decode without verification to check structure
            DecodedJWT unverifiedJWT = JWT.decode(token);
            log.debug("JWT token decoded successfully, issuer: {}, subject: {}",
                     unverifiedJWT.getIssuer(), unverifiedJWT.getSubject());

            // Now verify the token with the secret
            Algorithm algorithm = Algorithm.HMAC256(jwtSecret);
            JWTVerifier verifier = JWT.require(algorithm).build();
            DecodedJWT verifiedJWT = verifier.verify(token);

            log.debug("JWT token verified successfully");
            return verifiedJWT;

        } catch (JWTDecodeException e) {
            log.error("JWT token decode failed: {}", e.getMessage());
            throw new TokenResolutionException("Invalid JWT token format", e);
        } catch (JWTVerificationException e) {
            log.error("JWT token verification failed: {}", e.getMessage());
            throw new TokenResolutionException("JWT token verification failed", e);
        }
    }

    /**
     * Navigates through JWT claims using the dot-notation path.
     *
     * @param decodedJWT the decoded JWT token
     * @param path the dot-notation path (e.g., "claims.user.profile.name")
     * @return the claim value, or null if not found
     */
    private Object navigateClaimsPath(DecodedJWT decodedJWT, String path) {
        // Remove "claims." prefix if present (for consistency with other resolvers)
        String claimPath = path;
        if (path.startsWith("claims.")) {
            claimPath = path.substring(7); // Remove "claims." prefix
        }

        String[] pathSegments = claimPath.split("\\.");
        Object current = null;

        // Get the root claim
        String rootClaimName = pathSegments[0];
        Claim rootClaim = decodedJWT.getClaim(rootClaimName);

        if (rootClaim.isNull()) {
            log.debug("Root claim '{}' not found in JWT", rootClaimName);
            return null;
        }

        // Handle single-level claim (no nested navigation needed)
        if (pathSegments.length == 1) {
            return getClaimValue(rootClaim);
        }

        // Handle nested claims by converting to JSON and navigating
        try {
            current = getClaimValue(rootClaim);

            // Navigate through remaining path segments
            for (int i = 1; i < pathSegments.length; i++) {
                if (current == null) {
                    return null;
                }

                current = navigateObjectSegment(current, pathSegments[i]);
            }

            return current;

        } catch (Exception e) {
            log.debug("Failed to navigate nested claim path '{}': {}", claimPath, e.getMessage());
            return null;
        }
    }

    /**
     * Extracts the actual value from a JWT Claim object.
     *
     * @param claim the JWT claim
     * @return the claim value as an appropriate Java object
     */
    private Object getClaimValue(Claim claim) {
        if (claim.isNull()) {
            return null;
        }

        // Try different claim types
        try {
            // String claims
            String stringValue = claim.asString();
            if (stringValue != null) {
                return stringValue;
            }
        } catch (Exception e) {
            // Not a string, try other types
        }

        try {
            // Number claims
            Integer intValue = claim.asInt();
            if (intValue != null) {
                return intValue;
            }
        } catch (Exception e) {
            // Not an integer, try other types
        }

        try {
            // Boolean claims
            Boolean boolValue = claim.asBoolean();
            if (boolValue != null) {
                return boolValue;
            }
        } catch (Exception e) {
            // Not a boolean, try object
        }

        try {
            // Complex object claims
            Map<String, Object> mapValue = claim.asMap();
            if (mapValue != null) {
                return mapValue;
            }
        } catch (Exception e) {
            // Not a map, try array
        }

        try {
            // Array claims
            Object[] arrayValue = claim.asArray(Object.class);
            if (arrayValue != null) {
                return arrayValue;
            }
        } catch (Exception e) {
            // Not an array
        }

        log.debug("Unable to extract value from claim, returning null");
        return null;
    }

    /**
     * Navigates a single segment in an object structure.
     *
     * @param obj the current object
     * @param segment the path segment
     * @return the value at the segment, or null if not found
     */
    private Object navigateObjectSegment(Object obj, String segment) {
        if (obj == null) {
            return null;
        }

        // Handle Map objects
        if (obj instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> map = (Map<String, Object>) obj;
            return map.get(segment);
        }

        // Handle arrays (simple index access)
        if (obj.getClass().isArray() && segment.matches("\\d+")) {
            try {
                int index = Integer.parseInt(segment);
                Object[] array = (Object[]) obj;
                if (index >= 0 && index < array.length) {
                    return array[index];
                }
            } catch (Exception e) {
                log.debug("Failed to access array index '{}': {}", segment, e.getMessage());
            }
        }

        // For other object types, convert to JSON and navigate
        try {
            JsonNode jsonNode = objectMapper.valueToTree(obj);
            JsonNode childNode = jsonNode.get(segment);
            if (childNode != null && !childNode.isNull()) {
                return objectMapper.treeToValue(childNode, Object.class);
            }
        } catch (Exception e) {
            log.debug("Failed to navigate object segment '{}': {}", segment, e.getMessage());
        }

        return null;
    }

    /**
     * Converts a claim value to its string representation.
     *
     * @param claimValue the claim value
     * @return the string representation
     */
    private String convertClaimToString(Object claimValue) {
        if (claimValue == null) {
            return null;
        }

        if (claimValue instanceof String) {
            return (String) claimValue;
        }

        // For complex objects, convert to JSON string
        if (isComplexObject(claimValue)) {
            try {
                return objectMapper.writeValueAsString(claimValue);
            } catch (Exception e) {
                log.debug("Failed to convert claim to JSON, using toString(): {}", e.getMessage());
                return claimValue.toString();
            }
        }

        return claimValue.toString();
    }

    /**
     * Checks if an object is complex and needs JSON serialization.
     *
     * @param value the value to check
     * @return true if the object is complex, false otherwise
     */
    private boolean isComplexObject(Object value) {
        if (value == null) {
            return false;
        }

        Class<?> clazz = value.getClass();

        return !clazz.isPrimitive()
            && !Number.class.isAssignableFrom(clazz)
            && !Boolean.class.isAssignableFrom(clazz)
            && !Character.class.isAssignableFrom(clazz)
            && !(value instanceof String)
            && !clazz.isEnum();
    }

    /**
     * Validates that a JWT token path is properly formatted.
     *
     * @param path the path to validate
     * @return true if the path is valid, false otherwise
     */
    public boolean isValidPath(String path) {
        if (path == null || path.trim().isEmpty()) {
            return false;
        }

        // Accept paths with or without "claims." prefix
        String cleanPath = path.startsWith("claims.") ? path.substring(7) : path;

        // Basic validation: no consecutive dots, no leading/trailing dots
        return !cleanPath.contains("..")
            && !cleanPath.startsWith(".")
            && !cleanPath.endsWith(".")
            && cleanPath.matches("^[a-zA-Z][a-zA-Z0-9_.\\[\\]]*$");
    }

    /**
     * Custom exception for token resolution errors.
     */
    public static class TokenResolutionException extends RuntimeException {
        public TokenResolutionException(String message) {
            super(message);
        }

        public TokenResolutionException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}