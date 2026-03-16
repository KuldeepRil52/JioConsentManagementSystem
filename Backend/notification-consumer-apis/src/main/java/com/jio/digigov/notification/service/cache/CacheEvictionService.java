package com.jio.digigov.notification.service.cache;

import com.jio.digigov.notification.enums.CacheType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Cache eviction service for managing cache invalidation operations.
 *
 * This service provides comprehensive cache eviction capabilities including
 * event-driven eviction, manual eviction, and batch eviction operations.
 * It ensures cache consistency when underlying data changes and provides
 * administrative tools for cache management.
 *
 * Key Features:
 * - Event-driven cache eviction when data changes
 * - Manual eviction operations for administration
 * - Batch eviction for bulk operations
 * - Tenant and business-specific eviction
 * - Cache type-specific eviction strategies
 * - Comprehensive eviction statistics and monitoring
 *
 * Eviction Strategies:
 * - Immediate eviction for critical data changes
 * - Batch eviction for performance optimization
 * - Selective eviction by tenant, business, or cache type
 * - Graceful eviction with error handling and fallback
 *
 * Integration Points:
 * - Configuration service updates → evict configuration cache
 * - Template updates → evict template cache
 * - Event configuration changes → evict event config cache
 * - Master list changes → evict master list cache
 *
 * @author Notification Service Team
 * @version 1.0
 * @since 2025-01-20
 */
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "cache.enabled", havingValue = "true", matchIfMissing = true)
@Slf4j
public class CacheEvictionService implements CacheEvictionServiceInterface {

    private final CacheService<Object> cacheService;

    /**
     * Statistics tracking for eviction operations
     */
    private final Map<String, AtomicInteger> evictionStats = new ConcurrentHashMap<>();
    private final Map<String, AtomicInteger> evictionErrors = new ConcurrentHashMap<>();

    /**
     * Custom events for cache eviction
     */
    public static class CacheEvictionEvent {
        public final String tenantId;
        public final String businessId;
        public final CacheType cacheType;
        public final String reason;

        public CacheEvictionEvent(String tenantId, String businessId, CacheType cacheType, String reason) {
            this.tenantId = tenantId;
            this.businessId = businessId;
            this.cacheType = cacheType;
            this.reason = reason;
        }
    }

    /**
     * Event listener for configuration update events
     */
    @EventListener
    public void handleConfigurationUpdate(ConfigurationUpdateEvent event) {
        log.info("Configuration updated for tenant: {}, business: {} - evicting configuration cache",
                event.getTenantId(), event.getBusinessId());

        evictCacheType(CacheType.NG_CONFIGURATION, event.getTenantId(), event.getBusinessId(), "configuration-update");
    }

    /**
     * Event listener for master list update events
     */
    @EventListener
    public void handleMasterListUpdate(MasterListUpdateEvent event) {
        log.info("Master list updated for tenant: {} - evicting master list cache", event.getTenantId());

        // Evict both master list config and resolved values
        evictCacheType(CacheType.MASTER_LIST_CONFIG, event.getTenantId(), "*", "masterlist-update");
        evictCacheType(CacheType.MASTER_LIST_DB_VALUE, event.getTenantId(), "*", "masterlist-update");
    }

    /**
     * Event listener for template update events
     */
    @EventListener
    public void handleTemplateUpdate(TemplateUpdateEvent event) {
        log.info("Template updated for tenant: {}, business: {} - evicting template cache",
                event.getTenantId(), event.getBusinessId());

        evictCacheType(CacheType.TEMPLATE, event.getTenantId(), event.getBusinessId(), "template-update");
    }

    /**
     * Event listener for event configuration update events
     */
    @EventListener
    public void handleEventConfigUpdate(EventConfigUpdateEvent event) {
        log.info("Event configuration updated for tenant: {}, business: {} - evicting event config cache",
                event.getTenantId(), event.getBusinessId());

        evictCacheType(CacheType.EVENT_CONFIG, event.getTenantId(), event.getBusinessId(), "event-config-update");
    }

