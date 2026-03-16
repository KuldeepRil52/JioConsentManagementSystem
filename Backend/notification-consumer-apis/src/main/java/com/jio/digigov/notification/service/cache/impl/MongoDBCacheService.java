package com.jio.digigov.notification.service.cache.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jio.digigov.notification.config.MultiTenantMongoConfig;
import com.jio.digigov.notification.config.properties.CacheProperties;
import com.jio.digigov.notification.dto.cache.CacheMetrics;
import com.jio.digigov.notification.entity.cache.CacheEntry;
import com.jio.digigov.notification.enums.CacheType;
import com.jio.digigov.notification.service.cache.CacheService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

/**
 * MongoDB-based distributed cache service implementation.
 *
 * Uses tenant-specific databases following the existing multi-tenant pattern:
 * - tenant_db_shared for global data
 * - tenant_db_{tenantId} for tenant-specific cache entries
 * - cms_db_admin as fallback
 *
 * Key Features:
 * - Multi-tenant cache isolation
 * - TTL-based automatic expiry
 * - JSON serialization for complex objects
 * - Cache statistics and monitoring
 * - Distributed caching across application instances
 *
 * @param <T> the type of objects stored in the cache
 */
@Service
@ConditionalOnProperty(name = "cache.type", havingValue = "mongodb")
@Slf4j
public class MongoDBCacheService<T> implements CacheService<T> {

    private final MultiTenantMongoConfig mongoConfig;
    private final CacheProperties cacheProperties;
    private final ObjectMapper objectMapper;
    private final Map<String, AtomicLong> hitCounters;
    private final Map<String, AtomicLong> missCounters;

    public MongoDBCacheService(MultiTenantMongoConfig mongoConfig,
                              CacheProperties cacheProperties,
                              ObjectMapper objectMapper) {
        this.mongoConfig = mongoConfig;
        this.cacheProperties = cacheProperties;
        this.objectMapper = objectMapper;
        this.hitCounters = new HashMap<>();
        this.missCounters = new HashMap<>();
    }

    @PostConstruct
    public void initialize() {
        if (cacheProperties.getMongodb().isCreateTtlIndex()) {
            createTtlIndexes();
        }
        log.info("MongoDB cache service initialized");
    }

    @Override
    @SuppressWarnings("unchecked")
    public Optional<T> get(String key, String tenantId, String businessId) {
        try {
            MongoTemplate mongoTemplate = mongoConfig.getMongoTemplateForTenant(tenantId);
            String cacheKey = buildCacheKey(key, tenantId, businessId);

            Query query = new Query()
                .addCriteria(Criteria.where("cacheKey").is(cacheKey))
                .addCriteria(Criteria.where("businessId").is(businessId));

            CacheEntry entry = mongoTemplate.findOne(query, CacheEntry.class);

            if (entry == null || entry.isExpired()) {
                incrementMissCounter(extractTypeFromKey(key));
                log.debug("Cache miss for key: {}", cacheKey);

                // Clean up expired entry
                if (entry != null && entry.isExpired()) {
                    mongoTemplate.remove(query, CacheEntry.class);
                }

                return Optional.empty();
            }

            T value = deserializeValue(entry.getValue(), entry.getValueType());
            incrementHitCounter(extractTypeFromKey(key));
            log.debug("Cache hit for key: {}", cacheKey);

            return Optional.of(value);

        } catch (Exception e) {
            log.error("Error retrieving from cache for key '{}': {}", key, e.getMessage());
            incrementMissCounter(extractTypeFromKey(key));
            return Optional.empty();
        }
    }

    @Override
    public void put(String key, T value, String tenantId, String businessId, Duration ttl) {
        try {
            MongoTemplate mongoTemplate = mongoConfig.getMongoTemplateForTenant(tenantId);
            String cacheKey = buildCacheKey(key, tenantId, businessId);

            LocalDateTime now = LocalDateTime.now();
            LocalDateTime expiresAt = now.plus(ttl);

            CacheEntry entry = CacheEntry.builder()
                .businessId(businessId)
                .cacheKey(cacheKey)
                .cacheType(extractTypeFromKey(key))
                .value(serializeValue(value))
                .valueType(value.getClass().getName())
                .createdAt(now)
                .expiresAt(expiresAt)
                .ttlMinutes(ttl.toMinutes())
                .build();

            // Remove existing entry first to avoid duplicates
            Query query = new Query()
                .addCriteria(Criteria.where("cacheKey").is(cacheKey))
                .addCriteria(Criteria.where("businessId").is(businessId));
            mongoTemplate.remove(query, CacheEntry.class);

            // Insert new entry
            mongoTemplate.save(entry);

            log.debug("Cached value for key: {} with TTL: {} minutes", cacheKey, ttl.toMinutes());

        } catch (Exception e) {
            log.error("Error storing in cache for key '{}': {}", key, e.getMessage());
        }
    }

