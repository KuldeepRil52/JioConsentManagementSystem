package com.example.scanner.exception;

import lombok.Getter;

@Getter
public class ConsentException extends Exception {
    private final String errorCode;
    private final String userMessage;
    private final String developerDetails;

    /**
     * Standard constructor with proper error code (MUST be R-format)
     * @param errorCode - ONLY R-format codes allowed (e.g., "R4001", "R5001")
     * @param userMessage - User-friendly message
     * @param developerDetails - Technical details for developers
     */
    public ConsentException(String errorCode, String userMessage, String developerDetails) {
        super(developerDetails);

        this.errorCode = errorCode;
        this.userMessage = userMessage;
        this.developerDetails = developerDetails;
    }

    /**
     * DEPRECATED: Legacy constructor for backwards compatibility
     * Use proper 3-parameter constructor instead
     */
    @Deprecated
    public ConsentException(String errorCode) {
        this(errorCode, "Operation failed", "No additional details provided");
    }
}