package com.jio.digigov.notification.repository.config.impl;

import com.jio.digigov.notification.entity.config.RetentionConfig;
import com.jio.digigov.notification.repository.config.RetentionConfigRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Implementation of RetentionConfigRepository using multi-tenant MongoTemplate approach.
 *
 * This repository manages RetentionConfig entities in a multi-tenant environment where each tenant
 * maintains their own MongoDB database.
 */
@Repository
@RequiredArgsConstructor
@Slf4j
public class RetentionConfigRepositoryImpl implements RetentionConfigRepository {

    @Override
    public Optional<RetentionConfig> findByBusinessId(String businessId, MongoTemplate mongoTemplate) {
        log.debug("Finding retention config by businessId: {}", businessId);

        try {
            Query query = Query.query(Criteria.where("businessId").is(businessId));

            RetentionConfig result = mongoTemplate.findOne(query, RetentionConfig.class);
            return Optional.ofNullable(result);
        } catch (Exception e) {
            log.error("Error finding retention config by businessId: {}: {}",
                    businessId, e.getMessage(), e);
            return Optional.empty();
        }
    }
}
