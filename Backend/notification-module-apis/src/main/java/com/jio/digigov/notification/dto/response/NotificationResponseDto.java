package com.jio.digigov.notification.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Generic API response wrapper for notification module responses.
 *
 * Provides a standardized response format for all notification-related operations.
 * Contains success flag, message, data payload, error information, and timestamp.
 *
 * @param <T> the type of data contained in the response
 */
@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Generic API response")
public class NotificationResponseDto<T> {

    @Schema(description = "Success flag", example = "true")
    private boolean success;

    @Schema(description = "Response message", example = "Operation completed successfully")
    private String message;

    @Schema(description = "Response data")
    private T data;

    @Schema(description = "Error code (if any)", example = "VALIDATION_ERROR")
    private String errorCode;

    @Schema(description = "Error details (if any)", example = "Missing required field")
    private String errorDetails;

    @Schema(description = "Response timestamp", example = "2024-01-01T10:30:00")
    private LocalDateTime timestamp;

    public static <T> NotificationResponseDto<T> success(T data, String message) {
        return NotificationResponseDto.<T>builder()
                .success(true)
                .data(data)
                .message(message)
                .timestamp(LocalDateTime.now())
                .build();
    }

    public static <T> NotificationResponseDto<T> error(int statusCode, String message, String errorCode) {
        return NotificationResponseDto.<T>builder()
                .success(false)
                .message(message)
                .errorCode(errorCode)
                .timestamp(LocalDateTime.now())
                .build();
    }

    public static <T> NotificationResponseDto<T> error(String message, String errorCode, String errorDetails) {
        return NotificationResponseDto.<T>builder()
                .success(false)
                .message(message)
                .errorCode(errorCode)
                .errorDetails(errorDetails)
                .timestamp(LocalDateTime.now())
                .build();
    }
}