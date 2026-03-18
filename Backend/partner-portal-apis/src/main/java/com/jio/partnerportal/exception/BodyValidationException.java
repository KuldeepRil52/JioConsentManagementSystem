package com.jio.partnerportal.exception;

import java.util.List;
import java.util.Map;

public class BodyValidationException extends Exception {

    private static final long serialVersionUID = 1L;

    private final List<Map<String, Object>> errors;

    public BodyValidationException(List<Map<String, Object>> errors) {
        super("Validation failed");
        this.errors = errors;
    }

    public List<Map<String, Object>> getErrors() {
        return errors;
    }

}
