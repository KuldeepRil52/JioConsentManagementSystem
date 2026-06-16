package com.jio.digigov.notification.enums;

/**
 * Enumeration for customer identification types.
 *
 * <p>Used to specify how a customer is identified in the notification system.
 * This enum provides type safety for customer identifier validation and template selection.</p>
 *
 * <p><b>Identifier Types:</b></p>
 * <ul>
 *   <li><b>MOBILE</b> - Customer identified by mobile phone number (used for SMS notifications)</li>
 *   <li><b>EMAIL</b> - Customer identified by email address (used for email notifications)</li>
 * </ul>
 *
 * <p><b>Usage Examples:</b></p>
 * <pre>
 * // Template selection based on event type
 * templateKey = eventType;
 *
 * // API parameter validation
 * PagedResponseDto&lt;NotificationResponseDto&gt; notifications = notificationService.searchNotificationsByCustomer(
 *     tenantId, CustomerIdentifierType.MOBILE.name(), phoneNumber, page, size);
 * </pre>
 *
 * @see com.jio.digigov.notification.controller.v1.NotificationController#searchNotificationsByCustomer
 * @since 1.7.0
 */
public enum CustomerIdentifierType {
    /** Mobile phone number identifier */
    MOBILE,

    /** Email address identifier */
    EMAIL
}