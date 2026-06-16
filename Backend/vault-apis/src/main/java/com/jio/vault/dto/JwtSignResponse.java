package com.jio.vault.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class JwtSignResponse {

    @JsonProperty("jwt")
    private String jwt;

    public JwtSignResponse(String jwt) {
        this.jwt = jwt;
    }

    public String getJwt() {
        return jwt;
    }

    public void setJwt(String jwt) {
        this.jwt = jwt;
    }
}

