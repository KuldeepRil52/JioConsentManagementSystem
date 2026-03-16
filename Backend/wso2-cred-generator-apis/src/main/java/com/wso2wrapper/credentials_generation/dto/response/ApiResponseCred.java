package com.wso2wrapper.credentials_generation.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

public class ApiResponseCred<T> {

    private boolean success;
    private String message;
    private String code;
    private T data;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
    private Instant timestamp;

    public ApiResponseCred() {
        this.timestamp = Instant.now().truncatedTo(ChronoUnit.MILLIS);
    }

    public ApiResponseCred(boolean success, String message, String code, T data) {
        this.success = success;
        this.message = message;
        this.code = code;
        this.data = data;
        this.timestamp = Instant.now().truncatedTo(ChronoUnit.MILLIS);
    }

    // Getters and Setters
    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public T getData() { return data; }
    public void setData(T data) { this.data = data; }

    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }
}
