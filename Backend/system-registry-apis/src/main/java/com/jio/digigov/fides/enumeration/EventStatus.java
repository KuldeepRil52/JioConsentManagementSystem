package com.jio.digigov.fides.enumeration;

/**
 * Enumeration for Event processing status
 */
public enum EventStatus {
    PENDING,        // Event received, pending processing
    PROCESSING,     // Event is being processed
    COMPLETED,      // Event processing completed successfully
    PARTIALLY_FAILED, // Some notifications failed
    FAILED          // Event processing failed
}