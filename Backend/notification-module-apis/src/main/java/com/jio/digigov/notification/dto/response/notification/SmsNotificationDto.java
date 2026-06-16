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
 * Response DTO for SMS Notification details.
 *
 * This DTO represents comprehensive SMS notification information
 * returned when includeNotifications=true in the events API.
 * Contains all relevant fields for tracking SMS delivery status,
 * content, and audit information.
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
public class SmsNotificationDto {

    /**
     * Unique identifier for this SMS notification.
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
     * Template identifier for message content resolution.
     */
    @JsonProperty("templateId")
    private String templateId;

    /**
     * Recipient mobile number with country code.
     */
    @JsonProperty("mobile")
    private String mobile;

    /**
     * Resolved SMS message content after template processing.
     */
    @JsonProperty("messageContent")
    private String messageContent;

    /**
     * Template argument values used for placeholder substitution.
     */
    @JsonProperty("templateArgs")
    private Map<String, Object> templateArgs;

    /**
     * DLT Entity ID for TRAI compliance.
     */
    @JsonProperty("dltEntityId")
    private String dltEntityId;

    /**
     * DLT Template ID for TRAI compliance.
     */
    @JsonProperty("dltTemplateId")
    private String dltTemplateId;

    /**
     * Sender ID or shortcode for SMS delivery.
     */
    @JsonProperty("from")
    private String from;

    /**
     * Operator country codes for delivery routing.
     */
    @JsonProperty("oprCountries")
    private List<String> oprCountries;

    /**
     * Current processing status of the SMS notification.
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
     * Timestamp when SMS processing started.
     */
    @JsonProperty("processedAt")
    private LocalDateTime processedAt;

    /**
     * Timestamp when SMS was successfully sent to DigiGov.
     */
    @JsonProperty("sentAt")
    private LocalDateTime sentAt;

    /**
     * Timestamp when final delivery was confirmed.
     */
    @JsonProperty("deliveredAt")
    private LocalDateTime deliveredAt;

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
     * DigiGov API response from SMS delivery request.
     */
    @JsonProperty("digiGovResponse")
    private Map<String, Object> digiGovResponse;

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
