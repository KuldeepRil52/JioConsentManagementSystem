package com.example.scanner.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class CookieCategorizationResponse {
    private String category;
    private double confidence;
    @JsonProperty("name")
    private String name;
    private String description;
    private String description_gpt;

}