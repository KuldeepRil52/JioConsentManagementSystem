package com.jio.digigov.grievance.repository;

import com.jio.digigov.grievance.config.MultiTenantMongoConfig;
import com.jio.digigov.grievance.entity.JwkKey;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for managing JWK (JSON Web Key) keys in MongoDB.
 *
 * <p>This repository provides access to the 'auth_key' collection where
 * RSA private keys are stored in JWK format for signing API responses.</p>
 *
 * <p><b>Multi-Tenancy Support:</b></p>
 * <p>This repository uses MongoTemplateProvider to access tenant-specific
 * databases. Each tenant can have their own signing keys in their database.</p>
 *
 * <p><b>Usage Example:</b></p>
 * <pre>
 * JwkKey signingKey = jwkKeyRepository.findFirstByOrderByKidAsc(tenantId);
 * if (signingKey != null) {
 *     // Use the key for signing
 * }
 * </pre>
 *
 * @see com.jio.digigov.grievance.entity.JwkKey
 * @since 2.0.0
 */
@Slf4j
@Repository
public class JwkKeyRepository {

    private static final String COLLECTION_AUTH_KEY = "auth_key";
    private static final String FIELD_KID = "kid";
    private static final String FIELD_KTY = "kty";
    private static final String FIELD_USE = "use";
    private static final String FIELD_ALG = "alg";

    @Autowired
    private MultiTenantMongoConfig mongoTemplateProvider;

    /**
     * Finds the first JWK key ordered by kid (Key ID) in ascending order.
     *
     * <p>This method retrieves the primary signing key from the auth_key collection.
     * In production environments with key rotation, this will return the current
     * active signing key.</p>
     *
     * @param tenantId the tenant ID for multi-tenancy isolation
     * @return the first JWK key ordered by kid, or null if no keys exist
     */
    public JwkKey findFirstByOrderByKidAsc(String tenantId) {
        try {
            MongoTemplate template = mongoTemplateProvider.getMongoTemplateForTenant(tenantId);

            Query query = new Query();
            query.with(Sort.by(Sort.Direction.ASC, FIELD_KID));
            query.limit(1);

            JwkKey key = template.findOne(query, JwkKey.class, COLLECTION_AUTH_KEY);

            if (key == null) {
                log.warn("No JWK key found in auth_key collection for tenant={}", tenantId);
            } else {
                log.debug("Found JWK key with kid={} for tenant={}", key.getKid(), tenantId);
            }

            return key;

        } catch (Exception e) {
            log.error("Error retrieving JWK key for tenant={}: {}", tenantId, e.getMessage());
            return null;
        }
    }

    /**
     * Finds a JWK key by its Key ID.
     *
     * <p>This method is useful for key rotation scenarios where you need to
     * retrieve a specific key by its identifier.</p>
     *
     * @param tenantId the tenant ID for multi-tenancy isolation
     * @param kid the Key ID to search for
     * @return an Optional containing the JwkKey if found, or empty if not found
     */
    public Optional<JwkKey> findByKid(String tenantId, String kid) {
        try {
            MongoTemplate template = mongoTemplateProvider.getMongoTemplateForTenant(tenantId);

            Query query = new Query(Criteria.where(FIELD_KID).is(kid));
            JwkKey key = template.findOne(query, JwkKey.class, COLLECTION_AUTH_KEY);

            if (key != null) {
                log.debug("Found JWK key with kid={} for tenant={}", kid, tenantId);
            }

            return Optional.ofNullable(key);

        } catch (Exception e) {
            log.error("Error retrieving JWK key with kid={} for tenant={}: {}",
                    kid, tenantId, e.getMessage(), e);
            return Optional.empty();
        }
    }

