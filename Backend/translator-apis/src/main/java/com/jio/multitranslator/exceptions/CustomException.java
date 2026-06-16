package com.jio.multitranslator.exceptions;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class CustomException extends RuntimeException {

    private final HttpStatus httpStatus;
    private final String errorMessage;

    public CustomException(String message, HttpStatus httpStatus, String errorMessage) {
        super(message);
        this.httpStatus = httpStatus;
        this.errorMessage = errorMessage;
    }


}
