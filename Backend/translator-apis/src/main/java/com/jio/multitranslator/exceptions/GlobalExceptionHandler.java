package com.jio.multitranslator.exceptions;

import com.jio.multitranslator.constant.Constants;
import com.jio.multitranslator.constant.ErrorCodes;
import com.jio.multitranslator.dto.request.TranslateRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.*;
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

@ControllerAdvice
public class GlobalExceptionHandler {

    private final MessageSource messageSource;
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

    @ExceptionHandler(TranslateException.class)
    public ResponseEntity<Object> handleCustomApiException(TranslateException ex) {
        logger.debug("Exception from external API: {}", ex.getMessage(), ex);
        return buildResponse(ex.getHttpStatus(), ErrorCodes.JCMPT025, ex.getErrorMessage());
    }

    @ExceptionHandler(CustomException.class)
    public ResponseEntity<Object> handleCustomException(CustomException ex) {
        logger.debug("Error : {}", ex.getMessage(), ex);
        return buildResponse(ex.getHttpStatus(), ErrorCodes.JCMPT034, ex.getErrorMessage());
    }

    /** Default fallback for any other exceptions */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleGenericException(Exception ex) {
        logger.debug("Unhandled Exception: {}", ex.getMessage(), ex);
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, ErrorCodes.JCMPT025, ex.getMessage());
    }

    /** Bean Validation: @Valid on @RequestBody DTOs */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex) {

        Object target = ex.getBindingResult().getTarget();
        // Check if target is TranslateRequest instance (not by equality, but by class)
        if (target instanceof TranslateRequest) {
            Map<String, Object> errorBody = Map.of(
                    Constants.ERRORS, List.of(Map.of(
                            Constants.ERROR_CODE, "JCMPT036",
                            Constants.PARAMETER, "translateRequest",
                            Constants.ERROR_MESSAGE, "Request body cannot be empty"
                    )),
                    Constants.TIMESTAMP, LocalDateTime.now().toString()
            );
            return new ResponseEntity<>(errorBody, HttpStatus.BAD_REQUEST);
        }

        List<Map<String, Object>> errors = ex.getBindingResult()
                .getAllErrors()
                .stream()
                .map(error -> {
                    String errorCode = error.getDefaultMessage();
                    String fieldName = (error instanceof FieldError fieldError) ? fieldError.getField() : error.getObjectName();
                    Map<String, Object> detail = new LinkedHashMap<>();
                    detail.put(Constants.ERROR_CODE, errorCode);
                    detail.put(Constants.PARAMETER, fieldName);
                    detail.put(Constants.ERROR_MESSAGE, messageSource.getMessage(errorCode, null, LocaleContextHolder.getLocale()));
                    return detail;
                })
                .toList();

        Map<String, Object> body = new LinkedHashMap<>();
        body.put(Constants.ERRORS, errors);
        body.put(Constants.TIMESTAMP, LocalDateTime.now().toString());
        return new ResponseEntity<>(body, HttpStatus.BAD_REQUEST);
    }

    /** Bean Validation: @Validated on params/path/query producing constraint violations */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Object> handleConstraintViolation(ConstraintViolationException ex) {
        List<Map<String, Object>> errors = ex.getConstraintViolations()
                .stream()
                .map(this::buildViolationMap)
                .toList();

        Map<String, Object> body = new LinkedHashMap<>();
        body.put(Constants.ERRORS, errors);
        body.put(Constants.TIMESTAMP, LocalDateTime.now().toString());
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

    @ExceptionHandler(BodyValidationException.class)
    public ResponseEntity<Object> handleBodyValidationException(BodyValidationException ex) {
        List<Map<String, String>> errors = ex.getErrors();

        List<Map<String, String>> enrichedErrors = errors.stream()
                .map(errorDetail -> {
                    String errorCode = errorDetail.get(Constants.ERROR_CODE);
                    String errorMessage = messageSource.getMessage(
                            errorCode,
                            null,
                            LocaleContextHolder.getLocale()
                    );
                    Map<String, String> enrichedDetail = new LinkedHashMap<>(errorDetail);
                    enrichedDetail.put(Constants.ERROR_MESSAGE, errorMessage);
                    return enrichedDetail;
                })
                .toList();

        Map<String, Object> errorResponse = new LinkedHashMap<>();
        errorResponse.put(Constants.ERRORS, enrichedErrors);
        errorResponse.put(Constants.TIMESTAMP, LocalDateTime.now().toString());

        if ( (enrichedErrors.size() == 1) && (ErrorCodes.JCMPT026.equals(enrichedErrors.getFirst().get(Constants.ERROR_CODE))) ) {
                return new ResponseEntity<>(errorResponse, HttpStatus.CONFLICT);
        }
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    /**
     * 1. Method Not Allowed (405)
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<Object> handleMethodNotAllowed(HttpRequestMethodNotSupportedException ex) {
        logger.warn("HTTP Method Not Allowed: {}", ex.getMessage());
        return buildResponse(HttpStatus.METHOD_NOT_ALLOWED, ErrorCodes.JCMPT026, ex.getMessage());
    }

    /** 2. Unsupported Media Type (415) */
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<Object> handleUnsupportedMediaType(HttpMediaTypeNotSupportedException ex) {
        logger.warn("Unsupported Media Type: {}", ex.getMessage());
        return buildResponse(HttpStatus.UNSUPPORTED_MEDIA_TYPE, ErrorCodes.JCMPT027, ex.getMessage());
    }

    /** 3. Missing Request Parameter (400) */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<Object> handleMissingParameter(MissingServletRequestParameterException ex) {
        logger.warn("Missing request parameter: {}", ex.getMessage());
        return buildResponse(HttpStatus.BAD_REQUEST, ErrorCodes.JCMPT028, ex.getMessage());
    }

    /** 4. Invalid JSON or unreadable body (400) */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Object> handleMessageNotReadable(HttpMessageNotReadableException ex) {
        logger.warn("Unreadable request body: {}", ex.getMessage());

        String message = ex.getMessage();
        String errorMessage;

        if (message != null && message.contains("no content to map")) {
            // Empty body (e.g. {})
            errorMessage = "Request body cannot be empty";
        } else {
            // Malformed JSON or unreadable body
            errorMessage = "Invalid JSON format or unreadable request body";
        }

        return buildResponse(HttpStatus.BAD_REQUEST, ErrorCodes.JCMPT029, errorMessage);
    }

}
