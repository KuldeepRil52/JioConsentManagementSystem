package com.jio.auth.dto;


public class RevokeResponse {

    private boolean revoked;
    private String token;

    public RevokeResponse() {}

    public RevokeResponse(String token, boolean revoked) {
        this.token = token;
        this.revoked = revoked;
    }

    public boolean isRevoked() {
        return revoked;
    }

    public void setRevoked(boolean revoked) {
        this.revoked = revoked;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }
}
