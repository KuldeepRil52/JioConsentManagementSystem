package com.jio.multitranslator.exceptions;

import lombok.Getter;

import java.io.Serial;
import java.util.List;
import java.util.Map;

@Getter
public class BodyValidationException extends Exception {


    @Serial
    private static final long serialVersionUID = 1L;

    private final List<Map<String, String>> errors;

    public BodyValidationException(List<Map<String, String>> errors) {
        super("Validation failed");
        this.errors = errors;
    }

}
