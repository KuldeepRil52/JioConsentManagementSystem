package com.jio.digigov.notification.repository.impl;

import com.jio.digigov.notification.entity.DpoConfiguration;
import com.jio.digigov.notification.repository.DpoConfigurationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Implementation of DpoConfigurationRepository using MongoDB operations.
 *
 * <p>This repository handles CRUD operations for the Data Protection Officer configuration
 * in a multi-tenant environment. Each tenant has exactly one DPO configuration document
 * stored in their tenant-specific database.</p>
 *
 * <p><b>Design Pattern:</b></p>
 * <ul>
 *   <li>Singleton per tenant - enforced by business logic, not database constraints</li>
 *   <li>No businessId field - configuration is tenant-scoped</li>
 *   <li>Hard delete available for complete removal</li>
 * </ul>
 *
 * <p><b>Thread Safety:</b> This repository is thread-safe as it's stateless and relies
 * on thread-safe MongoTemplate operations.</p>
 *
 * @since 2.0.0
 * @author DPDP Notification Team
 */
@Repository
@RequiredArgsConstructor
@Slf4j
public class DpoConfigurationRepositoryImpl implements DpoConfigurationRepository {

    @Override
    public Optional<DpoConfiguration> findDpoConfiguration(MongoTemplate mongoTemplate) {
        log.debug("Finding DPO configuration for tenant");

        Query query = new Query();
        query.limit(1); // Optimization: only fetch one document

        DpoConfiguration result = mongoTemplate.findOne(query, DpoConfiguration.class);
        return Optional.ofNullable(result);
    }

    @Override
    public Optional<DpoConfiguration> findDpoConfigurationHierarchical(String businessId, MongoTemplate mongoTemplate) {
        log.debug("Finding DPO configuration hierarchically for businessId: {}", businessId);

        // Step 1: Try to find business-scoped DPO
        Query businessQuery = Query.query(
            Criteria.where("businessId").is(businessId)
                .and("scopeLevel").is("BUSINESS")
        );
        businessQuery.limit(1);

        DpoConfiguration businessDpo = mongoTemplate.findOne(businessQuery, DpoConfiguration.class);
        if (businessDpo != null) {
            log.debug("Found business-scoped DPO for businessId: {}", businessId);
            return Optional.of(businessDpo);
        }

        // Step 2: Fallback to tenant-scoped DPO
        log.debug("Business-scoped DPO not found, falling back to tenant-scoped DPO");
        Query tenantQuery = Query.query(
            Criteria.where("scopeLevel").is("TENANT")
        );
        tenantQuery.limit(1);

        DpoConfiguration tenantDpo = mongoTemplate.findOne(tenantQuery, DpoConfiguration.class);
        if (tenantDpo != null) {
            log.debug("Found tenant-scoped DPO");
        } else {
            log.debug("No DPO configuration found at any scope level");
        }

        return Optional.ofNullable(tenantDpo);
    }

    @Override
    public DpoConfiguration save(DpoConfiguration dpoConfiguration, MongoTemplate mongoTemplate) {
        log.debug("Saving DPO configuration for tenant");

        // Set timestamps
        if (dpoConfiguration.getCreatedAt() == null) {
            dpoConfiguration.setCreatedAt(LocalDateTime.now());
        }
        dpoConfiguration.setUpdatedAt(LocalDateTime.now());

        // Save and return
        return mongoTemplate.save(dpoConfiguration);
    }

    @Override
    public long delete(MongoTemplate mongoTemplate) {
        log.debug("Deleting DPO configuration for tenant");

        Query query = new Query();

        long deletedCount = mongoTemplate.remove(query, DpoConfiguration.class).getDeletedCount();
        log.info("Deleted {} DPO configuration document(s)", deletedCount);

        return deletedCount;
    }

    @Override
    public boolean exists(MongoTemplate mongoTemplate) {
        log.debug("Checking if DPO configuration exists for tenant");

        Query query = new Query();
        query.limit(1);

        return mongoTemplate.exists(query, DpoConfiguration.class);
    }
}
