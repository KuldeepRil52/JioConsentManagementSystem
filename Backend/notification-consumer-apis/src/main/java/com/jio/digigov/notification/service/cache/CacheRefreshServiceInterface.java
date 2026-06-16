package com.jio.digigov.notification.service.cache;

import com.jio.digigov.notification.enums.CacheType;

import java.util.Map;

/**
 * Interface for cache refresh service operations.
 *
 * This interface defines cache refresh capabilities for proactive cache warming
 * and refresh operations. Provides contract for managing cache refresh operations
 * to maintain optimal cache performance and prevent cache misses.
 *
 * @author Notification Service Team
 * @since 1.0.0
 */
public interface CacheRefreshServiceInterface {

    /**
     * Refresh cache entries that are about to expire.
     * This method is typically called by a scheduled job.
     */
    void refreshExpiringCacheEntries();

    /**
     * Get refresh operation statistics.
     *
     * @return map containing refresh statistics
     */
    Map<String, Object> getRefreshStatistics();

    /**
     * Manually trigger refresh for a specific cache type.
     *
     * @param cacheType the type of cache to refresh
     */
    void manualRefresh(CacheType cacheType);

    /**
     * Reset refresh statistics.
     */
    void resetStatistics();
}