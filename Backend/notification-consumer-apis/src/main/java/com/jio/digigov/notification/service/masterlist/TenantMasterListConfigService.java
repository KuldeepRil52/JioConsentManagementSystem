package com.jio.digigov.notification.service.masterlist;

import com.jio.digigov.notification.config.MultiTenantMongoConfig;
import com.jio.digigov.notification.dto.masterlist.MasterListEntry;
import com.jio.digigov.notification.dto.masterlist.EventMappingConfig;
import com.jio.digigov.notification.entity.TenantMasterListConfig;
import com.jio.digigov.notification.enums.EventType;
import com.jio.digigov.notification.repository.TenantMasterListConfigRepository;
import com.jio.digigov.notification.repository.TenantMasterListConfigRepositoryImpl;
import com.jio.digigov.notification.service.masterlist.resolver.DatabaseResolver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

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
    private final MasterListConfigLoader masterListConfigLoader;
    private final DatabaseResolver databaseResolver;

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
     * Creates or updates the master list configuration for a tenant.
     *
     * @param tenantId the tenant identifier
     * @param masterListConfig the master list configuration map
     * @param description optional description for the configuration
     * @return the created/updated configuration
     * @throws TenantMasterListConfigException if the operation fails
     */
    public TenantMasterListConfig createOrUpdateTenantConfig(String tenantId,
                                                            Map<String, MasterListEntry> masterListConfig,
                                                            String description) {
        log.info("Creating/updating master list configuration for tenant {} with {} entries",
                tenantId, masterListConfig.size());

        try {
            MongoTemplate mongoTemplate = mongoConfig.getMongoTemplateForTenant(tenantId);
            TenantMasterListConfigRepository repository = createRepository(mongoTemplate);

            // Validate the configuration
            if (!isValidMasterListConfig(masterListConfig)) {
                throw new TenantMasterListConfigException("Invalid master list configuration provided");
            }

            // Deactivate existing active configuration
            Optional<TenantMasterListConfig> existingConfig = repository.findByIsActive(true);
            if (existingConfig.isPresent()) {
                TenantMasterListConfig existing = existingConfig.get();
                existing.setIsActive(false);
                repository.save(existing);
                log.debug("Deactivated previous configuration version {} for tenant {}",
                         existing.getVersion(), tenantId);
            }

            // Get next version number
            Integer nextVersion = getNextVersion(repository);

            // Create new configuration
            TenantMasterListConfig newConfig = new TenantMasterListConfig();
            newConfig.setMasterListConfig(masterListConfig);
            newConfig.setDescription(description);
            newConfig.setVersion(nextVersion);
            newConfig.setIsActive(true);

            TenantMasterListConfig savedConfig = repository.save(newConfig);

            // Evict cached master list configuration and database values
            evictMasterListCache(tenantId);

            log.info("Successfully created master list configuration version {} for tenant {} with {} entries",
                    nextVersion, tenantId, masterListConfig.size());

            return savedConfig;

        } catch (TenantMasterListConfigException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to create/update master list configuration for tenant {}: {}",
                     tenantId, e.getMessage(), e);
            throw new TenantMasterListConfigException("Failed to create/update master list configuration", e);
        }
    }

    /**
     * Deletes the active master list configuration for a tenant.
     * This will cause the system to fall back to the static file configuration.
     *
     * @param tenantId the tenant identifier
     * @throws TenantMasterListConfigException if the operation fails
     */
    public void deleteActiveTenantConfig(String tenantId) {
        log.info("Deleting active master list configuration for tenant {}", tenantId);

        try {
            MongoTemplate mongoTemplate = mongoConfig.getMongoTemplateForTenant(tenantId);
            TenantMasterListConfigRepository repository = createRepository(mongoTemplate);

            Optional<TenantMasterListConfig> activeConfig = repository.findByIsActive(true);
            if (activeConfig.isPresent()) {
                repository.delete(activeConfig.get());

                // Evict cached master list configuration and database values
                evictMasterListCache(tenantId);

                log.info("Successfully deleted active master list configuration for tenant {}", tenantId);
            } else {
                log.warn("No active master list configuration found to delete for tenant {}", tenantId);
            }

        } catch (Exception e) {
            log.error("Failed to delete master list configuration for tenant {}: {}",
                     tenantId, e.getMessage(), e);
            throw new TenantMasterListConfigException("Failed to delete master list configuration", e);
        }
    }

    /**
     * Gets all master list configurations for a tenant (including inactive ones).
     *
     * @param tenantId the tenant identifier
     * @return list of all configurations for the tenant
     */
    public java.util.List<TenantMasterListConfig> getAllTenantConfigs(String tenantId) {
        try {
            MongoTemplate mongoTemplate = mongoConfig.getMongoTemplateForTenant(tenantId);
            TenantMasterListConfigRepository repository = createRepository(mongoTemplate);

            return repository.findAll();
        } catch (Exception e) {
            log.error("Error retrieving all master list configurations for tenant {}: {}",
                     tenantId, e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    /**
     * Activates a specific version of the master list configuration.
     *
     * @param tenantId the tenant identifier
     * @param version the version to activate
     * @throws TenantMasterListConfigException if the operation fails
     */
    public void activateVersion(String tenantId, Integer version) {
        log.info("Activating master list configuration version {} for tenant {}", version, tenantId);

        try {
            MongoTemplate mongoTemplate = mongoConfig.getMongoTemplateForTenant(tenantId);
            TenantMasterListConfigRepository repository = createRepository(mongoTemplate);

            // Find the version to activate
            Optional<TenantMasterListConfig> targetConfig = repository.findByVersion(version);
            if (!targetConfig.isPresent()) {
                throw new TenantMasterListConfigException(
                    String.format("Version %d not found for tenant %s", version, tenantId));
            }

            // Deactivate current active configuration
            Optional<TenantMasterListConfig> currentActive = repository.findByIsActive(true);
            if (currentActive.isPresent()) {
                TenantMasterListConfig current = currentActive.get();
                current.setIsActive(false);
                repository.save(current);
            }

            // Activate target version
            TenantMasterListConfig target = targetConfig.get();
            target.setIsActive(true);
            repository.save(target);

            log.info("Successfully activated version {} for tenant {}", version, tenantId);

        } catch (TenantMasterListConfigException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to activate version {} for tenant {}: {}",
                     version, tenantId, e.getMessage(), e);
            throw new TenantMasterListConfigException("Failed to activate configuration version", e);
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
     * Gets the next version number for a new configuration.
     *
     * @param repository the repository to query
     * @return the next version number
     */
    private Integer getNextVersion(TenantMasterListConfigRepository repository) {
        Optional<TenantMasterListConfig> latest = repository.findTopByOrderByVersionDesc();
        return latest.map(config -> config.getVersion() + 1).orElse(1);
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
     * Evicts all master list related caches for a tenant.
     * This includes master list configuration cache and database value caches.
     *
     * @param tenantId the tenant identifier
     */
    private void evictMasterListCache(String tenantId) {
        log.info("Evicting master list caches for tenant: {}", tenantId);

        try {
            // Evict master list configuration cache for all businesses in this tenant
            // Note: We don't have a specific business ID here, so we need to evict broadly
            masterListConfigLoader.evictAllConfigurations(tenantId, "*");

            // Evict all database value caches for this tenant
            // Note: We don't have a specific business ID here, so we need to evict broadly
            databaseResolver.evictBusinessCache(tenantId, "*");

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
            masterListConfigLoader.evictConfiguration(tenantId, businessId);

            // Evict database value caches
            databaseResolver.evictBusinessCache(tenantId, businessId);

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