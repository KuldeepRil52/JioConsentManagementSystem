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
 * Response DTO for Email Notification details.
 *
 * This DTO represents comprehensive email notification information
 * returned when includeNotifications=true in the events API.
 * Contains all relevant fields for tracking email delivery status,
 * content, recipients, and audit information.
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
public class EmailNotificationDto {

    /**
     * Unique identifier for this email notification.
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
     * Template identifier for email content resolution.
     */
    @JsonProperty("templateId")
    private String templateId;

    /**
     * Primary recipient email addresses.
     */
    @JsonProperty("to")
    private List<String> to;

    /**
     * Carbon copy recipient email addresses.
     */
    @JsonProperty("cc")
    private List<String> cc;

    /**
     * Blind carbon copy recipient email addresses.
     */
    @JsonProperty("bcc")
    private List<String> bcc;

    /**
     * Email subject line after template resolution.
     */
    @JsonProperty("subject")
    private String subject;

    /**
     * Email body content after template resolution.
     */
    @JsonProperty("content")
    private String content;

    /**
     * Email content type (text/plain or text/html).
     */
    @JsonProperty("contentType")
    private String contentType;

    /**
     * Template argument values used for placeholder substitution.
     */
    @JsonProperty("templateArgs")
    private Map<String, Object> templateArgs;

    /**
     * Email type classification for tracking and analytics.
     */
    @JsonProperty("emailType")
    private String emailType;

    /**
     * Reply-to email address for recipient responses.
     */
    @JsonProperty("replyTo")
    private String replyTo;

    /**
     * Sender email address for the notification.
     */
    @JsonProperty("from")
    private String from;

    /**
     * Sender display name for the notification.
     */
    @JsonProperty("fromName")
    private String fromName;

    /**
     * File attachments included with the email.
     */
    @JsonProperty("attachments")
    private List<Object> attachments;

    /**
     * Current processing status of the email notification.
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
     * Timestamp when email processing started.
     */
    @JsonProperty("processedAt")
    private LocalDateTime processedAt;

    /**
     * Timestamp when email was successfully sent to mail server.
     */
    @JsonProperty("sentAt")
    private LocalDateTime sentAt;

    /**
     * Timestamp when email delivery was confirmed.
     */
    @JsonProperty("deliveredAt")
    private LocalDateTime deliveredAt;

    /**
     * Timestamp when email was opened by recipient.
     */
    @JsonProperty("openedAt")
    private LocalDateTime openedAt;

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
     * DigiGov API response from email delivery request.
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
