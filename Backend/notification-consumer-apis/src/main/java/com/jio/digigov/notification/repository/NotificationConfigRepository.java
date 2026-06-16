package com.jio.digigov.notification.repository;

import com.jio.digigov.notification.entity.NotificationConfig;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository interface for NotificationConfig entity.
 * Provides basic CRUD operations and custom query methods.
 *
 * @author Notification Service Team
 * @since 2025-01-09
 */
@Repository
public interface NotificationConfigRepository extends MongoRepository<NotificationConfig, String>, NotificationConfigRepositoryCustom {

    /**
     * Find configuration by business ID.
     *
     * @param businessId Business identifier
     * @return Optional containing the configuration if found
     */
    Optional<NotificationConfig> findByBusinessId(String businessId);

    /**
     * Delete configuration by business ID.
     *
     * @param businessId Business identifier
     */
    void deleteByBusinessId(String businessId);

    /**
     * Check if configuration exists for business ID.
     *
     * @param businessId Business identifier
     * @return true if configuration exists
     */
    boolean existsByBusinessId(String businessId);
}
