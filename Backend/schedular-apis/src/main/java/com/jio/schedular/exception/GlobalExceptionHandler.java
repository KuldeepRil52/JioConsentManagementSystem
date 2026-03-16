package com.jio.schedular.exception;

import com.jio.schedular.constant.Constants;
import com.jio.schedular.constant.ErrorCodes;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
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
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * This class is used to handle common exceptions
 *
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
        errorResponse.put(Constants.TIMESTAMP, LocalDateTime.now().toString());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        return new ResponseEntity<>(errorResponse, headers, status);
    }

    /** Default fallback for any other exceptions */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleGenericException(Exception ex) {
        logger.error("Unhandled exception occurred");
        // Stack trace only in debug logs
        logger.debug("Unhandled Exception: {}", ex.getMessage(), ex);
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, ErrorCodes.JCMP0001, ex.getMessage());
    }

    /** Bean Validation: @Valid on @RequestBody DTOs */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex) {
        List<Map<String, Object>> errors = ex.getBindingResult()
                .getAllErrors()
                .stream()
                .map(error -> {
                    String errorCode = error.getDefaultMessage();
                    String fieldName = (error instanceof FieldError) ? ((FieldError) error).getField() : error.getObjectName();
                    Map<String, Object> detail = new LinkedHashMap<>();
                    detail.put(Constants.ERROR_CODE, errorCode);
                    detail.put(Constants.PARAMETER, fieldName);
                    detail.put(Constants.ERROR_MESSAGE, messageSource.getMessage(errorCode, null, LocaleContextHolder.getLocale()));
                    return detail;
                })
                .collect(Collectors.toList());

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("errors", errors);
        body.put("timestamp", LocalDateTime.now().toString());
        return new ResponseEntity<>(body, HttpStatus.BAD_REQUEST);
    }

    /** Bean Validation: @Validated on params/path/query producing constraint violations */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Object> handleConstraintViolation(ConstraintViolationException ex) {
        List<Map<String, Object>> errors = ex.getConstraintViolations()
                .stream()
                .map(violation -> buildViolationMap(violation))
                .collect(Collectors.toList());

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("errors", errors);
        body.put("timestamp", LocalDateTime.now().toString());
        return new ResponseEntity<>(body, HttpStatus.BAD_REQUEST);
    }

    private Map<String, Object> buildViolationMap(ConstraintViolation<?> violation) {
        String errorCode = violation.getMessage();
        String parameter = violation.getPropertyPath() != null ? violation.getPropertyPath().toString() : null;
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put(Constants.ERROR_CODE, errorCode);
        if (parameter != null) {
            detail.put(Constants.PARAMETER, parameter);
        }
        detail.put("errorMessage", messageSource.getMessage(errorCode, null, LocaleContextHolder.getLocale()));
        return detail;
    }


    @ExceptionHandler(SchedularException.class)
    public ResponseEntity<Object> handleSchedularException(SchedularException ex) {
        String errorCode = ex.getErrorCode();
        List<Map<String, Object>> errors = new ArrayList<>();
        Map<String, Object> errorDetail = new HashMap<>();
        errorDetail.put("errorCode", errorCode);
        errorDetail.put("errorMessage", this.messageSource.getMessage(
                errorCode,
                null,
                LocaleContextHolder.getLocale()));

        errors.add(errorDetail);
        if (errorCode.equals(ErrorCodes.JCMP3001) || errorCode.equals(ErrorCodes.JCMP3002) || errorCode.equals(ErrorCodes.JCMP3003)) {
            return new ResponseEntity<>(errors, HttpStatus.NOT_FOUND);
        }
        if(errorCode.equals(ErrorCodes.JCMP3004) || errorCode.equals(ErrorCodes.JCMP3005)) {
            return new ResponseEntity<>(errors, HttpStatus.BAD_REQUEST);
        }
        return new ResponseEntity<>(errors, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    /**
     * 1. Method Not Allowed (405)
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<Object> handleMethodNotAllowed(HttpRequestMethodNotSupportedException ex) {
        logger.warn("HTTP Method Not Allowed: {}", ex.getMessage());
        return buildResponse(HttpStatus.METHOD_NOT_ALLOWED, ErrorCodes.JCMP4001, ex.getMessage());
    }

    /** 2. Unsupported Media Type (415) */
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<Object> handleUnsupportedMediaType(HttpMediaTypeNotSupportedException ex) {
        logger.warn("Unsupported Media Type: {}", ex.getMessage());
        return buildResponse(HttpStatus.UNSUPPORTED_MEDIA_TYPE, ErrorCodes.JCMP4002, ex.getMessage());
    }

    /** 3. Missing Request Parameter (400) */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<Object> handleMissingParameter(MissingServletRequestParameterException ex) {
        logger.warn("Missing request parameter: {}", ex.getMessage());
        return buildResponse(HttpStatus.BAD_REQUEST, ErrorCodes.JCMP4003, ex.getMessage());
    }

    /** 4. Invalid JSON or unreadable body (400) */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Object> handleMessageNotReadable(HttpMessageNotReadableException ex) {
        logger.warn("Unreadable request body: {}", ex.getMessage());
        return buildResponse(HttpStatus.BAD_REQUEST, ErrorCodes.JCMP4004, "Invalid request body format: error" + ex.getMessage());
    }
}