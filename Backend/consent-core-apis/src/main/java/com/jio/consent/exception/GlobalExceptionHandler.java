package com.jio.consent.exception;

import com.jio.consent.constant.Constants;
import com.jio.consent.constant.ErrorCodes;
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
        errorResponse.put("timestamp", LocalDateTime.now().toString());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        return new ResponseEntity<>(errorResponse, headers, status);
    }

    /** Default fallback for any other exceptions */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleGenericException(Exception ex) {
        logger.error("Unhandled Exception: {}", ex.getMessage(), ex);
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
                    detail.put("errorMessage", messageSource.getMessage(errorCode, null, LocaleContextHolder.getLocale()));
                    return detail;
                })
                .collect(Collectors.toList());

        logger.error("Method Argument Validation Failed - HTTP Status: {}, Errors: {}", HttpStatus.BAD_REQUEST.value(), errors, ex);
        
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

        logger.error("Constraint Violation Exception - HTTP Status: {}, Errors: {}", HttpStatus.BAD_REQUEST.value(), errors, ex);
        
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
                })
                .collect(Collectors.toList());

        HttpStatus httpStatus = HttpStatus.BAD_REQUEST;
        if (enrichedErrors.size() == 1) {
            if (enrichedErrors.getFirst().get(Constants.ERROR_CODE).toString().equals(ErrorCodes.JCMP1051)) {
                httpStatus = HttpStatus.CONFLICT;
            }
        }
        
        logger.error("Body Validation Exception - HTTP Status: {}, Errors: {}", httpStatus.value(), enrichedErrors, ex);

        Map<String, Object> errorResponse = new LinkedHashMap<>();
        errorResponse.put("errors", enrichedErrors); // Use the enriched list of maps
        errorResponse.put("timestamp", LocalDateTime.now().toString());

        return new ResponseEntity<>(errorResponse, httpStatus);
    }

    @ExceptionHandler(ConsentException.class)
    public ResponseEntity<Object> handleConsentException(ConsentException ex) {
        String errorCode = ex.getErrorCode();
        List<Map<String, Object>> errors = new ArrayList<>();
        Map<String, Object> errorDetail = new HashMap<>();
        errorDetail.put("errorCode", errorCode);
        errorDetail.put("errorMessage", this.messageSource.getMessage(
                errorCode,
                null,
                LocaleContextHolder.getLocale()));

        errors.add(errorDetail);
        
        HttpStatus httpStatus = HttpStatus.INTERNAL_SERVER_ERROR;
        if (errorCode.equals(ErrorCodes.JCMP3001) || errorCode.equals(ErrorCodes.JCMP3002) || errorCode.equals(ErrorCodes.JCMP3003)) {
            httpStatus = HttpStatus.NOT_FOUND;
        } else if (errorCode.equals(ErrorCodes.JCMP3004) || errorCode.equals(ErrorCodes.JCMP3006)) {
            httpStatus = HttpStatus.BAD_REQUEST;
        }
        
        logger.error("Consent Exception - Error Code: {}, HTTP Status: {}, Error Message: {}", errorCode, httpStatus.value(), errorDetail.get("errorMessage"), ex);
        
        return new ResponseEntity<>(errors, httpStatus);
    }

    /**
     * 1. Method Not Allowed (405)
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<Object> handleMethodNotAllowed(HttpRequestMethodNotSupportedException ex) {
        logger.error("HTTP Method Not Allowed - Error Code: {}, HTTP Status: {}, Message: {}", ErrorCodes.JCMP5001, HttpStatus.METHOD_NOT_ALLOWED.value(), ex.getMessage(), ex);
        return buildResponse(HttpStatus.METHOD_NOT_ALLOWED, ErrorCodes.JCMP5001, ex.getMessage());
    }

    /** 2. Unsupported Media Type (415) */
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<Object> handleUnsupportedMediaType(HttpMediaTypeNotSupportedException ex) {
        logger.error("Unsupported Media Type - Error Code: {}, HTTP Status: {}, Message: {}", ErrorCodes.JCMP5002, HttpStatus.UNSUPPORTED_MEDIA_TYPE.value(), ex.getMessage(), ex);
        return buildResponse(HttpStatus.UNSUPPORTED_MEDIA_TYPE, ErrorCodes.JCMP5002, ex.getMessage());
    }

    /** 3. Missing Request Parameter (400) */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<Object> handleMissingParameter(MissingServletRequestParameterException ex) {
        logger.error("Missing Request Parameter - Error Code: {}, HTTP Status: {}, Message: {}", ErrorCodes.JCMP5003, HttpStatus.BAD_REQUEST.value(), ex.getMessage(), ex);
        return buildResponse(HttpStatus.BAD_REQUEST, ErrorCodes.JCMP5003, ex.getMessage());
    }

    /** 4. Invalid JSON or unreadable body (400) */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Object> handleMessageNotReadable(HttpMessageNotReadableException ex) {
        logger.error("Unreadable Request Body - Error Code: {}, HTTP Status: {}, Message: {}", ErrorCodes.JCMP5004, HttpStatus.BAD_REQUEST.value(), ex.getMessage(), ex);
        return buildResponse(HttpStatus.BAD_REQUEST, ErrorCodes.JCMP5004, "Invalid request body format: error" + ex.getMessage());
    }


}

