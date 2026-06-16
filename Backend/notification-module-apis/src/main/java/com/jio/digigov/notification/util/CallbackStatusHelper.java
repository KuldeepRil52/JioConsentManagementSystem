package com.jio.digigov.notification.util;

import com.jio.digigov.notification.enums.NotificationStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Helper utility for categorizing callback notification statuses into success and failure groups.
 *
 * This utility provides a centralized, maintainable way to manage status categorization
 * for callback notification statistics and reporting. It eliminates hardcoded status
 * checks throughout the codebase and provides consistent status categorization logic.
 *
 * Status Categories:
 * - Success: SENT, RETRIEVED, ACKNOWLEDGED, PROCESSING, PROCESSED, DELETED
 * - Failure: FAILED, PENDING, RETRY_SCHEDULED
 *
 * Usage:
 * - Check if a status is successful: CallbackStatusHelper.isSuccessStatus(status)
 * - Check if a status is failed: CallbackStatusHelper.isFailureStatus(status)
 * - Get all success statuses for queries: CallbackStatusHelper.getSuccessStatuses()
 * - Get all failure statuses for queries: CallbackStatusHelper.getFailureStatuses()
 *
 * @author Notification Service Team
 * @since 1.0.0
 */
@Component
@Slf4j
public class CallbackStatusHelper {

    /**
     * Statuses that indicate successful callback processing.
     * These represent callbacks that have been successfully sent, delivered, or acknowledged.
     */
    private static final List<NotificationStatus> SUCCESS_STATUSES = Arrays.asList(
            NotificationStatus.SENT,          // Callback sent to recipient
            NotificationStatus.RETRIEVED,     // Callback retrieved by recipient
            NotificationStatus.ACKNOWLEDGED,  // Callback acknowledged by recipient
            NotificationStatus.PROCESSING,    // Currently being processed successfully
            NotificationStatus.PROCESSED,     // Successfully processed
            NotificationStatus.DELETED        // Deleted (considered successful as it was delivered)
    );

    /**
     * Statuses that indicate failed callback processing.
     * These represent callbacks that failed, are pending, or require retry.
     */
    private static final List<NotificationStatus> FAILURE_STATUSES = Arrays.asList(
            NotificationStatus.FAILED,           // Callback failed
            NotificationStatus.PENDING,          // Still pending (not yet successful)
            NotificationStatus.RETRY_SCHEDULED   // Scheduled for retry (previous attempt failed)
    );

    /**
     * Success status names as strings (for MongoDB queries).
     */
    private static final List<String> SUCCESS_STATUS_NAMES = SUCCESS_STATUSES.stream()
            .map(Enum::name)
            .collect(Collectors.toList());

    /**
     * Failure status names as strings (for MongoDB queries).
     */
    private static final List<String> FAILURE_STATUS_NAMES = FAILURE_STATUSES.stream()
            .map(Enum::name)
            .collect(Collectors.toList());

    /**
     * Success status names as a Set for faster lookup.
     */
    private static final Set<String> SUCCESS_STATUS_SET = SUCCESS_STATUSES.stream()
            .map(Enum::name)
            .collect(Collectors.toSet());

    /**
     * Failure status names as a Set for faster lookup.
     */
    private static final Set<String> FAILURE_STATUS_SET = FAILURE_STATUSES.stream()
            .map(Enum::name)
            .collect(Collectors.toSet());

    /**
     * Checks if the given status represents a successful callback.
     *
     * @param status the notification status to check
     * @return true if the status is a success status, false otherwise
     */
    public static boolean isSuccessStatus(NotificationStatus status) {
        if (status == null) {
            log.warn("Null status provided to isSuccessStatus check");
            return false;
        }
        return SUCCESS_STATUSES.contains(status);
    }

    /**
     * Checks if the given status string represents a successful callback.
     *
     * @param statusName the notification status name to check
     * @return true if the status is a success status, false otherwise
     */
    public static boolean isSuccessStatus(String statusName) {
        if (statusName == null || statusName.trim().isEmpty()) {
            log.warn("Null or empty status name provided to isSuccessStatus check");
            return false;
        }
        return SUCCESS_STATUS_SET.contains(statusName.toUpperCase());
    }

    /**
     * Checks if the given status represents a failed callback.
     *
     * @param status the notification status to check
     * @return true if the status is a failure status, false otherwise
     */
    public static boolean isFailureStatus(NotificationStatus status) {
        if (status == null) {
            log.warn("Null status provided to isFailureStatus check");
            return false;
        }
        return FAILURE_STATUSES.contains(status);
    }

    /**
     * Checks if the given status string represents a failed callback.
     *
     * @param statusName the notification status name to check
     * @return true if the status is a failure status, false otherwise
     */
    public static boolean isFailureStatus(String statusName) {
        if (statusName == null || statusName.trim().isEmpty()) {
            log.warn("Null or empty status name provided to isFailureStatus check");
            return false;
        }
        return FAILURE_STATUS_SET.contains(statusName.toUpperCase());
    }

    /**
     * Gets the list of all success statuses (enum values).
     *
     * @return unmodifiable list of success NotificationStatus enums
     */
    public static List<NotificationStatus> getSuccessStatuses() {
        return List.copyOf(SUCCESS_STATUSES);
    }

    /**
     * Gets the list of all failure statuses (enum values).
     *
     * @return unmodifiable list of failure NotificationStatus enums
     */
    public static List<NotificationStatus> getFailureStatuses() {
        return List.copyOf(FAILURE_STATUSES);
    }

    /**
     * Gets the list of success status names (for MongoDB queries).
     * Returns status names as strings in uppercase.
     *
     * @return list of success status names
     */
    public static List<String> getSuccessStatusNames() {
        return List.copyOf(SUCCESS_STATUS_NAMES);
    }

    /**
     * Gets the list of failure status names (for MongoDB queries).
     * Returns status names as strings in uppercase.
     *
     * @return list of failure status names
     */
    public static List<String> getFailureStatusNames() {
        return List.copyOf(FAILURE_STATUS_NAMES);
    }

    /**
     * Validates if a status name is either a success or failure status.
     *
     * @param statusName the status name to validate
     * @return true if the status is recognized (success or failure), false otherwise
     */
    public static boolean isValidCallbackStatus(String statusName) {
        if (statusName == null || statusName.trim().isEmpty()) {
            return false;
        }
        String upperStatus = statusName.toUpperCase();
        return SUCCESS_STATUS_SET.contains(upperStatus) || FAILURE_STATUS_SET.contains(upperStatus);
    }

    /**
     * Gets a descriptive category name for a given status.
     *
     * @param statusName the status name
     * @return "SUCCESS" if success status, "FAILURE" if failure status, "UNKNOWN" otherwise
     */
    public static String getStatusCategory(String statusName) {
        if (isSuccessStatus(statusName)) {
            return "SUCCESS";
        } else if (isFailureStatus(statusName)) {
            return "FAILURE";
        } else {
            log.warn("Unknown callback status category for status: {}", statusName);
            return "UNKNOWN";
        }
    }
}
