package com.jio.digigov.notification.repository.consent.impl;

import com.jio.digigov.notification.entity.consent.Consent;
import com.jio.digigov.notification.repository.consent.ConsentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Implementation of ConsentRepository using multi-tenant MongoTemplate approach.
 *
 * This repository manages Consent entities in a multi-tenant environment where each tenant
 * maintains their own MongoDB database.
 */
@Repository
@RequiredArgsConstructor
@Slf4j
public class ConsentRepositoryImpl implements ConsentRepository {

    @Override
    public Optional<Consent> findByConsentIdAndBusinessId(String consentId, String businessId, MongoTemplate mongoTemplate) {
        log.debug("Finding consent by consentId: {} and businessId: {}", consentId, businessId);

        try {
            Query query = Query.query(
                    Criteria.where("consentId").is(consentId)
                            .and("businessId").is(businessId)
            );

            Consent result = mongoTemplate.findOne(query, Consent.class);
            return Optional.ofNullable(result);
        } catch (Exception e) {
            log.error("Error finding consent by consentId: {} and businessId: {}: {}",
                    consentId, businessId, e.getMessage(), e);
            return Optional.empty();
        }
    }
}
