package com.jio.digigov.notification.dto.kafka;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Metadata container for Kafka notification messages in the DPDP system.
 *
 * This class contains comprehensive tracking and status information for notification
 * messages throughout their processing lifecycle. It enables detailed monitoring,
 * debugging, and audit capabilities for the asynchronous notification system.
 *
 * Lifecycle Tracking:
 * - Message creation and source identification
 * - Processing attempts and retry management
 * - Error tracking and failure analysis
 * - Delivery status updates and confirmations
 * - Performance metrics and timing data
 *
 * Status Progression:
 * PENDING → PROCESSING → SENT → DELIVERED/FAILED
 *                    ↓
 *              ACKNOWLEDGED (for callbacks)
 *                    ↓
 *               RETRIEVED (via Pull API)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageMetadata {

    /**
     * Unique identifier for the notification being processed.
     * Links the message to its corresponding database record.
     */
    private String notificationId;

    /**
     * Source system or component that created this message.
     * Examples: "async-producer", "retry-scheduler", "manual-retry"
     */
    private String source;

    /**
     * Distributed tracing identifier for request correlation.
     * Links message processing across multiple microservices and systems.
     */
    private String traceId;

    /**
     * Timestamp when the message was initially created.
     * Used for aging analysis and performance metrics.
     */
    private LocalDateTime createdAt;

    /**
     * Timestamp when message processing started.
     * Used to calculate processing latency and identify bottlenecks.
     */
    private LocalDateTime processedAt;

    /**
     * Current attempt number for message processing.
     * Starts at 1, incremented on each retry attempt.
     */
    private Integer attemptNumber;

    /**
     * Last error message encountered during processing.
     * Provides detailed information for debugging and monitoring.
     */
    private String lastError;

    /**
     * Timestamp for the next scheduled retry attempt.
     * Calculated based on exponential backoff policy and attempt number.
     */
    private LocalDateTime nextRetryAt;

    /**
     * Current delivery status of the notification.
     * Updated throughout the message lifecycle for tracking and monitoring.
     */
    private DeliveryStatus deliveryStatus;

    /**
     * Additional custom properties for extensibility.
     * Allows storing context-specific metadata without schema changes.
     */
    private Map<String, String> additionalProperties;

    /**
     * Enumeration of notification delivery statuses.
     * Represents the complete lifecycle of a notification message.
     */
    public enum DeliveryStatus {
        /**
         * Message created and waiting for processing.
         * Initial status when message is published to Kafka.
         */
        PENDING,

        /**
         * Message currently being processed by a consumer.
         * Indicates active processing to prevent duplicate handling.
         */
        PROCESSING,

        /**
         * Message successfully sent to external service.
         * For SMS/Email: sent to DigiGov, For Callbacks: HTTP request sent.
         */
        SENT,

        /**
         * Final delivery confirmed by external service.
         * For SMS: delivery receipt received, For Email: accepted by mail server.
         */
        DELIVERED,

        /**
         * Message processing failed permanently.
         * Occurs after max retry attempts or on non-retryable errors.
         */
        FAILED,

        /**
         * Callback notification acknowledged by recipient.
         * Specific to webhook callbacks from Data Fiduciaries/Processors.
         */
        ACKNOWLEDGED,

        /**
         * Notification retrieved via Pull API.
         * Indicates successful pull-based notification delivery.
         */
        RETRIEVED
    }
}