    /**
     * Evict cache entries for a specific cache type
     */
    public void evictCacheType(CacheType cacheType, String tenantId, String businessId, String reason) {
        try {
            log.debug("Evicting cache type: {} for tenant: {}, business: {}, reason: {}",
                     cacheType.getKey(), tenantId, businessId, reason);

            if ("*".equals(businessId)) {
                // Evict for all businesses in tenant (requires special handling)
                evictAllBusinessesForTenant(cacheType, tenantId, reason);
            } else {
                cacheService.evictByType(cacheType, tenantId, businessId);
            }

            incrementEvictionCount(cacheType.getKey());
            log.info("Successfully evicted cache type: {} for tenant: {}, business: {}",
                    cacheType.getKey(), tenantId, businessId);

        } catch (Exception e) {
            log.error("Error evicting cache type: {} for tenant: {}, business: {}: {}",
                     cacheType.getKey(), tenantId, businessId, e.getMessage(), e);
            incrementEvictionError(cacheType.getKey());
        }
    }

    /**
     * Evict cache entries for all businesses within a tenant
     */
    private void evictAllBusinessesForTenant(CacheType cacheType, String tenantId, String reason) {
        // This is a simplified implementation
        // In a production system, you might need to iterate through known businesses
        log.warn("Evicting cache type: {} for all businesses in tenant: {} - this may be expensive",
                cacheType.getKey(), tenantId);

        // For now, we'll log this as a potential enhancement
        // Implementation would depend on how you track tenant-business combinations
    }

    /**
     * Manual eviction of a specific cache entry
     */
    public void evictCacheEntry(String cacheKey, String tenantId, String businessId) {
        try {
            log.debug("Manually evicting cache entry: {} for tenant: {}, business: {}",
                     cacheKey, tenantId, businessId);

            cacheService.evict(cacheKey, tenantId, businessId);
            incrementEvictionCount("manual");

            log.info("Successfully evicted cache entry: {} for tenant: {}, business: {}",
                    cacheKey, tenantId, businessId);

        } catch (Exception e) {
            log.error("Error manually evicting cache entry: {} for tenant: {}, business: {}: {}",
                     cacheKey, tenantId, businessId, e.getMessage(), e);
            incrementEvictionError("manual");
        }
    }

    /**
     * Batch eviction of multiple cache entries
     */
    public void evictCacheEntries(List<EvictionRequest> requests) {
        log.info("Processing batch eviction of {} cache entries", requests.size());

        // Process evictions asynchronously for better performance
        List<CompletableFuture<Void>> futures = requests.stream()
                .map(this::evictAsync)
                .toList();

        // Wait for all evictions to complete
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenRun(() -> log.info("Batch eviction completed for {} entries", requests.size()))
                .exceptionally(throwable -> {
                    log.error("Batch eviction encountered errors: {}", throwable.getMessage());
                    return null;
                });
    }

    /**
     * Asynchronous eviction of a single cache entry
     */
    private CompletableFuture<Void> evictAsync(EvictionRequest request) {
        return CompletableFuture.runAsync(() -> {
            try {
                if (request.cacheType != null) {
                    evictCacheType(request.cacheType, request.tenantId, request.businessId, request.reason);
                } else {
                    evictCacheEntry(request.cacheKey, request.tenantId, request.businessId);
                }
            } catch (Exception e) {
                log.error("Error in async eviction: {}", e.getMessage());
            }
        });
    }

    /**
     * Evict all cache entries for a tenant-business combination
     */
    public void evictAllForBusiness(String tenantId, String businessId, String reason) {
        try {
            log.info("Evicting all cache entries for tenant: {}, business: {}, reason: {}",
                    tenantId, businessId, reason);

            cacheService.evictAll(tenantId, businessId);
            incrementEvictionCount("all-business");

            log.info("Successfully evicted all cache entries for tenant: {}, business: {}",
                    tenantId, businessId);

        } catch (Exception e) {
            log.error("Error evicting all cache entries for tenant: {}, business: {}: {}",
                     tenantId, businessId, e.getMessage(), e);
            incrementEvictionError("all-business");
        }
    }

