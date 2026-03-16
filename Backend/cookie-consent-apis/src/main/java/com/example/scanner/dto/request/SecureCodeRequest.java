package com.example.scanner.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SecureCodeRequest {

    @JsonProperty("identityValue")
    private String identityValue;

    @JsonProperty("identityType")
    private String identityType;
}