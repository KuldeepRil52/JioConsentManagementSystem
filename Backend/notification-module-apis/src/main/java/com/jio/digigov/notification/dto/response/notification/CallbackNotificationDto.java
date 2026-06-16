package com.jio.digigov.notification.dto.response.notification;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Response DTO for Callback Notification details.
 *
 * This DTO represents comprehensive callback/webhook notification information
 * returned when includeNotifications=true in the events API.
 * Contains all relevant fields for tracking webhook delivery status,
 * recipient details, and audit information.
 *
 * @author Notification Service Team
 * @version 1.0
 * @since 2025-01-01
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CallbackNotificationDto {

    /**
     * Unique identifier for this callback notification.
     */
    @JsonProperty("notificationId")
    private String notificationId;

    /**
     * Event identifier linking this notification to the triggering event.
     */
    @JsonProperty("eventId")
    private String eventId;

    /**
     * Correlation identifier for linking related notifications.
     */
    @JsonProperty("correlationId")
    private String correlationId;

    /**
     * Business identifier for configuration and authorization.
     */
    @JsonProperty("businessId")
    private String businessId;

    /**
     * Type of recipient for the callback notification.
     * Values: "DATA_FIDUCIARY", "DATA_PROCESSOR", "SYSTEM_WEBHOOK"
     */
    @JsonProperty("recipientType")
    private String recipientType;

    /**
     * Unique identifier of the recipient organization.
     */
    @JsonProperty("recipientId")
    private String recipientId;

    /**
     * Target webhook URL for the callback delivery.
     */
    @JsonProperty("callbackUrl")
    private String callbackUrl;

    /**
     * Event type classification for the callback.
     */
    @JsonProperty("eventType")
    private String eventType;

    /**
     * Event-specific data payload for the callback.
     */
    @JsonProperty("eventData")
    private Map<String, Object> eventData;

    /**
     * JWT token expiry timestamp.
     */
    @JsonProperty("jwtExpiresAt")
    private LocalDateTime jwtExpiresAt;

    /**
     * Custom HTTP headers to include in the webhook request.
     */
    @JsonProperty("customHeaders")
    private Map<String, String> customHeaders;

    /**
     * Timeout in seconds for the webhook HTTP request.
     */
    @JsonProperty("timeoutSeconds")
    private Integer timeoutSeconds;

    /**
     * Expected response format from the webhook recipient.
     */
    @JsonProperty("expectedResponseFormat")
    private String expectedResponseFormat;

    /**
     * Current processing status of the callback notification.
     */
    @JsonProperty("status")
    private String status;

    /**
     * Processing priority level (HIGH, MEDIUM, LOW).
     */
    @JsonProperty("priority")
    private String priority;

    /**
     * Number of delivery attempts made.
     */
    @JsonProperty("attemptCount")
    private Integer attemptCount;

    /**
     * Maximum number of retry attempts allowed.
     */
    @JsonProperty("maxAttempts")
    private Integer maxAttempts;

    /**
     * Timestamp when callback processing started.
     */
    @JsonProperty("processedAt")
    private LocalDateTime processedAt;

    /**
     * Timestamp when callback was successfully delivered.
     */
    @JsonProperty("deliveredAt")
    private LocalDateTime deliveredAt;

    /**
     * Timestamp when callback was acknowledged by recipient.
     */
    @JsonProperty("acknowledgedAt")
    private LocalDateTime acknowledgedAt;

    /**
     * Timestamp of last processing attempt.
     */
    @JsonProperty("lastAttemptAt")
    private LocalDateTime lastAttemptAt;

    /**
     * Scheduled timestamp for next retry attempt.
     */
    @JsonProperty("nextRetryAt")
    private LocalDateTime nextRetryAt;

    /**
     * Error message from last failed delivery attempt.
     */
    @JsonProperty("lastErrorMessage")
    private String lastErrorMessage;

    /**
     * HTTP response from webhook recipient.
     */
    @JsonProperty("webhookResponse")
    private Map<String, Object> webhookResponse;

    /**
     * HTTP status code from webhook delivery.
     */
    @JsonProperty("httpStatusCode")
    private Integer httpStatusCode;

    /**
     * Request duration in milliseconds.
     */
    @JsonProperty("requestDurationMs")
    private Long requestDurationMs;

    /**
     * Status history entries for audit trail.
     */
    @JsonProperty("statusHistory")
    private List<Object> statusHistory;

    /**
     * Timestamp when notification was created.
     */
    @JsonProperty("createdAt")
    private LocalDateTime createdAt;

    /**
     * Timestamp when notification was last updated.
     */
    @JsonProperty("updatedAt")
    private LocalDateTime updatedAt;
}
