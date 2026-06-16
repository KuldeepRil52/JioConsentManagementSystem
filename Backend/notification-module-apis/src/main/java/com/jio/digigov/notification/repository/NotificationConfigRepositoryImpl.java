package com.jio.digigov.notification.repository;

import com.jio.digigov.notification.entity.NotificationConfig;
import com.jio.digigov.notification.enums.ScopeLevel;
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

    @Override
    public Optional<NotificationConfig> findWithFallback(String businessId, String tenantId) {
        return findWithFallback(businessId, tenantId, defaultMongoTemplate);
    }

    @Override
    public Optional<NotificationConfig> findWithFallback(String businessId, String tenantId, MongoTemplate mongoTemplate) {
        log.debug("Finding NotificationConfig with 3-level fallback: businessId={}, tenantId={}", businessId, tenantId);

        // Step 1: Try businessId
        Query step1Query = new Query(Criteria.where("businessId").is(businessId));
        NotificationConfig config = mongoTemplate.findOne(step1Query, NotificationConfig.class);

        if (config != null) {
            log.debug("Found config for businessId: {} with providerType: {}", businessId, config.getProviderType());
            return Optional.of(config);
        }

        log.debug("Config not found for businessId: {}, trying fallback step 2", businessId);

        // Step 2: Try tenantId as businessId
        if (tenantId != null && !tenantId.trim().isEmpty()) {
            Query step2Query = new Query(Criteria.where("businessId").is(tenantId));
            config = mongoTemplate.findOne(step2Query, NotificationConfig.class);

            if (config != null) {
                log.debug("Found config for tenantId: {} with providerType: {}", tenantId, config.getProviderType());
                return Optional.of(config);
            }

            log.debug("Config not found for tenantId: {}, trying fallback step 3", tenantId);
        }

        // Step 3: Try scopeLevel=TENANT
        Query step3Query = new Query(Criteria.where("scopeLevel").is(ScopeLevel.TENANT.name()));
        config = mongoTemplate.findOne(step3Query, NotificationConfig.class);

        if (config != null) {
            log.debug("Found TENANT-level config with providerType: {}", config.getProviderType());
            return Optional.of(config);
        }

        log.warn("No NotificationConfig found after 3-level fallback for businessId: {}, tenantId: {}", businessId, tenantId);
        return Optional.empty();
    }
}
