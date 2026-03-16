package com.jio.digigov.notification.service.ratelimit.impl;

import com.jio.digigov.notification.config.RateLimitConfig;
import com.jio.digigov.notification.dto.ratelimit.RateLimitResult;
import com.jio.digigov.notification.entity.event.NotificationEvent;
import com.jio.digigov.notification.exception.RateLimitExceededException;
import com.jio.digigov.notification.service.cache.impl.CaffeineCacheService;
import com.jio.digigov.notification.service.ratelimit.RateLimitService;
import com.jio.digigov.notification.util.MongoTemplateProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Implementation of RateLimitService for notification event rate limiting.
 *
 * This service implements sliding window rate limiting with the following features:
 * - Configurable max requests and time window
 * - Caffeine cache for performance optimization
 * - Multi-tenant support with tenant-specific database routing
 * - No status filtering (counts all requests regardless of status)
 *
 * Rate Limit Algorithm:
 * 1. Check if event type has rate limiting enabled
 * 2. Check cache for recent rate limit state
 * 3. On cache miss, query NotificationEvent collection
 * 4. Count requests in sliding time window (now - windowMinutes)
 * 5. If count >= maxRequests, throw RateLimitExceededException
 * 6. Cache the result for subsequent requests
 *
 * Cache Key Format: rate-limit:{tenantId}:{businessId}:{recipientValue}:{eventType}
 *
 * @author Notification Service Team
 * @since 1.0.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RateLimitServiceImpl implements RateLimitService {

    private final MongoTemplateProvider mongoTemplateProvider;
    private final CaffeineCacheService<Long> cacheService;
    private final RateLimitConfig rateLimitConfig;

    private static final String CACHE_KEY_PREFIX = "rate-limit";

    @Override
    public void checkRateLimit(String tenantId, String businessId, String recipientValue, String eventType) {
        log.debug("Checking rate limit for tenant: {}, business: {}, recipient: {}, eventType: {}",
                tenantId, businessId, recipientValue, eventType);

        // Skip if rate limiting is not enabled for this event type
        if (!isRateLimitEnabled(eventType)) {
            log.debug("Rate limiting not enabled for event type: {}", eventType);
            return;
        }

        // Get current request count
        long currentCount = getCurrentRequestCount(tenantId, businessId, recipientValue, eventType);
        int maxRequests = rateLimitConfig.getMaxRequests();
        int windowMinutes = rateLimitConfig.getWindowMinutes();

        log.debug("Current count: {}, Max requests: {}, Window: {} minutes",
                currentCount, maxRequests, windowMinutes);

        // Check if limit is exceeded
        if (currentCount >= maxRequests) {
            // Calculate retry-after time in seconds (window duration)
            long retryAfterSeconds = windowMinutes * 60L;

            // Create rate limit result
            RateLimitResult result = RateLimitResult.rateLimited(
                    currentCount,
                    maxRequests,
                    windowMinutes,
                    retryAfterSeconds
            );

            log.warn("Rate limit exceeded for tenant: {}, business: {}, recipient: {}, eventType: {} - Count: {}/{}",
                    tenantId, businessId, recipientValue, eventType, currentCount, maxRequests);

            // Throw exception
            throw new RateLimitExceededException(recipientValue, businessId, eventType, result);
        }

        log.debug("Rate limit check passed - Count: {}/{}", currentCount, maxRequests);
    }

    @Override
    public boolean isRateLimitEnabled(String eventType) {
        return rateLimitConfig.isRateLimitEnabled(eventType);
    }

    @Override
    public long getCurrentRequestCount(String tenantId, String businessId, String recipientValue, String eventType) {
        // Build cache key
        String cacheKey = buildCacheKey(recipientValue, eventType);

        // Check cache first
        Optional<Long> cachedCount = cacheService.get(cacheKey, tenantId, businessId);
        if (cachedCount.isPresent()) {
            log.debug("Cache hit for rate limit key: {}", cacheKey);
            return cachedCount.get();
        }

        log.debug("Cache miss for rate limit key: {}, querying database", cacheKey);

        // Cache miss - query database
        long count = queryNotificationEventCount(tenantId, businessId, recipientValue, eventType);

        // Cache the result
        Duration cacheTtl = Duration.ofMinutes(rateLimitConfig.getCacheTtlMinutes());
        cacheService.put(cacheKey, count, tenantId, businessId, cacheTtl);

        log.debug("Cached rate limit count: {} for key: {}", count, cacheKey);

        return count;
    }

    @Override
    public void evictRateLimitCache(String tenantId, String businessId, String recipientValue, String eventType) {
        String cacheKey = buildCacheKey(recipientValue, eventType);
        cacheService.evict(cacheKey, tenantId, businessId);
        log.debug("Evicted rate limit cache for key: {}", cacheKey);
    }

    /**
     * Queries the NotificationEvent collection to count requests in the time window.
     *
     * Query criteria:
     * - customer_identifiers.value = recipientValue
     * - businessId = businessId
     * - eventType = eventType
     * - createdAt >= (now - windowMinutes)
     * - NO status filter (counts all requests regardless of status)
     *
     * @param tenantId tenant identifier
     * @param businessId business identifier
     * @param recipientValue recipient value (mobile/email)
     * @param eventType event type
     * @return count of requests in the time window
     */
    private long queryNotificationEventCount(String tenantId, String businessId,
                                               String recipientValue, String eventType) {
        try {
            // Get the correct MongoTemplate for this tenant
            MongoTemplate mongoTemplate = mongoTemplateProvider.getTemplate(tenantId);

            // Calculate time window start
            LocalDateTime windowStart = LocalDateTime.now()
                    .minusMinutes(rateLimitConfig.getWindowMinutes());

            // Build query criteria
            Criteria criteria = Criteria.where("customer_identifiers.value").is(recipientValue)
                    .and("business_id").is(businessId)
                    .and("event_type").is(eventType)
                    .and("createdAt").gte(windowStart);

            Query query = new Query(criteria);

            // Execute count query
            long count = mongoTemplate.count(query, NotificationEvent.class);

            log.debug("Query result for tenant: {}, business: {}, recipient: {}, eventType: {} - Count: {} (since {})",
                    tenantId, businessId, recipientValue, eventType, count, windowStart);

            return count;

        } catch (Exception e) {
            log.error("Error querying NotificationEvent count for rate limit - tenant: {}, business: {}, recipient: {}, eventType: {}",
                    tenantId, businessId, recipientValue, eventType, e);
            // On error, return 0 to allow the request (fail open)
            return 0L;
        }
    }

    /**
     * Builds the cache key for rate limit state.
     *
     * Format: rate-limit:{recipientValue}:{eventType}
     * The tenantId and businessId are added by the cache service
     *
     * @param recipientValue recipient value
     * @param eventType event type
     * @return cache key
     */
    private String buildCacheKey(String recipientValue, String eventType) {
        return String.format("%s:%s:%s", CACHE_KEY_PREFIX, recipientValue, eventType);
    }
}
