package com.jio.digigov.notification.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Enumeration for allowed notification status values in the Update Status API.
 *
 * <p>This enum restricts the status values that can be used when updating
 * notification status through the missed-notification API endpoints.
 * Only specific status transitions are allowed to maintain data integrity.</p>
 *
 * <p><b>Allowed Status Values:</b></p>
 * <ul>
 *   <li>ACKNOWLEDGED - Notification has been acknowledged by the recipient</li>
 *   <li>DELETED - Notification has been deleted by the recipient</li>
 *   <li>DEFERRED - Notification has been deferred (data cannot be deleted due to regulatory hold)</li>
 *   <li>PROCESSING - Notification is currently being processed</li>
 *   <li>PROCESSED - Notification has been successfully processed</li>
 * </ul>
 *
 * <p><b>Usage Example:</b></p>
 * <pre>
 * PUT /v1/missed-notifications/{notificationId}/status
 * {
 *   "status": "ACKNOWLEDGED",
 *   "acknowledgementId": "ACK_XYZ_123",
 *   "remark": "Successfully processed"
 * }
 * </pre>
 *
 * @see com.jio.digigov.notification.dto.request.UpdateNotificationStatusRequestDto
 * @see com.jio.digigov.notification.service.NotificationStatusUpdateService
 * @author Notification Service Team
 * @version 1.0
 * @since 2025-01-20
 */
public enum UpdateNotificationStatusEnum {
    /** Notification has been acknowledged by recipient */
    ACKNOWLEDGED("ACKNOWLEDGED"),

    /** Notification has been deleted by recipient */
    DELETED("DELETED"),

    /** Notification has been deferred - data cannot be deleted (e.g., regulatory hold) */
    DEFERRED("DEFERRED"),

    /** Notification is currently being processed */
    PROCESSING("PROCESSING"),

    /** Notification has been processed successfully */
    PROCESSED("PROCESSED");

    private final String value;

    UpdateNotificationStatusEnum(String value) {
        this.value = value;
    }

    /**
     * Gets the string value of the enum.
     *
     * @return the status value as string
     */
    @JsonValue
    public String getValue() {
        return value;
    }

    /**
     * Creates enum from string value (case-insensitive).
     * Used by Jackson for deserialization.
     *
     * @param value the status value as string
     * @return the corresponding enum value
     * @throws IllegalArgumentException if the value is not valid
     */
    @JsonCreator
    public static UpdateNotificationStatusEnum fromValue(String value) {
        if (value == null) {
            throw new IllegalArgumentException("Status value cannot be null");
        }

        String upperValue = value.trim().toUpperCase();
        for (UpdateNotificationStatusEnum status : UpdateNotificationStatusEnum.values()) {
            if (status.value.equals(upperValue)) {
                return status;
            }
        }

        throw new IllegalArgumentException(
            "Invalid status value: " + value +
            ". Allowed values are: ACKNOWLEDGED, DELETED, DEFERRED, PROCESSING, PROCESSED"
        );
    }

    /**
     * Converts the enum to its string representation.
     *
     * @return the status value as string
     */
    @Override
    public String toString() {
        return value;
    }
}
