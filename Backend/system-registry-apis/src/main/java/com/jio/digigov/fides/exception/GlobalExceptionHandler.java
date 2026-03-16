package com.jio.digigov.fides.exception;

import com.jio.digigov.fides.constant.ErrorCodes;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.http.converter.HttpMessageNotReadableException;

@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

        private MessageSource messageSource;

        public GlobalExceptionHandler(MessageSource messageSource) {
            this.messageSource = messageSource;
        }

    // 🔹 @Valid / @NotBlank / @NotNull
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationExceptions(
            MethodArgumentNotValidException ex) {

        FieldError fieldError = ex.getBindingResult().getFieldError();

        String message = fieldError != null
                ? messageSource.getMessage(fieldError.getDefaultMessage(), null, LocaleContextHolder.getLocale())
                : "Invalid request";
                
        ErrorResponse response = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                "Validation Failed",
                message,
                fieldError.getDefaultMessage()
        );

        return ResponseEntity.badRequest().body(response);
    }

    // 🔹 Malformed JSON / enum parse
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleInvalidJson(
            HttpMessageNotReadableException ex) {

        ErrorResponse response = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                "Malformed Request",
                ex.getMostSpecificCause().getMessage(),
                null
        );

        return ResponseEntity.badRequest().body(response);
    }

    // 🔹 Business validation
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusinessException(
            BusinessException ex) {

        ErrorResponse response = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                "Business Validation Failed",
                ex.getMessage(),
                ex.getErrorCode()
        );

        return ResponseEntity.badRequest().body(response);
    }

    // 🔹 Header / body validation (Tenant, Business, etc.)
    @ExceptionHandler(BodyValidationException.class)
    public ResponseEntity<ErrorResponse> handleBodyValidationException(
            BodyValidationException ex) {

        ErrorResponse response = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                "Validation Failed",
                ex.getMessage(),
                ex.getErrorCode()
        );

        return ResponseEntity.badRequest().body(response);
    }

    // 🔹 Illegal argument
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(
            IllegalArgumentException ex) {

        ErrorResponse response = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                "Invalid Request",
                ex.getMessage(),
                ErrorCodes.JCMP0001
        );

        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<ErrorResponse> handleMissingRequestHeaderException(
            MissingRequestHeaderException ex) {

        ErrorResponse response = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                "Invalid Request",
                ex.getMessage(),
                ErrorCodes.JCMP2001
        );

        return ResponseEntity.badRequest().body(response);
    }

    // 🔹 Fallback
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex) {
        log.error(String.valueOf(ex.getClass()));
        ErrorResponse response = new ErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "Internal Server Error",
                ex.getMessage(),
                ErrorCodes.JCMP0001
        );

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}