package com.jio.digigov.notification.dto.cache;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Data transfer object for cache statistics and metrics.
 * Provides insights into cache performance and usage patterns.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CacheMetrics {

    /**
     * Total number of cache requests (hits + misses)
     */
    private long requestCount;

    /**
     * Total number of successful cache hits
     */
    private long hitCount;

    /**
     * Total number of cache misses
     */
    private long missCount;

    /**
     * Cache hit rate as a percentage (0.0 to 1.0)
     */
    private double hitRate;

    /**
     * Cache miss rate as a percentage (0.0 to 1.0)
     */
    private double missRate;

    /**
     * Total number of cache evictions
     */
    private long evictionCount;

    /**
     * Current number of entries in cache
     */
    private long size;

    /**
     * Maximum allowed cache size
     */
    private long maxSize;

    /**
     * Cache type being measured
     */
    private String cacheType;

    /**
     * Timestamp when statistics were collected
     */
    private LocalDateTime collectedAt;

    /**
     * Additional metadata about cache performance
     */
    private Map<String, Object> metadata;

    /**
     * Creates cache statistics with basic metrics
     */
    public static CacheMetrics basic(long hits, long misses, long size, String cacheType) {
        CacheMetrics stats = new CacheMetrics();
        stats.hitCount = hits;
        stats.missCount = misses;
        stats.requestCount = hits + misses;
        stats.size = size;
        stats.cacheType = cacheType;
        stats.collectedAt = LocalDateTime.now();

        if (stats.requestCount > 0) {
            stats.hitRate = (double) hits / stats.requestCount;
            stats.missRate = (double) misses / stats.requestCount;
        }

        return stats;
    }

    /**
     * Creates empty cache statistics
     */
    public static CacheMetrics empty(String cacheType) {
        return basic(0, 0, 0, cacheType);
    }
}