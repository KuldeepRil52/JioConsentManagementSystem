package com.jio.digigov.notification.dto.response.notification;

import com.jio.digigov.notification.dto.response.common.PagedResponseDto;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Response DTO for consent deletion dashboard.
 *
 * Contains overview metrics and a paginated list of consent deletion requests
 * for CONSENT_EXPIRED and CONSENT_WITHDRAWN events.
 *
 * @author Notification Service Team
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Consent deletion dashboard response with overview metrics and paginated list")
public class ConsentDeletionDashboardResponseDto {

    /**
     * Overview metrics for consent deletion requests.
     */
    @Schema(description = "Overview metrics for consent deletion")
    private OverviewMetrics overview;

    /**
     * Paginated list of consent deletion requests.
     */
    @Schema(description = "Paginated list of consent deletion requests")
    private PagedResponseDto<ConsentDeletionItemDto> deletionRequests;

    /**
     * Overview metrics for consent deletion dashboard.
     *
     * Metrics are calculated based on unique consentIds:
     * - deletionRequests: Total unique consent IDs for CONSENT_EXPIRED/CONSENT_WITHDRAWN events
     * - completed: Consents where ALL recipients (DF + all DPs) have DELETED status
     * - deferred: Consents where ANY recipient has DEFERRED status
     * - inProgress: deletionRequests - completed - deferred
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Overview metrics for consent deletion")
    public static class OverviewMetrics {

        /**
         * Total count of unique consent deletion requests.
         */
        @Schema(description = "Total unique consent deletion requests", example = "100")
        private Long deletionRequests;

        /**
         * Count of consents where ALL recipients have DELETED status in statusHistory.
         */
        @Schema(description = "Consents with ALL recipients (DF + all DPs) having DELETED status", example = "60")
        private Long completed;

        /**
         * Count of consents where ANY recipient has DEFERRED status in statusHistory.
         */
        @Schema(description = "Consents where ANY recipient has DEFERRED status", example = "10")
        private Long deferred;

        /**
         * Count of in-progress deletions (deletionRequests - completed - deferred).
         */
        @Schema(description = "In-progress deletions (Deletion Requests - Completed - Deferred)", example = "30")
        private Long inProgress;

        /**
         * Count of completed consents where Data Principal was notified.
         * A consent is considered "user notified" when a DATA_DELETION_NOTIFICATION
         * event exists for the same businessId and consentId.
         */
        @Schema(description = "Count of completed consents where Data Principal was notified", example = "55")
        private Long userNotified;

        /**
         * Count of completed consents pending user notification.
         * Calculated as: completed - userNotified
         */
        @Schema(description = "Count of completed consents pending notification (completed - userNotified)", example = "5")
        private Long notificationPending;
    }

    /**
     * Individual consent deletion request item.
     *
     * Represents a single consent deletion request with its current status
     * across Data Fiduciary and Data Processors.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Individual consent deletion request item")
    public static class ConsentDeletionItemDto {

        /**
         * Consent ID from event_payload.consentId.
         */
        @Schema(description = "Consent ID from event_payload.consentId", example = "CONSENT_123")
        private String consentId;

        /**
         * Data principal identifier (customer_identifiers.value).
         */
        @Schema(description = "Data principal identifier (customer_identifiers.value)", example = "9876543210")
        private String dataPrincipal;

        /**
         * Event type that triggered the deletion (CONSENT_EXPIRED or CONSENT_WITHDRAWN).
         */
        @Schema(description = "Event type that triggered deletion",
                example = "CONSENT_EXPIRED",
                allowableValues = {"CONSENT_EXPIRED", "CONSENT_WITHDRAWN"})
        private String trigger;

        /**
         * Data Fiduciary deletion status.
         * Values: DELETED, DEFERRED, FAILED, PENDING, or NOT_APPLICABLE.
         */
        @Schema(description = "Data Fiduciary deletion status",
                example = "DELETED",
                allowableValues = {"DELETED", "DEFERRED", "FAILED", "PENDING", "NOT_APPLICABLE"})
        private String dfStatus;

        /**
         * Data Processor status in "X/Y Done" format.
         * X = count of DPs with DELETED status
         * Y = total count of DPs for this consent
         */
        @Schema(description = "Processor status in X/Y Done format", example = "3/5 Done")
        private String processors;

        /**
         * Overall status for this consent deletion request.
         * Priority: Done > Deferred > Failed > Partial > Pending
         * - Done: All recipients (DF + all DPs) have DELETED status
         * - Deferred: Any recipient has DEFERRED status
         * - Failed: DF has FAILED status
         * - Partial: At least one recipient has DELETED, but not all
         * - Pending: No recipient has DELETED status yet (deletion not started)
         */
        @Schema(description = "Overall status: Done, Partial, Pending, Deferred, or Failed",
                example = "Partial",
                allowableValues = {"Done", "Partial", "Pending", "Deferred", "Failed"})
        private String overall;

        /**
         * Overall completion status for this consent deletion request.
         * - Completed: DF has withdrawal_data AND all DPs have DELETED or DEFERRED status
         * - Inprogress: Any recipient has not yet responded
         */
        @Schema(description = "Overall completion status: Completed or Inprogress",
                example = "Inprogress",
                allowableValues = {"Completed", "Inprogress"})
        private String overallCompletion;

        /**
         * Event timestamp (latest event for this consent).
         */
        @Schema(description = "Event timestamp (latest event for this consent)")
        private LocalDateTime eventTimestamp;

        /**
         * Event ID for reference (latest event for this consent).
         */
        @Schema(description = "Event ID for the latest event")
        private String eventId;
    }
}
