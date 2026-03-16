package com.jio.consent.exception;

import java.io.Serial;

public class ConsentException extends Exception {

    @Serial
    private static final long serialVersionUID = 1L;

    private final String errorCode;

    public ConsentException(String errorCode) {
        super("Exception Occurred");
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }

}