    @Override
    public void put(String key, T value, String tenantId, String businessId, CacheType cacheType) {
        Duration ttl = cacheProperties.getTtlForType(cacheType);
        put(key, value, tenantId, businessId, ttl);
    }

    @Override
    public void evict(String key, String tenantId, String businessId) {
        try {
            MongoTemplate mongoTemplate = mongoConfig.getMongoTemplateForTenant(tenantId);
            String cacheKey = buildCacheKey(key, tenantId, businessId);

            Query query = new Query()
                .addCriteria(Criteria.where("cacheKey").is(cacheKey))
                .addCriteria(Criteria.where("businessId").is(businessId));

            long deletedCount = mongoTemplate.remove(query, CacheEntry.class).getDeletedCount();
            log.debug("Evicted {} cache entries for key: {}", deletedCount, cacheKey);

        } catch (Exception e) {
            log.error("Error evicting from cache for key '{}': {}", key, e.getMessage());
        }
    }

    @Override
    public void evictAll(String tenantId, String businessId) {
        try {
            MongoTemplate mongoTemplate = mongoConfig.getMongoTemplateForTenant(tenantId);

            Query query = new Query()
                .addCriteria(Criteria.where("businessId").is(businessId));

            long deletedCount = mongoTemplate.remove(query, CacheEntry.class).getDeletedCount();
            log.debug("Evicted {} cache entries for tenant: {}, business: {}",
                     deletedCount, tenantId, businessId);

        } catch (Exception e) {
            log.error("Error evicting all cache entries for tenant '{}', business '{}': {}",
                     tenantId, businessId, e.getMessage(), e);
        }
    }

    @Override
    public void evictByType(CacheType type, String tenantId, String businessId) {
        try {
            MongoTemplate mongoTemplate = mongoConfig.getMongoTemplateForTenant(tenantId);

            Query query = new Query()
                .addCriteria(Criteria.where("cacheType").is(type.getKey()))
                .addCriteria(Criteria.where("businessId").is(businessId));

            long deletedCount = mongoTemplate.remove(query, CacheEntry.class).getDeletedCount();
            log.debug("Evicted {} cache entries of type '{}' for tenant: {}, business: {}",
                     deletedCount, type.getKey(), tenantId, businessId);

        } catch (Exception e) {
            log.error("Error evicting cache entries by type '{}': {}", type.getKey(), e.getMessage());
        }
    }

    @Override
    public boolean exists(String key, String tenantId, String businessId) {
        try {
            MongoTemplate mongoTemplate = mongoConfig.getMongoTemplateForTenant(tenantId);
            String cacheKey = buildCacheKey(key, tenantId, businessId);

            Query query = new Query()
                .addCriteria(Criteria.where("cacheKey").is(cacheKey))
                .addCriteria(Criteria.where("businessId").is(businessId))
                .addCriteria(Criteria.where("expiresAt").gt(LocalDateTime.now()));

            return mongoTemplate.exists(query, CacheEntry.class);

        } catch (Exception e) {
            log.warn("Error checking cache existence for key '{}': {}", key, e.getMessage());
            return false;
        }
    }

    @Override
    public CacheMetrics getStats() {
        long totalHits = hitCounters.values().stream().mapToLong(AtomicLong::get).sum();
        long totalMisses = missCounters.values().stream().mapToLong(AtomicLong::get).sum();
        long totalRequests = totalHits + totalMisses;

        return CacheMetrics.builder()
            .requestCount(totalRequests)
            .hitCount(totalHits)
            .missCount(totalMisses)
            .hitRate(totalRequests > 0 ? (double) totalHits / totalRequests : 0.0)
            .missRate(totalRequests > 0 ? (double) totalMisses / totalRequests : 0.0)
            .size(getTotalCacheSize())
            .cacheType("mongodb")
            .collectedAt(LocalDateTime.now())
            .build();
    }

    @Override
    public CacheMetrics getStats(CacheType cacheType) {
        String typeKey = cacheType.getKey();
        long hits = hitCounters.getOrDefault(typeKey, new AtomicLong(0)).get();
        long misses = missCounters.getOrDefault(typeKey, new AtomicLong(0)).get();
        long requests = hits + misses;

        return CacheMetrics.builder()
            .requestCount(requests)
            .hitCount(hits)
            .missCount(misses)
            .hitRate(requests > 0 ? (double) hits / requests : 0.0)
            .missRate(requests > 0 ? (double) misses / requests : 0.0)
            .size(getCacheSizeByType(cacheType))
            .cacheType("mongodb-" + typeKey)
            .collectedAt(LocalDateTime.now())
            .build();
    }

