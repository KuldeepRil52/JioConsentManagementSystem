package com.jio.partnerportal.exception;

import com.jio.partnerportal.constant.Constants;
import com.jio.partnerportal.constant.ErrorCodes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.time.LocalDateTime;
import java.util.*;

/**
 * This class is used to handle common exceptions
 *
 * @author Kirte.Bhatt
 *
 */

@ControllerAdvice
public class GlobalExceptionHandler {

    private MessageSource messageSource;

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @Autowired
    public GlobalExceptionHandler(MessageSource messageSource) {
        this.messageSource = messageSource;
    }

    /**
     * Utility method for consistent JSON response
     */
    private ResponseEntity<Object> buildResponse(HttpStatus status, String errorCode, String errorMsg) {
        Map<String, Object> errorResponse = new LinkedHashMap<>();
        errorResponse.put("status", status.value());
        errorResponse.put("errorCd", errorCode);
        errorResponse.put("errorMsg", errorMsg);
        errorResponse.put("timestamp", LocalDateTime.now().toString());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        return new ResponseEntity<>(errorResponse, headers, status);
    }

    /**
     * Default fallback for any other exceptions
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleGenericException(Exception ex) {
        logger.error("Unhandled Exception", ex);
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, ErrorCodes.JCMP0001, ex.getMessage());
    }

    @ExceptionHandler(BodyValidationException.class)
    public ResponseEntity<Object> handleBodyValidationException(BodyValidationException ex) {
        List<Map<String, Object>> errors = ex.getErrors();

        List<Map<String, Object>> enrichedErrors = errors.stream()
                .map(errorDetail -> {
                    String errorCode = (String) errorDetail.get("errorCode"); // Get the error code
                    String errorMessage = messageSource.getMessage(
                            errorCode,
                            null,
                            LocaleContextHolder.getLocale()
                    );
                    errorDetail.put("errorMessage", errorMessage); // Add the new field
                    return errorDetail;
                }).toList();

        Map<String, Object> errorResponse = new LinkedHashMap<>();
        errorResponse.put("errors", enrichedErrors); // Use the enriched list of maps
        errorResponse.put("timestamp", LocalDateTime.now().toString());

        if (enrichedErrors.size() == 1 && enrichedErrors.getFirst().get(Constants.ERROR_CODE).toString().equals(ErrorCodes.JCMP1003)) {
            return new ResponseEntity<>(errorResponse, HttpStatus.CONFLICT);
        }
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(PartnerPortalException.class)
    public ResponseEntity<Object> handlePartnerPortalException(PartnerPortalException ex) {
        String errorCode = ex.getErrorCode();
        List<Map<String, Object>> errors = new ArrayList<>();
        Map<String, Object> errorDetail = new HashMap<>();
        errorDetail.put("errorCode", errorCode);
        errorDetail.put("errorMessage", this.messageSource.getMessage(
                errorCode,
                null,
                LocaleContextHolder.getLocale()));

        errors.add(errorDetail);
        if (errorCode.equals(ErrorCodes.JCMP3001)) {
            return new ResponseEntity<>(errors, HttpStatus.NOT_FOUND);
        } else if (errorCode.equals(ErrorCodes.JCMP1009) || errorCode.equals(ErrorCodes.JCMP3046) || errorCode.equals(ErrorCodes.JCMP3076)) {
            return new ResponseEntity<>(errors, HttpStatus.BAD_REQUEST);
        } else if (errorCode.equals(ErrorCodes.JCMP4001) || errorCode.equals(ErrorCodes.JCMP4002) || errorCode.equals(ErrorCodes.JCMP4003)) {
            return new ResponseEntity<>(errors, HttpStatus.UNAUTHORIZED);
        } else if (errorCode.equals(ErrorCodes.JCMP3059) || errorCode.equals(ErrorCodes.JCMP3060) || errorCode.equals(ErrorCodes.JCMP3061) ||
                   errorCode.equals(ErrorCodes.JCMP3062) || errorCode.equals(ErrorCodes.JCMP3063) || errorCode.equals(ErrorCodes.JCMP3064) ||
                   errorCode.equals(ErrorCodes.JCMP3065) || errorCode.equals(ErrorCodes.JCMP3066) || errorCode.equals(ErrorCodes.JCMP3067) ||
                   errorCode.equals(ErrorCodes.JCMP3068) || errorCode.equals(ErrorCodes.JCMP3069) || errorCode.equals(ErrorCodes.JCMP3070) ||
                   errorCode.equals(ErrorCodes.JCMP3071)) {
            return new ResponseEntity<>(errors, HttpStatus.CONFLICT);
        } else {
            return new ResponseEntity<>(errors, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * 1. Method Not Allowed (405)
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<Object> handleMethodNotAllowed(HttpRequestMethodNotSupportedException ex) {
        logger.warn("HTTP Method Not Allowed: {}", ex.getMessage());
        return buildResponse(HttpStatus.METHOD_NOT_ALLOWED, ErrorCodes.JCMP6001, ex.getMessage());
    }

    /**
     * 2. Unsupported Media Type (415)
     */
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<Object> handleUnsupportedMediaType(HttpMediaTypeNotSupportedException ex) {
        logger.warn("Unsupported Media Type: {}", ex.getMessage());
        return buildResponse(HttpStatus.UNSUPPORTED_MEDIA_TYPE, ErrorCodes.JCMP6002, ex.getMessage());
    }

