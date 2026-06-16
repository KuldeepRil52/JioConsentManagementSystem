package com.jio.digigov.notification.exception;

import lombok.Getter;

/**
 * Custom exception for onboarding-related errors.
 *
 * This exception is thrown when onboarding operations encounter errors such as:
 * - Prerequisites validation failures (existing templates/configs)
 * - Job creation failures
 * - Processing errors during async execution
 *
 * @author Notification Service Team
 * @version 1.0
 * @since 2025-01-21
 */
@Getter
public class OnboardingException extends RuntimeException {

    /**
     * JDNM error code for standardized error responses
     */
    private final String errorCode;

    /**
     * Constructs a new onboarding exception with the specified error code and message.
     *
     * @param errorCode The JDNM error code (e.g., "JDNM1001")
     * @param message   The detailed error message
     */
    public OnboardingException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    /**
     * Constructs a new onboarding exception with error code, message, and cause.
     *
     * @param errorCode The JDNM error code (e.g., "JDNM1001")
     * @param message   The detailed error message
     * @param cause     The underlying cause of the exception
     */
    public OnboardingException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }
}
