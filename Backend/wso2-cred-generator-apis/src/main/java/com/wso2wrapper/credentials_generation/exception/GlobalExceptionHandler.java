package com.wso2wrapper.credentials_generation.exception;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wso2wrapper.credentials_generation.dto.response.ErrorResponse;
import com.wso2wrapper.credentials_generation.constants.ErrorCodes;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.client.HttpClientErrorException;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.prefs.Preferences;

@ControllerAdvice
public class GlobalExceptionHandler {

    private final MessageSource messageSource;

    public GlobalExceptionHandler(MessageSource messageSource) {
        this.messageSource = messageSource;
    }

    // ================== Custom API Errors ==================
    @ExceptionHandler(ApiException.class)
    public ResponseEntity<Object> handleApiException(ApiException ex) {
        String message = messageSource.getMessage(
                ex.getErrorCode(),
                ex.getArgs(),
                ex.getErrorCode(), // fallback
                LocaleContextHolder.getLocale()
        );

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("success", false);
        body.put("code", ex.getErrorCode());
        body.put("message", message);
        String timestamp = OffsetDateTime.now(ZoneOffset.UTC)
                .withNano(0)
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'"));
        body.put("timestamp", timestamp);


        return new ResponseEntity<>(body, ex.getStatus());
    }

    @ExceptionHandler(HttpClientErrorException.class)
    public ResponseEntity<ErrorResponse> handleHttpClientError(HttpClientErrorException ex) {
        String body = ex.getResponseBodyAsString();

        // Try to parse WSO2 response into a Map
        Map<String, Object> wso2Error;
        try {
            wso2Error = new ObjectMapper().readValue(body, Map.class);
        } catch (Exception e) {
            // fallback if parsing fails
            wso2Error = Map.of("message", body);
        }

        String code = (String) wso2Error.getOrDefault("code", "WSO2_ERROR");
        String message = (String) wso2Error.getOrDefault("description", wso2Error.getOrDefault("message", "Unexpected error"));

        ErrorResponse response = new ErrorResponse(code, message, Instant.now().truncatedTo(ChronoUnit.SECONDS));
        return ResponseEntity.status(ex.getStatusCode()).body(response);
    }


    // ================== Fallback for uncaught exceptions ==================
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex) {
        ex.printStackTrace(); // or log.error("Unexpected error", ex)

        // Use the actual exception message instead of a static one
        String message = ex.getMessage() != null ? ex.getMessage() : "Unexpected error occurred";

        return buildErrorResponse(
                ErrorCodes.UNKNOWN_ERROR,
                message,
                HttpStatus.INTERNAL_SERVER_ERROR
        );
    }

    // ================== Utility Method ==================
    private ResponseEntity<ErrorResponse> buildErrorResponse(String code, String message, HttpStatus status) {
        ErrorResponse response = new ErrorResponse(code, message, Instant.now().truncatedTo(ChronoUnit.SECONDS));
        return ResponseEntity.status(status).body(response);
    }
}
