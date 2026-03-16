package com.jio.digigov.auditmodule.exception;

/**
 * Exception thrown when request validation fails
 */
public class ValidationException extends RuntimeException {

    public ValidationException(String message) {
        super(message);
    }

    public ValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}
