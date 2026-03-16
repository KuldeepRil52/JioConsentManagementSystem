package com.jio.digigov.notification.entity.notification;

import com.jio.digigov.notification.entity.base.BaseEntity;
import com.jio.digigov.notification.enums.EventPriority;
import com.jio.digigov.notification.enums.NotificationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.mapping.Field;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Callback Notification entity for tracking webhook deliveries.
 *
 * This entity represents a single webhook callback notification record in the
 * asynchronous processing pipeline. It contains all information necessary for
 * secure webhook delivery to Data Fiduciaries and Data Processors, including
 * JWT authentication, HTTP configuration, and comprehensive delivery tracking.
 *
 * Multi-Tenant Architecture:
 * - Stored in tenant-specific MongoDB databases (tenant_db_{tenantId})
 * - No tenantId field required due to database-level isolation
 * - Indexed for optimal query performance on common access patterns
 *
 * Processing Lifecycle:
 * PENDING → PROCESSING → SENT → ACKNOWLEDGED/FAILED
 *
 * Security Features:
 * - JWT token authentication with configurable expiry
 * - Webhook signature verification for data integrity
 * - HTTPS-only delivery for secure communication
 * - Timeout configuration for reliable delivery
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "notification_callback")
@CompoundIndexes({
    @CompoundIndex(name = "notification_id_idx",
                  def = "{'notificationId': 1}",
                  unique = true),
    @CompoundIndex(name = "event_id_idx",
                  def = "{'eventId': 1}"),
    @CompoundIndex(name = "recipient_status_idx",
                  def = "{'recipientId': 1, 'status': 1}"),
    @CompoundIndex(name = "callback_url_status_idx",
                  def = "{'callbackUrl': 1, 'status': 1}"),
    @CompoundIndex(name = "status_created_idx",
                  def = "{'status': 1, 'createdAt': -1}"),
    @CompoundIndex(name = "correlation_id_idx",
                  def = "{'correlationId': 1}"),
    // Indexes for callback statistics queries
    @CompoundIndex(name = "stats_query_idx",
                  def = "{'eventType': 1, 'recipientType': 1, 'createdAt': -1}"),
    @CompoundIndex(name = "recipient_event_idx",
                  def = "{'recipientType': 1, 'recipientId': 1, 'eventType': 1}"),
    // Indexes for purge statistics queries
    @CompoundIndex(name = "purge_stats_idx",
                  def = "{'eventType': 1, 'acknowledgedAt': -1}"),
    @CompoundIndex(name = "purge_recipient_idx",
                  def = "{'recipientType': 1, 'eventType': 1, 'acknowledgedAt': -1}")
})
public class NotificationCallback extends BaseEntity {

    /**
     * Unique identifier for this callback notification.
     * Generated when notification record is created.
     * Used for Kafka message routing and status tracking.
     */
    @NotBlank(message = "Notification ID is required")
    @Indexed(unique = true)
    @Field("notificationId")
    private String notificationId;

    /**
     * Event identifier linking this notification to the triggering event.
     * Used for event-level status tracking and audit correlation.
     */
    @NotBlank(message = "Event ID is required")
    @Field("eventId")
    private String eventId;

    /**
     * Correlation identifier for linking related notifications.
     * Multiple notifications from the same event share this ID.
     */
    @NotBlank(message = "Correlation ID is required")
    @Field("correlationId")
    private String correlationId;

    /**
     * Business identifier for configuration and authorization.
     * Used to retrieve webhook credentials and notification settings.
     */
    @NotBlank(message = "Business ID is required")
    @Field("businessId")
    private String businessId;

    /**
     * Type of recipient for the callback notification.
     * Determines the payload structure and expected response format.
     * Values: "DATA_FIDUCIARY", "DATA_PROCESSOR", "SYSTEM_WEBHOOK"
     */
    @NotBlank(message = "Recipient type is required")
    @Field("recipientType")
    private String recipientType;

    /**
     * Unique identifier of the recipient organization.
     * Used for webhook URL resolution and audit tracking.
     * Maps to registered Data Fiduciary or Data Processor ID.
     */
    @NotBlank(message = "Recipient ID is required")
    @Field("recipientId")
    private String recipientId;

