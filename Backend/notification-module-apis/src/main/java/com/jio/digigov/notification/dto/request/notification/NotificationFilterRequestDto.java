package com.jio.digigov.notification.dto.request.notification;

import com.jio.digigov.notification.enums.RecipientType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO for filtering notification queries across all notification types.
 *
 * Supports comprehensive filtering across notification_events and all associated
 * notification collections (SMS, Email, Callback) with pagination and sorting.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationFilterRequestDto {

    /**
     * Page number for pagination (0-based)
     */
    @Min(value = 0, message = "Page number must be non-negative")
    @Builder.Default
    private Integer page = 0;

    /**
     * Page size for pagination (1-100)
     */
    @Min(value = 1, message = "Page size must be at least 1")
    @Max(value = 100, message = "Page size cannot exceed 100")
    @Builder.Default
    private Integer size = 20;

    /**
     * Filter by specific event ID
     */
    private String eventId;

    /**
     * Filter by event type (e.g., CONSENT_GRANTED, DATA_BREACH)
     */
    private String eventType;

    /**
     * Filter by business ID
     */
    private String businessId;

    /**
     * Filter by overall event status
     */
    private String status;

    /**
     * Filter by event priority
     */
    private String priority;

    /**
     * Filter by resource type
     */
    private String resource;

    /**
     * Filter by source system
     */
    private String source;

    /**
     * Filter by specific notification type (SMS, EMAIL, CALLBACK)
     */
    private String notificationType;

    /**
     * Filter by notification status within specific type
     */
    private String notificationStatus;

    /**
     * Filter by customer identifier type (MOBILE, EMAIL)
     */
    private String customerIdentifierType;

    /**
     * Filter by customer identifier value
     */
    private String customerIdentifierValue;

    /**
     * Filter by specific data processor IDs
     */
    private List<String> dataProcessorIds;

    /**
     * Filter by specific event IDs
     */
    private List<String> eventIds;

    /**
     * Filter by mobile number for SMS notifications
     */
    private String mobile;

    /**
     * Filter by email address for email notifications
     */
    private String email;

    /**
     * Filter by recipient type for callback notifications
     */
    private String recipientType;

    /**
     * Filter by recipient ID for callback notifications
     */
    private String recipientId;

    /**
     * Filter by template ID
     */
    private String templateId;

    /**
     * Start date for filtering (inclusive)
     */
    private LocalDateTime fromDate;

    /**
     * End date for filtering (exclusive)
     */
    private LocalDateTime toDate;

    /**
     * Minimum attempt count filter
     */
    @Min(value = 0, message = "Minimum attempt count must be non-negative")
    private Integer minAttemptCount;

    /**
     * Maximum attempt count filter
     */
    @Min(value = 0, message = "Maximum attempt count must be non-negative")
    private Integer maxAttemptCount;

    /**
     * Field to sort by (default: createdAt)
     */
    @Builder.Default
    private String sortBy = "createdAt";

    /**
     * Sort direction (ASC or DESC, default: DESC)
     */
    @Builder.Default
    private String sortDirection = "DESC";

    /**
     * Include only events with notifications
     */
    @Builder.Default
    private Boolean hasNotifications = null;

    /**
     * Include only events with failed notifications
     */
    @Builder.Default
    private Boolean hasFailedNotifications = null;

    /**
     * Include only events with pending notifications
     */
    @Builder.Default
    private Boolean hasPendingNotifications = null;

    /**
     * Filter by correlation ID
     */
    private String correlationId;

    /**
     * Filter by language
     */
    private String language;

    /**
     * Filter events created after this timestamp
     */
    private LocalDateTime createdAfter;

    /**
     * Filter events created before this timestamp
     */
    private LocalDateTime createdBefore;

    /**
     * Filter events completed after this timestamp
     */
    private LocalDateTime completedAfter;

    /**
     * Filter events completed before this timestamp
     */
    private LocalDateTime completedBefore;

    /**
     * Include detailed notification information in response
     */
    @Builder.Default
    private Boolean includeNotificationDetails = true;

    /**
     * Include event payload in response
     */
    @Builder.Default
    private Boolean includeEventPayload = false;

    /**
     * Maximum number of notifications per type to include
     */
    @Min(value = 1, message = "Notification limit must be at least 1")
    @Max(value = 50, message = "Notification limit cannot exceed 50")
    @Builder.Default
    private Integer notificationLimit = 10;

    /**
     * Validates the filter request parameters
     */
    public void validate() {
        if (fromDate != null && toDate != null && fromDate.isAfter(toDate)) {
            throw new IllegalArgumentException("fromDate cannot be after toDate");
        }

        if (createdAfter != null && createdBefore != null && createdAfter.isAfter(createdBefore)) {
            throw new IllegalArgumentException("createdAfter cannot be after createdBefore");
        }

        if (completedAfter != null && completedBefore != null && completedAfter.isAfter(completedBefore)) {
            throw new IllegalArgumentException("completedAfter cannot be after completedBefore");
        }

        if (minAttemptCount != null && maxAttemptCount != null && minAttemptCount > maxAttemptCount) {
            throw new IllegalArgumentException("minAttemptCount cannot be greater than maxAttemptCount");
        }

        if (sortDirection != null && !sortDirection.equalsIgnoreCase("ASC") && !sortDirection.equalsIgnoreCase("DESC")) {
            throw new IllegalArgumentException("sortDirection must be ASC or DESC");
        }

        if (notificationType != null) {
            String upperType = notificationType.toUpperCase();
            if (!upperType.equals("SMS") && !upperType.equals("EMAIL") && !upperType.equals("CALLBACK")) {
                throw new IllegalArgumentException("notificationType must be SMS, EMAIL, or CALLBACK");
            }
        }

        if (recipientType != null) {
            try {
                RecipientType.valueOf(recipientType.toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("recipientType must be one of: DATA_PRINCIPAL, DATA_FIDUCIARY, DATA_PROCESSOR, DATA_PROTECTION_OFFICER");
            }
        }

        if (customerIdentifierType != null) {
            String upperIdentifierType = customerIdentifierType.toUpperCase();
            if (!upperIdentifierType.equals("MOBILE") && !upperIdentifierType.equals("EMAIL")) {
                throw new IllegalArgumentException("customerIdentifierType must be MOBILE or EMAIL");
            }
        }
    }
}