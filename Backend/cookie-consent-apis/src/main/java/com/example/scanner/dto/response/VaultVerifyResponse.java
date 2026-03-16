package com.example.scanner.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VaultVerifyResponse {

    @JsonProperty("valid")
    private boolean valid;

    @JsonProperty("payload")
    private Map<String, Object> payload;  // Generic Map for any payload structure
}