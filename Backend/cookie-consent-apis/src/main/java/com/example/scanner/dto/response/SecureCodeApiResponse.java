package com.example.scanner.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SecureCodeApiResponse {

    @JsonProperty("secureCode")
    private String secureCode;

    @JsonProperty("identity")
    private String identity;

    @JsonProperty("expiry")
    private Long expiry;
}