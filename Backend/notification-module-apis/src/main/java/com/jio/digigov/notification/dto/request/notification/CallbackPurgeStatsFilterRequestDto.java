package com.jio.digigov.notification.dto.request.notification;

import com.jio.digigov.notification.enums.RecipientType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

/**
 * DTO for filtering callback purge statistics queries.
 *
 * Supports filtering purge statistics for CONSENT_EXPIRED and CONSENT_WITHDRAWN events
 * by recipient type, recipient ID, and date range based on acknowledged timestamp.
 *
 * @author Notification Service Team
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Filter parameters for callback purge statistics")
public class CallbackPurgeStatsFilterRequestDto {

    /**
     * Valid event types for purge statistics.
     */
    private static final List<String> VALID_EVENT_TYPES = Arrays.asList(
            "CONSENT_EXPIRED", "CONSENT_WITHDRAWN"
    );

    /**
     * Filter by specific event type (CONSENT_EXPIRED or CONSENT_WITHDRAWN).
     * If not provided, statistics will include both event types.
     */
    @Schema(description = "Filter by event type (CONSENT_EXPIRED or CONSENT_WITHDRAWN)",
            example = "CONSENT_EXPIRED",
            allowableValues = {"CONSENT_EXPIRED", "CONSENT_WITHDRAWN"})
    private String eventType;

    /**
     * Filter by recipient type (DATA_FIDUCIARY or DATA_PROCESSOR only).
     * If not provided, statistics will include both Data Fiduciaries and Data Processors.
     */
    @Schema(description = "Filter by recipient type (DATA_FIDUCIARY or DATA_PROCESSOR)",
            example = "DATA_FIDUCIARY",
            allowableValues = {"DATA_FIDUCIARY", "DATA_PROCESSOR"})
    private String recipientType;

    /**
     * Filter by specific recipient ID.
     * For Data Fiduciaries, this is the businessId.
     * For Data Processors, this is the dataProcessorId.
     */
    @Schema(description = "Filter by specific recipient ID (business ID for DF, data processor ID for DP)",
            example = "DF_12345")
    private String recipientId;

    /**
     * Start date for filtering callbacks (inclusive).
     * Filters based on the ACKNOWLEDGED timestamp from statusHistory.
     */
    @Schema(description = "Start date for filtering (inclusive, based on acknowledged timestamp)",
            example = "2025-01-01T00:00:00")
    private LocalDateTime fromDate;

    /**
     * End date for filtering callbacks (exclusive).
     * Filters based on the ACKNOWLEDGED timestamp from statusHistory.
     */
    @Schema(description = "End date for filtering (exclusive, based on acknowledged timestamp)",
            example = "2025-12-31T23:59:59")
    private LocalDateTime toDate;

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

        // Validate recipient type if provided - MUST be DATA_FIDUCIARY or DATA_PROCESSOR only
        if (recipientType != null && !recipientType.trim().isEmpty()) {
            String upperRecipientType = recipientType.trim().toUpperCase();
            try {
                RecipientType parsedType = RecipientType.valueOf(upperRecipientType);

                // Only allow DATA_FIDUCIARY and DATA_PROCESSOR for callback statistics
                if (parsedType != RecipientType.DATA_FIDUCIARY && parsedType != RecipientType.DATA_PROCESSOR) {
                    throw new IllegalArgumentException(
                        "recipientType must be DATA_FIDUCIARY or DATA_PROCESSOR only. " +
                        "Callback purge statistics are only available for these recipient types."
                    );
                }
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException(
                    "Invalid recipientType: " + recipientType + ". Must be DATA_FIDUCIARY or DATA_PROCESSOR."
                );
            }
        }

        // Validate recipientId if provided
        if (recipientId != null && recipientId.trim().isEmpty()) {
            throw new IllegalArgumentException("recipientId cannot be empty if provided");
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
        if (recipientType != null) {
            recipientType = recipientType.trim().toUpperCase();
        }
        if (recipientId != null) {
            recipientId = recipientId.trim();
        }
    }

    /**
     * Checks if any filters are applied.
     *
     * @return true if at least one filter is provided, false otherwise
     */
    public boolean hasFilters() {
        return (eventType != null && !eventType.trim().isEmpty()) ||
               (recipientType != null && !recipientType.trim().isEmpty()) ||
               (recipientId != null && !recipientId.trim().isEmpty()) ||
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
