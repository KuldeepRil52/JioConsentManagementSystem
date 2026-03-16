package com.jio.digigov.notification.service.masterlist;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jio.digigov.notification.config.MultiTenantMongoConfig;
import com.jio.digigov.notification.dto.masterlist.MasterListConfig;
import com.jio.digigov.notification.dto.masterlist.MasterListEntry;
import com.jio.digigov.notification.entity.TenantMasterListConfig;
import com.jio.digigov.notification.enums.CacheType;
import com.jio.digigov.notification.service.cache.CacheService;
import com.jio.digigov.notification.service.masterlist.TenantMasterListConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Service responsible for loading master list configuration from multiple sources.
 *
 * This service implements a hierarchical configuration loading strategy:
 * 1. Load base configuration from static file: master-list-config.json
 * 2. Check for database override in tenant-specific collection: master_list_config
 * 3. Merge configurations with database taking precedence
 *
 * The database configuration allows for tenant and business-specific customizations
 * without requiring application restarts.
 *
 * @author Notification Service Team
 * @version 1.0
 * @since 2025-01-15
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MasterListConfigLoader {

    private final MultiTenantMongoConfig mongoConfig;
    private final ObjectMapper objectMapper;
    private final TenantMasterListConfigService tenantMasterListConfigService;
    private final CacheService<MasterListConfig> cacheService;

    private static final String MASTER_LIST_CONFIG_FILE = "master-list-config.json";

    /**
     * Loads the master list configuration for a specific tenant and business.
     *
     * Loading strategy:
     * 1. Check cache first for existing configuration
     * 2. If cache miss, check for tenant-specific database configuration
     * 3. Fall back to static file configuration if no tenant-specific config found
     * 4. Cache the result for future requests
     *
     * @param tenantId the tenant identifier
     * @param businessId the business identifier (for logging purposes, not used in selection)
     * @return the loaded master list configuration
     * @throws MasterListConfigException if configuration cannot be loaded
     */
    public MasterListConfig loadConfiguration(String tenantId, String businessId) {
        String cacheKey = buildMasterListCacheKey();

        try {
            // Step 1: Check cache first
            Optional<MasterListConfig> cachedConfig = cacheService.get(cacheKey, tenantId, businessId);

            if (cachedConfig.isPresent()) {
                log.debug("Cache hit for master list configuration: tenant: {}, business: {}", tenantId, businessId);
                return cachedConfig.get();
            }

            // Step 2: Cache miss - load from database or file
            log.debug("Cache miss for master list configuration: tenant: {}, business: {} - loading from source",
                     tenantId, businessId);

            MasterListConfig config = loadFromSource(tenantId, businessId);

            // Step 3: Cache the loaded configuration
            cacheService.put(cacheKey, config, tenantId, businessId, CacheType.MASTER_LIST_CONFIG);

            log.debug("Cached master list configuration for tenant: {}, business: {}, entries: {}",
                     tenantId, businessId, config.size());

            return config;

        } catch (Exception e) {
            log.error("Error retrieving cached master list configuration for tenant: {}, business: {}: {}",
                     tenantId, businessId, e.getMessage(), e);

            // Fallback to direct loading
            log.warn("Falling back to direct master list configuration loading for tenant: {}, business: {}",
                    tenantId, businessId);

            return loadFromSource(tenantId, businessId);
        }
    }

    /**
     * Loads master list configuration directly from database or file without caching.
     *
     * @param tenantId the tenant identifier
     * @param businessId the business identifier
     * @return the loaded master list configuration
     * @throws MasterListConfigException if configuration cannot be loaded
     */
    private MasterListConfig loadFromSource(String tenantId, String businessId) {
        log.info("Loading master list configuration from source for tenantId={}, businessId={}", tenantId, businessId);

        try {
            // Step 1: Check for tenant-specific database configuration
            Optional<TenantMasterListConfig> tenantConfig =
                tenantMasterListConfigService.getActiveTenantConfig(tenantId);

            if (tenantConfig.isPresent()) {
                MasterListConfig dbConfig = convertToMasterListConfig(tenantConfig.get());
                log.info("Using tenant-specific database configuration for tenantId={} with {} entries",
                        tenantId, dbConfig.size());
                dbConfig.setSource("DATABASE");
                return dbConfig;
            } else {
                // Step 2: Fall back to static file configuration
                MasterListConfig fileConfig = loadFromFile();
                log.info("Using static file configuration for tenantId={} with {} entries",
                        tenantId, fileConfig.size());
                fileConfig.setSource("FILE");
                return fileConfig;
            }

        } catch (Exception e) {
            log.error("Failed to load master list configuration for tenantId={}, businessId={}: {}",
                     tenantId, businessId, e.getMessage(), e);
            throw new MasterListConfigException("Failed to load master list configuration", e);
        }
    }

    /**
     * Loads the base master list configuration from the classpath file.
     *
     * @return the base configuration
     * @throws IOException if the file cannot be read or parsed
     */
    private MasterListConfig loadFromFile() throws IOException {
        ClassPathResource resource = new ClassPathResource(MASTER_LIST_CONFIG_FILE);

        if (!resource.exists()) {
            throw new IOException("Master list configuration file not found: " + MASTER_LIST_CONFIG_FILE);
        }

        try (InputStream inputStream = resource.getInputStream()) {
            // Parse JSON as Map first, then convert to MasterListEntry objects
            Map<String, Object> rawEntries = objectMapper.readValue(inputStream,
                new TypeReference<Map<String, Object>>() {});

            Map<String, MasterListEntry> entries = new HashMap<>();

            for (Map.Entry<String, Object> entry : rawEntries.entrySet()) {
                String masterLabel = entry.getKey();
                MasterListEntry listEntry = objectMapper.convertValue(entry.getValue(), MasterListEntry.class);

                if (!listEntry.isValid()) {
                    log.warn("Invalid master list entry for label '{}': {}", masterLabel, listEntry.getDescription());
                    continue;
                }

                entries.put(masterLabel, listEntry);
            }

            MasterListConfig config = MasterListConfig.builder()
                .entries(entries)
                .source("FILE")
                .createdAt(LocalDateTime.now())
                .build();

            log.debug("Successfully loaded {} valid entries from file configuration", entries.size());
            return config;
        }
    }

    /**
     * Converts a TenantMasterListConfig entity to a MasterListConfig DTO.
     *
     * @param tenantConfig the tenant configuration entity
     * @return the master list configuration DTO
     */
    private MasterListConfig convertToMasterListConfig(TenantMasterListConfig tenantConfig) {
        return MasterListConfig.builder()
            .entries(tenantConfig.getMasterListConfig())
            .createdAt(tenantConfig.getCreatedAt())
            .updatedAt(tenantConfig.getUpdatedAt())
            .source("DATABASE")
            .build();
    }

    /**
     * Evicts the master list configuration from cache for a specific tenant and business.
     *
     * @param tenantId the tenant identifier
     * @param businessId the business identifier
     */
    public void evictConfiguration(String tenantId, String businessId) {
        String cacheKey = buildMasterListCacheKey();
        cacheService.evict(cacheKey, tenantId, businessId);

        log.info("Evicted master list configuration from cache for tenant: {}, business: {}",
                tenantId, businessId);
    }

    /**
     * Evicts all master list configurations from cache for a tenant-business combination.
     *
     * @param tenantId the tenant identifier
     * @param businessId the business identifier
     */
    public void evictAllConfigurations(String tenantId, String businessId) {
        cacheService.evictByType(CacheType.MASTER_LIST_CONFIG, tenantId, businessId);

        log.info("Evicted all master list configurations from cache for tenant: {}, business: {}",
                tenantId, businessId);
    }

    /**
     * Preloads a master list configuration into the cache (cache warming).
     *
     * @param tenantId the tenant identifier
     * @param businessId the business identifier
     * @return true if configuration was successfully preloaded
     */
    public boolean preloadConfiguration(String tenantId, String businessId) {
        try {
            // This will load the configuration and cache it
            loadConfiguration(tenantId, businessId);
            log.debug("Successfully preloaded master list configuration for tenant: {}, business: {}",
                     tenantId, businessId);
            return true;
        } catch (Exception e) {
            log.warn("Failed to preload master list configuration for tenant: {}, business: {}: {}",
                    tenantId, businessId, e.getMessage());
            return false;
        }
    }

    /**
     * Refreshes a cached master list configuration by reloading from source.
     *
     * @param tenantId the tenant identifier
     * @param businessId the business identifier
     * @return refreshed configuration
     */
    public MasterListConfig refreshConfiguration(String tenantId, String businessId) {
        String cacheKey = buildMasterListCacheKey();

        log.debug("Refreshing master list configuration cache for tenant: {}, business: {}", tenantId, businessId);

        // Evict from cache first
        cacheService.evict(cacheKey, tenantId, businessId);

        // Reload from source (will automatically cache)
        return loadConfiguration(tenantId, businessId);
    }

    /**
     * Checks if a master list configuration exists in cache.
     *
     * @param tenantId the tenant identifier
     * @param businessId the business identifier
     * @return true if configuration exists in cache
     */
    public boolean isConfigurationCached(String tenantId, String businessId) {
        String cacheKey = buildMasterListCacheKey();
        return cacheService.exists(cacheKey, tenantId, businessId);
    }

    /**
     * Gets cache statistics for master list configuration caching.
     *
     * @return cache statistics
     */
    public Object getMasterListCacheStats() {
        return cacheService.getStats(CacheType.MASTER_LIST_CONFIG);
    }

    /**
     * Build cache key for master list configuration.
     * Following the pattern: masterlist:{tenantId}:{businessId}
     */
    private String buildMasterListCacheKey() {
        return CacheType.MASTER_LIST_CONFIG.getKey();
    }

    /**
     * Custom exception for master list configuration errors.
     */
    public static class MasterListConfigException extends RuntimeException {
        public MasterListConfigException(String message) {
            super(message);
        }

        public MasterListConfigException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}