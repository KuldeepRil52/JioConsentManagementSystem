package com.jio.digigov.notification.repository;

import com.jio.digigov.notification.entity.NotificationConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Implementation of NotificationConfigRepositoryCustom.
 * Provides custom query implementations with tenant-specific MongoTemplate support.
 *
 * @author Notification Service Team
 * @since 2025-01-09
 */
@Repository
@Slf4j
public class NotificationConfigRepositoryImpl implements NotificationConfigRepositoryCustom {

    private final MongoTemplate defaultMongoTemplate;

    @Autowired
    public NotificationConfigRepositoryImpl(MongoTemplate mongoTemplate) {
        this.defaultMongoTemplate = mongoTemplate;
    }

    @Override
    public Optional<NotificationConfig> findByBusinessIdCustom(String businessId) {
        return findByBusinessIdCustom(businessId, defaultMongoTemplate);
    }

    @Override
    public Optional<NotificationConfig> findByBusinessIdCustom(String businessId, MongoTemplate mongoTemplate) {
        log.debug("Finding NotificationConfig by businessId: {} in collection: notification_configurations",
                businessId);

        Query query = new Query(Criteria.where("businessId").is(businessId));
        NotificationConfig config = mongoTemplate.findOne(query, NotificationConfig.class);

        if (config != null) {
            log.debug("Found NotificationConfig: configId={}, businessId={}",
                    config.getConfigId(), config.getBusinessId());
        } else {
            log.debug("No NotificationConfig found for businessId: {}", businessId);
        }

        return Optional.ofNullable(config);
    }

    @Override
    public NotificationConfig saveCustom(NotificationConfig notificationConfig) {
        return saveCustom(notificationConfig, defaultMongoTemplate);
    }

    @Override
    public NotificationConfig saveCustom(NotificationConfig notificationConfig, MongoTemplate mongoTemplate) {
        log.debug("Saving NotificationConfig: configId={}, businessId={} to collection: notification_configurations",
                notificationConfig.getConfigId(), notificationConfig.getBusinessId());

        NotificationConfig saved = mongoTemplate.save(notificationConfig);

        log.info("Saved NotificationConfig: configId={}, businessId={}, id={}",
                saved.getConfigId(), saved.getBusinessId(), saved.getId());

        return saved;
    }

    @Override
    public void deleteByBusinessIdCustom(String businessId) {
        deleteByBusinessIdCustom(businessId, defaultMongoTemplate);
    }

    @Override
    public void deleteByBusinessIdCustom(String businessId, MongoTemplate mongoTemplate) {
        log.debug("Deleting NotificationConfig by businessId: {} from collection: notification_configurations",
                businessId);

        Query query = new Query(Criteria.where("businessId").is(businessId));
        mongoTemplate.remove(query, NotificationConfig.class);

        log.info("Deleted NotificationConfig for businessId: {}", businessId);
    }

    @Override
    public boolean existsByBusinessIdCustom(String businessId) {
        return existsByBusinessIdCustom(businessId, defaultMongoTemplate);
    }

    @Override
    public boolean existsByBusinessIdCustom(String businessId, MongoTemplate mongoTemplate) {
        log.debug("Checking existence of NotificationConfig by businessId: {}", businessId);

        Query query = new Query(Criteria.where("businessId").is(businessId));
        boolean exists = mongoTemplate.exists(query, NotificationConfig.class);

        log.debug("NotificationConfig exists for businessId {}: {}", businessId, exists);

        return exists;
    }
}
