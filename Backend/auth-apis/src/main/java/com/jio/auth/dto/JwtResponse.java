package com.jio.auth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Getter;
import lombok.Setter;

@JsonPropertyOrder({
        "accessToken",
        "expiry"
})
public class JwtResponse {

    @Getter @Setter
    private Long expiry;

    @Getter @Setter
    @JsonProperty("accessToken")
    private String accessToken;

    public JwtResponse() {}

    public JwtResponse(String accessToken, Long expiry) {
        this.accessToken = accessToken;
        this.expiry = expiry;
    }
}

