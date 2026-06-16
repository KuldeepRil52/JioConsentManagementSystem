package com.jio.digigov.notification.exception;

/**
 * Exception thrown when a callback URL cannot be found for a recipient.
 *
 * This exception is used in the callback notification processing pipeline
 * when the system fails to resolve callback URLs for Data Fiduciaries or
 * Data Processors based on their recipient type and identifier.
 *
 * Common scenarios:
 * - Data Fiduciary configuration missing callback URL in NGConfiguration
 * - Data Processor missing or invalid callback URL in DataProcessor collection
 * - Invalid recipient type that cannot be resolved
 *
 * @author Notification Service Team
 * @version 1.0
 * @since 2024-01-01
 */
public class CallbackUrlNotFoundException extends RuntimeException {

    /**
     * Constructs a new CallbackUrlNotFoundException with the specified detail message.
     *
     * @param message the detail message explaining the reason for the exception
     */
    public CallbackUrlNotFoundException(String message) {
        super(message);
    }

    /**
     * Constructs a new CallbackUrlNotFoundException with the specified detail message and cause.
     *
     * @param message the detail message explaining the reason for the exception
     * @param cause the cause of the exception
     */
    public CallbackUrlNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}