package com.jio.digigov.fides.exception;

/**
 * Exception thrown for business logic errors
 */
public class BusinessException extends RuntimeException {

    private final String errorCode;

    public BusinessException(String errorCode) {
        super(errorCode);
        this.errorCode = errorCode;
    }

    public BusinessException(String errorCode, Throwable cause) {
        super(errorCode, cause);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}

