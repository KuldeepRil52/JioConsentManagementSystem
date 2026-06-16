package com.jio.digigov.notification.enums;

/**
 * Enumeration for the source of status history updates.
 *
 * <p>This enum identifies where a status update originated from, providing
 * complete traceability for audit purposes.</p>
 *
 * <p><b>Usage:</b></p>
 * <ul>
 *   <li>CALLBACK_RESPONSE - Status updated based on callback response from DF/DP</li>
 *   <li>API_ENDPOINT - Status updated via manual API call</li>
 *   <li>SYSTEM - Status updated by system/automated process</li>
 * </ul>
 *
 * @author Notification Service Team
 * @version 1.0
 * @since 2025-01-20
 */
public enum StatusHistorySource {

    /**
     * Status update originated from a callback response from DF/DP.
     * The DF/DP endpoint returned an acknowledgement response that was parsed
     * and used to update the notification status.
     */
    CALLBACK_RESPONSE,

    /**
     * Status update originated from a manual API call.
     * A DF/DP system called the status update endpoint to explicitly
     * acknowledge or update a notification.
     */
    API_ENDPOINT,

    /**
     * Status update originated from the system itself.
     * Used for automated system processes like retry scheduling,
     * timeout handling, or other internal status changes.
     */
    SYSTEM
}