    @Override
    public void clearAll() {
        try {
            // Clear all cache entries across all tenants (use with caution)
            // This is a dangerous operation and should be used carefully
            log.warn("Clearing all MongoDB cache entries across all tenants");

            // Note: This would require iterating through all tenant databases
            // For now, we'll clear the counters
            hitCounters.clear();
            missCounters.clear();

            log.info("Cleared all cache statistics");

        } catch (Exception e) {
            log.error("Error clearing all cache entries: {}", e.getMessage());
        }
    }

    @Override
    public String getCacheType() {
        return "mongodb";
    }

    @Override
    public boolean isHealthy() {
        try {
            // Simple health check - try to access a tenant database
            MongoTemplate mongoTemplate = mongoConfig.getMongoTemplateForTenant("default");
            return mongoTemplate != null &&
                   mongoTemplate.getCollection(cacheProperties.getMongodb().getCollection()) != null;
        } catch (Exception e) {
            log.warn("MongoDB cache health check failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Find cache entries approaching expiry for refresh
     */
    public List<CacheEntry> findEntriesApproachingExpiry(String tenantId, Duration beforeExpiry) {
        try {
            MongoTemplate mongoTemplate = mongoConfig.getMongoTemplateForTenant(tenantId);
            LocalDateTime threshold = LocalDateTime.now().plus(beforeExpiry);

            Query query = new Query()
                .addCriteria(Criteria.where("expiresAt").lte(threshold))
                .addCriteria(Criteria.where("expiresAt").gt(LocalDateTime.now()));

            return mongoTemplate.find(query, CacheEntry.class);

        } catch (Exception e) {
            log.error("Error finding entries approaching expiry for tenant '{}': {}",
                     tenantId, e.getMessage(), e);
            return List.of();
        }
    }

    /**
     * Build cache key with tenant and business isolation
     */
    private String buildCacheKey(String key, String tenantId, String businessId) {
        return String.format("%s:%s:%s", key, tenantId, businessId);
    }

    /**
     * Extract cache type from cache key
     */
    private String extractTypeFromKey(String key) {
        if (key.contains(":")) {
            return key.split(":")[0];
        }
        return "unknown";
    }

    /**
     * Serialize value to JSON string
     */
    private String serializeValue(T value) throws Exception {
        if (value instanceof String) {
            return (String) value;
        }
        return objectMapper.writeValueAsString(value);
    }

    /**
     * Deserialize value from JSON string
     */
    @SuppressWarnings("unchecked")
    private T deserializeValue(String jsonValue, String valueType) throws Exception {
        if (String.class.getName().equals(valueType)) {
            return (T) jsonValue;
        }

        Class<?> clazz = Class.forName(valueType);
        return (T) objectMapper.readValue(jsonValue, clazz);
    }

    /**
     * Increment hit counter for a cache type
     */
    private void incrementHitCounter(String cacheType) {
        hitCounters.computeIfAbsent(cacheType, k -> new AtomicLong(0)).incrementAndGet();
    }

    /**
     * Increment miss counter for a cache type
     */
    private void incrementMissCounter(String cacheType) {
        missCounters.computeIfAbsent(cacheType, k -> new AtomicLong(0)).incrementAndGet();
    }

    /**
     * Get total cache size across all tenants (expensive operation)
     */
    private long getTotalCacheSize() {
        // This would be expensive to calculate in real-time
        // Consider implementing a cached counter or estimated size
        return 0;
    }

    /**
     * Get cache size for a specific type (expensive operation)
     */
    private long getCacheSizeByType(CacheType cacheType) {
        // This would be expensive to calculate in real-time
        // Consider implementing a cached counter or estimated size
        return 0;
    }

    /**
     * Create TTL indexes on cache collections
     */
    private void createTtlIndexes() {
        try {
            // Create indexes on commonly used tenant databases
            createTtlIndexForTenant("shared");
            createTtlIndexForTenant("default");

            log.info("Created TTL indexes for MongoDB cache collections");

        } catch (Exception e) {
            log.error("Error creating TTL indexes: {}", e.getMessage());
        }
    }

    /**
     * Create TTL index for a specific tenant database
     */
    private void createTtlIndexForTenant(String tenantId) {
        try {
            MongoTemplate mongoTemplate = mongoConfig.getMongoTemplateForTenant(tenantId);

            // Create TTL index on expiresAt field
            Index ttlIndex = new Index().on("expiresAt", org.springframework.data.domain.Sort.Direction.ASC).expire(Duration.ZERO);
            mongoTemplate.indexOps(CacheEntry.class).ensureIndex(ttlIndex);

            // Create compound index for efficient queries
            Index compoundIndex = new Index()
                .on("cacheKey", org.springframework.data.domain.Sort.Direction.ASC)
                .on("businessId", org.springframework.data.domain.Sort.Direction.ASC);
            mongoTemplate.indexOps(CacheEntry.class).ensureIndex(compoundIndex);

        } catch (Exception e) {
            log.warn("Error creating indexes for tenant '{}': {}", tenantId, e.getMessage());
        }
    }
}