package com.jio.digigov.notification.enums;

/**
 * Enumeration for notification processing status.
 *
 * <p>Used to track the lifecycle of notification delivery across different channels (SMS, Email, Callback).
 * This enum replaces string literals throughout the codebase to provide type safety and better maintainability.</p>
 *
 * <p><b>Lifecycle Flow:</b></p>
 * <pre>
 * PENDING → PROCESSING → QUEUED → ACCEPTED → SENT → DELIVERED
 *        ↓                   ↓              ↓
 *     FAILED ←────────── RETRY_SCHEDULED ──┘
 *        ↓
 *     ERROR
 * </pre>
 *
 * <p><b>Usage Examples:</b></p>
 * <pre>
 * // Setting status in repository
 * repository.updateStatus(notificationId, NotificationStatus.PROCESSING.name(), mongoTemplate);
 *
 * // Checking status in business logic
 * if (NotificationStatus.SENT.name().equals(notification.getStatus())) {
 *     // Handle successful send
 * }
 * </pre>
 *
 * @see com.jio.digigov.notification.service.kafka.impl.EmailNotificationConsumer
 * @see com.jio.digigov.notification.service.kafka.impl.SmsNotificationConsumer
 * @see com.jio.digigov.notification.service.kafka.impl.CallbackNotificationConsumer
 * @since 1.7.0
 */
public enum NotificationStatus {
    /** Notification has been queued for processing */
    PENDING,

    /** Notification is currently being processed */
    PROCESSING,

    /** Notification has been successfully sent to the gateway */
    SENT,

    /** Notification has been delivered to the recipient */
    DELIVERED,

    /** Notification processing or delivery failed */
    FAILED,

    /** Notification has been scheduled for retry after failure */
    RETRY_SCHEDULED,

    /** An error occurred during notification processing */
    ERROR,

    /** Notification has been queued in the messaging system */
    QUEUED,

    /** Notification has been accepted by the gateway */
    ACCEPTED,

    /** Notification was successfully processed (legacy compatibility) */
    SUCCESS,

    /** Notification has been acknowledged by recipient */
    ACKNOWLEDGED,

    /** Notification has been retrieved by recipient */
    RETRIEVED,

    /** Notification has been deleted by recipient */
    DELETED,

    /**
     * Notification has been deferred, data cannot be deleted.
     *
     * <p><b>Terminal State:</b> This is a terminal/final state indicating
     * the data deletion request could not be fulfilled (e.g., regulatory hold,
     * legal requirement, or business constraint). No further status transitions
     * are expected from this state.</p>
     *
     * <p>Common reasons for DEFERRED status:</p>
     * <ul>
     *   <li>Regulatory hold on the data</li>
     *   <li>Legal retention requirements</li>
     *   <li>Ongoing investigation or audit</li>
     *   <li>Business-critical data that cannot be deleted</li>
     * </ul>
     */
    DEFERRED,

    /** Notification has been processed successfully */
    PROCESSED
}