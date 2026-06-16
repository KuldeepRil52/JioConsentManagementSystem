package com.jio.vault.dto;

import java.util.Map;

public class VerifyJwtSignResponse {

    private boolean valid;
    private Map<String, Object> payload;

    public VerifyJwtSignResponse() {}

    public VerifyJwtSignResponse(boolean valid, Map<String, Object> payload) {
        this.valid = valid;
        this.payload = payload;
    }

    public boolean isValid() {
        return valid;
    }

    public void setValid(boolean valid) {
        this.valid = valid;
    }

    public Map<String, Object> getPayload() {
        return payload;
    }

    public void setPayload(Map<String, Object> payload) {
        this.payload = payload;
    }
}

