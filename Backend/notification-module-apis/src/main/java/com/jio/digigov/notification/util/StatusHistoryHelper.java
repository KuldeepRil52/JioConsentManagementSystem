package com.jio.digigov.notification.util;

import com.jio.digigov.notification.entity.notification.StatusHistoryEntry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Helper utility for working with statusHistory in callback notifications.
 *
 * Provides constants, validation methods, and extraction utilities for analyzing
 * statusHistory arrays in callback notifications. Particularly useful for purge
 * statistics and SLA compliance tracking.
 *
 * Status Transition Patterns:
 * - Normal flow: PENDING → SENT → RETRIEVED → ACKNOWLEDGED
 * - Purge flow: ACKNOWLEDGED → DELETED
 * - Failure flow: PENDING → FAILED or RETRY_SCHEDULED
 *
 * Usage:
 * - Extract timestamps: StatusHistoryHelper.getAcknowledgedTimestamp(statusHistory)
 * - Extract timestamps: StatusHistoryHelper.getDeletedTimestamp(statusHistory)
 * - Validate transitions: StatusHistoryHelper.hasStatusTransition(statusHistory, from, to)
 *
 * @author Notification Service Team
 * @since 1.0.0
 */
@Component
@Slf4j
public class StatusHistoryHelper {

    /**
     * Status value for ACKNOWLEDGED status in statusHistory.
     */
    public static final String STATUS_ACKNOWLEDGED = "ACKNOWLEDGED";

    /**
     * Status value for DELETED status in statusHistory.
     */
    public static final String STATUS_DELETED = "DELETED";

    /**
     * Status value for SENT status in statusHistory.
     */
    public static final String STATUS_SENT = "SENT";

    /**
     * Status value for RETRIEVED status in statusHistory.
     */
    public static final String STATUS_RETRIEVED = "RETRIEVED";

    /**
     * Status value for PENDING status in statusHistory.
     */
    public static final String STATUS_PENDING = "PENDING";

    /**
     * Status value for FAILED status in statusHistory.
     */
    public static final String STATUS_FAILED = "FAILED";

    /**
     * Purge status category: callback has been purged (ACKNOWLEDGED → DELETED).
     */
    public static final String PURGE_STATUS_PURGED = "PURGED";

    /**
     * Purge status category: callback is pending purge (ACKNOWLEDGED but not DELETED, within SLA).
     */
    public static final String PURGE_STATUS_PENDING = "PENDING";

    /**
     * Purge status category: callback is overdue for purge (ACKNOWLEDGED but not DELETED, exceeded SLA).
     */
    public static final String PURGE_STATUS_OVERDUE = "OVERDUE";

    /**
     * Extracts the timestamp when the callback was ACKNOWLEDGED from statusHistory.
     *
     * @param statusHistory List of status history entries
     * @return Optional containing the ACKNOWLEDGED timestamp, or empty if not found
     */
    public static Optional<LocalDateTime> getAcknowledgedTimestamp(List<StatusHistoryEntry> statusHistory) {
        if (statusHistory == null || statusHistory.isEmpty()) {
            return Optional.empty();
        }

        return statusHistory.stream()
                .filter(entry -> STATUS_ACKNOWLEDGED.equals(entry.getStatus()))
                .map(StatusHistoryEntry::getTimestamp)
                .findFirst();
    }

    /**
     * Extracts the timestamp when the callback was DELETED from statusHistory.
     *
     * @param statusHistory List of status history entries
     * @return Optional containing the DELETED timestamp, or empty if not found
     */
    public static Optional<LocalDateTime> getDeletedTimestamp(List<StatusHistoryEntry> statusHistory) {
        if (statusHistory == null || statusHistory.isEmpty()) {
            return Optional.empty();
        }

        return statusHistory.stream()
                .filter(entry -> STATUS_DELETED.equals(entry.getStatus()))
                .map(StatusHistoryEntry::getTimestamp)
                .findFirst();
    }

    /**
     * Checks if the statusHistory contains a specific status.
     *
     * @param statusHistory List of status history entries
     * @param status Status to check for
     * @return true if the status exists in history, false otherwise
     */
    public static boolean hasStatus(List<StatusHistoryEntry> statusHistory, String status) {
        if (statusHistory == null || statusHistory.isEmpty() || status == null) {
            return false;
        }

        return statusHistory.stream()
                .anyMatch(entry -> status.equals(entry.getStatus()));
    }