    /**
     * 3. Missing Request Parameter (400)
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<Object> handleMissingParameter(MissingServletRequestParameterException ex) {
        logger.warn("Missing request parameter: {}", ex.getMessage());
        return buildResponse(HttpStatus.BAD_REQUEST, ErrorCodes.JCMP6003, ex.getMessage());
    }

    /**
     * 4. Invalid JSON or unreadable body (400)
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Object> handleMessageNotReadable(HttpMessageNotReadableException ex) {
        logger.warn("Unreadable request body: {}", ex.getMessage());
        return buildResponse(HttpStatus.BAD_REQUEST, ErrorCodes.JCMP6004, "Invalid request body format");
    }

    /**
     * 5. Mongo Exception (500)
     */
    @ExceptionHandler(com.mongodb.MongoException.class)
    public ResponseEntity<Object> handleMongoException(com.mongodb.MongoException ex) {
        logger.error("MongoDB Exception: {}", ex.getMessage(), ex);
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, ErrorCodes.JCMP6005, "Database error occurred");
    }

    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<Object> handleNoHandlerFound(NoHandlerFoundException ex) {
        logger.warn("No endpoint found: {}", ex.getRequestURL());
        return buildResponse(HttpStatus.NOT_FOUND, ErrorCodes.JCMP6006, "No endpoint found for the requested URL: " + ex.getRequestURL());
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Object> handleResponseStatus(ResponseStatusException ex, Locale locale) {
        HttpStatus status = (HttpStatus) ex.getStatusCode();

        // Treat ex.getReason() as the error code
        String errorCode = ex.getReason() != null ? ex.getReason() : "JCMP0001";

        // Fetch corresponding message from messages.properties
        String errorMsg = messageSource.getMessage(errorCode, null, "Unexpected error", locale);

        // Logging
        switch (status) {
            case NOT_FOUND -> logger.warn("Resource Not Found: {} / {}", errorCode, errorMsg);
            case BAD_REQUEST -> logger.warn("Bad Request: {} / {}", errorCode, errorMsg);
            case UNAUTHORIZED -> logger.warn("Unauthorized: {} / {}", errorCode, errorMsg);
            default -> logger.error("Unexpected ResponseStatusException: {} / {}", errorCode, errorMsg);
        }

        return buildResponse(status, errorCode, errorMsg);
    }


}

