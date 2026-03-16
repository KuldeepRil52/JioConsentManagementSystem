package com.jio.digigov.notification.dto.ratelimit;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Result DTO for rate limit check operations.
 *
 * Contains information about whether a request is allowed or rate limited,
 * along with relevant metadata for client information and retry logic.
 *
 * @author Notification Service Team
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RateLimitResult {

    /**
     * Whether the request is allowed (true) or rate limited (false).
     */
    private boolean allowed;

    /**
     * Current number of requests made in the time window.
     */
    private long currentCount;

    /**
     * Maximum number of requests allowed in the time window.
     */
    private int maxRequests;

    /**
     * Time window in minutes for rate limiting.
     */
    private int windowMinutes;

    /**
     * Number of seconds to wait before retrying.
     * Only populated when allowed = false.
     */
    private Long retryAfterSeconds;

    /**
     * Creates a successful (allowed) rate limit result.
     *
     * @param currentCount current number of requests
     * @param maxRequests maximum allowed requests
     * @param windowMinutes time window in minutes
     * @return RateLimitResult with allowed = true
     */
    public static RateLimitResult allowed(long currentCount, int maxRequests, int windowMinutes) {
        return RateLimitResult.builder()
                .allowed(true)
                .currentCount(currentCount)
                .maxRequests(maxRequests)
                .windowMinutes(windowMinutes)
                .build();
    }

    /**
     * Creates a rate limited (denied) result.
     *
     * @param currentCount current number of requests
     * @param maxRequests maximum allowed requests
     * @param windowMinutes time window in minutes
     * @param retryAfterSeconds seconds to wait before retry
     * @return RateLimitResult with allowed = false
     */
    public static RateLimitResult rateLimited(long currentCount, int maxRequests,
                                               int windowMinutes, long retryAfterSeconds) {
        return RateLimitResult.builder()
                .allowed(false)
                .currentCount(currentCount)
                .maxRequests(maxRequests)
                .windowMinutes(windowMinutes)
                .retryAfterSeconds(retryAfterSeconds)
                .build();
    }
}
