package com.example.scanner.exception;

import com.example.scanner.constants.ErrorCodes;

public class CookieNotFoundException extends ScannerException {
    public CookieNotFoundException(String cookieName, String transactionId) {
        super(ErrorCodes.NOT_FOUND,
                "The requested cookie was not found in this scan",
                "Cookie '" + cookieName + "' not found in transaction: " + transactionId);
    }

}