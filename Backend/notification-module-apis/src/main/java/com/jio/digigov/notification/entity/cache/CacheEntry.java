package com.jio.digigov.notification.entity.cache;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

/**
 * MongoDB document representing a cache entry.
 *
 * Stored in tenant-specific databases following the pattern:
 * - tenant_db_shared for global data
 * - tenant_db_{tenantId} for tenant-specific cache entries
 * - cms_db_admin as fallback
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "cache_entries")
@CompoundIndex(def = "{'cacheKey': 1, 'businessId': 1}", name = "cache_key_business_idx")
public class CacheEntry {

    @Id
    private String id;

    /**
     * Business identifier for multi-business isolation within tenant
     */
    @Indexed
    private String businessId;

    /**
     * Cache key including type and identifier
     * Format: {type}:{tenantId}:{businessId}:{identifier}
     */
    @Indexed
    private String cacheKey;

    /**
     * Cache type for categorization and management
     */
    @Indexed
    private String cacheType;

    /**
     * Serialized cache value (JSON format)
     */
    private String value;

    /**
     * Value type for proper deserialization
     */
    private String valueType;

    /**
     * When the cache entry was created
     */
    private LocalDateTime createdAt;

    /**
     * When the cache entry expires (TTL index)
     */
    @Indexed(expireAfterSeconds = 0)
    private LocalDateTime expiresAt;

    /**
     * TTL in minutes for reference
     */
    private long ttlMinutes;

    /**
     * Additional metadata for monitoring
     */
    private String metadata;

    /**
     * Checks if this cache entry has expired
     */
    public boolean isExpired() {
        return expiresAt != null && LocalDateTime.now().isAfter(expiresAt);
    }

    /**
     * Checks if this cache entry is approaching expiry
     */
    public boolean isApproachingExpiry(long minutesBeforeExpiry) {
        if (expiresAt == null) {
            return false;
        }
        LocalDateTime threshold = LocalDateTime.now().plusMinutes(minutesBeforeExpiry);
        return threshold.isAfter(expiresAt);
    }

    /**
     * Gets remaining TTL in minutes
     */
    public long getRemainingTtlMinutes() {
        if (expiresAt == null) {
            return 0;
        }
        LocalDateTime now = LocalDateTime.now();
        if (now.isAfter(expiresAt)) {
            return 0;
        }
        return java.time.Duration.between(now, expiresAt).toMinutes();
    }
}