package com.jio.digigov.notification.service.cache;

import com.jio.digigov.notification.config.properties.CacheProperties;
import com.jio.digigov.notification.entity.cache.CacheEntry;
import com.jio.digigov.notification.enums.CacheType;
import com.jio.digigov.notification.service.cache.impl.MongoDBCacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Cache refresh service for proactive cache entry refresh before expiry.
 *
 * This service implements a background refresh strategy to ensure cache entries
 * are refreshed before they expire, maintaining optimal cache hit rates and
 * system performance. It supports different refresh schedules for different
 * types of cache entries based on their importance and usage patterns.
 *
 * Key Features:
 * - Scheduled background refresh for critical cache entries
 * - Priority-based refresh with configurable time windows
 * - Async refresh operations to prevent blocking
 * - Comprehensive monitoring and metrics
 * - Tenant-aware refresh operations
 * - Configurable refresh schedules per cache type
 *
 * Refresh Strategy:
 * - Tokens: 5 minutes before expiry (high priority)
 * - Configurations: 10 minutes before expiry (medium priority)
 * - Templates: 15 minutes before expiry (low priority)
 * - Database queries: No proactive refresh (TTL-based only)
 *
 * Performance Considerations:
 * - Uses async operations to prevent scheduler blocking
 * - Batches refresh operations for efficiency
 * - Monitors refresh success rates for tuning
 * - Limits concurrent refresh operations
 *
 * @author Notification Service Team
 * @version 1.0
 * @since 2025-01-20
 */
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "cache.type", havingValue = "mongodb")
@Slf4j
public class CacheRefreshService implements CacheRefreshServiceInterface {

    private final CacheProperties cacheProperties;
    private final CacheService<Object> cacheService;
    private final MongoDBCacheService<Object> mongoDBCacheService; // For MongoDB-specific operations

    /**
     * Statistics tracking for refresh operations
     */
    private final Map<String, AtomicInteger> refreshStats = new ConcurrentHashMap<>();
    private final Map<String, AtomicInteger> refreshErrors = new ConcurrentHashMap<>();

    /**
     * Maximum concurrent refresh operations to prevent resource exhaustion
     */
    private static final int MAX_CONCURRENT_REFRESHES = 10;
    private final AtomicInteger activeRefreshCount = new AtomicInteger(0);

    /**
     * Scheduled refresh job that runs based on configured cron expression.
     * Default: Every 5 minutes
     */
    @Scheduled(cron = "${cache.refresh.schedule.cron:0 */5 * * * *}")
    public void refreshExpiringCacheEntries() {
        if (!cacheProperties.isEnabled() || !cacheProperties.getRefresh().isEnabled()) {
            log.debug("Cache refresh is disabled, skipping refresh cycle");
            return;
        }

        LocalDateTime startTime = LocalDateTime.now();
        log.debug("Starting cache refresh cycle at {}", startTime);

        try {
            // Only perform refresh for MongoDB cache (Caffeine handles TTL automatically)
            if (cacheProperties.isMongoDBEnabled()) {
                performMongoDBCacheRefresh();
            } else {
                log.debug("Caffeine cache detected - skipping proactive refresh (TTL-based expiry)");
            }

        } catch (Exception e) {
            log.error("Error during cache refresh cycle: {}", e.getMessage());
        }

        LocalDateTime endTime = LocalDateTime.now();
        Duration duration = Duration.between(startTime, endTime);
        log.info("Cache refresh cycle completed in {} ms", duration.toMillis());
    }

    /**
     * Performs cache refresh for MongoDB cache entries approaching expiry
     */
    private void performMongoDBCacheRefresh() {
        log.debug("Performing MongoDB cache refresh");

        // Get refresh schedules for different cache types
        Map<CacheType, Duration> refreshSchedule = getRefreshSchedule();

        for (Map.Entry<CacheType, Duration> entry : refreshSchedule.entrySet()) {
            CacheType cacheType = entry.getKey();
            Duration beforeExpiry = entry.getValue();

            CompletableFuture.runAsync(() -> refreshCacheType(cacheType, beforeExpiry))
                .exceptionally(throwable -> {
                    log.error("Error refreshing cache type {}: {}", cacheType, throwable.getMessage());
                    incrementErrorCount(cacheType.getKey());
                    return null;
                });
        }
    }

