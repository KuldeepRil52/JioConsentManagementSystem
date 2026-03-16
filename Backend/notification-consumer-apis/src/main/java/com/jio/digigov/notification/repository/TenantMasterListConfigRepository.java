package com.jio.digigov.notification.repository;

import com.jio.digigov.notification.entity.TenantMasterListConfig;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository interface for TenantMasterListConfig entity.
 *
 * Since we're using a multi-tenant database approach, this repository
 * operates on tenant-specific databases. Each tenant database should
 * have only one master list configuration record.
 *
 * @author Notification Service Team
 * @version 1.0
 * @since 2025-01-15
 */
@Repository
public interface TenantMasterListConfigRepository extends MongoRepository<TenantMasterListConfig, String> {

    /**
     * Finds the active master list configuration for the current tenant.
     * Since we're in a tenant-specific database, there should be only one active record.
     *
     * @param isActive whether the configuration is active
     * @return the active master list configuration
     */
    Optional<TenantMasterListConfig> findByIsActive(Boolean isActive);

    /**
     * Finds the master list configuration by version.
     * Useful for retrieving specific versions or rollback scenarios.
     *
     * @param version the version number
     * @return the master list configuration with the specified version
     */
    Optional<TenantMasterListConfig> findByVersion(Integer version);

    /**
     * Finds the latest version of the master list configuration.
     * This can be used to get the most recent configuration regardless of active status.
     *
     * @return the latest version of the configuration
     */
    Optional<TenantMasterListConfig> findTopByOrderByVersionDesc();

    /**
     * Checks if any master list configuration exists for the current tenant.
     *
     * @return true if any configuration exists, false otherwise
     */
    boolean existsByIsActive(Boolean isActive);
}