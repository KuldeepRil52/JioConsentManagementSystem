package com.jio.digigov.notification.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

import java.util.List;

/**
 * Configuration properties for OTP rate limiting.
 *
 * Loads rate limit configuration from application.yml under the prefix:
 * rate-limit.otp
 *
 * Properties:
 * - max-requests: Maximum number of requests allowed within the time window
 * - window-minutes: Time window in minutes for rate limiting
 * - enabled-event-types: List of event types that have rate limiting enabled
 * - cache-ttl-minutes: Cache TTL in minutes for rate limit state
 *
 * Example configuration in application.yml:
 * <pre>
 * rate-limit:
 *   otp:
 *     max-requests: 5
 *     window-minutes: 10
 *     enabled-event-types:
 *       - INIT_OTP
 *     cache-ttl-minutes: 10
 * </pre>
 *
 * Can be overridden via environment variables:
 * RATE_LIMIT_OTP_MAX_REQUESTS=3
 * RATE_LIMIT_OTP_WINDOW_MINUTES=15
 *
 * @author Notification Service Team
 * @since 1.0.0
 */
@Configuration
@ConfigurationProperties(prefix = "rate-limit.otp")
@Data
@Validated
public class RateLimitConfig {

    /**
     * Maximum number of requests allowed within the time window.
     * Default: 5 requests
     * Minimum: 1 request
     *
     * If a customer identifier (recipientValue + businessId) makes more than this
     * number of requests within the window-minutes period, subsequent requests
     * will be rejected with HTTP 429 (Too Many Requests).
     */
    @Min(value = 1, message = "Max requests must be at least 1")
    private int maxRequests = 5;

    /**
     * Time window in minutes for rate limiting.
     * Default: 10 minutes
     * Minimum: 1 minute
     *
     * Requests are counted within this sliding time window.
     * For example, if set to 10, the system will count all requests made
     * in the past 10 minutes.
     */
    @Min(value = 1, message = "Window minutes must be at least 1")
    private int windowMinutes = 10;

    /**
     * List of event types that have rate limiting enabled.
     * Default: ["INIT_OTP"]
     *
     * Only events in this list will be subject to rate limiting.
     * This allows for flexible rate limiting configuration across different
     * event types without code changes.
     *
     * Special values:
     * - ["ALL"] or ["*"] - Apply rate limiting to ALL event types
     *
     * Example:
     * - ["INIT_OTP"] - Only OTP initialization is rate limited
     * - ["INIT_OTP", "VERIFY_OTP"] - Both OTP init and verify are rate limited
     * - ["ALL"] - All event types are rate limited
     */
    @NotEmpty(message = "At least one event type must be configured for rate limiting")
    private List<String> enabledEventTypes = List.of("INIT_OTP");

    /**
     * Cache TTL in minutes for rate limit state.
     * Default: 10 minutes (same as window-minutes)
     * Minimum: 1 minute
     *
     * The cache stores recent rate limit checks to avoid excessive database queries.
     * This value should typically match or be slightly larger than window-minutes
     * to ensure accurate rate limiting.
     */
    @Min(value = 1, message = "Cache TTL must be at least 1 minute")
    private int cacheTtlMinutes = 10;

    /**
     * Gets the maximum number of requests allowed.
     *
     * @return max requests
     */
    public int getMaxRequests() {
        return maxRequests;
    }

    /**
     * Sets the maximum number of requests allowed.
     *
     * @param maxRequests max requests (must be >= 1)
     */
    public void setMaxRequests(int maxRequests) {
        this.maxRequests = maxRequests;
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
     * Sets the time window in minutes.
     *
     * @param windowMinutes window minutes (must be >= 1)
     */
    public void setWindowMinutes(int windowMinutes) {
        this.windowMinutes = windowMinutes;
    }

    /**
     * Gets the list of enabled event types.
     *
     * @return enabled event types
     */
    public List<String> getEnabledEventTypes() {
        return enabledEventTypes;
    }

    /**
     * Sets the list of enabled event types.
     *
     * @param enabledEventTypes enabled event types (must not be empty)
     */
    public void setEnabledEventTypes(List<String> enabledEventTypes) {
        this.enabledEventTypes = enabledEventTypes;
    }

    /**
     * Gets the cache TTL in minutes.
     *
     * @return cache TTL minutes
     */
    public int getCacheTtlMinutes() {
        return cacheTtlMinutes;
    }

    /**
     * Sets the cache TTL in minutes.
     *
     * @param cacheTtlMinutes cache TTL minutes (must be >= 1)
     */
    public void setCacheTtlMinutes(int cacheTtlMinutes) {
        this.cacheTtlMinutes = cacheTtlMinutes;
    }

    /**
     * Checks if rate limiting is enabled for the given event type.
     *
     * Special handling for "ALL" and "*" keywords:
     * - If the list contains "ALL" or "*", rate limiting is enabled for all event types
     * - Otherwise, only listed event types have rate limiting enabled
     *
     * @param eventType the event type to check
     * @return true if rate limiting is enabled for this event type
     */
    public boolean isRateLimitEnabled(String eventType) {
        if (enabledEventTypes == null || enabledEventTypes.isEmpty()) {
            return false;
        }

        // Check if "ALL" or "*" is configured (case-insensitive)
        boolean applyToAll = enabledEventTypes.stream()
                .anyMatch(type -> "ALL".equalsIgnoreCase(type) || "*".equals(type));

        if (applyToAll) {
            return true;
        }

        // Otherwise, check if the specific event type is in the list
        return enabledEventTypes.contains(eventType);
    }
}
