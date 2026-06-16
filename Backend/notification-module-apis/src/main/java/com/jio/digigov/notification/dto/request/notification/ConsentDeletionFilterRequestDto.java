package com.jio.digigov.notification.dto.request.notification;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

/**
 * DTO for filtering consent deletion dashboard queries.
 *
 * Supports filtering consent deletion requests for CONSENT_EXPIRED and CONSENT_WITHDRAWN events
 * by overall status, processor ID, consent ID, data principal, and date range.
 *
 * @author Notification Service Team
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Filter parameters for consent deletion dashboard")
public class ConsentDeletionFilterRequestDto {

    /**
     * Valid event types for consent deletion.
     */
    private static final List<String> VALID_EVENT_TYPES = Arrays.asList(
            "CONSENT_EXPIRED", "CONSENT_WITHDRAWN"
    );

    /**
     * Valid overall statuses for consent deletion.
     */
    private static final List<String> VALID_OVERALL_STATUSES = Arrays.asList(
            "DONE", "PARTIAL", "PENDING", "DEFERRED", "FAILED"
    );

    /**
     * Valid overall completion statuses for consent deletion.
     */
    private static final List<String> VALID_OVERALL_COMPLETION_STATUSES = Arrays.asList(
            "COMPLETED", "INPROGRESS"
    );

    /**
     * Filter by specific event type (CONSENT_EXPIRED or CONSENT_WITHDRAWN).
     * If not provided, results will include both event types.
     */
    @Schema(description = "Filter by event type (Trigger)",
            example = "CONSENT_EXPIRED",
            allowableValues = {"CONSENT_EXPIRED", "CONSENT_WITHDRAWN"})
    private String eventType;

    /**
     * Filter by overall status (DONE, PARTIAL, PENDING, DEFERRED, or FAILED).
     * If not provided, results will include all statuses.
     */
    @Schema(description = "Filter by overall status",
            example = "DONE",
            allowableValues = {"DONE", "PARTIAL", "PENDING", "DEFERRED", "FAILED"})
    private String overallStatus;

    /**
     * Filter by overall completion status (COMPLETED or INPROGRESS).
     * COMPLETED: DF has withdrawal_data AND all DPs have DELETED or DEFERRED status.
     * INPROGRESS: Any recipient has not yet responded.
     */
    @Schema(description = "Filter by overall completion status",
            example = "COMPLETED",
            allowableValues = {"COMPLETED", "INPROGRESS"})
    private String overallCompletion;

    /**
     * Filter by specific Data Processor recipient ID.
     * Filters consents that have callbacks for the specified processor.
     */
    @Schema(description = "Filter by specific Data Processor recipient ID",
            example = "DP_12345")
    private String processorId;

    /**
     * Filter by specific consent ID.
     */
    @Schema(description = "Filter by specific consent ID",
            example = "CONSENT_123")
    private String consentId;

    /**
     * Filter by data principal (customer identifier value).
     */
    @Schema(description = "Filter by data principal (customer identifier value)",
            example = "9876543210")
    private String dataPrincipal;

    /**
     * Start date for filtering (inclusive).
     * Filters based on the event creation timestamp.
     */
    @Schema(description = "Start date for filtering (inclusive)",
            example = "2025-01-01T00:00:00")
    private LocalDateTime fromDate;

    /**
     * End date for filtering (exclusive).
     * Filters based on the event creation timestamp.
     */
    @Schema(description = "End date for filtering (exclusive)",
            example = "2025-12-31T23:59:59")
    private LocalDateTime toDate;

    /**
     * Page number (0-indexed).
     */
    @Schema(description = "Page number (0-indexed)", example = "0")
    @Builder.Default
    private Integer page = 0;

    /**
     * Page size (max 100).
     */
    @Schema(description = "Page size (max 100)", example = "20")
    @Builder.Default
    private Integer size = 20;

    /**
     * Validates the filter request parameters.
     * Ensures data integrity and business rule compliance.
     *
     * @throws IllegalArgumentException if validation fails
     */
    public void validate() {
        // Validate date range
        if (fromDate != null && toDate != null && fromDate.isAfter(toDate)) {
            throw new IllegalArgumentException("fromDate cannot be after toDate");
        }

        // Validate event type - must be CONSENT_EXPIRED or CONSENT_WITHDRAWN
        if (eventType != null && !eventType.trim().isEmpty()) {
            String upperEventType = eventType.trim().toUpperCase();
            if (!VALID_EVENT_TYPES.contains(upperEventType)) {
                throw new IllegalArgumentException(
                    "Invalid event type: " + eventType + ". Must be CONSENT_EXPIRED or CONSENT_WITHDRAWN."
                );
            }
        }

        // Validate overall status if provided
        if (overallStatus != null && !overallStatus.trim().isEmpty()) {
            String upperStatus = overallStatus.trim().toUpperCase();
            if (!VALID_OVERALL_STATUSES.contains(upperStatus)) {
                throw new IllegalArgumentException(
                    "Invalid overall status: " + overallStatus + ". Must be DONE, PARTIAL, PENDING, DEFERRED, or FAILED."
                );
            }
        }

        // Validate overall completion if provided
        if (overallCompletion != null && !overallCompletion.trim().isEmpty()) {
            String upperCompletion = overallCompletion.trim().toUpperCase();
            if (!VALID_OVERALL_COMPLETION_STATUSES.contains(upperCompletion)) {
                throw new IllegalArgumentException(
                    "Invalid overall completion: " + overallCompletion + ". Must be COMPLETED or INPROGRESS."
                );
            }
        }

        // Validate processorId if provided
        if (processorId != null && processorId.trim().isEmpty()) {
            throw new IllegalArgumentException("processorId cannot be empty if provided");
        }

        // Validate consentId if provided
        if (consentId != null && consentId.trim().isEmpty()) {
            throw new IllegalArgumentException("consentId cannot be empty if provided");
        }

        // Validate dataPrincipal if provided
        if (dataPrincipal != null && dataPrincipal.trim().isEmpty()) {
            throw new IllegalArgumentException("dataPrincipal cannot be empty if provided");
        }

        // Validate pagination
        if (page != null && page < 0) {
            throw new IllegalArgumentException("page must be >= 0");
        }
        if (size != null && (size < 1 || size > 100)) {
            throw new IllegalArgumentException("size must be between 1 and 100");
        }
    }

    /**
     * Normalizes the filter parameters to ensure consistency.
     * Converts string values to uppercase and trims whitespace.
     */
    public void normalize() {
        if (eventType != null) {
            eventType = eventType.trim().toUpperCase();
        }
        if (overallStatus != null) {
            overallStatus = overallStatus.trim().toUpperCase();
        }
        if (overallCompletion != null) {
            overallCompletion = overallCompletion.trim().toUpperCase();
        }
        if (processorId != null) {
            processorId = processorId.trim();
        }
        if (consentId != null) {
            consentId = consentId.trim();
        }
        if (dataPrincipal != null) {
            dataPrincipal = dataPrincipal.trim();
        }
        // Set defaults for pagination
        if (page == null) {
            page = 0;
        }
        if (size == null) {
            size = 20;
        }
    }

    /**
     * Checks if any filters are applied.
     *
     * @return true if at least one filter is provided, false otherwise
     */
    public boolean hasFilters() {
        return (eventType != null && !eventType.trim().isEmpty()) ||
               (overallStatus != null && !overallStatus.trim().isEmpty()) ||
               (overallCompletion != null && !overallCompletion.trim().isEmpty()) ||
               (processorId != null && !processorId.trim().isEmpty()) ||
               (consentId != null && !consentId.trim().isEmpty()) ||
               (dataPrincipal != null && !dataPrincipal.trim().isEmpty()) ||
               fromDate != null ||
               toDate != null;
    }

    /**
     * Gets the list of event types to filter by.
     * If eventType is specified, returns single-item list.
     * Otherwise, returns both CONSENT_EXPIRED and CONSENT_WITHDRAWN.
     *
     * @return list of event types to filter
     */
    public List<String> getEventTypesToFilter() {
        if (eventType != null && !eventType.trim().isEmpty()) {
            return Arrays.asList(eventType.trim().toUpperCase());
        }
        return VALID_EVENT_TYPES;
    }
}
