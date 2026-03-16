package com.jio.digigov.notification.config.properties;

import com.jio.digigov.notification.enums.CacheType;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Configuration properties for the cache service.
 * Supports both Caffeine and MongoDB cache implementations.
 */
@Data
@Component
@ConfigurationProperties(prefix = "cache")
public class CacheProperties {

    /**
     * Whether caching is enabled globally
     */
    private boolean enabled = true;

    /**
     * Cache implementation type: "caffeine" or "mongodb"
     */
    private String type = "caffeine";

    /**
     * Caffeine-specific configuration
     */
    private Caffeine caffeine = new Caffeine();

    /**
     * MongoDB-specific configuration
     */
    private MongoDB mongodb = new MongoDB();

    /**
     * TTL settings per cache type
     */
    private Map<String, Duration> ttl = new HashMap<>();

    /**
     * Cache refresh configuration
     */
    private Refresh refresh = new Refresh();

    /**
     * Cache validation settings
     */
    private Validation validation = new Validation();

    /**
     * Caffeine cache configuration
     */
    @Data
    public static class Caffeine {
        private boolean enabled = true;
        private Map<String, Spec> specs = new HashMap<>();

        @Data
        public static class Spec {
            private long maxSize = 1000;
            private Duration expireAfterWrite = Duration.ofMinutes(30);
            private Duration expireAfterAccess;
            private Duration refreshAfterWrite;
        }
    }

    /**
     * MongoDB cache configuration
     */
    @Data
    public static class MongoDB {
        private boolean enabled = false;
        private String collection = "cache_entries";
        private boolean createTtlIndex = true;
    }

    /**
     * Cache refresh configuration
     */
    @Data
    public static class Refresh {
        private boolean enabled = true;
        private Map<String, Duration> beforeExpiry = new HashMap<>();
        private Schedule schedule = new Schedule();

        @Data
        public static class Schedule {
            private String cron = "0 */5 * * * *"; // Every 5 minutes
        }
    }

    /**
     * Cache validation configuration
     */
    @Data
    public static class Validation {
        private boolean failOnInvalidConfig = true;
        private Duration maxTtl = Duration.ofHours(24);
        private Duration minTtl = Duration.ofMinutes(1);
    }

    /**
     * Gets the TTL for a specific cache type
     */
    public Duration getTtlForType(CacheType cacheType) {
        return ttl.getOrDefault(cacheType.getKey(), Duration.ofMinutes(30));
    }

    /**
     * Gets the refresh before expiry duration for a cache type
     */
    public Duration getRefreshBeforeExpiryForType(CacheType cacheType) {
        String category = getCategoryForCacheType(cacheType);
        return refresh.beforeExpiry.getOrDefault(category, Duration.ofMinutes(5));
    }

    /**
     * Maps cache types to refresh categories
     */
    private String getCategoryForCacheType(CacheType cacheType) {
        switch (cacheType) {
            case DIGIGOV_TOKEN:
            case DIGIGOV_ADMIN_TOKEN:
                return "tokens";
            case NG_CONFIGURATION:
            case MASTER_LIST_CONFIG:
            case EVENT_CONFIG:
                return "configurations";
            case TEMPLATE:
                return "templates";
            default:
                return "other";
        }
    }

    /**
     * Validates cache configuration
     */
    public void validate() {
        if (!enabled) {
            return;
        }

        // Ensure only one cache type is enabled
        if (caffeine.enabled && mongodb.enabled) {
            throw new IllegalArgumentException("Only one cache type can be enabled at a time");
        }

        if (!caffeine.enabled && !mongodb.enabled) {
            throw new IllegalArgumentException("At least one cache type must be enabled when cache is enabled");
        }

        // Validate TTL values
        for (Map.Entry<String, Duration> entry : ttl.entrySet()) {
            Duration ttlValue = entry.getValue();
            if (ttlValue.compareTo(validation.minTtl) < 0) {
                throw new IllegalArgumentException("TTL for " + entry.getKey() + " is below minimum: " + ttlValue);
            }
            if (ttlValue.compareTo(validation.maxTtl) > 0) {
                throw new IllegalArgumentException("TTL for " + entry.getKey() + " exceeds maximum: " + ttlValue);
            }
        }
    }

    /**
     * Checks if MongoDB cache is enabled
     */
    public boolean isMongoDBEnabled() {
        return enabled && "mongodb".equalsIgnoreCase(type) && mongodb.enabled;
    }

    /**
     * Checks if Caffeine cache is enabled
     */
    public boolean isCaffeineEnabled() {
        return enabled && "caffeine".equalsIgnoreCase(type) && caffeine.enabled;
    }
}