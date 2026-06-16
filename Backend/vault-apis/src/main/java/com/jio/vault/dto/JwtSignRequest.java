package com.jio.vault.dto;

import java.util.Map;

public class JwtSignRequest {
    private Map<String, Object> claims;
    private long expiry; // expiry in seconds from now

    public Map<String, Object> getClaims() {
        return claims;
    }

    public void setClaims(Map<String, Object> claims) {
        this.claims = claims;
    }

    public long getExpiry() {
        return expiry;
    }

    public void setExpiry(long expiry) {
        this.expiry = expiry;
    }
}


