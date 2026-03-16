package com.example.scanner.exception;

import lombok.Getter;

@Getter
public class ScannerException extends Exception {
    private final String errorCode;
    private final String userMessage;
    private final String developerDetails;

    /**
     * Standard constructor - ALWAYS use this parameter order
     * @param errorCode - Structured identifier (e.g., "R4001")
     * @param userMessage - User-friendly message
     * @param developerDetails - Technical details for developers
     */
    public ScannerException(String errorCode, String userMessage, String developerDetails) {
        super(developerDetails); // Technical details go to getMessage()
        this.errorCode = errorCode;
        this.userMessage = userMessage;
        this.developerDetails = developerDetails;
    }

    /**
     * Constructor with cause
     */
    public ScannerException(String errorCode, String userMessage, String developerDetails, Throwable cause) {
        super(developerDetails, cause);
        this.errorCode = errorCode;
        this.userMessage = userMessage;
        this.developerDetails = developerDetails;
    }

}