    /**
     * Checks if the statusHistory shows a transition from one status to another.
     * The transition is valid if 'fromStatus' appears before 'toStatus' in the history.
     *
     * @param statusHistory List of status history entries
     * @param fromStatus Starting status
     * @param toStatus Ending status
     * @return true if the transition exists in chronological order, false otherwise
     */
    public static boolean hasStatusTransition(List<StatusHistoryEntry> statusHistory,
                                              String fromStatus,
                                              String toStatus) {
        if (statusHistory == null || statusHistory.isEmpty() ||
            fromStatus == null || toStatus == null) {
            return false;
        }

        boolean foundFrom = false;
        for (StatusHistoryEntry entry : statusHistory) {
            if (fromStatus.equals(entry.getStatus())) {
                foundFrom = true;
            } else if (toStatus.equals(entry.getStatus()) && foundFrom) {
                return true;
            }
        }

        return false;
    }

    /**
     * Checks if a callback has been purged (transitioned from ACKNOWLEDGED to DELETED).
     *
     * @param statusHistory List of status history entries
     * @return true if the callback has been purged, false otherwise
     */
    public static boolean isPurged(List<StatusHistoryEntry> statusHistory) {
        return hasStatusTransition(statusHistory, STATUS_ACKNOWLEDGED, STATUS_DELETED);
    }

    /**
     * Determines the purge status category for a callback.
     *
     * @param statusHistory List of status history entries
     * @param acknowledgedAt Acknowledged timestamp from callback entity (fallback)
     * @param slaHours SLA threshold in hours
     * @return Purge status: PURGED, PENDING, OVERDUE, or null if not applicable
     */
    public static String determinePurgeStatus(List<StatusHistoryEntry> statusHistory,
                                             LocalDateTime acknowledgedAt,
                                             int slaHours) {
        // Check if purged (ACKNOWLEDGED → DELETED)
        if (isPurged(statusHistory)) {
            return PURGE_STATUS_PURGED;
        }

        // Get acknowledged timestamp
        Optional<LocalDateTime> ackTimestamp = getAcknowledgedTimestamp(statusHistory);
        LocalDateTime acknowledged = ackTimestamp.orElse(acknowledgedAt);

        if (acknowledged == null) {
            // No acknowledged timestamp, cannot determine purge status
            return null;
        }

        // Check if deleted
        boolean isDeleted = hasStatus(statusHistory, STATUS_DELETED);
        if (isDeleted) {
            // Has DELETED but not in proper sequence (shouldn't happen, but handle gracefully)
            return PURGE_STATUS_PURGED;
        }

        // Not deleted, check if overdue
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime slaDeadline = acknowledged.plusHours(slaHours);

        if (now.isAfter(slaDeadline)) {
            return PURGE_STATUS_OVERDUE;
        } else {
            return PURGE_STATUS_PENDING;
        }
    }

    /**
     * Validates that a statusHistory entry is not null and has required fields.
     *
     * @param entry StatusHistoryEntry to validate
     * @return true if valid, false otherwise
     */
    public static boolean isValidEntry(StatusHistoryEntry entry) {
        return entry != null &&
               entry.getStatus() != null &&
               !entry.getStatus().trim().isEmpty() &&
               entry.getTimestamp() != null;
    }

    /**
     * Counts the number of times a specific status appears in statusHistory.
     *
     * @param statusHistory List of status history entries
     * @param status Status to count
     * @return Count of occurrences
     */
    public static long countStatus(List<StatusHistoryEntry> statusHistory, String status) {
        if (statusHistory == null || statusHistory.isEmpty() || status == null) {
            return 0;
        }

        return statusHistory.stream()
                .filter(entry -> status.equals(entry.getStatus()))
                .count();
    }

    /**
     * Gets the most recent status from statusHistory.
     *
     * @param statusHistory List of status history entries
     * @return Optional containing the most recent status, or empty if no history
     */
    public static Optional<String> getMostRecentStatus(List<StatusHistoryEntry> statusHistory) {
        if (statusHistory == null || statusHistory.isEmpty()) {
            return Optional.empty();
        }

        // StatusHistory is typically ordered chronologically, so get the last entry
        StatusHistoryEntry lastEntry = statusHistory.get(statusHistory.size() - 1);
        return Optional.ofNullable(lastEntry.getStatus());
    }

    /**
     * Calculates the time elapsed since a callback was acknowledged.
     *
     * @param statusHistory List of status history entries
     * @param acknowledgedAt Acknowledged timestamp from callback entity (fallback)
     * @return Optional containing hours elapsed, or empty if no acknowledged timestamp
     */
    public static Optional<Long> getHoursSinceAcknowledged(List<StatusHistoryEntry> statusHistory,
                                                          LocalDateTime acknowledgedAt) {
        Optional<LocalDateTime> ackTimestamp = getAcknowledgedTimestamp(statusHistory);
        LocalDateTime acknowledged = ackTimestamp.orElse(acknowledgedAt);

        if (acknowledged == null) {
            return Optional.empty();
        }

        LocalDateTime now = LocalDateTime.now();
        long hours = java.time.Duration.between(acknowledged, now).toHours();
        return Optional.of(hours);
    }
}
