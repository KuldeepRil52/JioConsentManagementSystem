package com.jio.digigov.notification.dto.request.notification;

import com.jio.digigov.notification.enums.RecipientType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO for filtering callback notification statistics queries.
 *
 * Supports filtering callback statistics by event type, recipient type, recipient ID,
 * and date range to provide targeted statistical insights.
 *
 * @author Notification Service Team
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Filter parameters for callback notification statistics")
public class CallbackStatsFilterRequestDto {

    /**
     * Filter by specific event type (e.g., CONSENT_CREATED, DATA_BREACH, GRIEVANCE_RAISED).
     * Accepts any event type string value from the database without enum validation.
     * If not provided, statistics will include all event types.
     */
    @Schema(description = "Filter by event type (accepts any string value)", example = "CONSENT_CREATED")
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
     * Filters based on the created_at timestamp of the callback.
     */
    @Schema(description = "Start date for filtering (inclusive)",
            example = "2025-01-01T00:00:00")
    private LocalDateTime fromDate;

    /**
     * End date for filtering callbacks (exclusive).
     * Filters based on the created_at timestamp of the callback.
     */
    @Schema(description = "End date for filtering (exclusive)",
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

        // No validation for eventType - accept any string value from database
        // This allows flexibility for custom event types

        // Validate recipient type if provided - MUST be DATA_FIDUCIARY or DATA_PROCESSOR only
        if (recipientType != null && !recipientType.trim().isEmpty()) {
            String upperRecipientType = recipientType.trim().toUpperCase();
            try {
                RecipientType parsedType = RecipientType.valueOf(upperRecipientType);

                // Only allow DATA_FIDUCIARY and DATA_PROCESSOR for callback statistics
                if (parsedType != RecipientType.DATA_FIDUCIARY && parsedType != RecipientType.DATA_PROCESSOR) {
                    throw new IllegalArgumentException(
                        "recipientType must be DATA_FIDUCIARY or DATA_PROCESSOR only. " +
                        "Callback statistics are only available for these recipient types."
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
}