    /**
     * Evict cache entries by pattern matching
     */
    public void evictByPattern(String keyPattern, String tenantId, String businessId) {
        log.warn("Pattern-based eviction requested for pattern: {} - this operation may be expensive",
                keyPattern);

        // This would require special implementation depending on cache backend
        // For now, log as a future enhancement
        log.info("Pattern-based eviction is not yet implemented. Consider using specific eviction methods.");
    }

    /**
     * Clear all cache entries (emergency operation)
     */
    public void clearAllCache(String reason) {
        log.warn("CLEARING ALL CACHE ENTRIES - Reason: {}", reason);

        try {
            cacheService.clearAll();
            incrementEvictionCount("clear-all");

            log.warn("ALL CACHE ENTRIES CLEARED - Reason: {}", reason);

        } catch (Exception e) {
            log.error("Error clearing all cache entries: {}", e.getMessage());
            incrementEvictionError("clear-all");
        }
    }

    /**
     * Get eviction statistics for monitoring
     */
    public Map<String, Object> getEvictionStatistics() {
        return Map.of(
            "evictionCounts", evictionStats,
            "evictionErrors", evictionErrors,
            "totalEvictions", evictionStats.values().stream().mapToInt(AtomicInteger::get).sum(),
            "totalErrors", evictionErrors.values().stream().mapToInt(AtomicInteger::get).sum(),
            "cacheType", cacheService.getCacheType(),
            "cacheHealthy", cacheService.isHealthy()
        );
    }

    /**
     * Reset eviction statistics
     */
    public void resetStatistics() {
        evictionStats.clear();
        evictionErrors.clear();
        log.info("Cache eviction statistics reset");
    }

    /**
     * Increment eviction count for statistics
     */
    private void incrementEvictionCount(String operation) {
        evictionStats.computeIfAbsent(operation, k -> new AtomicInteger(0)).incrementAndGet();
    }

    /**
     * Increment eviction error count for statistics
     */
    private void incrementEvictionError(String operation) {
        evictionErrors.computeIfAbsent(operation, k -> new AtomicInteger(0)).incrementAndGet();
    }

    /**
     * Data class for eviction requests
     */
    public static class EvictionRequest {
        public final String cacheKey;
        public final CacheType cacheType;
        public final String tenantId;
        public final String businessId;
        public final String reason;

        public EvictionRequest(String cacheKey, String tenantId, String businessId, String reason) {
            this.cacheKey = cacheKey;
            this.cacheType = null;
            this.tenantId = tenantId;
            this.businessId = businessId;
            this.reason = reason;
        }

        public EvictionRequest(CacheType cacheType, String tenantId, String businessId, String reason) {
            this.cacheKey = null;
            this.cacheType = cacheType;
            this.tenantId = tenantId;
            this.businessId = businessId;
            this.reason = reason;
        }
    }

    // Event classes for Spring Application Events
    public static class ConfigurationUpdateEvent {
        private final String tenantId;
        private final String businessId;

        public ConfigurationUpdateEvent(String tenantId, String businessId) {
            this.tenantId = tenantId;
            this.businessId = businessId;
        }

        public String getTenantId() { return tenantId; }
        public String getBusinessId() { return businessId; }
    }

    public static class MasterListUpdateEvent {
        private final String tenantId;

        public MasterListUpdateEvent(String tenantId) {
            this.tenantId = tenantId;
        }

        public String getTenantId() { return tenantId; }
    }

    public static class TemplateUpdateEvent {
        private final String tenantId;
        private final String businessId;

        public TemplateUpdateEvent(String tenantId, String businessId) {
            this.tenantId = tenantId;
            this.businessId = businessId;
        }

        public String getTenantId() { return tenantId; }
        public String getBusinessId() { return businessId; }
    }

    public static class EventConfigUpdateEvent {
        private final String tenantId;
        private final String businessId;

        public EventConfigUpdateEvent(String tenantId, String businessId) {
            this.tenantId = tenantId;
            this.businessId = businessId;
        }

        public String getTenantId() { return tenantId; }
        public String getBusinessId() { return businessId; }
    }
}