package com.example.scanner.exception;

import com.example.scanner.constants.ErrorCodes;

public class TransactionNotFoundException extends ScannerException {
    public TransactionNotFoundException(String transactionId) {
        super(ErrorCodes.TRANSACTION_NOT_FOUND,
                "The requested scan transaction was not found",
                "No scan record exists in database for transaction ID: " + transactionId);
    }

    public TransactionNotFoundException(String transactionId, Throwable cause) {
        super(ErrorCodes.TRANSACTION_NOT_FOUND,
                "The requested scan transaction was not found",
                "No scan record exists in database for transaction ID: " + transactionId,
                cause);
    }
}