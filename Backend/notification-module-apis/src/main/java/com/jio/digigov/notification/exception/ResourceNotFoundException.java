package com.jio.digigov.notification.exception;

import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

/**
 * Exception thrown when a requested resource cannot be found.
 *
 * <p>This exception supports optional metadata to provide additional context
 * about what was being searched for (e.g., eventId, businessId, tenantId).
 *
 * @author Notification Service Team
 * @since 1.0.0
 */
@Getter
public class ResourceNotFoundException extends RuntimeException {

    private final Map<String, Object> metadata;

    public ResourceNotFoundException(String message) {
        super(message);
        this.metadata = new HashMap<>();
    }

    public ResourceNotFoundException(String message, Throwable cause) {
        super(message, cause);
        this.metadata = new HashMap<>();
    }

    public ResourceNotFoundException(String message, Map<String, Object> metadata) {
        super(message);
        this.metadata = metadata != null ? new HashMap<>(metadata) : new HashMap<>();
    }

    /**
     * Add metadata to the exception for better error context.
     *
     * @param key metadata key
     * @param value metadata value
     * @return this exception for chaining
     */
    public ResourceNotFoundException addMetadata(String key, Object value) {
        this.metadata.put(key, value);
        return this;
    }

    // Static factory methods for common scenarios

    /**
     * Create exception for event not found.
     *
     * @param eventId the event ID that was not found
     * @param businessId the business ID that was searched
     * @return ResourceNotFoundException with context
     */
    public static ResourceNotFoundException eventNotFound(String eventId, String businessId) {
        String message = String.format(
            "Event not found with ID '%s' for business '%s'. Please verify the event ID and business ID combination is correct.",
            eventId, businessId);
        return new ResourceNotFoundException(message)
                .addMetadata("eventId", eventId)
                .addMetadata("businessId", businessId);
    }

    /**
     * Create exception for consent event not found.
     *
     * @param eventId the event ID that was not found
     * @param businessId the business ID that was searched
     * @param tenantId the tenant ID for context
     * @return ResourceNotFoundException with context
     */
    public static ResourceNotFoundException consentEventNotFound(String eventId, String businessId, String tenantId) {
        String message = String.format(
            "Consent deletion event not found with ID '%s' for business '%s' and tenant '%s'. Please verify the event ID, business ID, and tenant ID combination is correct.",
            eventId, businessId, tenantId);
        return new ResourceNotFoundException(message)
                .addMetadata("eventId", eventId)
                .addMetadata("businessId", businessId)
                .addMetadata("tenantId", tenantId);
    }

    /**
     * Create exception for notification not found.
     *
     * @param notificationId the notification ID that was not found
     * @param businessId the business ID that was searched
     * @return ResourceNotFoundException with context
     */
    public static ResourceNotFoundException notificationNotFound(String notificationId, String businessId) {
        String message = String.format(
            "Notification not found with ID '%s' for business '%s'.",
            notificationId, businessId);
        return new ResourceNotFoundException(message)
                .addMetadata("notificationId", notificationId)
                .addMetadata("businessId", businessId);
    }
}