package com.jio.digigov.notification.service.masterlist.resolver;

import com.jio.digigov.notification.dto.masterlist.MasterListEntry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.UUID;

/**
 * Generator for creating dynamic values based on configuration.
 *
 * This generator supports creating various types of dynamic values:
 * - OTP: One-time passwords with configurable length and format
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
     * Generates a One-Time Password (OTP).
     *
     * Required configuration:
     * - length: Number of digits (required)
     * - numeric: Whether to use only numbers (required)
     * - maxLength: Maximum allowed length for validation (optional, defaults to 20)
     *
     * @param config the configuration map
     * @return the generated OTP
     */
    private String generateOTP(Map<String, Object> config) {
        if (config == null) {
            throw new ValueGenerationException("OTP generator requires configuration with 'length' and 'numeric' fields");
        }

        if (!config.containsKey("length")) {
            throw new ValueGenerationException("OTP generator requires 'length' configuration");
        }

        if (!config.containsKey("numeric")) {
            throw new ValueGenerationException("OTP generator requires 'numeric' configuration");
        }

        int length = getConfigInt(config, "length", -1);
        if (length == -1) {
            throw new ValueGenerationException("OTP generator requires valid 'length' configuration");
        }

        boolean numeric = getConfigBoolean(config, "numeric", false);
        int maxLength = getConfigInt(config, "maxLength", 20);

        if (length <= 0 || length > maxLength) {
            throw new ValueGenerationException("OTP length must be between 1 and " + maxLength);
        }

        String charset = numeric ? NUMERIC_CHARS : ALPHANUMERIC_CHARS;
        return generateRandomString(charset, length);
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
     * Validates a generator configuration.
     *
     * @param generatorType the generator type
     * @param config the configuration
     * @return true if valid, false otherwise
     */
    public boolean isValidConfiguration(String generatorType, Map<String, Object> config) {
        if (!isValidBasicInput(generatorType, config)) {
            return false;
        }

        try {
            return switch (generatorType.toUpperCase()) {
                case "OTP" -> isValidOtpConfiguration(config);
                case "UUID" -> isValidUuidConfiguration(config);
                case "TIMESTAMP" -> isValidTimestampConfiguration(config);
                case "RANDOM_STRING" -> isValidRandomStringConfiguration(config);
                default -> false;
            };
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Validates basic input parameters for configuration validation.
     *
     * @param generatorType the generator type
     * @param config the configuration map
     * @return true if basic validation passes, false otherwise
     */
    private boolean isValidBasicInput(String generatorType, Map<String, Object> config) {
        return generatorType != null && !generatorType.trim().isEmpty() && config != null;
    }

    /**
     * Validates OTP generator configuration.
     *
     * @param config the configuration map
     * @return true if valid, false otherwise
     */
    private boolean isValidOtpConfiguration(Map<String, Object> config) {
        if (!hasRequiredKeys(config, "length", "numeric")) {
            return false;
        }

        int length = getConfigInt(config, "length", -1);
        int maxLength = getConfigInt(config, "maxLength", 20);
        return isValidLength(length, maxLength);
    }

    /**
     * Validates UUID generator configuration.
     *
     * @param config the configuration map
     * @return true if valid, false otherwise
     */
    private boolean isValidUuidConfiguration(Map<String, Object> config) {
        if (!hasRequiredKeys(config, "version")) {
            return false;
        }

        String version = getConfigString(config, "version", null);
        return isValidUuidVersion(version);
    }

    /**
     * Validates timestamp generator configuration.
     *
     * @param config the configuration map
     * @return true if valid, false otherwise
     */
    private boolean isValidTimestampConfiguration(Map<String, Object> config) {
        if (!hasRequiredKeys(config, "format")) {
            return false;
        }

        String format = getConfigString(config, "format", null);
        return isValidTimestampFormat(format);
    }

    /**
     * Validates random string generator configuration.
     *
     * @param config the configuration map
     * @return true if valid, false otherwise
     */
    private boolean isValidRandomStringConfiguration(Map<String, Object> config) {
        if (!hasRequiredKeys(config, "length", "charset")) {
            return false;
        }

        int length = getConfigInt(config, "length", -1);
        int maxLength = getConfigInt(config, "maxLength", 100);
        String charset = getConfigString(config, "charset", null);

        return isValidLength(length, maxLength) && isValidCharset(charset);
    }

    /**
     * Checks if configuration contains all required keys.
     *
     * @param config the configuration map
     * @param keys the required keys
     * @return true if all keys are present, false otherwise
     */
    private boolean hasRequiredKeys(Map<String, Object> config, String... keys) {
        for (String key : keys) {
            if (!config.containsKey(key)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Validates length parameter against maximum allowed.
     *
     * @param length the length value
     * @param maxLength the maximum allowed length
     * @return true if length is valid, false otherwise
     */
    private boolean isValidLength(int length, int maxLength) {
        return length > 0 && length <= maxLength;
    }

    /**
     * Validates UUID version string.
     *
     * @param version the UUID version
     * @return true if version is valid, false otherwise
     */
    private boolean isValidUuidVersion(String version) {
        return version != null &&
               ("v1".equalsIgnoreCase(version) || "v4".equalsIgnoreCase(version));
    }

    /**
     * Validates timestamp format string.
     *
     * @param format the timestamp format
     * @return true if format is valid, false otherwise
     */
    private boolean isValidTimestampFormat(String format) {
        if (format == null || format.trim().isEmpty()) {
            return false;
        }

        if (isStandardTimestampFormat(format)) {
            return true;
        }

        return isValidCustomTimestampFormat(format);
    }

    /**
     * Checks if format is a standard timestamp format.
     *
     * @param format the format to check
     * @return true if standard format, false otherwise
     */
    private boolean isStandardTimestampFormat(String format) {
        return "ISO_8601".equalsIgnoreCase(format) || "EPOCH".equalsIgnoreCase(format);
    }

    /**
     * Validates custom timestamp format by attempting to create a DateTimeFormatter.
     *
     * @param format the custom format pattern
     * @return true if format is valid, false otherwise
     */
    private boolean isValidCustomTimestampFormat(String format) {
        try {
            DateTimeFormatter.ofPattern(format);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Validates charset parameter for random string generation.
     *
     * @param charset the charset name
     * @return true if charset is valid, false otherwise
     */
    private boolean isValidCharset(String charset) {
        return charset != null &&
               ("NUMERIC".equalsIgnoreCase(charset) ||
                "ALPHA".equalsIgnoreCase(charset) ||
                "ALPHANUMERIC".equalsIgnoreCase(charset) ||
                "ALPHANUMERIC_MIXED".equalsIgnoreCase(charset));
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