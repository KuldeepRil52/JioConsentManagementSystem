package com.jio.digigov.notification.exception;

import com.jio.digigov.notification.dto.ratelimit.RateLimitResult;
import lombok.Getter;

/**
 * Exception thrown when a rate limit is exceeded.
 *
 * This exception is thrown when a customer identifier (recipient + business)
 * has made too many requests within the configured time window.
 *
 * The exception carries rate limit metadata including:
 * - Current request count
 * - Maximum allowed requests
 * - Time window in minutes
 * - Retry-after time in seconds
 *
 * @author Notification Service Team
 * @since 1.0.0
 */
@Getter
public class RateLimitExceededException extends RuntimeException {

    /**
     * The recipient value (mobile number or email) that exceeded the limit.
     */
    private final String recipientValue;

    /**
     * The business ID for which the limit was exceeded.
     */
    private final String businessId;

    /**
     * The event type that triggered the rate limit.
     */
    private final String eventType;

    /**
     * Time window in minutes for the rate limit.
     */
    private final int windowMinutes;

    /**
     * Number of seconds to wait before retrying.
     */
    private final long retryAfterSeconds;

    /**
     * Current number of requests in the time window.
     */
    private final long currentCount;

    /**
     * Maximum number of requests allowed in the time window.
     */
    private final int maxRequests;

    /**
     * Constructs a RateLimitExceededException.
     *
     * @param recipientValue the recipient value (mobile/email)
     * @param businessId the business ID
     * @param eventType the event type
     * @param result the rate limit result containing metadata
     */
    public RateLimitExceededException(String recipientValue, String businessId,
                                       String eventType, RateLimitResult result) {
        super(String.format("Rate limit exceeded for this recipient. Please try again after %d minutes.",
                result.getWindowMinutes()));
        this.recipientValue = recipientValue;
        this.businessId = businessId;
        this.eventType = eventType;
        this.windowMinutes = result.getWindowMinutes();
        this.retryAfterSeconds = result.getRetryAfterSeconds() != null ? result.getRetryAfterSeconds() : 0L;
        this.currentCount = result.getCurrentCount();
        this.maxRequests = result.getMaxRequests();
    }

    /**
     * Constructs a RateLimitExceededException with detailed parameters.
     *
     * @param recipientValue the recipient value (mobile/email)
     * @param businessId the business ID
     * @param eventType the event type
     * @param windowMinutes time window in minutes
     * @param retryAfterSeconds seconds to wait before retry
     * @param currentCount current request count
     * @param maxRequests maximum allowed requests
     */
    public RateLimitExceededException(String recipientValue, String businessId, String eventType,
                                       int windowMinutes, long retryAfterSeconds,
                                       long currentCount, int maxRequests) {
        super(String.format("Rate limit exceeded for this recipient. Please try again after %d minutes.",
                windowMinutes));
        this.recipientValue = recipientValue;
        this.businessId = businessId;
        this.eventType = eventType;
        this.windowMinutes = windowMinutes;
        this.retryAfterSeconds = retryAfterSeconds;
        this.currentCount = currentCount;
        this.maxRequests = maxRequests;
    }

    /**
     * Gets the recipient value that exceeded the limit.
     *
     * @return recipient value
     */
    public String getRecipientValue() {
        return recipientValue;
    }

    /**
     * Gets the business ID.
     *
     * @return business ID
     */
    public String getBusinessId() {
        return businessId;
    }

    /**
     * Gets the event type.
     *
     * @return event type
     */
    public String getEventType() {
        return eventType;
    }

    /**
     * Gets the time window in minutes.
     *
     * @return window minutes
     */
    public int getWindowMinutes() {
        return windowMinutes;
    }

    /**
     * Gets the retry-after time in seconds.
     *
     * @return retry after seconds
     */
    public long getRetryAfterSeconds() {
        return retryAfterSeconds;
    }

    /**
     * Gets the current request count.
     *
     * @return current count
     */
    public long getCurrentCount() {
        return currentCount;
    }

    /**
     * Gets the maximum allowed requests.
     *
     * @return max requests
     */
    public int getMaxRequests() {
        return maxRequests;
    }
}
