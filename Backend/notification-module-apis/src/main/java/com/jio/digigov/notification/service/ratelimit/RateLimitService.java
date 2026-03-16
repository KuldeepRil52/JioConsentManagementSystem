package com.jio.digigov.notification.service.ratelimit;

/**
 * Service interface for rate limiting operations.
 *
 * Provides rate limiting functionality for notification events based on:
 * - Tenant ID
 * - Business ID
 * - Recipient value (mobile/email)
 * - Event type
 *
 * Implements sliding window rate limiting with configurable thresholds
 * and time windows. Uses caching for performance optimization.
 *
 * @author Notification Service Team
 * @since 1.0.0
 */
public interface RateLimitService {

    /**
     * Checks if a request should be rate limited.
     *
     * This method performs the following:
     * 1. Checks if rate limiting is enabled for the event type
     * 2. Checks cache for recent rate limit state
     * 3. On cache miss, queries NotificationEvent collection
     * 4. Counts requests in the sliding time window
     * 5. Throws RateLimitExceededException if limit exceeded
     *
     * @param tenantId the tenant identifier
     * @param businessId the business identifier
     * @param recipientValue the recipient value (mobile number or email)
     * @param eventType the event type (e.g., "INIT_OTP")
     * @throws com.jio.digigov.notification.exception.RateLimitExceededException if rate limit is exceeded
     */
    void checkRateLimit(String tenantId, String businessId, String recipientValue, String eventType);

    /**
     * Checks if rate limiting is enabled for the given event type.
     *
     * @param eventType the event type to check
     * @return true if rate limiting is enabled for this event type
     */
    boolean isRateLimitEnabled(String eventType);

    /**
     * Gets the current request count for a recipient in the time window.
     *
     * This method is useful for monitoring and debugging purposes.
     * It does not throw exceptions if the limit is exceeded.
     *
     * @param tenantId the tenant identifier
     * @param businessId the business identifier
     * @param recipientValue the recipient value (mobile number or email)
     * @param eventType the event type
     * @return the current request count
     */
    long getCurrentRequestCount(String tenantId, String businessId, String recipientValue, String eventType);

    /**
     * Evicts the rate limit cache for a specific recipient.
     *
     * This forces the next request to query the database for fresh data.
     * Useful for testing or manual cache invalidation.
     *
     * @param tenantId the tenant identifier
     * @param businessId the business identifier
     * @param recipientValue the recipient value
     * @param eventType the event type
     */
    void evictRateLimitCache(String tenantId, String businessId, String recipientValue, String eventType);
}
