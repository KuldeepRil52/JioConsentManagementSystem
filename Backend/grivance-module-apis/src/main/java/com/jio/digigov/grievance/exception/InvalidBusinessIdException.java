package com.jio.digigov.grievance.exception;

public class InvalidBusinessIdException extends RuntimeException {

    public InvalidBusinessIdException(String message) {
        super(message);
    }

    public InvalidBusinessIdException(String message, Throwable cause) {
        super(message, cause);
    }
}