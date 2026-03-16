package com.jio.consent.repository.signature;

import com.jio.consent.entity.JwkKey;

import java.util.List;

/**
 * Repository interface for JWK key operations.
 */
public interface JwkKeyRepository {

    /**
     * Finds the first JWK key by tenant ID, key type, and use.
     *
     * @param tenantId the tenant ID
     * @param kty the key type (e.g., "RSA")
     * @param use the key use (e.g., "sig")
     * @return the first matching JWK key, or null if not found
     */
    JwkKey findFirstByTenantIdAndKtyAndUse(String tenantId, String kty, String use);

    /**
     * Finds the first JWK key by tenant ID and use.
     *
     * @param tenantId the tenant ID
     * @param use the key use (e.g., "sig")
     * @return the first matching JWK key, or null if not found
     */
    JwkKey findFirstByTenantIdAndUse(String tenantId, String use);

    /**
     * Finds the first JWK key by tenant ID, ordered by kid ascending.
     *
     * @param tenantId the tenant ID
     * @return the first matching JWK key, or null if not found
     */
    JwkKey findFirstByTenantIdOrderByKidAsc(String tenantId);

    /**
     * Finds all JWK keys by tenant ID.
     *
     * @param tenantId the tenant ID
     * @return list of matching JWK keys
     */
    List<JwkKey> findByTenantId(String tenantId);
}

