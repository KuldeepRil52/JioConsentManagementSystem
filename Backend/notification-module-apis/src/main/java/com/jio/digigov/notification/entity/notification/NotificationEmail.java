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
import java.util.List;
import java.util.Map;

/**
 * Email Notification entity for tracking individual email deliveries.
 *
 * This entity represents a single email notification record in the asynchronous
 * processing pipeline. It contains all information necessary for email delivery
 * via DigiGov, including recipient lists, template rendering, attachment handling,
 * and comprehensive delivery tracking for audit and monitoring.
 *
 * Multi-Tenant Architecture:
 * - Stored in tenant-specific MongoDB databases (tenant_db_{tenantId})
 * - No tenantId field required due to database-level isolation
 * - Indexed for optimal query performance on common access patterns
 *
 * Processing Lifecycle:
 * PENDING → PROCESSING → SENT → DELIVERED/FAILED
 *
 * Features Supported:
 * - Multi-recipient delivery (to, cc, bcc)
 * - HTML and text template rendering
 * - File attachments with size validation
 * - Read and delivery receipt tracking
 * - Priority-based processing
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "notification_email")
@CompoundIndexes({
    @CompoundIndex(name = "notification_id_idx",
                  def = "{'notificationId': 1}",
                  unique = true),
    @CompoundIndex(name = "event_id_idx",
                  def = "{'eventId': 1}"),
    @CompoundIndex(name = "recipients_status_idx",
                  def = "{'to': 1, 'status': 1}"),
    @CompoundIndex(name = "status_created_idx",
                  def = "{'status': 1, 'createdAt': -1}"),
    @CompoundIndex(name = "correlation_id_idx",
                  def = "{'correlationId': 1}")
})
public class NotificationEmail extends BaseEntity {

    /**
     * Unique identifier for this email notification.
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
     * Used to retrieve DigiGov credentials and notification settings.
     */
    @NotBlank(message = "Business ID is required")
    @Field("businessId")
    private String businessId;

    /**
     * Template identifier for email content resolution.
     * References NotificationTemplate collection for email template.
     */
    @NotBlank(message = "Template ID is required")
    @Field("templateId")
    private String templateId;

    /**
     * Primary recipient email addresses.
     * These recipients are directly addressed in the email header.
     */
    @NotNull(message = "To recipients are required")
    @Field("to")
    private List<String> to;

    /**
     * Carbon copy recipient email addresses.
     * These recipients receive a copy and are visible to all recipients.
     */
    @Field("cc")
    private List<String> cc;

    /**
     * Blind carbon copy recipient email addresses.
     * These recipients receive a copy but are hidden from other recipients.
     */
    @Field("bcc")
    private List<String> bcc;

    /**
     * Email subject line after template resolution.
     * Contains the final subject with all placeholders substituted.
     */
    @NotBlank(message = "Email subject is required")
    @Field("subject")
    private String subject;

    /**
     * Email body content after template resolution.
     * Contains the final message content (HTML or text format).
     */
    @NotBlank(message = "Email content is required")
    @Field("content")
    private String content;

    /**
     * Email content type (text/plain or text/html).
     * Determines how email clients should render the content.
     */
    @Field("contentType")
    @Builder.Default
    private String contentType = "text/html";

    /**
     * Template argument values used for placeholder substitution.
     * Stored for audit trail and potential retry scenarios.
     */
    @Field("templateArgs")
    private Map<String, Object> templateArgs;

    /**
     * Email type classification for tracking and analytics.
     * Examples: "CONSENT_NOTIFICATION", "BREACH_ALERT", "COMPLIANCE_REPORT"
     */
    @Field("emailType")
    private String emailType;

    /**
     * Reply-to email address for recipient responses.
     * If specified, replies are directed to this address.
     */
    @Field("replyTo")
    private String replyTo;

    /**
     * Sender email address for the notification.
     * Usually a no-reply address from the organization.
     */
    @Field("from")
    private String from;

    /**
     * Sender display name for the notification.
     * Friendly name shown in recipient email clients.
     */
    @Field("fromName")
    private String fromName;

    /**
     * File attachments included with the email.
     * Contains metadata about attached files.
     */
    @Field("attachments")
    private List<EmailAttachment> attachments;

    /**
     * Current processing status of the email notification.
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
     * Timestamp when email processing started.
     * Used for latency calculation and SLA monitoring.
     */
    @Field("processedAt")
    private LocalDateTime processedAt;

    /**
     * Timestamp when email was successfully sent to mail server.
     * Indicates successful SMTP submission, not final delivery.
     */
    @Field("sentAt")
    private LocalDateTime sentAt;

    /**
     * Timestamp when email delivery was confirmed.
     * Based on delivery receipt from mail server.
     */
    @Field("deliveredAt")
    private LocalDateTime deliveredAt;

    /**
     * Timestamp when email was opened by recipient.
     * Available if read receipt was requested and supported.
     */
    @Field("openedAt")
    private LocalDateTime openedAt;

    /**
     * Timestamp of last processing attempt.
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
     * DigiGov API response from email delivery request.
     * Stored for audit trail and delivery confirmation.
     */
    @Field("digiGovResponse")
    private Map<String, Object> digiGovResponse;

    /**
     * Delivery receipt information from mail server.
     * Contains delivery status and timing data.
     */
    @Field("deliveryReceipt")
    private Map<String, Object> deliveryReceipt;

    /**
     * Read receipt information if requested.
     * Contains open tracking data from email client.
     */
    @Field("readReceipt")
    private Map<String, Object> readReceipt;

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
     * Total size of all attachments in bytes.
     * Used for size validation and storage monitoring.
     */
    @Field("totalAttachmentSize")
    private Long totalAttachmentSize;

    /**
     * Flag indicating if read receipt was requested.
     * Used to track open rates and engagement metrics.
     */
    @Field("readReceiptRequested")
    @Builder.Default
    private Boolean readReceiptRequested = false;

    /**
     * Flag indicating if delivery receipt was requested.
     * Used for delivery confirmation and audit requirements.
     */
    @Field("deliveryReceiptRequested")
    @Builder.Default
    private Boolean deliveryReceiptRequested = false;

    /**
     * Email attachment metadata.
     * Contains information about files attached to the email.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EmailAttachment {

        /**
         * Display name for the attachment file.
         * Shown to recipients in their email client.
         */
        private String fileName;

        /**
         * MIME content type of the attachment.
         * Used by email clients for proper file handling.
         */
        private String contentType;

        /**
         * Size of the attachment file in bytes.
         * Used for validation and storage tracking.
         */
        private Long fileSize;

        /**
         * Content disposition (attachment or inline).
         * Determines how email clients handle the file.
         */
        @Builder.Default
        private String disposition = "attachment";

        /**
         * Content ID for inline attachments.
         * Used to reference embedded images in email content.
         */
        private String contentId;

        /**
         * File storage path or identifier.
         * Reference to where the actual file content is stored.
         */
        private String storageReference;
    }

    /**
     * Transient field to store tenant ID for processing.
     * Used during Kafka processing to determine the tenant context.
     * Not persisted to database due to database-per-tenant architecture.
     */
    @Transient
    private String tenantId;

    /**
     * Gets the tenant ID for this notification.
     * In database-per-tenant architecture, this is set during processing.
     * @return tenant identifier
     */
    public String getTenantId() {
        return tenantId;
    }

    /**
     * Sets the tenant ID for this notification.
     * Used during processing to maintain tenant context.
     * @param tenantId tenant identifier
     */
    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public String getToEmail() {
        return to != null && !to.isEmpty() ? to.get(0) : null;
    }
}