package com.example.scanner.exception;

import com.example.scanner.constants.ErrorCodes;

public class ScanExecutionException extends ScannerException {
    public ScanExecutionException(String developerDetails) {
        super(ErrorCodes.SCAN_EXECUTION_ERROR,
                "Unable to complete the scan. Please try again later",
                developerDetails);
    }

    public ScanExecutionException(String developerDetails, Throwable cause) {
        super(ErrorCodes.SCAN_EXECUTION_ERROR,
                "Unable to complete the scan. Please try again later",
                developerDetails,
                cause);
    }
}
