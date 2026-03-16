package com.jio.digigov.notification.dto.response.notification;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Response DTO for callback purge statistics.
 *
 * Contains comprehensive statistics about purge operations for CONSENT_EXPIRED and
 * CONSENT_WITHDRAWN events, categorizing callbacks as purged, pending, or overdue
 * based on SLA compliance.
 *
 * Purge Categories:
 * - Purged: Callbacks that transitioned from ACKNOWLEDGED to DELETED in statusHistory
 * - Pending: Callbacks that are ACKNOWLEDGED but not yet DELETED (within SLA)
 * - Overdue: Callbacks that are ACKNOWLEDGED but not DELETED and exceeded SLA time
 *
 * @author Notification Service Team
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Callback purge statistics response with breakdowns by Data Fiduciary and Data Processor")
public class CallbackPurgeStatsResponseDto {

    /**
     * Global statistics across all callbacks matching the filter.
     */
    @Schema(description = "Global purge statistics across all callbacks")
    private GlobalStats stats;

    /**
     * Statistics breakdown by Data Fiduciary.
     * Key: recipientId (businessId)
     * Value: RecipientStats with organization details and metrics
     */
    @Schema(description = "Statistics breakdown by Data Fiduciary (keyed by business ID)")
    private Map<String, RecipientStats> dataFiduciary;

    /**
     * Statistics breakdown by Data Processor.
     * Key: recipientId (dataProcessorId)
     * Value: RecipientStats with processor details and metrics
     */
    @Schema(description = "Statistics breakdown by Data Processor (keyed by data processor ID)")
    private Map<String, RecipientStats> dataProcessor;

    /**
     * Global statistics for all callbacks.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Global purge statistics")
    public static class GlobalStats {

        /**
         * Total number of callbacks (CONSENT_EXPIRED or CONSENT_WITHDRAWN events).
         */
        @Schema(description = "Total number of consent-related callbacks", example = "1000")
        private Long totalEvents;

        /**
         * Number of callbacks that have been purged (ACKNOWLEDGED then DELETED).
         */
        @Schema(description = "Number of callbacks that have been purged", example = "600")
        private Long purgedEvents;

        /**
         * Number of callbacks pending purge (ACKNOWLEDGED but not DELETED, within SLA).
         */
        @Schema(description = "Number of callbacks pending purge (within SLA)", example = "300")
        private Long pendingEvents;

        /**
         * Number of callbacks overdue for purge (ACKNOWLEDGED but not DELETED, exceeded SLA).
         */
        @Schema(description = "Number of callbacks overdue for purge (exceeded SLA)", example = "100")
        private Long overdueEvents;

        /**
         * Purge percentage (purged / total * 100).
         */
        @Schema(description = "Percentage of callbacks that have been purged", example = "60.0")
        private Double purgePercentage;

        /**
         * Pending percentage (pending / total * 100).
         */
        @Schema(description = "Percentage of callbacks pending purge", example = "30.0")
        private Double pendingPercentage;

        /**
         * Overdue percentage (overdue / total * 100).
         */
        @Schema(description = "Percentage of callbacks overdue for purge", example = "10.0")
        private Double overduePercentage;

        /**
         * Statistics breakdown by event type.
         */
        @Schema(description = "Statistics breakdown by event type (CONSENT_EXPIRED, CONSENT_WITHDRAWN)")
        private List<EventTypeStats> byEventType;
    }

    /**
     * Statistics for a specific recipient (Data Fiduciary or Data Processor).
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Statistics for a specific recipient")
    public static class RecipientStats {

        /**
         * Recipient name from business_applications or data_processors collection.
         * Falls back to recipientId if name not found.
         */
        @Schema(description = "Recipient name (organization name or data processor name)",
                example = "ABC Corporation")
        private String name;

        /**
         * All available fields from business_applications (for DF) or data_processors (for DP).
         * Contains complete recipient metadata.
         */
        @Schema(description = "Complete recipient data from business_applications or data_processors collection")
        private List<Object> dataItems;

        /**
         * Total number of callbacks for this recipient.
         */
        @Schema(description = "Total number of callbacks for this recipient", example = "250")
        private Long totalEvents;

        /**
         * Number of callbacks purged for this recipient.
         */
        @Schema(description = "Number of callbacks purged for this recipient", example = "150")
        private Long purgedEvents;

        /**
         * Number of callbacks pending purge for this recipient.
         */
        @Schema(description = "Number of callbacks pending purge for this recipient", example = "75")
        private Long pendingEvents;

        /**
         * Number of callbacks overdue for purge for this recipient.
         */
        @Schema(description = "Number of callbacks overdue for purge for this recipient", example = "25")
        private Long overdueEvents;

        /**
         * Purge percentage for this recipient.
         */
        @Schema(description = "Percentage of callbacks purged for this recipient", example = "60.0")
        private Double purgePercentage;

        /**
         * Pending percentage for this recipient.
         */
        @Schema(description = "Percentage of callbacks pending for this recipient", example = "30.0")
        private Double pendingPercentage;

        /**
         * Overdue percentage for this recipient.
         */
        @Schema(description = "Percentage of callbacks overdue for this recipient", example = "10.0")
        private Double overduePercentage;

        /**
         * Statistics breakdown by event type for this recipient.
         */
        @Schema(description = "Statistics breakdown by event type for this recipient")
        private List<EventTypeStats> byEventType;
    }

    /**
     * Statistics for a specific event type.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Statistics for a specific event type")
    public static class EventTypeStats {

        /**
         * Event type (CONSENT_EXPIRED or CONSENT_WITHDRAWN).
         * Can be null if not set in database.
         */
        @Schema(description = "Event type (CONSENT_EXPIRED, CONSENT_WITHDRAWN, or null)",
                example = "CONSENT_EXPIRED",
                nullable = true)
        private String eventType;

        /**
         * Total number of callbacks for this event type.
         */
        @Schema(description = "Total number of callbacks for this event type", example = "500")
        private Long totalEvents;

        /**
         * Number of callbacks purged for this event type.
         */
        @Schema(description = "Number of callbacks purged for this event type", example = "300")
        private Long purgedEvents;

        /**
         * Number of callbacks pending purge for this event type.
         */
        @Schema(description = "Number of callbacks pending purge for this event type", example = "150")
        private Long pendingEvents;

        /**
         * Number of callbacks overdue for purge for this event type.
         */
        @Schema(description = "Number of callbacks overdue for this event type", example = "50")
        private Long overdueEvents;

        /**
         * Purge percentage for this event type.
         */
        @Schema(description = "Percentage of callbacks purged for this event type", example = "60.0")
        private Double purgePercentage;

        /**
         * Pending percentage for this event type.
         */
        @Schema(description = "Percentage of callbacks pending for this event type", example = "30.0")
        private Double pendingPercentage;

        /**
         * Overdue percentage for this event type.
         */
        @Schema(description = "Percentage of callbacks overdue for this event type", example = "10.0")
        private Double overduePercentage;
    }
}
