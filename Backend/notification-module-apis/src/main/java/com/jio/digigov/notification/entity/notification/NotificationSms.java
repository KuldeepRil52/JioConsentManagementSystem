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
 * SMS Notification entity for tracking individual SMS deliveries.
 *
 * This entity represents a single SMS notification record in the asynchronous
 * processing pipeline. It contains all information necessary for SMS delivery
 * via DigiGov, including DLT compliance data, delivery tracking, and status
 * management for retry logic and audit trails.
 *
 * Multi-Tenant Architecture:
 * - Stored in tenant-specific MongoDB databases (tenant_db_{tenantId})
 * - No tenantId field required due to database-level isolation
 * - Indexed for optimal query performance on common access patterns
 *
 * Processing Lifecycle:
 * PENDING → PROCESSING → SENT → DELIVERED/FAILED
 *
 * Integration Points:
 * - Event trigger creates initial record
 * - Kafka producer publishes notification message
 * - SMS consumer processes delivery via DigiGov
 * - Status updates tracked for monitoring and retry
 *
 * @author Notification Service Team
 * @version 1.0
 * @since 2024-01-01
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "notification_sms")
@CompoundIndexes({
    @CompoundIndex(name = "notification_id_idx",
                  def = "{'notification_id': 1}",
                  unique = true),
    @CompoundIndex(name = "event_id_idx",
                  def = "{'event_id': 1}"),
    @CompoundIndex(name = "mobile_status_idx",
                  def = "{'mobile': 1, 'status': 1}"),
    @CompoundIndex(name = "status_created_idx",
                  def = "{'status': 1, 'created_at': -1}"),
    @CompoundIndex(name = "correlation_id_idx",
                  def = "{'correlation_id': 1}")
})
public class NotificationSms extends BaseEntity {

    /**
     * Unique identifier for this SMS notification.
     * Generated when notification record is created.
     * Used for Kafka message routing and status tracking.
     */
    @NotBlank(message = "Notification ID is required")
    @Indexed(unique = true)
    @Field("notification_id")
    private String notificationId;

    /**
     * Event identifier linking this notification to the triggering event.
     * Used for event-level status tracking and audit correlation.
     */
    @NotBlank(message = "Event ID is required")
    @Field("event_id")
    private String eventId;

    /**
     * Correlation identifier for linking related notifications.
     * Multiple notifications from the same event share this ID.
     */
    @NotBlank(message = "Correlation ID is required")
    @Field("correlation_id")
    private String correlationId;

    /**
     * Business identifier for configuration and authorization.
     * Used to retrieve DigiGov credentials and notification settings.
     */
    @NotBlank(message = "Business ID is required")
    @Field("business_id")
    private String businessId;

    /**
     * Template identifier for message content resolution.
     * References NotificationTemplate collection for SMS template.
     */
    @NotBlank(message = "Template ID is required")
    @Field("template_id")
    private String templateId;

    /**
     * Recipient mobile number with country code.
     * Format: Country code + mobile number (e.g., "919876543210")
     */
    @NotBlank(message = "Mobile number is required")
    @Field("mobile")
    private String mobile;

    /**
     * Resolved SMS message content after template processing.
     * Contains final message text with all placeholders substituted.
     */
    @Field("message_content")
    private String messageContent;

    /**
     * Template argument values used for placeholder substitution.
     * Stored for audit trail and potential retry scenarios.
     */
    @Field("template_args")
    private Map<String, Object> templateArgs;

    /**
     * DLT Entity ID for TRAI compliance.
     * Required for commercial SMS delivery in India.
     */
    @Field("dlt_entity_id")
    private String dltEntityId;

    /**
     * DLT Template ID for TRAI compliance.
     * Links to registered message template in DLT system.
     */
    @Field("dlt_template_id")
    private String dltTemplateId;

    /**
     * Sender ID or shortcode for SMS delivery.
     * Displayed as sender name in recipient's SMS inbox.
     */
    @Field("from")
    private String from;

    /**
     * Operator country codes for delivery routing.
     * Enables SMS delivery through specific operators.
     */
    @Field("opr_countries")
    private List<String> oprCountries;

    /**
     * Current processing status of the SMS notification.
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
    @Field("attempt_count")
    @Builder.Default
    private Integer attemptCount = 0;

    /**
     * Maximum number of retry attempts allowed.
     * After this limit, notification is moved to failed status.
     */
    @Field("max_attempts")
    @Builder.Default
    private Integer maxAttempts = 3;

    /**
     * Timestamp when SMS processing started.
     * Used for latency calculation and SLA monitoring.
     */
    @Field("processed_at")
    private LocalDateTime processedAt;

    /**
     * Timestamp when SMS was successfully sent to DigiGov.
     * Indicates successful API call, not final delivery.
     */
    @Field("sent_at")
    private LocalDateTime sentAt;

    /**
     * Timestamp when final delivery was confirmed.
     * Based on delivery receipt from telecom operator.
     */
    @Field("delivered_at")
    private LocalDateTime deliveredAt;

    /**
     * Timestamp of last processing attempt.
     * Used for retry scheduling and timeout detection.
     */
    @Field("last_attempt_at")
    private LocalDateTime lastAttemptAt;

    /**
     * Scheduled timestamp for next retry attempt.
     * Calculated based on exponential backoff policy.
     */
    @Field("next_retry_at")
    private LocalDateTime nextRetryAt;

    /**
     * Error message from last failed delivery attempt.
     * Provides debugging information for operational support.
     */
    @Field("last_error_message")
    private String lastErrorMessage;

    /**
     * DigiGov API response from SMS delivery request.
     * Stored for audit trail and delivery confirmation.
     */
    @Field("digi_gov_response")
    private Map<String, Object> digiGovResponse;

    /**
     * Delivery receipt information from telecom operator.
     * Contains final delivery status and timing data.
     */
    @Field("delivery_receipt")
    private Map<String, Object> deliveryReceipt;

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
    @Field("is_async")
    @Builder.Default
    private Boolean isAsync = true;

    /**
     * Kafka message ID for correlation with message processing.
     * Links notification record to specific Kafka message.
     */
    @Field("kafka_message_id")
    private String kafkaMessageId;

    /**
     * Processing node identifier for distributed processing tracking.
     * Helps identify which consumer instance processed the message.
     */
    @Field("processing_node_id")
    private String processingNodeId;

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
}