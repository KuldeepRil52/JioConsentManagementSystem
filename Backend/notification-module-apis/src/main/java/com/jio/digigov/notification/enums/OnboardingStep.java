package com.jio.digigov.notification.enums;

/**
 * Processing step enumeration for onboarding jobs.
 *
 * This enum represents the sequential steps executed during the onboarding process.
 * Each step represents a distinct phase of the onboarding workflow.
 *
 * @author Notification Service Team
 * @version 1.0
 * @since 2025-01-21
 */
public enum OnboardingStep {

    /**
     * Initial setup and validation phase
     */
    INITIALIZE,

    /**
     * Creating notification configuration for the tenant
     */
    CREATE_NOTIFICATION_CONFIG,

    /**
     * Extracting master labels from template definitions
     */
    EXTRACT_LABELS,

    /**
     * Creating master list configuration in the database
     */
    CREATE_MASTER_LIST,

    /**
     * Creating notification templates (SMS and Email)
     */
    CREATE_TEMPLATES,

    /**
     * Creating event configurations for notification routing
     */
    CREATE_EVENT_CONFIGS,

    /**
     * Final cleanup and status update
     */
    FINALIZE
}
