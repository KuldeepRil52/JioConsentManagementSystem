package com.jio.digigov.notification.enums;

/**
 * Enumeration for notification delivery channels.
 *
 * <p>Represents the different ways notifications can be delivered to recipients.
 * This enum provides type safety for channel identification throughout the notification system.</p>
 *
 * <p><b>Channel Types:</b></p>
 * <ul>
 *   <li><b>SMS</b> - Text message notifications sent to mobile numbers via DigiGov SMS gateway</li>
 *   <li><b>EMAIL</b> - Email notifications sent to email addresses via DigiGov email gateway</li>
 *   <li><b>CALLBACK</b> - HTTP POST callbacks sent to Data Processor endpoints for consent events</li>
 * </ul>
 *
 * <p><b>Usage Examples:</b></p>
 * <pre>
 * // Using in metrics recording
 * retryPolicyService.recordRetryMetrics(NotificationChannel.CALLBACK.name(), attemptCount, success, reason);
 *
 * // Using in template lookup
 * String templateKey = eventType + "_" + NotificationChannel.SMS.name() + "_TEMPLATE";
 * </pre>
 *
 * @see com.jio.digigov.notification.enumeration.NotificationStatus
 * @see com.jio.digigov.notification.service.kafka.RetryPolicyService
 * @since 1.7.0
 */
public enum NotificationChannel {
    /** SMS text message notifications */
    SMS,

    /** Email notifications */
    EMAIL,

    /** HTTP callback notifications to Data Processors */
    CALLBACK
}