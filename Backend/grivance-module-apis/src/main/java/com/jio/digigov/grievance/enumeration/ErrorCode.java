package com.jio.digigov.grievance.enumeration;

public enum ErrorCode {

    INVALID_REQUEST("Invalid request"),
    INTERNAL_ERROR("Internal server error"),
    UNAUTHORIZED("Unauthorized request");

    private final String message;

    ErrorCode(String message) {
        this.message = message;
    }

    public String message() {
        return message;
    }
}

