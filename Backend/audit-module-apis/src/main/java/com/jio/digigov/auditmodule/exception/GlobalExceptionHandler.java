package com.jio.digigov.auditmodule.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    //  404 – Business exception
    @ExceptionHandler(AuditNotFoundException.class)
    public ResponseEntity<?> handleNotFound(AuditNotFoundException ex) {

        log.info("Audit not found");

        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", "Requested audit record not found"));
    }

    //  400 – Missing headers
    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<?> handleMissingHeader(MissingRequestHeaderException ex) {

        log.debug("Missing request header: {}", ex.getHeaderName());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of(
                        "status", "failure",
                        "message", "Missing required header: " + ex.getHeaderName()
                ));
    }

    //  400 – Validation errors (field validation)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<?> handleValidation(MethodArgumentNotValidException ex) {

        log.debug("Validation error occurred");

        Map<String, String> fieldErrors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .collect(Collectors.toMap(
                        err -> err.getField(),
                        err -> err.getDefaultMessage(),
                        (msg1, msg2) -> msg1
                ));

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of(
                        "status", "failure",
                        "message", "Validation failed",
                        "errors", fieldErrors
                ));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleGeneric(Exception ex) {

        // DO NOT log stack trace directly in production
        log.error("Unexpected internal error occurred");

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Something went wrong. Please contact support."));
    }
}