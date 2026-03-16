package com.example.scanner.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

/**
 * Request DTO for cookie categorization API
 */
@Data
public class CookieCategorizationRequest {
    @JsonProperty("cookie_names")
    private List<String> cookieNames;

    public CookieCategorizationRequest(List<String> cookieNames) {
        this.cookieNames = cookieNames;
    }

}