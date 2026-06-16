package com.jio.digigov.notification.exception;

import lombok.Getter;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Exception thrown when an event has an invalid type for the requested operation.
 *
 * <p>This exception is used when:
 * <ul>
 *   <li>A consent deletion API is called with a non-consent event</li>
 *   <li>An operation requires a specific event type but receives a different one</li>
 * </ul>
 *
 * @author Notification Service Team
 * @since 1.0.0
 */
@Getter
public class InvalidEventTypeException extends RuntimeException {

    private final String eventId;
    private final String actualEventType;
    private final Set<String> expectedEventTypes;
    private final Map<String, Object> metadata;

    public InvalidEventTypeException(String message, String eventId, String actualEventType, Set<String> expectedEventTypes) {
        super(message);
        this.eventId = eventId;
        this.actualEventType = actualEventType;
        this.expectedEventTypes = expectedEventTypes;
        this.metadata = new HashMap<>();
        this.metadata.put("eventId", eventId);
        this.metadata.put("actualEventType", actualEventType);
        this.metadata.put("expectedEventTypes", expectedEventTypes);
    }

    /**
     * Add additional metadata to the exception.
     *
     * @param key metadata key
     * @param value metadata value
     * @return this exception for chaining
     */
    public InvalidEventTypeException addMetadata(String key, Object value) {
        this.metadata.put(key, value);
        return this;
    }

    // Static factory methods for common scenarios

    /**
     * Create exception for consent deletion with non-consent event.
     *
     * @param eventId the event ID
     * @param actualEventType the actual event type found
     * @param businessId the business ID for context
     * @return InvalidEventTypeException with context
     */
    public static InvalidEventTypeException notConsentDeletionEvent(String eventId, String actualEventType, String businessId) {
        Set<String> expectedTypes = Set.of("CONSENT_WITHDRAWN", "CONSENT_EXPIRED");
        String message = String.format(
            "Event '%s' is not a consent deletion event. Expected event type to be one of %s but found '%s'. This API only supports consent deletion events.",
            eventId, expectedTypes, actualEventType);
        return new InvalidEventTypeException(message, eventId, actualEventType, expectedTypes)
                .addMetadata("businessId", businessId);
    }

    /**
     * Create generic exception for invalid event type.
     *
     * @param eventId the event ID
     * @param actualEventType the actual event type found
     * @param expectedEventTypes the expected event types
     * @return InvalidEventTypeException with context
     */
    public static InvalidEventTypeException forEvent(String eventId, String actualEventType, Set<String> expectedEventTypes) {
        String message = String.format(
            "Event '%s' has invalid type '%s'. Expected one of: %s.",
            eventId, actualEventType, expectedEventTypes);
        return new InvalidEventTypeException(message, eventId, actualEventType, expectedEventTypes);
    }
}
