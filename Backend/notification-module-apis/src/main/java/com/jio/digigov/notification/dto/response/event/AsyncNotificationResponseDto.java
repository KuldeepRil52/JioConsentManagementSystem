package com.jio.digigov.notification.dto.response.event;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Response DTO for asynchronous notification processing in the DPDP system.
 *
 * This class represents the immediate response returned to clients when they
 * submit events for asynchronous processing via Kafka. It provides acknowledgment
 * of event acceptance and tracking information for monitoring notification
 * delivery progress.
 *
 * Response Characteristics:
 * - Immediate acknowledgment without waiting for delivery completion
 * - Comprehensive tracking identifiers for event correlation
 * - Estimated delivery times for client planning
 * - Status information for monitoring and debugging
 *
 * Client Usage:
 * - Use eventId to track overall event processing status
 * - Use correlationId to link related notifications
 * - Use notificationIds to monitor individual delivery status
 * - Check estimatedDeliveryTime for delivery planning
 *
 * Async Processing Benefits:
 * - Faster API response times (< 100ms vs 5-10 seconds)
 * - Improved system scalability and resource utilization
 * - Better error handling and retry mechanisms
 * - Enhanced monitoring and observability
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Response for asynchronous notification processing")
public class AsyncNotificationResponseDto {

    /**
     * Unique identifier for the triggered event.
     * Links back to the original TriggerEventRequestDto for comprehensive tracking.
     * Used for event status queries and audit trail correlation.
     */
    @Schema(description = "Unique event identifier", example = "EVT_20240315_12345")
    private String eventId;

    /**
     * Transaction identifier for the event (acts as OTP verification token for INIT_OTP events).
     * Generated from X-Transaction-ID header or auto-generated UUID.
     * Used as the security token for OTP verification API.
     */
    @Schema(description = "Transaction identifier", example = "f81d4fae-7dec-11d0-a765-00a0c91e6bf6")
    private String transactionId;

    /**
     * Correlation identifier linking all related notifications.
     * Multiple notifications from the same event share this identifier.
     * Enables tracking of complete notification delivery across channels.
     */
    @Schema(description = "Correlation identifier for related notifications", example = "COR_20240315_67890")
    private String correlationId;

    /**
     * List of individual notification identifiers created for this event.
     * Each notification (SMS, Email, Callback) gets a unique identifier.
     * Clients can use these IDs to track individual delivery status.
     */
    @Schema(description = "List of notification identifiers created")
    private List<String> notificationIds;

    /**
     * Current processing status of the event.
     * Indicates whether the event was accepted for async processing.
     * Values: "ACCEPTED", "REJECTED", "PROCESSING", "FAILED"
     */
    @Schema(description = "Event processing status", example = "ACCEPTED",
            allowableValues = {"ACCEPTED", "REJECTED", "PROCESSING", "FAILED"})
    private String status;

    /**
     * Timestamp when the async processing response was generated.
     * Indicates when the event was accepted into the async processing pipeline.
     * Used for performance monitoring and SLA tracking.
     */
    @Schema(description = "Response generation timestamp", example = "2024-03-15T10:30:45.123Z")
    private LocalDateTime timestamp;

    /**
     * Estimated time when all notifications will be delivered.
     * Based on current system load and historical processing times.
     * Provides clients with delivery expectations for planning.
     */
    @Schema(description = "Estimated delivery completion time", example = "2024-03-15T10:35:45.123Z")
    private LocalDateTime estimatedDeliveryTime;

    /**
     * Total number of notifications scheduled for delivery.
     * Includes SMS, Email, and Callback notifications based on configuration.
     * Helps clients understand the scope of the notification event.
     */
    @Schema(description = "Total number of notifications scheduled", example = "3")
    private Integer totalNotifications;

    /**
     * Breakdown of notifications by type for transparency.
     * Shows how many SMS, Email, and Callback notifications were created.
     */
    @Schema(description = "Notification count breakdown by type")
    private NotificationBreakdown notificationBreakdown;

    /**
     * Additional processing metadata for monitoring and debugging.
     * Contains information about the async processing pipeline.
     */
    @Schema(description = "Processing metadata")
    private ProcessingMetadata processingMetadata;

    /**
     * Nested class for notification type breakdown.
     * Provides transparency into how many notifications of each type were created.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Breakdown of notification counts by type")
    public static class NotificationBreakdown {

        /**
         * Number of SMS notifications created.
         * Based on Data Principal mobile number and SMS channel enablement.
         */
        @Schema(description = "Number of SMS notifications", example = "1")
        private Integer smsCount;

        /**
         * Number of Email notifications created.
         * Based on recipient configuration and Email channel enablement.
         */
        @Schema(description = "Number of Email notifications", example = "1")
        private Integer emailCount;

        /**
         * Number of Callback notifications created.
         * Based on Data Fiduciary and Data Processor webhook configurations.
         */
        @Schema(description = "Number of Callback notifications", example = "1")
        private Integer callbackCount;
    }

    /**
     * Nested class for processing metadata.
     * Contains technical information about the async processing pipeline.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Async processing metadata")
    public static class ProcessingMetadata {

        /**
         * Kafka topic assignments for the notifications.
         * Shows which topics the notifications were published to.
         */
        @Schema(description = "Kafka topics used for processing")
        private List<String> kafkaTopics;

        /**
         * Processing priority assigned to the notifications.
         * Affects queue position and retry behavior.
         */
        @Schema(description = "Processing priority", example = "HIGH",
                allowableValues = {"HIGH", "MEDIUM", "LOW"})
        private String priority;

        /**
         * Estimated queue position for processing.
         * Provides insight into current system load and processing delays.
         */
        @Schema(description = "Estimated queue position", example = "15")
        private Integer queuePosition;

        /**
         * Configuration version used for processing.
         * Enables tracking of configuration changes and rollbacks.
         */
        @Schema(description = "Configuration version", example = "v1.2.3")
        private String configVersion;
    }
}