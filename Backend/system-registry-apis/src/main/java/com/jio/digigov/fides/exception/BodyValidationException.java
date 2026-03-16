package com.jio.digigov.fides.exception;

public class BodyValidationException extends RuntimeException {

    private final String errorCode;

    public BodyValidationException(String errorCode) {
        super(errorCode);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}