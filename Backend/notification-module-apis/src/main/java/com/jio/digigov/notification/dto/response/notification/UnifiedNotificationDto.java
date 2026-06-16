package com.jio.digigov.notification.dto.response.notification;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Unified Notification DTO for REST API responses.
 *
 * This DTO provides a streamlined, flattened representation of all notification types
 * (SMS, Email, Callback) with a notificationType discriminator field.
 * Follows REST best practices by providing a consistent structure across notification types.
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
@Schema(description = "Unified notification response containing all notification types with a type discriminator")
public class UnifiedNotificationDto {

    /**
     * Type discriminator for notification type.
     */
    @Schema(description = "Notification type discriminator", example = "SMS", allowableValues = {"SMS", "EMAIL", "CALLBACK"})
    @JsonProperty("notificationType")
    private String notificationType;

    // Common fields across all notification types

    @Schema(description = "Unique notification identifier")
    @JsonProperty("notificationId")
    private String notificationId;

    @Schema(description = "Event identifier linking to the triggering event")
    @JsonProperty("eventId")
    private String eventId;

    @Schema(description = "Correlation identifier for linking related notifications")
    @JsonProperty("correlationId")
    private String correlationId;

    @Schema(description = "Business identifier")
    @JsonProperty("businessId")
    private String businessId;

    @Schema(description = "Template identifier")
    @JsonProperty("templateId")
    private String templateId;

    @Schema(description = "Event type classification")
    @JsonProperty("eventType")
    private String eventType;

    @Schema(description = "Current status of the notification")
    @JsonProperty("status")
    private String status;

    @Schema(description = "Priority level")
    @JsonProperty("priority")
    private String priority;

    @Schema(description = "Number of delivery attempts")
    @JsonProperty("attemptCount")
    private Integer attemptCount;

    @Schema(description = "Maximum retry attempts allowed")
    @JsonProperty("maxAttempts")
    private Integer maxAttempts;

    @Schema(description = "Timestamp when notification was created")
    @JsonProperty("createdAt")
    private LocalDateTime createdAt;

    @Schema(description = "Timestamp when notification was last updated")
    @JsonProperty("updatedAt")
    private LocalDateTime updatedAt;

    @Schema(description = "Timestamp when processing started")
    @JsonProperty("processedAt")
    private LocalDateTime processedAt;

    @Schema(description = "Timestamp when notification was sent")
    @JsonProperty("sentAt")
    private LocalDateTime sentAt;

    @Schema(description = "Timestamp when notification was delivered/acknowledged")
    @JsonProperty("deliveredAt")
    private LocalDateTime deliveredAt;

    @Schema(description = "Timestamp of last delivery attempt")
    @JsonProperty("lastAttemptAt")
    private LocalDateTime lastAttemptAt;

    @Schema(description = "Scheduled timestamp for next retry")
    @JsonProperty("nextRetryAt")
    private LocalDateTime nextRetryAt;

    @Schema(description = "Error message from last failed attempt")
    @JsonProperty("lastErrorMessage")
    private String lastErrorMessage;

    // SMS-specific fields

    @Schema(description = "Mobile number (SMS only)")
    @JsonProperty("mobile")
    private String mobile;

    @Schema(description = "SMS message content (SMS only)")
    @JsonProperty("messageContent")
    private String messageContent;

    @Schema(description = "DLT Entity ID (SMS only)")
    @JsonProperty("dltEntityId")
    private String dltEntityId;

    @Schema(description = "DLT Template ID (SMS only)")
    @JsonProperty("dltTemplateId")
    private String dltTemplateId;

    @Schema(description = "Sender ID (SMS only)")
    @JsonProperty("from")
    private String from;

    @Schema(description = "Operator countries (SMS only)")
    @JsonProperty("oprCountries")
    private List<String> oprCountries;

    @Schema(description = "DigiGov API response (SMS only)")
    @JsonProperty("digiGovResponse")
    private Map<String, Object> digiGovResponse;

    // Email-specific fields

    @Schema(description = "Primary recipient email addresses (Email only)")
    @JsonProperty("to")
    private List<String> to;

    @Schema(description = "CC recipient email addresses (Email only)")
    @JsonProperty("cc")
    private List<String> cc;

    @Schema(description = "BCC recipient email addresses (Email only)")
    @JsonProperty("bcc")
    private List<String> bcc;

    @Schema(description = "Email subject (Email only)")
    @JsonProperty("subject")
    private String subject;

    @Schema(description = "Email content (Email only)")
    @JsonProperty("content")
    private String content;

    @Schema(description = "Content type (Email only)")
    @JsonProperty("contentType")
    private String contentType;

    @Schema(description = "Email type (Email only)")
    @JsonProperty("emailType")
    private String emailType;

    @Schema(description = "Reply-to address (Email only)")
    @JsonProperty("replyTo")
    private String replyTo;

    @Schema(description = "From name (Email only)")
    @JsonProperty("fromName")
    private String fromName;

    @Schema(description = "Email attachments (Email only)")
    @JsonProperty("attachments")
    private List<Object> attachments;

    @Schema(description = "Timestamp when email was opened (Email only)")
    @JsonProperty("openedAt")
    private LocalDateTime openedAt;

    // Callback-specific fields

    @Schema(description = "Recipient type (Callback only)")
    @JsonProperty("recipientType")
    private String recipientType;

    @Schema(description = "Recipient identifier (Callback only)")
    @JsonProperty("recipientId")
    private String recipientId;

    @Schema(description = "Webhook callback URL (Callback only)")
    @JsonProperty("callbackUrl")
    private String callbackUrl;

    @Schema(description = "JWT expiry timestamp (Callback only)")
    @JsonProperty("jwtExpiresAt")
    private LocalDateTime jwtExpiresAt;

    @Schema(description = "Custom HTTP headers (Callback only)")
    @JsonProperty("customHeaders")
    private Map<String, String> customHeaders;

    @Schema(description = "Request timeout in seconds (Callback only)")
    @JsonProperty("timeoutSeconds")
    private Integer timeoutSeconds;

    @Schema(description = "Expected response format (Callback only)")
    @JsonProperty("expectedResponseFormat")
    private String expectedResponseFormat;

    @Schema(description = "Timestamp when callback was acknowledged (Callback only)")
    @JsonProperty("acknowledgedAt")
    private LocalDateTime acknowledgedAt;

    @Schema(description = "Webhook response (Callback only)")
    @JsonProperty("webhookResponse")
    private Map<String, Object> webhookResponse;

    @Schema(description = "HTTP status code from webhook (Callback only)")
    @JsonProperty("httpStatusCode")
    private Integer httpStatusCode;

    @Schema(description = "Request duration in milliseconds (Callback only)")
    @JsonProperty("requestDurationMs")
    private Long requestDurationMs;

    @Schema(description = "Status history (Callback only)")
    @JsonProperty("statusHistory")
    private List<Object> statusHistory;
}
