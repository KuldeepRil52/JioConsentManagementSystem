package com.jio.digigov.notification.repository;

import com.jio.digigov.notification.entity.DpoConfiguration;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for DPO (Data Protection Officer) Configuration operations.
 *
 * <p>This repository manages the tenant-scoped DPO configuration entity. Unlike other
 * entities that are business-scoped, DPO configuration is stored once per tenant and
 * shared across all businesses within that tenant.</p>
 *
 * <p><b>Key Characteristics:</b></p>
 * <ul>
 *   <li>ONE configuration per tenant (not per business)</li>
 *   <li>NO businessId in queries - tenant isolation via database name</li>
 *   <li>Simple CRUD operations for single-document collection</li>
 *   <li>Uses tenant-specific MongoTemplate for data isolation</li>
 * </ul>
 *
 * <p><b>Usage Pattern:</b></p>
 * <pre>
 * // Get tenant-specific MongoTemplate
 * MongoTemplate mongoTemplate = mongoTemplateProvider.getTemplate(tenantId);
 *
 * // Fetch DPO configuration
 * Optional&lt;DpoConfiguration&gt; dpoConfig = repository.findDpoConfiguration(mongoTemplate);
 *
 * // Save DPO configuration
 * DpoConfiguration config = DpoConfiguration.builder()
 *     .configurationJson(json)
 *     .build();
 * repository.save(config, mongoTemplate);
 * </pre>
 *
 * @since 2.0.0
 * @author DPDP Notification Team
 */
@Repository
public interface DpoConfigurationRepository {

    /**
     * Finds the DPO configuration for the current tenant.
     *
     * <p>Since there is only one DPO configuration per tenant, this method returns
     * the single configuration document from the dpo_configurations collection.</p>
     *
     * @param mongoTemplate Tenant-specific MongoTemplate (determines which tenant DB to query)
     * @return Optional containing the DPO configuration if it exists, empty otherwise
     */
    Optional<DpoConfiguration> findDpoConfiguration(MongoTemplate mongoTemplate);

    /**
     * Finds the DPO configuration with hierarchical scope lookup.
     *
     * <p><b>Lookup Strategy:</b></p>
     * <ol>
     *   <li>First, search for business-scoped DPO (businessId matches AND scopeLevel=BUSINESS)</li>
     *   <li>If not found, fallback to tenant-scoped DPO (businessId is null AND scopeLevel=TENANT)</li>
     * </ol>
     *
     * @param businessId Business identifier to search for
     * @param mongoTemplate Tenant-specific MongoTemplate
     * @return Optional containing the DPO configuration (business or tenant level)
     */
    Optional<DpoConfiguration> findDpoConfigurationHierarchical(String businessId, MongoTemplate mongoTemplate);

    /**
     * Saves or updates the DPO configuration.
     *
     * <p>If a configuration already exists, it will be updated. Otherwise, a new
     * configuration document will be created. Since there should be only one DPO
     * configuration per tenant, this method ensures singleton pattern.</p>
     *
     * @param dpoConfiguration DPO configuration entity to save
     * @param mongoTemplate Tenant-specific MongoTemplate
     * @return Saved DpoConfiguration with updated timestamps and ID
     */
    DpoConfiguration save(DpoConfiguration dpoConfiguration, MongoTemplate mongoTemplate);

    /**
     * Deletes the DPO configuration for the current tenant.
     *
     * <p>Performs delete by removing the configuration document entirely.</p>
     *
     * @param mongoTemplate Tenant-specific MongoTemplate
     * @return Number of deleted documents (0 if not found, 1 if deleted)
     */
    long delete(MongoTemplate mongoTemplate);

    /**
     * Checks if a DPO configuration exists for the current tenant.
     *
     * @param mongoTemplate Tenant-specific MongoTemplate
     * @return true if DPO configuration exists, false otherwise
     */
    boolean exists(MongoTemplate mongoTemplate);
}
