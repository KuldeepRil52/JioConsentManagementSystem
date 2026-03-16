package com.jio.digigov.notification.repository;

import com.jio.digigov.notification.entity.NotificationConfig;
import org.springframework.data.mongodb.core.MongoTemplate;

import java.util.Optional;

/**
 * Custom repository interface for NotificationConfig entity.
 * Provides custom query methods with tenant-specific MongoTemplate support.
 *
 * @author Notification Service Team
 * @since 2025-01-09
 */
public interface NotificationConfigRepositoryCustom {

    /**
     * Find configuration by business ID using default MongoTemplate.
     *
     * @param businessId Business identifier
     * @return Optional containing the configuration if found
     */
    Optional<NotificationConfig> findByBusinessIdCustom(String businessId);

    /**
     * Find configuration by business ID using provided MongoTemplate.
     *
     * @param businessId Business identifier
     * @param mongoTemplate Tenant-specific MongoTemplate
     * @return Optional containing the configuration if found
     */
    Optional<NotificationConfig> findByBusinessIdCustom(String businessId, MongoTemplate mongoTemplate);

    /**
     * Save configuration using default MongoTemplate.
     *
     * @param notificationConfig Configuration to save
     * @return Saved configuration
     */
    NotificationConfig saveCustom(NotificationConfig notificationConfig);

    /**
     * Save configuration using provided MongoTemplate.
     *
     * @param notificationConfig Configuration to save
     * @param mongoTemplate Tenant-specific MongoTemplate
     * @return Saved configuration
     */
    NotificationConfig saveCustom(NotificationConfig notificationConfig, MongoTemplate mongoTemplate);

    /**
     * Delete configuration by business ID using default MongoTemplate.
     *
     * @param businessId Business identifier
     */
    void deleteByBusinessIdCustom(String businessId);

    /**
     * Delete configuration by business ID using provided MongoTemplate.
     *
     * @param businessId Business identifier
     * @param mongoTemplate Tenant-specific MongoTemplate
     */
    void deleteByBusinessIdCustom(String businessId, MongoTemplate mongoTemplate);

    /**
     * Check if configuration exists by business ID using default MongoTemplate.
     *
     * @param businessId Business identifier
     * @return true if configuration exists
     */
    boolean existsByBusinessIdCustom(String businessId);

    /**
     * Check if configuration exists by business ID using provided MongoTemplate.
     *
     * @param businessId Business identifier
     * @param mongoTemplate Tenant-specific MongoTemplate
     * @return true if configuration exists
     */
    boolean existsByBusinessIdCustom(String businessId, MongoTemplate mongoTemplate);
}