    /**
     * Target webhook URL for the callback delivery.
     * Must be a valid HTTPS URL for security compliance.
     * Should be registered and verified by the recipient organization.
     */
    @NotBlank(message = "Callback URL is required")
    @Field("callbackUrl")
    private String callbackUrl;

    /**
     * Event type classification for the callback.
     * Helps recipients route and process the webhook appropriately.
     * Examples: "CONSENT_GRANTED", "DATA_BREACH", "PROCESSING_REQUEST"
     */
    @NotBlank(message = "Event type is required")
    @Field("eventType")
    private String eventType;

    /**
     * Event-specific data payload for the callback.
     * Contains all relevant information about the privacy event.
     * Structure varies based on event type and recipient needs.
     */
    @Field("eventData")
    private Map<String, Object> eventData;

    /**
     * JWT authentication token for webhook security.
     * Contains signed claims about the event and sender identity.
     * Generated fresh for each callback delivery.
     */
    @Field("jwtToken")
    private String jwtToken;

    /**
     * JWT token expiry timestamp.
     * Used to validate token freshness and security.
     */
    @Field("jwtExpiresAt")
    private LocalDateTime jwtExpiresAt;

    /**
     * Custom HTTP headers to include in the webhook request.
     * Supports additional authentication, routing, and processing hints.
     */
    @Field("customHeaders")
    private Map<String, String> customHeaders;

    /**
     * Timeout in seconds for the webhook HTTP request.
     * Defines maximum wait time for recipient response.
     */
    @Field("timeoutSeconds")
    @Builder.Default
    private Integer timeoutSeconds = 30;

    /**
     * Expected response format from the webhook recipient.
     * Defines the structure and content of the expected acknowledgment.
     * Examples: "JSON", "XML", "PLAIN_TEXT"
     */
    @Field("expectedResponseFormat")
    @Builder.Default
    private String expectedResponseFormat = "JSON";

    /**
     * Current processing status of the callback notification.
     * Updated throughout the delivery lifecycle.
     */
    @NotNull
    @Field("status")
    @Builder.Default
    private String status = NotificationStatus.PENDING.name();

    /**
     * Processing priority level (HIGH, MEDIUM, LOW).
     * Affects queue position and retry behavior.
     */
    @Field("priority")
    @Builder.Default
    private String priority = EventPriority.MEDIUM.name();

    /**
     * Number of delivery attempts made.
     * Incremented on each retry for exponential backoff calculation.
     */
    @Field("attemptCount")
    @Builder.Default
    private Integer attemptCount = 0;

    /**
     * Maximum number of retry attempts allowed.
     * After this limit, notification is moved to failed status.
     */
    @Field("maxAttempts")
    @Builder.Default
    private Integer maxAttempts = 5;

    /**
     * Timestamp when callback processing started.
     * Used for latency calculation and SLA monitoring.
     */
    @Field("processedAt")
    private LocalDateTime processedAt;

    /**
     * Timestamp when webhook request was sent.
     * Indicates successful HTTP request initiation.
     */
    @Field("sentAt")
    private LocalDateTime sentAt;

    /**
     * Timestamp when webhook was acknowledged by recipient.
     * Based on successful HTTP response from recipient.
     */
    @Field("acknowledgedAt")
    private LocalDateTime acknowledgedAt;

    /**
     * Timestamp of last delivery attempt.
     * Used for retry scheduling and timeout detection.
     */
    @Field("lastAttemptAt")
    private LocalDateTime lastAttemptAt;

    /**
     * Scheduled timestamp for next retry attempt.
     * Calculated based on exponential backoff policy.
     */
    @Field("nextRetryAt")
    private LocalDateTime nextRetryAt;

    /**
     * Error message from last failed delivery attempt.
     * Provides debugging information for operational support.
     */
    @Field("lastErrorMessage")
    private String lastErrorMessage;

