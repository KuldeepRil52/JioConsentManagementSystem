package com.jio.digigov.notification.service.masterlist.resolver;

import com.jio.digigov.notification.context.EventContext;
import com.jio.digigov.notification.dto.masterlist.MasterListEntry;
import com.jio.digigov.notification.dto.request.event.TriggerEventRequestDto;
import com.jio.digigov.notification.entity.otp.OTPRecord;
import com.jio.digigov.notification.enums.OTPStatus;
import com.jio.digigov.notification.service.otp.OTPRecordService;
import com.jio.digigov.notification.util.JwtTokenUtil;
import com.jio.digigov.notification.util.OTPEncryptionUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Map;
import java.util.UUID;

/**
 * Generator for creating dynamic values based on configuration.
 *
 * This generator supports creating various types of dynamic values:
 * - OTP: One-time credentialss with configurable length and format
 * - UUID: Universally unique identifiers with different versions
 * - TIMESTAMP: Current timestamps in various formats
 * - RANDOM_STRING: Random strings with configurable length and character sets
 *
 * All generated values are created fresh for each resolution (not cached)
 * to ensure uniqueness and security.
 *
 * Configuration Examples:
 * - OTP: {"length": 6, "numeric": true}
 * - UUID: {"version": "v4"}
 * - TIMESTAMP: {"format": "ISO_8601"} or {"format": "dd-MM-yyyy HH:mm:ss"}
 * - RANDOM_STRING: {"length": 10, "charset": "ALPHANUMERIC"}
 *
 * @author Notification Service Team
 * @version 1.0
 * @since 2025-01-15
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ValueGenerator {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final JwtTokenUtil jwtTokenUtil;
    private final OTPRecordService otpRecordService;
    private final OTPEncryptionUtil otpEncryptionUtil;

    @Value("${otp.default.expiry-minutes:5}")
    private int defaultExpiryMinutes;

    @Value("${otp.default.max-attempts:3}")
    private int defaultMaxAttempts;

    // Character sets for random string generation
    private static final String NUMERIC_CHARS = "0123456789";
    private static final String ALPHA_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final String ALPHANUMERIC_CHARS = ALPHA_CHARS + NUMERIC_CHARS;
    private static final String ALPHANUMERIC_LOWERCASE_CHARS = ALPHANUMERIC_CHARS + "abcdefghijklmnopqrstuvwxyz";

    /**
     * Generates a value based on the master list entry configuration.
     *
     * @param entry the master list entry with generator configuration
     * @return the generated value as a string
     * @throws ValueGenerationException if generation fails
     */
    public String generateValue(MasterListEntry entry) {
        if (entry == null) {
            throw new ValueGenerationException("Master list entry is null");
        }

        if (entry.getGenerator() == null || entry.getGenerator().trim().isEmpty()) {
            throw new ValueGenerationException("Generator type is null or empty");
        }

        String generator = entry.getGenerator().toUpperCase();
        Map<String, Object> config = entry.getConfig();

        log.debug("Generating value using generator: {}", generator);

        try {
            String generatedValue = switch (generator) {
                case "OTP" -> generateOTP(config);
                case "UUID" -> generateUUID(config);
                case "TIMESTAMP" -> generateTimestamp(config);
                case "RANDOM_STRING" -> generateRandomString(config);
                default -> throw new ValueGenerationException("Unsupported generator type: " + generator);
            };

            log.debug("Successfully generated value using {}: {}", generator, generatedValue);
            return generatedValue;

        } catch (ValueGenerationException e) {
            throw e; // Re-throw our custom exceptions
        } catch (Exception e) {
            log.error("Unexpected error generating value with {}: {}", generator, e.getMessage());
            throw new ValueGenerationException("Unexpected error during value generation", e);
        }
    }


    /**
     * Generates a value based on the master list entry configuration with request context.
     * This overloaded method supports JWT_TOKEN generation which requires request details.
     *
     * @param entry the master list entry with generator configuration
     * @param request the trigger event request
     * @param tenantId the tenant identifier
     * @param businessId the business identifier
     * @return the generated value as a string
     * @throws ValueGenerationException if generation fails
     */
    public String generateValue(MasterListEntry entry, TriggerEventRequestDto request, 
                               String tenantId, String businessId) {
        if (entry == null) {
            throw new ValueGenerationException("Master list entry is null");
        }

        if (entry.getGenerator() == null || entry.getGenerator().trim().isEmpty()) {
            throw new ValueGenerationException("Generator type is null or empty");
        }

        String generator = entry.getGenerator().toUpperCase();
        Map<String, Object> config = entry.getConfig();

        log.debug("Generating value using generator: {}", generator);

        try {
            String generatedValue = switch (generator) {
                case "OTP" -> generateOTP(config);
                case "UUID" -> generateUUID(config);
                case "TIMESTAMP" -> generateTimestamp(config);
                case "RANDOM_STRING" -> generateRandomString(config);
                case "JWT_TOKEN" -> generateJwtToken(request, tenantId, businessId);
                default -> throw new ValueGenerationException("Unsupported generator type: " + generator);
            };

            log.debug("Successfully generated value using {}: {}", generator, generatedValue);
            return generatedValue;

        } catch (ValueGenerationException e) {
            throw e; // Re-throw our custom exceptions
        } catch (Exception e) {
            log.error("Unexpected error generating value with {}: {}", generator, e.getMessage());
            throw new ValueGenerationException("Unexpected error during value generation", e);
        }
    }

        /**
     * Generates a One-Time Password (OTP).
     *
     * Supported configuration (all optional):
     * - length: Number of characters (optional, defaults to 6)
     * - numeric: Whether to use only numbers (optional, defaults to true)
     * - pattern: Custom regex pattern for allowed characters (optional, overrides numeric)
     * - charset: Predefined charset ("NUMERIC", "ALPHA", "ALPHANUMERIC", "ALPHANUMERIC_MIXED") (optional, overrides numeric)
     * - maxLength: Maximum allowed length for validation (optional, defaults to 20)
     *
     * Examples:
     * - {} -> 6-digit numeric OTP (default)
     * - {"length": 8} -> 8-digit numeric OTP
     * - {"length": 6, "numeric": false} -> 6-character alphanumeric OTP
     * - {"length": 4, "charset": "ALPHA"} -> 4-character alphabetic OTP
     * - {"length": 8, "pattern": "[A-Z0-9]"} -> 8-character OTP with uppercase letters and digits
     *
     * @param config the configuration map (can be null for defaults)
     * @return the generated OTP
     */
    private String generateOTP(Map<String, Object> config) {
        // Default values
        int defaultLength = 6;
        boolean defaultNumeric = true;
        int defaultMaxLength = 20;

        // Extract configuration with defaults
        int length = (config != null) ? getConfigInt(config, "length", defaultLength) : defaultLength;
        int maxLength = (config != null) ? getConfigInt(config, "maxLength", defaultMaxLength) : defaultMaxLength;

        // Validate length
        if (length <= 0 || length > maxLength) {
            throw new ValueGenerationException("OTP length must be between 1 and " + maxLength + ", got: " + length);
        }

        // Determine character set
        String charset;

        if (config != null && config.containsKey("pattern")) {
            // Pattern-based character set (most specific)
            String pattern = getConfigString(config, "pattern", null);
            if (pattern == null || pattern.trim().isEmpty()) {
                throw new ValueGenerationException("OTP pattern configuration cannot be empty");
            }
            charset = buildCharsetFromPattern(pattern);
            log.debug("Using pattern-based charset for OTP: {}", pattern);
        } else if (config != null && config.containsKey("charset")) {
            // Predefined charset
            String charsetName = getConfigString(config, "charset", null);
            if (charsetName == null || charsetName.trim().isEmpty()) {
                throw new ValueGenerationException("OTP charset configuration cannot be empty");
            }
            charset = switch (charsetName.toUpperCase()) {
                case "NUMERIC" -> NUMERIC_CHARS;
                case "ALPHA" -> ALPHA_CHARS;
                case "ALPHANUMERIC" -> ALPHANUMERIC_CHARS;
                case "ALPHANUMERIC_MIXED" -> ALPHANUMERIC_LOWERCASE_CHARS;
                default -> throw new ValueGenerationException("Unsupported OTP charset: " + charsetName);
            };
            log.debug("Using predefined charset for OTP: {}", charsetName);
        } else {
            // Numeric flag (default or explicit)
            boolean numeric = (config != null) ? getConfigBoolean(config, "numeric", defaultNumeric) : defaultNumeric;
            charset = numeric ? NUMERIC_CHARS : ALPHANUMERIC_CHARS;
            log.debug("Using numeric={} for OTP", numeric);
        }

        if (charset.isEmpty()) {
            throw new ValueGenerationException("OTP charset cannot be empty");
        }

        // Generate the OTP
        String otpValue = generateRandomString(charset, length);

        // Persist OTP record if context is available and eventType is INIT_OTP
        try {
            if (EventContext.hasContext()) {
                EventContext context = EventContext.getContext();

                // Only persist for INIT_OTP events
                if ("INIT_OTP".equalsIgnoreCase(context.getEventType())) {
                    persistOTPRecord(otpValue, config, context);
                }
            }
        } catch (Exception e) {
            log.error("Error persisting OTP record, but OTP generation succeeded: {}", e.getMessage());
            // Don't fail OTP generation if persistence fails
        }

        return otpValue;
    }

    /**
     * Persists an OTP record to the database.
     *
     * @param otpValue the generated OTP value
     * @param config the OTP generator configuration
     * @param context the event context
     */
    private void persistOTPRecord(String otpValue, Map<String, Object> config, EventContext context) {
        log.debug("Persisting OTP record for eventId={}, txnId={}", context.getEventId(), context.getTxnId());

        // Extract configuration with defaults
        int expiryMinutes = (config != null) ? getConfigInt(config, "expiryMinutes", defaultExpiryMinutes) : defaultExpiryMinutes;
        int maxAttempts = (config != null) ? getConfigInt(config, "maxAttempts", defaultMaxAttempts) : defaultMaxAttempts;
        int length = (config != null) ? getConfigInt(config, "length", 6) : 6;

        // Calculate expiry time
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiryAt = now.plusMinutes(expiryMinutes);

        // Determine channel from recipientType
        String channel = determineChannel(context.getRecipientType());

        // Encrypt OTP value before saving to database
        String encryptedOTP = otpEncryptionUtil.encrypt(otpValue);
        log.debug("OTP value encrypted successfully for txnId={}", context.getTxnId());

        // Build OTP record
        OTPRecord otpRecord = OTPRecord.builder()
                .txnId(context.getTxnId())
                .correlationId(context.getCorrelationId())
                .eventId(context.getEventId())
                .eventType(context.getEventType())
                .notificationId(context.getNotificationId())
                .businessId(context.getBusinessId())
                .otpValue(encryptedOTP)
                .channel(channel)
                .recipientType(context.getRecipientType())
                .recipientValue(context.getRecipientValue())
                .expiryAt(expiryAt)
                .attemptCount(0)
                .maxAttempts(maxAttempts)
                .otpLength(length)
                .expiryMinutes(expiryMinutes)
                .status(OTPStatus.PENDING)
                .build();

        // Save to tenant-specific database
        otpRecordService.saveOTPRecord(otpRecord, context.getTenantId());

        log.info("OTP record persisted successfully: eventId={}, txnId={}, expiryAt={}, maxAttempts={}",
                context.getEventId(), context.getTxnId(), expiryAt, maxAttempts);
    }

    /**
     * Determines the notification channel from recipient type.
     *
     * @param recipientType the recipient type (MOBILE or EMAIL)
     * @return the channel (SMS or EMAIL)
     */
    private String determineChannel(String recipientType) {
        if (recipientType == null) {
            return "UNKNOWN";
        }

        return switch (recipientType.toUpperCase()) {
            case "MOBILE" -> "SMS";
            case "EMAIL" -> "EMAIL";
            default -> recipientType.toUpperCase();
        };
    }

    /**
     * Builds a character set from a regex pattern.
     * Supports simple patterns like [A-Z0-9], [a-z], [0-9], etc.
     *
     * @param pattern the regex pattern
     * @return the character set string
     */
    private String buildCharsetFromPattern(String pattern) {
        // Remove surrounding brackets if present
        String cleanPattern = pattern.replaceAll("^\\[|\\]$", "");

        StringBuilder charset = new StringBuilder();

        try {
            int i = 0;
            while (i < cleanPattern.length()) {
                char c = cleanPattern.charAt(i);

                // Check for range pattern (e.g., A-Z, a-z, 0-9)
                if (i + 2 < cleanPattern.length() && cleanPattern.charAt(i + 1) == '-') {
                    char start = c;
                    char end = cleanPattern.charAt(i + 2);

                    // Add all characters in range
                    for (char ch = start; ch <= end; ch++) {
                        charset.append(ch);
                    }
                    i += 3; // Skip the range
                } else {
                    // Single character
                    charset.append(c);
                    i++;
                }
            }
        } catch (Exception e) {
            log.error("Failed to parse OTP pattern '{}': {}", pattern, e.getMessage());
            throw new ValueGenerationException("Invalid OTP pattern: " + pattern + ". Use format like [A-Z0-9] or [a-z]", e);
        }

        String result = charset.toString();
        if (result.isEmpty()) {
            throw new ValueGenerationException("OTP pattern produced empty charset: " + pattern);
        }

        log.debug("Built charset from pattern '{}': {} characters", pattern, result.length());
        return result;
    }

    /**
     * Generates a UUID.
     *
     * Required configuration:
     * - version: UUID version ("v1", "v4") (required)
     *
     * @param config the configuration map
     * @return the generated UUID
     */
    private String generateUUID(Map<String, Object> config) {
        if (config == null) {
            throw new ValueGenerationException("UUID generator requires configuration with 'version' field");
        }

        if (!config.containsKey("version")) {
            throw new ValueGenerationException("UUID generator requires 'version' configuration");
        }

        String version = getConfigString(config, "version", null);
        if (version == null || version.trim().isEmpty()) {
            throw new ValueGenerationException("UUID generator requires valid 'version' configuration");
        }

        return switch (version.toLowerCase()) {
            case "v1" -> {
                // For v1, we'll use v4 but add timestamp-based randomness
                // True v1 UUIDs require MAC address which we want to avoid
                yield UUID.randomUUID().toString();
            }
            case "v4" -> UUID.randomUUID().toString();
            default -> throw new ValueGenerationException("Unsupported UUID version: " + version);
        };
    }

    /**
     * Generates a timestamp.
     *
     * Required configuration:
     * - format: Timestamp format ("ISO_8601", "EPOCH", or custom pattern) (required)
     *
     * @param config the configuration map
     * @return the generated timestamp
     */
    private String generateTimestamp(Map<String, Object> config) {
        if (config == null) {
            throw new ValueGenerationException("TIMESTAMP generator requires configuration with 'format' field");
        }

        if (!config.containsKey("format")) {
            throw new ValueGenerationException("TIMESTAMP generator requires 'format' configuration");
        }

        String format = getConfigString(config, "format", null);
        if (format == null || format.trim().isEmpty()) {
            throw new ValueGenerationException("TIMESTAMP generator requires valid 'format' configuration");
        }

        LocalDateTime now = LocalDateTime.now();

        return switch (format.toUpperCase()) {
            case "ISO_8601" -> now.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            case "EPOCH" -> String.valueOf(System.currentTimeMillis());
            default -> {
                // Try to parse as custom DateTimeFormatter pattern
                try {
                    DateTimeFormatter customFormatter = DateTimeFormatter.ofPattern(format);
                    yield now.format(customFormatter);
                } catch (Exception e) {
                    log.error("Invalid timestamp format '{}': {}", format, e.getMessage());
                    throw new ValueGenerationException("Invalid timestamp format: " + format, e);
                }
            }
        };
    }

    /**
     * Generates a random string.
     *
     * Required configuration:
     * - length: String length (required)
     * - charset: Character set ("NUMERIC", "ALPHA", "ALPHANUMERIC", "ALPHANUMERIC_MIXED") (required)
     * - maxLength: Maximum allowed length for validation (optional, defaults to 100)
     *
     * @param config the configuration map
     * @return the generated random string
     */
    private String generateRandomString(Map<String, Object> config) {
        if (config == null) {
            throw new ValueGenerationException("RANDOM_STRING generator requires configuration with 'length' and 'charset' fields");
        }

        if (!config.containsKey("length")) {
            throw new ValueGenerationException("RANDOM_STRING generator requires 'length' configuration");
        }

        if (!config.containsKey("charset")) {
            throw new ValueGenerationException("RANDOM_STRING generator requires 'charset' configuration");
        }

        int length = getConfigInt(config, "length", -1);
        if (length == -1) {
            throw new ValueGenerationException("RANDOM_STRING generator requires valid 'length' configuration");
        }

        String charsetName = getConfigString(config, "charset", null);
        if (charsetName == null || charsetName.trim().isEmpty()) {
            throw new ValueGenerationException("RANDOM_STRING generator requires valid 'charset' configuration");
        }

        int maxLength = getConfigInt(config, "maxLength", 100);

        if (length <= 0 || length > maxLength) {
            throw new ValueGenerationException("Random string length must be between 1 and " + maxLength);
        }

        String charset = switch (charsetName.toUpperCase()) {
            case "NUMERIC" -> NUMERIC_CHARS;
            case "ALPHA" -> ALPHA_CHARS;
            case "ALPHANUMERIC" -> ALPHANUMERIC_CHARS;
            case "ALPHANUMERIC_MIXED" -> ALPHANUMERIC_LOWERCASE_CHARS;
            default -> throw new ValueGenerationException("Unsupported charset: " + charsetName);
        };

        return generateRandomString(charset, length);
    }

    /**
     * Generates a random string from the specified character set.
     *
     * @param charset the character set to use
     * @param length the desired length
     * @return the generated random string
     */
    private String generateRandomString(String charset, int length) {
        StringBuilder result = new StringBuilder(length);

        for (int i = 0; i < length; i++) {
            int randomIndex = SECURE_RANDOM.nextInt(charset.length());
            result.append(charset.charAt(randomIndex));
        }

        return result.toString();
    }

    /**
     * Extracts a string configuration value with a default.
     *
     * @param config the configuration map
     * @param key the configuration key
     * @param defaultValue the default value
     * @return the configuration value or default
     */
    private String getConfigString(Map<String, Object> config, String key, String defaultValue) {
        if (config == null || !config.containsKey(key)) {
            return defaultValue;
        }

        Object value = config.get(key);
        if (value instanceof String) {
            return (String) value;
        }

        return value != null ? value.toString() : defaultValue;
    }

    /**
     * Extracts an integer configuration value with a default.
     *
     * @param config the configuration map
     * @param key the configuration key
     * @param defaultValue the default value
     * @return the configuration value or default
     */
    private int getConfigInt(Map<String, Object> config, String key, int defaultValue) {
        if (config == null || !config.containsKey(key)) {
            return defaultValue;
        }

        Object value = config.get(key);

        if (value instanceof Integer) {
            return (Integer) value;
        }

        if (value instanceof Number) {
            return ((Number) value).intValue();
        }

        if (value instanceof String) {
            try {
                return Integer.parseInt((String) value);
            } catch (NumberFormatException e) {
                log.warn("Invalid integer value for config key '{}': '{}', using default: {}",
                        key, value, defaultValue);
                return defaultValue;
            }
        }

        return defaultValue;
    }

    /**
     * Extracts a boolean configuration value with a default.
     *
     * @param config the configuration map
     * @param key the configuration key
     * @param defaultValue the default value
     * @return the configuration value or default
     */
    private boolean getConfigBoolean(Map<String, Object> config, String key, boolean defaultValue) {
        if (config == null || !config.containsKey(key)) {
            return defaultValue;
        }

        Object value = config.get(key);

        if (value instanceof Boolean) {
            return (Boolean) value;
        }

        if (value instanceof String) {
            return Boolean.parseBoolean((String) value);
        }

        return defaultValue;
    }

    /**
     * Generates a JWT token containing the event payload and headers.
     *
     * @param request the trigger event request containing payload and headers
     * @param tenantId the tenant identifier
     * @param businessId the business identifier
     * @return the generated JWT token
     */
    private String generateJwtToken(TriggerEventRequestDto request, String tenantId, String businessId) {
        if (request == null) {
            throw new ValueGenerationException("TriggerEventRequestDto is required for JWT_TOKEN generation");
        }

        Map<String, Object> eventPayload = request.getEventPayload();
        if (eventPayload == null) {
            throw new ValueGenerationException("Event payload is required for JWT_TOKEN generation");
        }

        // Extract event type from request
        String eventType = request.getEventType();
        
        // Generate transaction ID if not available (TriggerEventRequestDto doesn't have this field)
        String transactionId = "TXN-" + System.currentTimeMillis();

        log.debug("Generating JWT token for tenantId={}, businessId={}, eventType={}", 
                 tenantId, businessId, eventType);

        try {
            return jwtTokenUtil.generateToken(eventPayload, tenantId, businessId, transactionId, eventType);
        } catch (Exception e) {
            log.error("Failed to generate JWT token: {}", e.getMessage());
            throw new ValueGenerationException("Failed to generate JWT token", e);
        }
    }

        /**
     * Validates a generator configuration.
     *
     * @param generatorType the generator type
     * @param config the configuration
     * @return true if valid, false otherwise
     */
    public boolean isValidConfiguration(String generatorType, Map<String, Object> config) {
        if (generatorType == null || generatorType.trim().isEmpty()) {
            return false;
        }

        if (config == null) {
            return false;
        }

        try {
            switch (generatorType.toUpperCase()) {
                case "OTP" -> {
                    // All configs are optional with defaults, just validate if provided
                    int length = getConfigInt(config, "length", 6); // default 6
                    int maxLength = getConfigInt(config, "maxLength", 20);

                    // Validate length if provided
                    if (length <= 0 || length > maxLength) {
                        return false;
                    }

                    // Validate charset if provided
                    if (config.containsKey("charset")) {
                        String charset = getConfigString(config, "charset", null);
                        if (charset != null && !charset.trim().isEmpty()) {
                            String charsetUpper = charset.toUpperCase();
                            if (!("NUMERIC".equals(charsetUpper) || "ALPHA".equals(charsetUpper)
                                || "ALPHANUMERIC".equals(charsetUpper) || "ALPHANUMERIC_MIXED".equals(charsetUpper))) {
                                return false;
                            }
                        }
                    }

                    // Validate pattern if provided
                    if (config.containsKey("pattern")) {
                        String pattern = getConfigString(config, "pattern", null);
                        if (pattern == null || pattern.trim().isEmpty()) {
                            return false;
                        }
                    }

                    return true;
                }
                case "UUID" -> {
                    if (!config.containsKey("version")) {
                        return false;
                    }
                    String version = getConfigString(config, "version", null);
                    return version != null && ("v1".equalsIgnoreCase(version) || "v4".equalsIgnoreCase(version));
                }
                case "TIMESTAMP" -> {
                    if (!config.containsKey("format")) {
                        return false;
                    }
                    String format = getConfigString(config, "format", null);
                    if (format == null || format.trim().isEmpty()) {
                        return false;
                    }
                    if ("ISO_8601".equalsIgnoreCase(format) || "EPOCH".equalsIgnoreCase(format)) {
                        return true;
                    }
                    // Try to validate custom format
                    try {
                        DateTimeFormatter.ofPattern(format);
                        return true;
                    } catch (Exception e) {
                        return false;
                    }
                }
                case "RANDOM_STRING" -> {
                    if (!config.containsKey("length") || !config.containsKey("charset")) {
                        return false;
                    }
                    int length = getConfigInt(config, "length", -1);
                    int maxLength = getConfigInt(config, "maxLength", 100);
                    String charset = getConfigString(config, "charset", null);
                    return length > 0 && length <= maxLength && charset != null
                        && ("NUMERIC".equalsIgnoreCase(charset)
                            || "ALPHA".equalsIgnoreCase(charset)
                            || "ALPHANUMERIC".equalsIgnoreCase(charset)
                            || "ALPHANUMERIC_MIXED".equalsIgnoreCase(charset));
                }
                default -> {
                    return false;
                }
            }
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Custom exception for value generation errors.
     */
    public static class ValueGenerationException extends RuntimeException {
        public ValueGenerationException(String message) {
            super(message);
        }

        public ValueGenerationException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}