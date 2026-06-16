package com.jio.digigov.notification.exception;

/**
 * Exception thrown when template is not found
 */
public class TemplateNotFoundException extends RuntimeException {
    
    public TemplateNotFoundException(String message) {
        super(message);
    }
    
    public TemplateNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}