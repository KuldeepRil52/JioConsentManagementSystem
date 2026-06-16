package com.jio.digigov.notification.config;

import com.jio.digigov.notification.config.properties.CacheProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;

/**
 * Cache configuration for the notification service.
 *
 * Supports two cache implementations:
 * - Caffeine: High-performance in-memory cache
 * - MongoDB: Distributed cache using tenant-specific databases
 *
 * Only one cache type can be active at a time, configured via application properties.
 */
@Configuration
@EnableCaching
@EnableConfigurationProperties(CacheProperties.class)
@ConditionalOnProperty(name = "cache.enabled", havingValue = "true", matchIfMissing = true)
@Slf4j
public class CacheConfiguration {

    private final CacheProperties cacheProperties;

    public CacheConfiguration(CacheProperties cacheProperties) {
        this.cacheProperties = cacheProperties;
    }

    @PostConstruct
    public void initialize() {
        validateConfiguration();
        logCacheConfiguration();
    }

    /**
     * Validate cache configuration on startup
     */
    private void validateConfiguration() {
        try {
            cacheProperties.validate();
            log.info("Cache configuration validation passed");
        } catch (Exception e) {
            log.error("Cache configuration validation failed: {}", e.getMessage());
            if (cacheProperties.getValidation().isFailOnInvalidConfig()) {
                throw new IllegalStateException("Invalid cache configuration", e);
            }
        }
    }

    /**
     * Log active cache configuration
     */
    private void logCacheConfiguration() {
        if (!cacheProperties.isEnabled()) {
            log.info("Cache is DISABLED");
            return;
        }

        String activeType = cacheProperties.getType();
        log.info("Cache configuration:");
        log.info("  Type: {}", activeType);
        log.info("  Enabled: {}", cacheProperties.isEnabled());

        if (cacheProperties.isCaffeineEnabled()) {
            log.info("  Caffeine specs: {}", cacheProperties.getCaffeine().getSpecs().keySet());
        }

        if (cacheProperties.isMongoDBEnabled()) {
            log.info("  MongoDB collection: {}", cacheProperties.getMongodb().getCollection());
            log.info("  TTL index creation: {}", cacheProperties.getMongodb().isCreateTtlIndex());
        }

        log.info("  TTL settings: {}", cacheProperties.getTtl().keySet());
        log.info("  Refresh enabled: {}", cacheProperties.getRefresh().isEnabled());

        // Log environment-specific warnings
        if ("caffeine".equals(activeType)) {
            log.info("Using Caffeine cache - suitable for single-instance deployments");
        } else if ("mongodb".equals(activeType)) {
            log.info("Using MongoDB cache - suitable for multi-instance deployments");
        }
    }

    /**
     * Configuration for Caffeine cache
     */
    @Configuration
    @ConditionalOnProperty(name = "cache.type", havingValue = "caffeine")
    static class CaffeineConfiguration {

        @PostConstruct
        public void logConfiguration() {
            log.info("Caffeine cache configuration loaded");
        }
    }

    /**
     * Configuration for MongoDB cache
     */
    @Configuration
    @ConditionalOnProperty(name = "cache.type", havingValue = "mongodb")
    static class MongoDBConfiguration {

        @PostConstruct
        public void logConfiguration() {
            log.info("MongoDB cache configuration loaded");
        }
    }

    /**
     * Disabled cache configuration
     */
    @Configuration
    @ConditionalOnProperty(name = "cache.enabled", havingValue = "false")
    static class DisabledCacheConfiguration {

        @PostConstruct
        public void logConfiguration() {
            log.info("Cache is DISABLED - all cache operations will be no-ops");
        }
    }
}