package com.jio.schedular.exception;

import java.io.Serial;

public class SchedularException extends Exception {

    @Serial
    private static final long serialVersionUID = 1L;

    private final String errorCode;

    public SchedularException(String errorCode) {
        super("Exception Occurred");
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }

}
