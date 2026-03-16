package com.jio.digigov.grievance.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.MediaType;

import java.time.LocalDateTime;

/**
 * Utility class to build standardized API error responses.
 */
public class ErrorResponseBuilder {

    private ErrorResponseBuilder() {
        // Utility class — prevent instantiation
    }

    public static ResponseEntity<Object> buildErrorResponse(
            String errorCode,
            String errorMessage,
            HttpStatus status,
            String transactionId) {

        ApiErrorResponse response = ApiErrorResponse.builder()
                .transactionId(transactionId)
                .status(status.value())
                .errorCode(errorCode)
                .errorMessage(errorMessage)
                .timestamp(LocalDateTime.now())
                .build();

        return ResponseEntity
                .status(status)
                .contentType(MediaType.APPLICATION_JSON)
                .body(response);
    }
}