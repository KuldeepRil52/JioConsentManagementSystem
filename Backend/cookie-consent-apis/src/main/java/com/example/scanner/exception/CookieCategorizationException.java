package com.example.scanner.exception;

import com.example.scanner.constants.ErrorCodes;

public class CookieCategorizationException extends ScannerException {
    public CookieCategorizationException(String developerDetails) {
        super(ErrorCodes.CATEGORIZATION_ERROR,
                "Cookie categorization is temporarily unavailable. Your scan will continue without categorization",
                developerDetails);
    }

    public CookieCategorizationException(String developerDetails, Throwable cause) {
        super(ErrorCodes.CATEGORIZATION_ERROR,
                "Cookie categorization is temporarily unavailable. Your scan will continue without categorization",
                developerDetails,
                cause);
    }
}