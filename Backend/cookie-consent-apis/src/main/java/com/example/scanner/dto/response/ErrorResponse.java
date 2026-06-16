package com.example.scanner.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.Instant;

@Data
public class ErrorResponse {
    private String errorCode;
    private String message;
    private String details;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Instant timestamp;

    private String path;

    // Constructor with details
    public ErrorResponse(String errorCode, String message, String details, Instant timestamp, String path) {
        this.errorCode = errorCode;
        this.message = message;
        this.details = details;
        this.timestamp = timestamp;
        this.path = path;
    }

}