    /**
     * Refresh cache entries of a specific type that are approaching expiry
     */
    private void refreshCacheType(CacheType cacheType, Duration beforeExpiry) {
        if (activeRefreshCount.get() >= MAX_CONCURRENT_REFRESHES) {
            log.warn("Maximum concurrent refresh operations reached, skipping refresh for type: {}",
                    cacheType.getKey());
            return;
        }

        try {
            activeRefreshCount.incrementAndGet();
            log.debug("Refreshing cache type: {} (before expiry: {})", cacheType.getKey(), beforeExpiry);

            // For MongoDB cache, find entries approaching expiry
            // Note: This requires tenant information, which we'll handle in a simplified way
            // In a production system, you might want to iterate through known tenants
            refreshEntriesForTenant("default", cacheType, beforeExpiry);

        } finally {
            activeRefreshCount.decrementAndGet();
        }
    }

    /**
     * Refresh entries for a specific tenant and cache type
     */
    private void refreshEntriesForTenant(String tenantId, CacheType cacheType, Duration beforeExpiry) {
        try {
            if (mongoDBCacheService == null) {
                log.debug("MongoDB cache service not available for refresh");
                return;
            }

            List<CacheEntry> expiringEntries = mongoDBCacheService.findEntriesApproachingExpiry(tenantId, beforeExpiry);

            if (expiringEntries.isEmpty()) {
                log.debug("No entries approaching expiry for tenant: {}, type: {}", tenantId, cacheType.getKey());
                return;
            }

            log.info("Found {} entries approaching expiry for tenant: {}, type: {}",
                    expiringEntries.size(), tenantId, cacheType.getKey());

            for (CacheEntry entry : expiringEntries) {
                refreshCacheEntry(entry, cacheType);
            }

            incrementRefreshCount(cacheType.getKey(), expiringEntries.size());

        } catch (Exception e) {
            log.error("Error refreshing entries for tenant: {}, type: {}: {}",
                     tenantId, cacheType.getKey(), e.getMessage(), e);
            incrementErrorCount(cacheType.getKey());
        }
    }

    /**
     * Refresh a specific cache entry
     */
    private void refreshCacheEntry(CacheEntry entry, CacheType cacheType) {
        try {
            log.debug("Refreshing cache entry: {} for business: {}", entry.getCacheKey(), entry.getBusinessId());

            // The actual refresh logic would depend on the cache type
            // For now, we'll implement a basic refresh that evicts the entry
            // In a real implementation, you'd call the appropriate service to reload the data

            switch (cacheType) {
                case DIGIGOV_TOKEN:
                case DIGIGOV_ADMIN_TOKEN:
                    refreshTokenEntry(entry);
                    break;
                case NG_CONFIGURATION:
                    refreshConfigurationEntry(entry);
                    break;
                case MASTER_LIST_CONFIG:
                case MASTER_LIST_DB_VALUE:
                    refreshMasterListEntry(entry);
                    break;
                case EVENT_CONFIG:
                    refreshEventConfigEntry(entry);
                    break;
                case TEMPLATE:
                    refreshTemplateEntry(entry);
                    break;
                default:
                    log.debug("No specific refresh logic for cache type: {}", cacheType.getKey());
                    // Generic refresh: evict the entry to force reload on next access
                    evictEntry(entry);
            }

        } catch (Exception e) {
            log.error("Error refreshing cache entry: {} - {}", entry.getCacheKey(), e.getMessage());
        }
    }

    /**
     * Refresh token cache entries
     */
    private void refreshTokenEntry(CacheEntry entry) {
        // TODO: Implement token refresh logic
        // This would typically involve calling the token service to generate a new token
        log.debug("Refreshing token entry: {}", entry.getCacheKey());
        evictEntry(entry); // For now, just evict to force regeneration
    }

    /**
     * Refresh configuration cache entries
     */
    private void refreshConfigurationEntry(CacheEntry entry) {
        // TODO: Implement configuration refresh logic
        // This would typically involve calling the configuration service to reload
        log.debug("Refreshing configuration entry: {}", entry.getCacheKey());
        evictEntry(entry); // For now, just evict to force reload
    }

    /**
     * Refresh master list cache entries
     */
    private void refreshMasterListEntry(CacheEntry entry) {
        // TODO: Implement master list refresh logic
        // This would typically involve calling the master list service to reload
        log.debug("Refreshing master list entry: {}", entry.getCacheKey());
        evictEntry(entry); // For now, just evict to force reload
    }

