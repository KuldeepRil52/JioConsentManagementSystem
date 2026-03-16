package com.jio.digigov.notification.service.cache;

import com.jio.digigov.notification.enums.CacheType;

import java.util.List;

/**
 * Interface for cache eviction service operations.
 *
 * This interface defines comprehensive cache eviction capabilities including
 * event-driven eviction, manual eviction, and batch eviction operations.
 * Provides contract for managing cache invalidation operations across
 * multi-tenant environments with proper isolation and error handling.
 *
 * @author Notification Service Team
 * @since 1.0.0
 */
public interface CacheEvictionServiceInterface {

    /**
     * Evict all cache entries of a specific type for a tenant/business.
     *
     * @param cacheType the type of cache to evict
     * @param tenantId tenant identifier
     * @param businessId business identifier
     * @param reason reason for eviction (for auditing)
     */
    void evictCacheType(CacheType cacheType, String tenantId, String businessId, String reason);

    /**
     * Evict a specific cache entry.
     *
     * @param cacheKey the cache key to evict
     * @param tenantId tenant identifier
     * @param businessId business identifier
     */
    void evictCacheEntry(String cacheKey, String tenantId, String businessId);

    /**
     * Evict multiple cache entries in a batch operation.
     *
     * @param requests list of eviction requests
     */
    void evictCacheEntries(List<CacheEvictionService.EvictionRequest> requests);

    /**
     * Evict all cache entries for a specific business.
     *
     * @param tenantId tenant identifier
     * @param businessId business identifier
     * @param reason reason for eviction (for auditing)
     */
    void evictAllForBusiness(String tenantId, String businessId, String reason);

    /**
     * Evict cache entries matching a pattern.
     *
     * @param keyPattern pattern to match cache keys
     * @param tenantId tenant identifier
     * @param businessId business identifier
     */
    void evictByPattern(String keyPattern, String tenantId, String businessId);

    /**
     * Clear all cache entries (emergency operation).
     *
     * @param reason reason for clearing all cache
     */
    void clearAllCache(String reason);
}