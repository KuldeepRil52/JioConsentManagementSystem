package com.example.scanner.exception;

import lombok.Getter;

@Getter
public class CategoryUpdateException extends RuntimeException {
    private final String errorCode;
    private final String userMessage;
    private final String developerDetails;

    public CategoryUpdateException(String errorCode, String userMessage, String developerDetails) {
        super(userMessage);
        this.errorCode = errorCode;
        this.userMessage = userMessage;
        this.developerDetails = developerDetails;
    }
}