    /**
     * Refresh event configuration cache entries
     */
    private void refreshEventConfigEntry(CacheEntry entry) {
        // TODO: Implement event config refresh logic
        log.debug("Refreshing event config entry: {}", entry.getCacheKey());
        evictEntry(entry); // For now, just evict to force reload
    }

    /**
     * Refresh template cache entries
     */
    private void refreshTemplateEntry(CacheEntry entry) {
        // TODO: Implement template refresh logic
        log.debug("Refreshing template entry: {}", entry.getCacheKey());
        evictEntry(entry); // For now, just evict to force reload
    }

    /**
     * Generic method to evict a cache entry
     */
    private void evictEntry(CacheEntry entry) {
        try {
            // Extract tenant ID from cache key (simplified approach)
            String[] keyParts = entry.getCacheKey().split(":");
            if (keyParts.length >= 3) {
                String cacheType = keyParts[0];
                String tenantId = keyParts[1];
                String businessId = entry.getBusinessId();

                cacheService.evict(cacheType, tenantId, businessId);
                log.debug("Evicted cache entry: {} for refresh", entry.getCacheKey());
            }
        } catch (Exception e) {
            log.error("Error evicting cache entry: {} - {}", entry.getCacheKey(), e.getMessage());
        }
    }

    /**
     * Get refresh schedule for different cache types
     */
    private Map<CacheType, Duration> getRefreshSchedule() {
        Map<CacheType, Duration> schedule = new ConcurrentHashMap<>();

        // High priority: Tokens (refresh 5 minutes before expiry)
        Duration tokenRefresh = cacheProperties.getRefreshBeforeExpiryForType(CacheType.DIGIGOV_TOKEN);
        schedule.put(CacheType.DIGIGOV_TOKEN, tokenRefresh);
        schedule.put(CacheType.DIGIGOV_ADMIN_TOKEN, tokenRefresh);

        // Medium priority: Configurations (refresh 10 minutes before expiry)
        Duration configRefresh = cacheProperties.getRefreshBeforeExpiryForType(CacheType.NG_CONFIGURATION);
        schedule.put(CacheType.NG_CONFIGURATION, configRefresh);
        schedule.put(CacheType.MASTER_LIST_CONFIG, configRefresh);
        schedule.put(CacheType.EVENT_CONFIG, configRefresh);

        // Lower priority: Templates (refresh 15 minutes before expiry)
        Duration templateRefresh = cacheProperties.getRefreshBeforeExpiryForType(CacheType.TEMPLATE);
        schedule.put(CacheType.TEMPLATE, templateRefresh);

        // Database queries: No proactive refresh (TTL-based only)
        // schedule.put(CacheType.MASTER_LIST_DB_VALUE, ...);
        // schedule.put(CacheType.DB_QUERY_RESULT, ...);

        return schedule;
    }

    /**
     * Increment refresh count for statistics
     */
    private void incrementRefreshCount(String cacheType, int count) {
        refreshStats.computeIfAbsent(cacheType, k -> new AtomicInteger(0)).addAndGet(count);
    }

    /**
     * Increment error count for statistics
     */
    private void incrementErrorCount(String cacheType) {
        refreshErrors.computeIfAbsent(cacheType, k -> new AtomicInteger(0)).incrementAndGet();
    }

    /**
     * Get refresh statistics for monitoring
     */
    public Map<String, Object> getRefreshStatistics() {
        return Map.of(
            "refreshCounts", refreshStats,
            "refreshErrors", refreshErrors,
            "activeRefreshCount", activeRefreshCount.get(),
            "maxConcurrentRefreshes", MAX_CONCURRENT_REFRESHES,
            "refreshEnabled", cacheProperties.getRefresh().isEnabled(),
            "refreshSchedule", cacheProperties.getRefresh().getSchedule().getCron()
        );
    }

    /**
     * Manually trigger refresh for a specific cache type
     */
    @Async("cacheExecutor")
    public void manualRefresh(CacheType cacheType) {
        log.info("Manual refresh triggered for cache type: {}", cacheType.getKey());

        Duration beforeExpiry = cacheProperties.getRefreshBeforeExpiryForType(cacheType);
        refreshCacheType(cacheType, beforeExpiry);
    }

    /**
     * Reset refresh statistics
     */
    public void resetStatistics() {
        refreshStats.clear();
        refreshErrors.clear();
        log.info("Cache refresh statistics reset");
    }
}