package com.example.scanner.exception;

import com.example.scanner.constants.ErrorCodes;

public class UrlValidationException extends ScannerException {
    public UrlValidationException(String errorCodes,String userMessage, String developerDetails) {
        super(errorCodes, userMessage, developerDetails);
    }

    public UrlValidationException(String userMessage, String developerDetails, Throwable cause) {
        super(ErrorCodes.VALIDATION_ERROR, userMessage, developerDetails, cause);
    }
}