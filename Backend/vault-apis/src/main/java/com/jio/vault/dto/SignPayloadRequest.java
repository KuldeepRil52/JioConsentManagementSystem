package com.jio.vault.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class SignPayloadRequest {

    private JsonNode payload; // can hold any JSON object

    @JsonIgnore
    private static final ObjectMapper mapper = new ObjectMapper();

    public JsonNode getPayload() {
        return payload;
    }

    public void setPayload(JsonNode payload) {
        this.payload = payload;
    }

    // New helper to get string representation
    @JsonIgnore
    public String getPayloadAsString() {
        try {
            return payload != null ? mapper.writeValueAsString(payload) : null;
        } catch (Exception e) {
            return null;
        }
    }
}



