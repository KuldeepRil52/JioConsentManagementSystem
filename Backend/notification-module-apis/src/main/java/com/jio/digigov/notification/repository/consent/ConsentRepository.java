package com.jio.digigov.notification.repository.consent;

import com.jio.digigov.notification.entity.consent.Consent;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for Consent operations using multi-tenant approach.
 * Uses explicit MongoTemplate per tenant for database isolation.
 */
@Repository
public interface ConsentRepository {

    /**
     * Find consent by consentId and businessId.
     *
     * @param consentId     The consent ID
     * @param businessId    The business ID
     * @param mongoTemplate Tenant-specific MongoTemplate
     * @return Optional containing the consent if found
     */
    Optional<Consent> findByConsentIdAndBusinessId(String consentId, String businessId, MongoTemplate mongoTemplate);
}
