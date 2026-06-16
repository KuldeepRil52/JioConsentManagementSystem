package com.wso2wrapper.credentials_generation.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {

    private boolean success;
    private String code;
    private String message;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
    private Instant timestamp;

    public ErrorResponse() {
        this.success = false;
        this.timestamp = Instant.now().truncatedTo(ChronoUnit.MILLIS);
    }

    public ErrorResponse(String code, String message, Instant timestamp) {
        this.success = false;
        this.code = code;
        this.message = message;
        this.timestamp = Instant.now().truncatedTo(ChronoUnit.MILLIS);
    }

    // getters and setters
    public boolean isSuccess() { return success; }
    public String getCode() { return code; }
    public String getMessage() { return message; }
    public Instant getTimestamp() { return timestamp; }

    public void setSuccess(boolean success) { this.success = success; }
    public void setCode(String code) { this.code = code; }
    public void setMessage(String message) { this.message = message; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }
}
