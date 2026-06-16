package com.jio.digigov.notification.dto.response.kafka;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.jio.digigov.notification.enums.NotificationStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Response DTO for asynchronous notification processing requests.
 *
 * This DTO is returned when events are processed asynchronously via Kafka,
 * providing immediate response with tracking information for monitoring
 * the progress of notification delivery.
 *
 * Key Features:
 * - Immediate response for async processing (sub-100ms)
 * - Comprehensive tracking information for all notification channels
 * - Status monitoring capabilities with correlation IDs
 * - Integration with notification tracking APIs
 * - Performance metrics and processing information
 *
 * Response Structure:
 * - Event processing confirmation
 * - Notification tracking IDs for each channel (SMS, Email, Callback)
 * - Kafka message IDs for debugging and monitoring
 * - Expected processing timelines
 * - Status monitoring endpoints
 *
 * Usage Context:
 * - Returned by /v1/events/trigger when async=true
 * - Used for tracking notification delivery progress
 * - Integration with monitoring and analytics systems
 * - Client-side status polling and updates
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = "Response for asynchronous notification processing")
public class AsyncNotificationResponseDto {

    /**
     * Unique identifier for the triggered event.
     * Used for tracking and correlation across the system.
     */
    @Schema(description = "Event ID", example = "evt_123e4567-e89b-12d3-a456-426614174000")
    private String eventId;

    /**
     * Transaction identifier for OTP verification and tracking.
     * This is the security token used for OTP verification API calls.
     */
    @Schema(description = "Transaction ID for OTP verification", example = "f81d4fae-7dec-11d0-a765-00a0c91e6bf6")
    private String transactionId;

    /**
     * Current processing status of the async request.
     * Indicates whether the event was successfully queued for processing.
     */
    @Schema(description = "Processing status", example = "QUEUED", allowableValues = {"QUEUED", "FAILED", "INVALID"})
    private String status;

    /**
     * Human-readable message describing the processing status.
     */
    @Schema(description = "Status message", example = "Event queued for asynchronous processing")
    private String message;

    /**
     * Timestamp when the event was queued for processing.
     */
    @Schema(description = "Queued timestamp")
    private LocalDateTime queuedAt;

    /**
     * Event metadata containing basic information about the processed event.
     * Contains information about event type, customer identifiers, and data processor count.
     */
    @Schema(description = "Event metadata")
    private Map<String, Object> eventMetadata;

    /**
     * Nested class for performance metrics.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Performance metrics")
    public static class PerformanceMetrics {

        /**
         * Time taken to process the request (in milliseconds).
         */
        @Schema(description = "Processing time in milliseconds", example = "45")
        private Long processingTimeMs;

        /**
         * Time taken to validate the event configuration.
         */
        @Schema(description = "Validation time in milliseconds", example = "12")
        private Long validationTimeMs;

        /**
         * Time taken to create notification records.
         */
        @Schema(description = "Database operation time in milliseconds", example = "18")
        private Long databaseTimeMs;

        /**
         * Time taken to publish messages to Kafka.
         */
        @Schema(description = "Kafka publishing time in milliseconds", example = "15")
        private Long kafkaTimeMs;
    }

    /**
     * Nested class for monitoring endpoint information.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Monitoring endpoints")
    public static class MonitoringEndpoints {

        /**
         * URL to check the status of the event processing.
         */
        @Schema(description = "Event status URL", example = "/v1/events/evt_123/status")
        private String eventStatusUrl;

        /**
         * URL to get detailed notification delivery information.
         */
        @Schema(description = "Notification details URL", example = "/v1/notifications/status")
        private String notificationDetailsUrl;

        /**
         * URL for real-time delivery status updates.
         */
        @Schema(description = "Real-time status URL", example = "/v1/events/evt_123/stream")
        private String realTimeStatusUrl;
    }

    /**
     * Creates a failed async response with error information.
     */
    public static AsyncNotificationResponseDto failure(String eventId,
                                                   String transactionId, String errorMessage) {
        return AsyncNotificationResponseDto.builder()
                .eventId(eventId)
                .transactionId(transactionId)
                .status(NotificationStatus.FAILED.name())
                .message(errorMessage)
                .queuedAt(LocalDateTime.now())
                .build();
    }
}