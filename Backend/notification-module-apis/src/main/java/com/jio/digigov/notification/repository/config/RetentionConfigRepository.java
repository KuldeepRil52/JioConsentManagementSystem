package com.jio.digigov.notification.repository.config;

import com.jio.digigov.notification.entity.config.RetentionConfig;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for RetentionConfig operations using multi-tenant approach.
 * Uses explicit MongoTemplate per tenant for database isolation.
 */
@Repository
public interface RetentionConfigRepository {

    /**
     * Find retention configuration by businessId.
     *
     * @param businessId    The business ID
     * @param mongoTemplate Tenant-specific MongoTemplate
     * @return Optional containing the retention config if found
     */
    Optional<RetentionConfig> findByBusinessId(String businessId, MongoTemplate mongoTemplate);
}
