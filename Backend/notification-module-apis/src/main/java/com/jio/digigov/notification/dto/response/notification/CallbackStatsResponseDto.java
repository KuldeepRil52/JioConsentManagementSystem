package com.jio.digigov.notification.dto.response.notification;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Response DTO for callback notification statistics.
 *
 * Provides comprehensive statistics about callback notifications including:
 * - Global statistics across all callbacks (filtered)
 * - Breakdown by Data Fiduciary (DF) with recipient names
 * - Breakdown by Data Processor (DP) with recipient names
 * - Event-type-specific statistics for each level
 *
 * @author Notification Service Team
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Callback notification statistics response")
public class CallbackStatsResponseDto {

    /**
     * Global statistics for all callbacks matching the filter criteria.
     */
    @Schema(description = "Global statistics across all callbacks")
    private GlobalStats stats;

    /**
     * Statistics breakdown by Data Fiduciary.
     * Key: Data Fiduciary ID (businessId)
     * Value: Statistics for that Data Fiduciary including name
     */
    @Schema(description = "Statistics breakdown by Data Fiduciary")
    @Builder.Default
    private Map<String, RecipientStats> dataFiduciary = new HashMap<>();

    /**
     * Statistics breakdown by Data Processor.
     * Key: Data Processor ID (dataProcessorId)
     * Value: Statistics for that Data Processor including name
     */
    @Schema(description = "Statistics breakdown by Data Processor")
    @Builder.Default
    private Map<String, RecipientStats> dataProcessor = new HashMap<>();

    /**
     * Global statistics for all callbacks.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Schema(description = "Global callback statistics")
    public static class GlobalStats {

        /**
         * Total number of callbacks.
         */
        @Schema(description = "Total number of callbacks", example = "1500")
        @Builder.Default
        private Long totalCallbacks = 0L;

        /**
         * Number of successful callbacks.
         * Success statuses: SENT, RETRIEVED, ACKNOWLEDGED, PROCESSING, PROCESSED, DELETED
         */
        @Schema(description = "Number of successful callbacks", example = "1350")
        @Builder.Default
        private Long successfulCallbacks = 0L;

        /**
         * Number of failed callbacks.
         * Failure statuses: FAILED, PENDING, RETRY_SCHEDULED
         */
        @Schema(description = "Number of failed callbacks", example = "150")
        @Builder.Default
        private Long failedCallbacks = 0L;

        /**
         * Success percentage (0-100, rounded to 2 decimal places).
         */
        @Schema(description = "Success percentage", example = "90.00")
        @Builder.Default
        private Double successPercentage = 0.0;

        /**
         * Failure percentage (0-100, rounded to 2 decimal places).
         */
        @Schema(description = "Failure percentage", example = "10.00")
        @Builder.Default
        private Double failurePercentage = 0.0;

        /**
         * Statistics breakdown by event type.
         */
        @Schema(description = "Statistics breakdown by event type")
        @Builder.Default
        private List<EventTypeStats> byEventType = new ArrayList<>();