    /**
     * Finds a JWK key by algorithm and use.
     *
     * <p>This method allows filtering keys by their intended algorithm and use case.
     * For example, find RSA256 keys intended for signature generation.</p>
     *
     * @param tenantId the tenant ID for multi-tenancy isolation
     * @param alg the algorithm (e.g., "RS256", "ES256")
     * @param use the intended use (e.g., "sig", "enc")
     * @return the first matching JwkKey, or null if not found
     */
    public JwkKey findFirstByAlgAndUse(String tenantId, String alg, String use) {
        try {
            MongoTemplate template = mongoTemplateProvider.getMongoTemplateForTenant(tenantId);

            Query query = new Query(Criteria.where(FIELD_ALG).is(alg).and(FIELD_USE).is(use));
            query.limit(1);

            JwkKey key = template.findOne(query, JwkKey.class, COLLECTION_AUTH_KEY);

            if (key != null) {
                log.debug("Found JWK key with alg={}, use={} for tenant={}", alg, use, tenantId);
            }

            return key;

        } catch (Exception e) {
            log.error("Error retrieving JWK key with alg={}, use={} for tenant={}: {}",
                    alg, use, tenantId, e.getMessage(), e);
            return null;
        }
    }

    /**
     * Finds a JWK key by key type and use.
     *
     * <p>This method allows filtering keys by their key type and use case.
     * For example, find RSA keys intended for signature generation.</p>
     *
     * @param tenantId the tenant ID for multi-tenancy isolation
     * @param kty the key type (e.g., "RSA", "EC", "oct")
     * @param use the intended use (e.g., "sig", "enc")
     * @return the first matching JwkKey, or null if not found
     */
    public JwkKey findFirstByKtyAndUse(String tenantId, String kty, String use) {
        try {
            MongoTemplate template = mongoTemplateProvider.getMongoTemplateForTenant(tenantId);

            Query query = new Query(Criteria.where(FIELD_KTY).is(kty).and(FIELD_USE).is(use));
            query.limit(1);

            JwkKey key = template.findOne(query, JwkKey.class, COLLECTION_AUTH_KEY);

            if (key != null) {
                log.debug("Found JWK key with kty={}, use={} for tenant={}", kty, use, tenantId);
            } else {
                log.warn("No JWK key found with kty={}, use={} for tenant={}", kty, use, tenantId);
            }

            return key;

        } catch (Exception e) {
            log.error("Error retrieving JWK key with kty={}, use={} for tenant={}: {}",
                    kty, use, tenantId, e.getMessage(), e);
            return null;
        }
    }

    /**
     * Finds the first JWK key by use.
     *
     * <p>This method retrieves the first key with the specified use case.
     * Useful when you have a single signing key in the auth_key collection.</p>
     *
     * @param tenantId the tenant ID for multi-tenancy isolation
     * @param use the intended use (e.g., "sig", "enc")
     * @return the first matching JwkKey, or null if not found
     */
    public JwkKey findFirstByUse(String tenantId, String use) {
        try {
            MongoTemplate template = mongoTemplateProvider.getMongoTemplateForTenant(tenantId);

            Query query = new Query(Criteria.where(FIELD_USE).is(use));
            query.limit(1);

            JwkKey key = template.findOne(query, JwkKey.class, COLLECTION_AUTH_KEY);

            if (key != null) {
                log.debug("Found JWK key with use={} for tenant={}", use, tenantId);
            }

            return key;

        } catch (Exception e) {
            log.error("Error retrieving JWK key with use={} for tenant={}: {}",
                    use, tenantId, e.getMessage(), e);
            return null;
        }
    }

    /**
     * Finds all JWK keys for a tenant.
     *
     * <p>This method retrieves all signing keys from the auth_key collection.
     * Useful for key rotation management.</p>
     *
     * @param tenantId the tenant ID for multi-tenancy isolation
     * @return list of all JWK keys, or empty list if none found
     */
    public List<JwkKey> findAll(String tenantId) {
        try {
            MongoTemplate template = mongoTemplateProvider.getMongoTemplateForTenant(tenantId);
            List<JwkKey> keys = template.findAll(JwkKey.class, COLLECTION_AUTH_KEY);

            log.debug("Found {} JWK keys for tenant={}", keys.size(), tenantId);
            return keys;

        } catch (Exception e) {
            log.error("Error retrieving all JWK keys for tenant={}: {}", tenantId, e.getMessage());
            return List.of();
        }
    }
}