    /**
     * HTTP status code from last webhook delivery attempt.
     * Used for error classification and retry decisions.
     */
    @Field("lastHttpStatusCode")
    private Integer lastHttpStatusCode;

    /**
     * HTTP response body from webhook recipient.
     * Stored for audit trail and acknowledgment validation.
     */
    @Field("webhookResponse")
    private String webhookResponse;

    /**
     * HTTP response headers from webhook recipient.
     * Contains metadata about the response processing.
     */
    @Field("responseHeaders")
    private Map<String, String> responseHeaders;

    /**
     * Request duration in milliseconds for performance monitoring.
     * Used to track webhook endpoint performance and SLA compliance.
     */
    @Field("requestDurationMs")
    private Long requestDurationMs;

    /**
     * Flag indicating if webhook signature verification is enabled.
     * When true, includes HMAC signature for payload integrity.
     */
    @Field("signatureEnabled")
    @Builder.Default
    private Boolean signatureEnabled = false;

    /**
     * Signature algorithm used for webhook verification.
     * Examples: "HMAC-SHA256", "HMAC-SHA512"
     */
    @Field("signatureAlgorithm")
    private String signatureAlgorithm;

    /**
     * Generated signature for webhook payload verification.
     * Recipient can use this to verify payload integrity.
     */
    @Field("payloadSignature")
    private String payloadSignature;

    /**
     * Additional metadata for monitoring and debugging.
     * Extensible field for operational information.
     */
    @Field("metadata")
    private Map<String, Object> metadata;

    /**
     * Flag indicating if this notification was delivered via async processing.
     * Used for performance comparison and monitoring.
     */
    @Field("isAsync")
    @Builder.Default
    private Boolean isAsync = true;

    /**
     * Kafka message ID for correlation with message processing.
     * Links notification record to specific Kafka message.
     */
    @Field("kafkaMessageId")
    private String kafkaMessageId;

    /**
     * Processing node identifier for distributed processing tracking.
     * Helps identify which consumer instance processed the message.
     */
    @Field("processingNodeId")
    private String processingNodeId;

    /**
     * Flag indicating if HTTP redirects should be followed.
     * When true, webhook client follows 3xx redirect responses.
     */
    @Field("followRedirects")
    @Builder.Default
    private Boolean followRedirects = true;

    /**
     * Maximum number of HTTP redirects to follow.
     * Prevents infinite redirect loops while allowing endpoint relocations.
     */
    @Field("maxRedirects")
    @Builder.Default
    private Integer maxRedirects = 5;

    /**
     * User agent string for webhook requests.
     * Identifies the notification service to webhook recipients.
     */
    @Field("userAgent")
    @Builder.Default
    private String userAgent = "DPDP-Notification-Service/1.0";

    /**
     * Delivery confirmation details if acknowledgment was received.
     * Contains structured data about successful delivery.
     */
    @Field("deliveryConfirmation")
    private Map<String, Object> deliveryConfirmation;

    /**
     * Complete audit trail of all status changes for this notification.
     * Each entry records: timestamp, status, acknowledgementId, remark, source, updatedBy.
     * Used to track the lifecycle from initial creation through acknowledgement.
     *
     * Example entries:
     * - Initial processing: {timestamp, status: "PROCESSING", source: "SYSTEM", updatedBy: "CONSUMER"}
     * - Callback acknowledgement: {timestamp, status: "ACKNOWLEDGED", acknowledgementId: "ACK_123", source: "CALLBACK_RESPONSE", updatedBy: "DF_001"}
     * - Manual update: {timestamp, status: "ACKNOWLEDGED", remark: "Updated via API", source: "API_ENDPOINT", updatedBy: "DP_XYZ"}
     */
    @Field("statusHistory")
    @Builder.Default
    private List<StatusHistoryEntry> statusHistory = new ArrayList<>();

    /**
     * Transient field to store tenant ID for processing.
     * Not persisted due to database-per-tenant architecture.
     */
    @Transient
    private String tenantId;

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    /**
     * Gets the webhook URL for this callback notification.
     * @return webhook URL
     */
    public String getWebhookUrl() {
        return callbackUrl;
    }
}