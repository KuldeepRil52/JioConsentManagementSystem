package com.jio.digigov.notification.exception;

import com.jio.digigov.notification.enums.JdnmErrorCode;

/**
 * Exception thrown when a user attempts to access or modify a notification they are not authorized for.
 *
 * <p>This exception is thrown when:</p>
 * <ul>
 *   <li>The notification's recipientId doesn't match the requester's recipientId</li>
 *   <li>The notification's recipientType doesn't match the requester's recipientType</li>
 *   <li>The notification doesn't exist but authorization check failed</li>
 * </ul>
 *
 * <p><b>Example Usage:</b></p>
 * <pre>
 * Optional&lt;NotificationCallback&gt; notification = repository.findByIdAndRecipient(
 *     notificationId, recipientType, recipientId, mongoTemplate);
 *
 * if (notification.isEmpty()) {
 *     throw new UnauthorizedNotificationAccessException(notificationId);
 * }
 * </pre>
 *
 * <p><b>HTTP Response:</b> This exception should be mapped to HTTP 403 Forbidden.</p>
 *
 * @author Notification Service Team
 * @version 1.0
 * @since 2025-01-20
 */
public class UnauthorizedNotificationAccessException extends BusinessException {

    private final String notificationId;

    /**
     * Constructs a new UnauthorizedNotificationAccessException with the specified notification ID.
     *
     * @param notificationId The ID of the notification that was attempted to be accessed
     */
    public UnauthorizedNotificationAccessException(String notificationId) {
        super(JdnmErrorCode.JDNM4011.getCode(), String.format("You are not authorized to update notification: %s", notificationId));
        this.notificationId = notificationId;
    }

    /**
     * Gets the notification ID that was attempted to be accessed.
     *
     * @return The notification ID
     */
    public String getNotificationId() {
        return notificationId;
    }
}
