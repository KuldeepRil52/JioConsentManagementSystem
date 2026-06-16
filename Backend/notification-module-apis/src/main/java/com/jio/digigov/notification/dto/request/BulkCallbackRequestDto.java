package com.jio.digigov.notification.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Request DTO for bulk fetching missed callback notifications with filters.
 *
 * This DTO encapsulates all filtering and pagination parameters for retrieving
 * multiple callback notifications in a single request. It supports filtering by
 * recipient details, status, notification IDs, and date range.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BulkCallbackRequestDto {

    /**
     * Page number for pagination (0-based).
     * Default: 0 (first page)
     */
    @Min(0)
    @Builder.Default
    private int page = 0;

    /**
     * Page size for pagination.
     * Minimum: 1, Maximum: 100, Default: 20
     */
    @Min(1)
    @Max(100)
    @Builder.Default
    private int size = 20;

    /**
     * Filter by recipient type.
     * Examples: DATA_FIDUCIARY, DATA_PROCESSOR
     */
    private String recipientType;

    /**
     * Filter by specific recipient ID.
     * Used to retrieve notifications for a specific organization.
     */
    private String recipientId;

    /**
     * Filter by notification status.
     * Examples: FAILED, RETRY_SCHEDULED
     */
    private String status;

    /**
     * List of specific notification IDs to retrieve.
     * When provided, other filters are applied on this subset.
     */
    private List<String> notificationIds;

    /**
     * Start date for date range filtering.
     * Format: ISO LocalDateTime
     */
    private LocalDateTime fromDate;

    /**
     * End date for date range filtering.
     * Format: ISO LocalDateTime
     */
    private LocalDateTime toDate;

    /**
     * Flag to mark retrieved notifications as RETRIEVED.
     * When true, prevents future retry attempts for these notifications.
     * Default: false
     */
    @Builder.Default
    private boolean markAsRetrieved = false;

    /**
     * Sort field for ordering results.
     * Default: createdAt
     */
    @Builder.Default
    private String sortBy = "createdAt";

    /**
     * Sort direction (ASC or DESC).
     * Default: DESC
     */
    @Builder.Default
    private String sortDirection = "DESC";
}