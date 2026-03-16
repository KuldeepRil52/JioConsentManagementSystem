package com.jio.digigov.notification.exception;

import com.jio.digigov.notification.enums.JdnmErrorCode;

/**
 * Exception thrown when attempting an invalid status transition.
 *
 * <p>This exception is thrown when a notification status update request
 * attempts to transition from one status to another in a way that violates
 * the defined status transition rules.</p>
 *
 * <p><b>Valid Transitions:</b></p>
 * <ul>
 *   <li>RETRIEVED → ACKNOWLEDGED ✅</li>
 *   <li>ACKNOWLEDGED → ACKNOWLEDGED ✅ (allows re-updates)</li>
 *   <li>FAILED → ACKNOWLEDGED ❌</li>
 *   <li>RETRY_SCHEDULED → ACKNOWLEDGED ❌</li>
 * </ul>
 *
 * <p><b>Example Usage:</b></p>
 * <pre>
 * if (!isValidTransition(currentStatus, newStatus)) {
 *     throw new InvalidStatusTransitionException(currentStatus, newStatus);
 * }
 * </pre>
 *
 * @author Notification Service Team
 * @version 1.0
 * @since 2025-01-20
 */
public class InvalidStatusTransitionException extends BusinessException {

    private final String currentStatus;
    private final String newStatus;

    /**
     * Constructs a new InvalidStatusTransitionException with the specified current and new status.
     *
     * @param currentStatus The current status of the notification
     * @param newStatus The requested new status
     */
    public InvalidStatusTransitionException(String currentStatus, String newStatus) {
        super(JdnmErrorCode.JDNM1016.getCode(), String.format("Invalid status transition from %s to %s", currentStatus, newStatus));
        this.currentStatus = currentStatus;
        this.newStatus = newStatus;
    }

    /**
     * Gets the current status that was being transitioned from.
     *
     * @return The current status
     */
    public String getCurrentStatus() {
        return currentStatus;
    }

    /**
     * Gets the new status that was being transitioned to.
     *
     * @return The new status
     */
    public String getNewStatus() {
        return newStatus;
    }
}
