package com.jio.digigov.notification.enums;

/**
 * Status enumeration for onboarding jobs.
 *
 * This enum represents the various states an onboarding job can be in during its lifecycle,
 * from initial queuing through completion or failure.
 *
 * @author Notification Service Team
 * @version 1.0
 * @since 2025-01-21
 */
public enum OnboardingJobStatus {

    /**
     * Job has been created and is waiting to start processing
     */
    QUEUED,

    /**
     * Job is currently being processed
     */
    IN_PROGRESS,

    /**
     * Job completed successfully with all operations successful
     */
    COMPLETED,

    /**
     * Job completed but some operations failed (partial success)
     */
    COMPLETED_WITH_ERRORS,

    /**
     * Job failed completely and could not be completed
     */
    FAILED,

    /**
     * Job was cancelled before completion
     */
    CANCELLED
}
