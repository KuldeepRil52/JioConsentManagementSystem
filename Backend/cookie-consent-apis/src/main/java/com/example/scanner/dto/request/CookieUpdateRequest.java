package com.example.scanner.dto.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.hibernate.validator.constraints.URL;

import java.time.Instant;

@Data
public class CookieUpdateRequest {

    @NotBlank(message = "Cookie name is required and cannot be empty or whitespace")
    private String name;

    @NotBlank(message = "Category is required and cannot be empty or whitespace")
    private String category;

    @NotBlank(message = "Description is required and cannot be empty or whitespace")
    private String description;

    private String domain;

    @URL(message = "Privacy policy URL must be a valid URL")
    private String privacyPolicyUrl;

    @JsonFormat(shape = JsonFormat.Shape.STRING, timezone = "UTC")
    private Instant expires;

    private String provider;
}