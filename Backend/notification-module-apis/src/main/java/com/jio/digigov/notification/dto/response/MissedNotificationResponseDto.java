package com.jio.digigov.notification.dto.response;

import com.jio.digigov.notification.enums.NotificationChannel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * DTO for missed callback notifications.
 *
 * This DTO provides a consistent response structure for missed callback notifications
 * that have failed delivery or are scheduled for retry. It focuses specifically on
 * callback/webhook notifications and includes all relevant callback-specific information.
 *
 * Supported notification type: CALLBACK only
 *
 * Only includes notifications with status: FAILED, PROCESSING, RETRY_SCHEDULED
 * Excludes: SUCCESSFUL, ACKNOWLEDGED, PENDING
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MissedNotificationResponseDto {

    /**
     * Database ID of the notification record.
     */
    private String id;

    /**
     * Unique identifier for this notification.
     * Used for tracking and correlation across systems.
     */
    private String notificationId;

    /**
     * Type of notification channel.
     * Always "CALLBACK" for missed callback notifications
     */
    @Builder.Default
    private String type = NotificationChannel.CALLBACK.name();

    /**
     * Current processing status.
     * Only failed/processing/scheduled notifications are included.
     * Values: FAILED, PROCESSING, RETRY_SCHEDULED
     */
    private String status;

    /**
     * Event identifier linking this notification to the triggering event.
     */
    private String eventId;

    /**
     * Type of event that triggered this notification.
     * Examples: CONSENT_GRANTED, DATA_BREACH, PROCESSING_REQUEST
     */
    private String eventType;

    /**
     * Correlation identifier for linking related notifications.
     */
    private String correlationId;

    /**
     * Business identifier for configuration and authorization.
     */
    private String businessId;

    /**
     * Type of recipient for the notification.
     * Values: DATA_FIDUCIARY, DATA_PROCESSOR, SYSTEM_WEBHOOK
     */
    private String recipientType;

    /**
     * Unique identifier of the recipient organization.
     */
    private String recipientId;

    /**
     * Processing priority level.
     * Values: HIGH, MEDIUM, LOW
     */
    private String priority;

    /**
     * Number of delivery attempts made.
     */
    private Integer attemptCount;

    /**
     * Maximum number of retry attempts allowed.
     */
    private Integer maxAttempts;

    /**
     * Timestamp when notification was created.
     */
    private LocalDateTime createdAt;

    /**
     * Timestamp when notification was last updated.
     */
    private LocalDateTime updatedAt;

    /**
     * Timestamp when notification processing started.
     */
    private LocalDateTime processedAt;

    /**
     * Timestamp of last delivery attempt.
     */
    private LocalDateTime lastAttemptAt;

    /**
     * Scheduled timestamp for next retry attempt.
     */
    private LocalDateTime nextRetryAt;

    /**
     * Last error message if delivery failed.
     */
    private String lastErrorMessage;

    /**
     * Last HTTP response code for callback notifications.
     */
    private Integer lastResponseCode;

    /**
     * Callback-specific data for webhook notifications.
     * Contains: callbackUrl, jwtToken, jwtExpiresAt, timeoutSeconds,
     *          expectedResponseFormat, customHeaders, eventData
     */
    private Map<String, Object> callbackData;

    /**
     * Additional metadata for audit and debugging.
     */
    private Map<String, Object> metadata;
}