        /**
         * Calculates success and failure percentages based on total callbacks.
         */
        public void calculatePercentages() {
            if (totalCallbacks == null || totalCallbacks == 0) {
                this.successPercentage = 0.0;
                this.failurePercentage = 0.0;
            } else {
                this.successPercentage = calculatePercentage(successfulCallbacks, totalCallbacks);
                this.failurePercentage = calculatePercentage(failedCallbacks, totalCallbacks);
            }
        }
    }

    /**
     * Statistics for a specific recipient (Data Fiduciary or Data Processor).
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Schema(description = "Recipient-specific callback statistics")
    public static class RecipientStats {

        /**
         * Name of the recipient (organization name for DF, processor name for DP).
         * Falls back to recipient ID if name cannot be resolved.
         */
        @Schema(description = "Recipient name (organization name or data processor name)",
                example = "Acme Corporation")
        private String name;

        /**
         * Total number of callbacks for this recipient.
         */
        @Schema(description = "Total number of callbacks for this recipient", example = "500")
        @Builder.Default
        private Long totalCallbacks = 0L;

        /**
         * Number of successful callbacks for this recipient.
         */
        @Schema(description = "Number of successful callbacks", example = "450")
        @Builder.Default
        private Long successfulCallbacks = 0L;

        /**
         * Number of failed callbacks for this recipient.
         */
        @Schema(description = "Number of failed callbacks", example = "50")
        @Builder.Default
        private Long failedCallbacks = 0L;

        /**
         * Success percentage for this recipient (0-100, rounded to 2 decimal places).
         */
        @Schema(description = "Success percentage", example = "90.00")
        @Builder.Default
        private Double successPercentage = 0.0;

        /**
         * Failure percentage for this recipient (0-100, rounded to 2 decimal places).
         */
        @Schema(description = "Failure percentage", example = "10.00")
        @Builder.Default
        private Double failurePercentage = 0.0;

        /**
         * Statistics breakdown by event type for this recipient.
         */
        @Schema(description = "Statistics breakdown by event type for this recipient")
        @Builder.Default
        private List<EventTypeStats> byEventType = new ArrayList<>();

        /**
         * Calculates success and failure percentages based on total callbacks.
         */
        public void calculatePercentages() {
            if (totalCallbacks == null || totalCallbacks == 0) {
                this.successPercentage = 0.0;
                this.failurePercentage = 0.0;
            } else {
                this.successPercentage = calculatePercentage(successfulCallbacks, totalCallbacks);
                this.failurePercentage = calculatePercentage(failedCallbacks, totalCallbacks);
            }
        }
    }

    /**
     * Statistics for a specific event type.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Schema(description = "Event-type-specific callback statistics")
    public static class EventTypeStats {

        /**
         * The event type (e.g., CONSENT_CREATED, DATA_BREACH).
         * Can be null if the callback record has no eventType in the database.
         */
        @Schema(description = "Event type (can be null if not set in database)",
                example = "CONSENT_CREATED",
                nullable = true)
        private String eventType;

        /**
         * Total number of callbacks for this event type.
         */
        @Schema(description = "Total number of callbacks for this event type", example = "200")
        @Builder.Default
        private Long totalCallbacks = 0L;

        /**
         * Number of successful callbacks for this event type.
         */
        @Schema(description = "Number of successful callbacks", example = "180")
        @Builder.Default
        private Long successfulCallbacks = 0L;

        /**
         * Number of failed callbacks for this event type.
         */
        @Schema(description = "Number of failed callbacks", example = "20")
        @Builder.Default
        private Long failedCallbacks = 0L;

        /**
         * Success percentage for this event type (0-100, rounded to 2 decimal places).
         */
        @Schema(description = "Success percentage", example = "90.00")
        @Builder.Default
        private Double successPercentage = 0.0;

        /**
         * Failure percentage for this event type (0-100, rounded to 2 decimal places).
         */
        @Schema(description = "Failure percentage", example = "10.00")
        @Builder.Default
        private Double failurePercentage = 0.0;

        /**
         * Calculates success and failure percentages based on total callbacks.
         */
        public void calculatePercentages() {
            if (totalCallbacks == null || totalCallbacks == 0) {
                this.successPercentage = 0.0;
                this.failurePercentage = 0.0;
            } else {
                this.successPercentage = calculatePercentage(successfulCallbacks, totalCallbacks);
                this.failurePercentage = calculatePercentage(failedCallbacks, totalCallbacks);
            }
        }
    }

    /**
     * Utility method to calculate percentage with 2 decimal places.
     *
     * @param part the part value
     * @param total the total value
     * @return percentage rounded to 2 decimal places
     */
    private static Double calculatePercentage(Long part, Long total) {
        if (part == null || total == null || total == 0) {
            return 0.0;
        }

        BigDecimal partDecimal = new BigDecimal(part);
        BigDecimal totalDecimal = new BigDecimal(total);
        BigDecimal percentage = partDecimal
                .multiply(new BigDecimal("100"))
                .divide(totalDecimal, 2, RoundingMode.HALF_UP);

        return percentage.doubleValue();
    }
}
