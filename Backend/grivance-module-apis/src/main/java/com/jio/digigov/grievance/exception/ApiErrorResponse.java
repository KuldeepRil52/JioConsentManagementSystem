package com.jio.digigov.grievance.exception;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Standard structure for API error responses.
 */
@Data
@Builder
@JsonPropertyOrder({"transactionId", "status", "errorCode", "errorMessage", "timestamp"})
public class ApiErrorResponse {

    private String transactionId;
    private int status;
    private String errorCode;
    private String errorMessage;
    private LocalDateTime timestamp;
}