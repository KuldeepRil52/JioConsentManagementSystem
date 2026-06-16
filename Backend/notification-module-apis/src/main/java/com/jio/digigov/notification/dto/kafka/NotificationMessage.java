package com.jio.digigov.notification.dto.kafka;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Base Kafka message for all notification types in the DPDP Notification Module.
 *
 * This class serves as the primary message structure for Kafka-based asynchronous
 * notification processing. It contains all common fields required for message
 * routing, tracking, and processing across different notification channels.
 *
 * Supported Message Types:
 * - SMS: Direct SMS notifications to Data Principals
 * - EMAIL: Email notifications to Data Principals and stakeholders
 * - CALLBACK: Webhook callbacks to Data Fiduciaries and Data Processors
 *
 * Message Flow:
 * 1. Producer creates NotificationMessage with appropriate payload
 * 2. Message published to specific Kafka topic based on type
 * 3. Consumer processes message and updates delivery status
 * 4. Retry logic handles failures with exponential backoff
 * 5. Failed messages after max retries go to Dead Letter Queue
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class NotificationMessage {

    /**
     * Unique identifier for this specific message instance.
     * Generated when message is created and used for tracking across the entire lifecycle.
     */
    private String messageId;

    /**
     * Correlation identifier linking related messages in a notification event.
     * Multiple notifications from the same event (SMS + Email + Callback) share this ID.
     */
    private String correlationId;

    /**
     * Unique identifier for the specific notification record in the database.
     * Used to retrieve and update notification details during processing.
     */
    private String notificationId;

    /**
     * Unique identifier for the parent event that triggered this notification.
     * Links back to the original TriggerEventRequestDto.
     */
    private String eventId;

    /**
     * Tenant identifier for multi-tenant data isolation.
     * Determines which MongoDB database and configuration to use.
     */
    private String tenantId;

    /**
     * Business identifier for business-specific configuration lookup.
     * Used to retrieve event configurations and DigiGov credentials.
     */
    private String businessId;

    /**
     * Transaction identifier for tracking request flow across services.
     * Used for correlation and tracing in distributed processing.
     */
    private String transactionId;

    /**
     * Type of notification determining the processing channel and consumer.
     */
    private NotificationType type;

    /**
     * Type of recipient for this notification.
     * Determines which templates and processing logic to use.
     */
    private String recipientType;

    /**
     * Identifier of the specific recipient.
     * For SMS/Email: customer mobile/email, For Callbacks: DF/DP identifier
     */
    private String recipientId;

    /**
     * Template identifier for message content resolution.
     * Used to lookup appropriate template from the template repository.
     */
    private String templateId;

    /**
     * Processing priority for the notification.
     * Affects consumer processing order and retry behavior.
     */
    private Priority priority;

    /**
     * Number of processing attempts for this message.
     * Incremented on each retry, used for exponential backoff calculation.
     */
    private Integer retryCount;

    /**
     * Timestamp when the message was created.
     * Used for message aging, monitoring, and audit trails.
     */
    private LocalDateTime timestamp;

    /**
     * Type-specific payload containing delivery details.
     * Can be SmsPayload, EmailPayload, or CallbackPayload depending on type.
     */
    private Object payload;

    /**
     * Additional metadata for message processing and tracking.
     * Contains delivery status, error information, and processing details.
     */
    private MessageMetadata metadata;

    /**
     * Enumeration of supported notification types.
     * Determines which Kafka topic and consumer will process the message.
     */
    public enum NotificationType {
        /**
         * SMS notification for direct customer communication.
         * Processed by SmsNotificationConsumer.
         */
        SMS,

        /**
         * Email notification for customer and stakeholder communication.
         * Processed by EmailNotificationConsumer.
         */
        EMAIL,

        /**
         * Webhook callback for Data Fiduciary and Data Processor notifications.
         * Processed by CallbackNotificationConsumer.
         */
        CALLBACK
    }

    /**
     * Processing priority levels affecting consumer behavior.
     * Higher priority messages are processed first and have different retry policies.
     */
    public enum Priority {
        /**
         * High priority - immediate processing, faster retries.
         * Used for time-sensitive notifications like consent expiry.
         */
        HIGH,

        /**
         * Medium priority - standard processing.
         * Default priority for most notification types.
         */
        MEDIUM,

        /**
         * Low priority - background processing, longer retry intervals.
         * Used for informational notifications.
         */
        LOW
    }
}