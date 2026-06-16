package com.jio.auth.dto;

public class SessionResponseDto {

    private String secureCode;
    private String identity;
    private long expiry; // in seconds

    public SessionResponseDto(){}

    public String getSecureCode() {
        return secureCode;
    }

    public void setSecureCode(String accessToken) {
        this.secureCode = accessToken;
    }

    public String getIdentity() {
        return identity;
    }

    public void setIdentity(String identity) {
        this.identity = identity;
    }

    public long getExpiry() {
        return expiry;
    }

    public void setExpiry(long expiry) {
        this.expiry = expiry;
    }
}
