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
 * This DTO provides filtering capabilities specifically for callback/webhook
 * notifications. It supports filtering by recipient details, status, specific IDs,
 * and date ranges for missed callback notifications only.
 *
 * Only retrieves notifications with status: FAILED, PROCESSING, RETRY_SCHEDULED
 * Excludes: SUCCESSFUL, ACKNOWLEDGED, PENDING
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BulkNotificationRequestDto {

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
     * Only accepts: FAILED, PROCESSING, RETRY_SCHEDULED
     * Other statuses are automatically excluded
     */
    private String status;

    /**
     * Filter by event type.
     * Examples: CONSENT_GRANTED, DATA_BREACH, PROCESSING_REQUEST
     */
    private String eventType;

    /**
     * Filter by business ID.
     * Used to retrieve notifications for a specific business context.
     */
    private String businessId;

    /**
     * Filter by priority level.
     * Values: HIGH, MEDIUM, LOW
     */
    private String priority;

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
     * Sort field for ordering results.
     * Common fields: createdAt, updatedAt, lastAttemptAt, attemptCount
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

    /**
     * Include only notifications with attempt count greater than this value.
     * Useful for finding notifications that have failed multiple times.
     */
    private Integer minAttemptCount;

    /**
     * Include only notifications with attempt count less than this value.
     */
    private Integer maxAttemptCount;
}