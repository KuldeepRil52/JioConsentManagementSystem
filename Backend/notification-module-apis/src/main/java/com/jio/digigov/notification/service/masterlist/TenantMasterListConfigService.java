package com.jio.digigov.notification.service.masterlist;

import com.jio.digigov.notification.config.MultiTenantMongoConfig;
import com.jio.digigov.notification.dto.masterlist.MasterListEntry;
import com.jio.digigov.notification.dto.masterlist.EventMappingConfig;
import com.jio.digigov.notification.dto.request.masterlist.CreateMasterListRequestDto;
import com.jio.digigov.notification.entity.TenantMasterListConfig;
import com.jio.digigov.notification.enums.CacheType;
import com.jio.digigov.notification.enums.EventType;
import com.jio.digigov.notification.repository.TenantMasterListConfigRepository;
import com.jio.digigov.notification.repository.TenantMasterListConfigRepositoryImpl;
import com.jio.digigov.notification.service.cache.CacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Service for managing tenant-specific master list configurations.
 *
 * This service handles CRUD operations for tenant master list configurations
 * stored in tenant-specific databases. Each tenant can have its own customized
 * master list configuration that overrides the default static configuration.
 *
 * @author Notification Service Team
 * @version 1.0
 * @since 2025-01-15
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TenantMasterListConfigService {

    private final MultiTenantMongoConfig mongoConfig;
    private final CacheService<Object> cacheService;

    /**
     * Gets the active master list configuration for a tenant.
     *
     * @param tenantId the tenant identifier
     * @return the active master list configuration, or empty if not found
     */
    public Optional<TenantMasterListConfig> getActiveTenantConfig(String tenantId) {
        try {
            MongoTemplate mongoTemplate = mongoConfig.getMongoTemplateForTenant(tenantId);
            TenantMasterListConfigRepository repository = createRepository(mongoTemplate);

            Optional<TenantMasterListConfig> config = repository.findByIsActive(true);

            if (config.isPresent()) {
                log.debug("Found active master list configuration for tenant {} with {} entries",
                         tenantId, config.get().getEntryCount());
            } else {
                log.debug("No active master list configuration found for tenant {}", tenantId);
            }

            return config;
        } catch (Exception e) {
            log.error("Error retrieving active master list configuration for tenant {}: {}",
                     tenantId, e.getMessage(), e);
            return Optional.empty();
        }
    }

    /**
     * Gets the master list configuration for a tenant.
     *
     * @param tenantId the tenant identifier
     * @return the master list configuration, or empty if not found
     */
    public Optional<TenantMasterListConfig> getTenantConfig(String tenantId) {
        try {
            MongoTemplate mongoTemplate = mongoConfig.getMongoTemplateForTenant(tenantId);
            TenantMasterListConfigRepository repository = createRepository(mongoTemplate);

            List<TenantMasterListConfig> allConfigs = repository.findAll();
            Optional<TenantMasterListConfig> config = allConfigs.isEmpty()
                ? Optional.empty()
                : Optional.of(allConfigs.get(0));

            if (config.isPresent()) {
                log.debug("Found master list configuration for tenant {} with {} entries",
                         tenantId, config.get().getEntryCount());
            } else {
                log.debug("No master list configuration found for tenant {}", tenantId);
            }

            return config;
        } catch (Exception e) {
            log.error("Error retrieving master list configuration for tenant {}: {}",
                     tenantId, e.getMessage(), e);
            return Optional.empty();
        }
    }


    /**
     * Deletes the master list configuration for a tenant.
     * This will cause the system to fall back to the static file configuration.
     *
     * @param tenantId the tenant identifier
     * @throws TenantMasterListConfigException if the operation fails
     */
    public void deleteTenantConfig(String tenantId) {
        log.info("Deleting master list configuration for tenant {}", tenantId);

        try {
            MongoTemplate mongoTemplate = mongoConfig.getMongoTemplateForTenant(tenantId);
            TenantMasterListConfigRepository repository = createRepository(mongoTemplate);

            // Delete all configurations for this tenant (should only be one)
            repository.deleteAll();

            // Evict cached master list configuration and database values
            evictMasterListCache(tenantId);

            log.info("Successfully deleted master list configuration for tenant {}", tenantId);

        } catch (Exception e) {
            log.error("Failed to delete master list configuration for tenant {}: {}",
                     tenantId, e.getMessage(), e);
            throw new TenantMasterListConfigException("Failed to delete master list configuration", e);
        }
    }


    /**
     * Validates the master list configuration.
     *
     * @param masterListConfig the configuration to validate
     * @throws TenantMasterListConfigException if configuration is invalid
     */
    private void validateMasterListConfiguration(Map<String, MasterListEntry> masterListConfig) {
        if (!isValidMasterListConfig(masterListConfig)) {
            throw new TenantMasterListConfigException("Invalid master list configuration provided");
        }
    }


    /**
     * Validates a master list configuration map.
     *
     * @param masterListConfig the configuration to validate
     * @return true if valid, false otherwise
     */
    private boolean isValidMasterListConfig(Map<String, MasterListEntry> masterListConfig) {
        if (masterListConfig == null || masterListConfig.isEmpty()) {
            return false;
        }

        for (Map.Entry<String, MasterListEntry> entry : masterListConfig.entrySet()) {
            if (entry.getKey() == null || entry.getKey().trim().isEmpty()) {
                log.warn("Invalid master list entry: null or empty key");
                return false;
            }

            if (entry.getValue() == null || !entry.getValue().isValid()) {
                log.warn("Invalid master list entry for key '{}': {}",
                        entry.getKey(), entry.getValue());
                return false;
            }
        }

        return true;
    }


    /**
     * Creates a repository instance for the given MongoTemplate.
     * This is a workaround for multi-tenant repository usage.
     *
     * @param mongoTemplate the tenant-specific MongoTemplate
     * @return repository instance
     */
    private TenantMasterListConfigRepository createRepository(MongoTemplate mongoTemplate) {
        // Since we can't inject tenant-specific repositories, we'll use MongoTemplate directly
        // and create a wrapper that implements the repository interface
        return new TenantMasterListConfigRepositoryImpl(mongoTemplate);
    }

    /**
     * Creates a new master list configuration with event mappings.
     *
     * @param tenantId the tenant identifier
     * @param request the create request with configuration and event mappings
     * @return the created configuration
     * @throws TenantMasterListConfigException if the operation fails
     */
    public TenantMasterListConfig createMasterListConfig(String tenantId, CreateMasterListRequestDto request) {
        log.info("Creating master list config for tenant: {}", tenantId);

        // Validate no config exists
        Optional<TenantMasterListConfig> existing = getTenantConfig(tenantId);
        if (existing.isPresent()) {
            throw new TenantMasterListConfigException("Configuration already exists.");
        }

        // Build configuration
        TenantMasterListConfig config = new TenantMasterListConfig();
        config.setMasterListConfig(request.getMasterListConfig());
        config.setEventMappings(buildEventMappings(request.getEventMappings()));
        config.setDescription(request.getDescription());

        // Validate configuration
        if (!config.isValidConfiguration()) {
            throw new TenantMasterListConfigException("Invalid configuration");
        }

        try {
            MongoTemplate mongoTemplate = mongoConfig.getMongoTemplateForTenant(tenantId);
            TenantMasterListConfigRepository repository = createRepository(mongoTemplate);

            TenantMasterListConfig savedConfig = repository.save(config);

            // Evict cached master list configuration and database values
            evictMasterListCache(tenantId);

            return savedConfig;

        } catch (Exception e) {
            log.error("Failed to create master list configuration for tenant {}: {}",
                     tenantId, e.getMessage(), e);
            throw new TenantMasterListConfigException("Failed to create master list configuration", e);
        }
    }

    /**
     * Updates the master list configuration for a tenant.
     *
     * @param tenantId the tenant identifier
     * @param request the update request with configuration
     * @return the updated configuration
     * @throws TenantMasterListConfigException if the operation fails
     */
    public TenantMasterListConfig updateMasterListConfig(String tenantId, CreateMasterListRequestDto request) {
        log.info("Updating master list config for tenant: {}", tenantId);

        // Validate config exists
        Optional<TenantMasterListConfig> existing = getTenantConfig(tenantId);
        if (!existing.isPresent()) {
            throw new TenantMasterListConfigException("Configuration not found. Use POST to create.");
        }

        TenantMasterListConfig config = existing.get();
        config.setMasterListConfig(request.getMasterListConfig());
        config.setEventMappings(buildEventMappings(request.getEventMappings()));
        config.setDescription(request.getDescription());

        // Validate configuration
        if (!config.isValidConfiguration()) {
            throw new TenantMasterListConfigException("Invalid configuration");
        }

        try {
            MongoTemplate mongoTemplate = mongoConfig.getMongoTemplateForTenant(tenantId);
            TenantMasterListConfigRepository repository = createRepository(mongoTemplate);

            TenantMasterListConfig savedConfig = repository.save(config);

            // Evict cached master list configuration and database values
            evictMasterListCache(tenantId);

            return savedConfig;

        } catch (Exception e) {
            log.error("Failed to update master list configuration for tenant {}: {}",
                     tenantId, e.getMessage(), e);
            throw new TenantMasterListConfigException("Failed to update master list configuration", e);
        }
    }

    /**
     * Updates an existing tenant configuration without creating a new version.
     *
     * @param tenantId the tenant identifier
     * @param config the configuration to update
     * @return the updated configuration
     * @throws TenantMasterListConfigException if the operation fails
     */
    public TenantMasterListConfig updateTenantConfig(String tenantId, TenantMasterListConfig config) {
        log.debug("Updating master list configuration for tenant {}", tenantId);

        try {
            MongoTemplate mongoTemplate = mongoConfig.getMongoTemplateForTenant(tenantId);
            TenantMasterListConfigRepository repository = createRepository(mongoTemplate);

            TenantMasterListConfig savedConfig = repository.save(config);

            // Evict cached master list configuration and database values
            evictMasterListCache(tenantId);

            return savedConfig;

        } catch (Exception e) {
            log.error("Failed to update master list configuration for tenant {}: {}",
                     tenantId, e.getMessage(), e);
            throw new TenantMasterListConfigException("Failed to update master list configuration", e);
        }
    }

    /**
     * Adds or updates labels in the existing master list configuration.
     * This method merges new labels into the existing configuration without replacing it.
     *
     * @param tenantId the tenant identifier
     * @param newLabels the new labels to add/update
     * @param eventMappings optional event mappings to merge
     * @param description optional description update
     * @return the updated configuration
     * @throws TenantMasterListConfigException if the operation fails
     */
    public TenantMasterListConfig addLabelsToConfig(
            String tenantId,
            Map<String, MasterListEntry> newLabels,
            Map<EventType, Set<String>> eventMappings,
            String description) {

        log.info("Adding {} labels to existing master list config for tenant: {}", newLabels.size(), tenantId);

        // Get existing configuration
        Optional<TenantMasterListConfig> existing = getTenantConfig(tenantId);
        if (!existing.isPresent()) {
            throw new TenantMasterListConfigException(
                "Configuration not found. Cannot add labels to non-existent configuration.");
        }

        TenantMasterListConfig config = existing.get();

        // Merge new labels into existing configuration
        Map<String, MasterListEntry> currentLabels = config.getMasterListConfig();
        if (currentLabels == null) {
            currentLabels = new HashMap<>();
        }

        int addedCount = 0;
        int updatedCount = 0;

        for (Map.Entry<String, MasterListEntry> entry : newLabels.entrySet()) {
            if (currentLabels.containsKey(entry.getKey())) {
                log.debug("Updating existing label: {}", entry.getKey());
                updatedCount++;
            } else {
                log.debug("Adding new label: {}", entry.getKey());
                addedCount++;
            }
            currentLabels.put(entry.getKey(), entry.getValue());
        }

        config.setMasterListConfig(currentLabels);

        // Merge event mappings if provided
        if (eventMappings != null && !eventMappings.isEmpty()) {
            EventMappingConfig existingEventMappings = config.getEventMappings();
            if (existingEventMappings == null) {
                existingEventMappings = new EventMappingConfig();
            }

            for (Map.Entry<EventType, Set<String>> entry : eventMappings.entrySet()) {
                existingEventMappings.addEventMapping(entry.getKey(), entry.getValue());
            }
            config.setEventMappings(existingEventMappings);
        }

        // Update description if provided
        if (description != null && !description.trim().isEmpty()) {
            config.setDescription(description);
        }

        // Validate merged configuration
        if (!config.isValidConfiguration()) {
            throw new TenantMasterListConfigException("Merged configuration is invalid");
        }

        try {
            MongoTemplate mongoTemplate = mongoConfig.getMongoTemplateForTenant(tenantId);
            TenantMasterListConfigRepository repository = createRepository(mongoTemplate);

            TenantMasterListConfig savedConfig = repository.save(config);

            // Evict cached master list configuration and database values
            evictMasterListCache(tenantId);

            log.info("Successfully merged labels for tenant {}: {} added, {} updated, total: {}",
                    tenantId, addedCount, updatedCount, currentLabels.size());

            return savedConfig;

        } catch (Exception e) {
            log.error("Failed to add labels to master list configuration for tenant {}: {}",
                    tenantId, e.getMessage(), e);
            throw new TenantMasterListConfigException("Failed to add labels to configuration", e);
        }
    }

    private EventMappingConfig buildEventMappings(Map<EventType, Set<String>> eventMappings) {
        if (eventMappings == null || eventMappings.isEmpty()) {
            return new EventMappingConfig();
        }

        EventMappingConfig config = new EventMappingConfig();
        for (Map.Entry<EventType, Set<String>> entry : eventMappings.entrySet()) {
            config.addEventMapping(entry.getKey(), entry.getValue());
        }
        return config;
    }

    /**
     * Evicts all master list related caches for a tenant.
     * This includes master list configuration cache and database value caches.
     *
     * @param tenantId the tenant identifier
     */
    private void evictMasterListCache(String tenantId) {
        log.info("Evicting master list caches for tenant: {}", tenantId);

        try {
            // Evict master list configuration cache for all businesses in this tenant
            cacheService.evictByType(CacheType.MASTER_LIST_CONFIG, tenantId, "*");

            // Evict all database query caches for this tenant (using db-queries region)
            cacheService.evictByType(CacheType.MASTER_LIST_DB_VALUE, tenantId, "*");

            log.info("Successfully evicted master list caches for tenant: {}", tenantId);

        } catch (Exception e) {
            log.warn("Failed to evict some master list caches for tenant {}: {}",
                    tenantId, e.getMessage(), e);
            // Don't throw exception - cache eviction failures shouldn't break the main operation
        }
    }

    /**
     * Evicts master list caches for a specific tenant and business combination.
     *
     * @param tenantId the tenant identifier
     * @param businessId the business identifier
     */
    public void evictMasterListCacheForBusiness(String tenantId, String businessId) {
        log.info("Evicting master list caches for tenant: {}, business: {}", tenantId, businessId);

        try {
            // Evict master list configuration cache
            String configCacheKey = CacheType.MASTER_LIST_CONFIG.getKey();
            cacheService.evict(configCacheKey, tenantId, businessId);

            // Evict database value caches - using the correct cache region name
            // Use the db-queries region which is configured in application.yml
            String dbCacheKey = "db-queries";
            cacheService.evictByType(CacheType.MASTER_LIST_DB_VALUE, tenantId, businessId);

            log.info("Successfully evicted master list caches for tenant: {}, business: {}",
                    tenantId, businessId);

        } catch (Exception e) {
            log.warn("Failed to evict some master list caches for tenant {}, business {}: {}",
                    tenantId, businessId, e.getMessage(), e);
        }
    }

    /**
     * Custom exception for tenant master list configuration operations.
     */
    public static class TenantMasterListConfigException extends RuntimeException {
        public TenantMasterListConfigException(String message) {
            super(message);
        }

        public TenantMasterListConfigException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}