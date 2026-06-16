package com.jio.vault.exception;

import com.jio.vault.constants.ErrorCode;

public class CustomException extends RuntimeException {

    private final String code;

    public CustomException(String code, String message) {
        super(message);
        this.code = code;
    }

    public CustomException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.code = errorCode.getCode();
    }
    public CustomException(ErrorCode errorCode, String message) {
        super(message);
        this.code = errorCode.getCode();
    }

    public String getCode() {
        return code;
    }
}
