package com.jio.digigov.notification.service.cache;

import com.jio.digigov.notification.dto.cache.CacheMetrics;
import com.jio.digigov.notification.enums.CacheType;

import java.time.Duration;
import java.util.Optional;

/**
 * Generic cache service interface providing unified caching operations
 * for both Caffeine (in-memory) and MongoDB (distributed) implementations.
 *
 * Supports multi-tenant caching with tenant and business ID isolation.
 * All cache operations are tenant-aware and provide proper isolation
 * between different tenant-business combinations.
 *
 * @param <T> the type of objects stored in the cache
 */
public interface CacheService<T> {

    /**
     * Retrieves a cached value by key, tenant, and business ID
     *
     * @param key the cache key
     * @param tenantId the tenant identifier
     * @param businessId the business identifier
     * @return Optional containing the cached value, or empty if not found or expired
     */
    Optional<T> get(String key, String tenantId, String businessId);

    /**
     * Stores a value in cache with specified TTL
     *
     * @param key the cache key
     * @param value the value to cache
     * @param tenantId the tenant identifier
     * @param businessId the business identifier
     * @param ttl time-to-live for the cache entry
     */
    void put(String key, T value, String tenantId, String businessId, Duration ttl);

    /**
     * Stores a value in cache with default TTL for the cache type
     *
     * @param key the cache key
     * @param value the value to cache
     * @param tenantId the tenant identifier
     * @param businessId the business identifier
     * @param cacheType the type of cache entry for TTL lookup
     */
    void put(String key, T value, String tenantId, String businessId, CacheType cacheType);

    /**
     * Removes a specific cache entry
     *
     * @param key the cache key to remove
     * @param tenantId the tenant identifier
     * @param businessId the business identifier
     */
    void evict(String key, String tenantId, String businessId);

    /**
     * Removes all cache entries for a tenant-business combination
     *
     * @param tenantId the tenant identifier
     * @param businessId the business identifier
     */
    void evictAll(String tenantId, String businessId);

    /**
     * Removes all cache entries of a specific type for a tenant-business combination
     *
     * @param type the cache type to evict
     * @param tenantId the tenant identifier
     * @param businessId the business identifier
     */
    void evictByType(CacheType type, String tenantId, String businessId);

    /**
     * Checks if a cache entry exists
     *
     * @param key the cache key
     * @param tenantId the tenant identifier
     * @param businessId the business identifier
     * @return true if the entry exists and is not expired
     */
    boolean exists(String key, String tenantId, String businessId);

    /**
     * Gets cache statistics for monitoring and performance analysis
     *
     * @return cache statistics
     */
    CacheMetrics getStats();

    /**
     * Gets cache statistics for a specific cache type
     *
     * @param cacheType the cache type to get statistics for
     * @return cache statistics for the specified type
     */
    CacheMetrics getStats(CacheType cacheType);

    /**
     * Clears all cache entries (use with caution)
     */
    void clearAll();

    /**
     * Gets the cache implementation type (e.g., "caffeine", "mongodb")
     *
     * @return the cache implementation type
     */
    String getCacheType();

    /**
     * Checks if the cache service is healthy and operational
     *
     * @return true if the cache is healthy
     */
    boolean isHealthy();

    /**
     * Refreshes a cache entry by reloading it from the original source
     * This method should be implemented by services that use the cache
     *
     * @param key the cache key to refresh
     * @param tenantId the tenant identifier
     * @param businessId the business identifier
     * @return true if the refresh was successful
     */
    default boolean refresh(String key, String tenantId, String businessId) {
        // Default implementation evicts the entry, forcing reload on next access
        evict(key, tenantId, businessId);
        return true;
    }
}