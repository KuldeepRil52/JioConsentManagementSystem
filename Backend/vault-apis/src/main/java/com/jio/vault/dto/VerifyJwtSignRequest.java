package com.jio.vault.dto;

public class VerifyJwtSignRequest {

    private String jwt;

    public VerifyJwtSignRequest() {}

    public VerifyJwtSignRequest(String jwt) {
        this.jwt = jwt;
    }

    public String getJwt() {
        return jwt;
    }

    public void setJwt(String jwt) {
        this.jwt = jwt;
    }
}

