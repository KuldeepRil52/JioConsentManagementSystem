package com.jio.digigov.notification.service.kafka;

import java.time.LocalDateTime;

/**
 * Service interface for managing retry policies in the notification system.
 *
 * Provides configurable retry strategies with exponential backoff algorithms
 * for different types of notification failures. Supports channel-specific
 * retry policies and adaptive backoff based on failure patterns.
 *
 * Key Features:
 * - Exponential backoff with jitter to prevent thundering herd
 * - Channel-specific retry policies (SMS, Email, Webhook)
 * - Configurable maximum retry attempts and backoff multipliers
 * - Circuit breaker integration for systematic failures
 * - Retry policy metrics and monitoring
 *
 * @author Notification Service Team
 * @version 1.0
 * @since 2024-01-01
 */
public interface RetryPolicyService {

    /**
     * Calculates the next retry time using exponential backoff.
     *
     * @param attemptCount Current attempt count (0-based)
     * @return Next retry timestamp
     */
    LocalDateTime calculateNextRetry(int attemptCount);

    /**
     * Calculates the next retry time for a specific notification channel.
     *
     * @param attemptCount Current attempt count (0-based)
     * @param channel Notification channel (SMS, EMAIL, CALLBACK)
     * @return Next retry timestamp
     */
    LocalDateTime calculateNextRetry(int attemptCount, String channel);

    /**
     * Calculates retry delay with custom base delay and multiplier.
     *
     * @param attemptCount Current attempt count (0-based)
     * @param baseDelayMs Base delay in milliseconds
     * @param multiplier Exponential backoff multiplier
     * @param maxDelayMs Maximum delay cap in milliseconds
     * @return Next retry timestamp
     */
    LocalDateTime calculateNextRetry(int attemptCount, long baseDelayMs, double multiplier, long maxDelayMs);

    /**
     * Determines if a notification should be retried based on attempt count.
     *
     * @param attemptCount Current attempt count
     * @param channel Notification channel
     * @return true if retry should be attempted
     */
    boolean shouldRetry(int attemptCount, String channel);

    /**
     * Gets the maximum retry attempts for a specific channel.
     *
     * @param channel Notification channel
     * @return Maximum retry attempts
     */
    int getMaxRetryAttempts(String channel);

    /**
     * Determines if an error is retryable based on error type and message.
     *
     * @param error Error message or exception
     * @param channel Notification channel
     * @return true if the error is retryable
     */
    boolean isRetryableError(String error, String channel);

    /**
     * Records retry metrics for monitoring and analysis.
     *
     * @param channel Notification channel
     * @param attemptCount Current attempt count
     * @param success Whether the retry was successful
     * @param errorType Type of error encountered
     */
    void recordRetryMetrics(String channel, int attemptCount, boolean success, String errorType);
}