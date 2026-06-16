package com.jio.digigov.grievance.exception;

public class CustomException extends RuntimeException {

    private final Object errorCode;

    public CustomException(Object errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public Object getErrorCode() {
        return errorCode;
    }
}

