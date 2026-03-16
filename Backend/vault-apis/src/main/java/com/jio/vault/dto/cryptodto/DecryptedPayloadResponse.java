package com.jio.vault.dto.cryptodto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class DecryptedPayloadResponse {

    @JsonProperty("referenceId")
    private String uuid;

    @JsonProperty("decryptedPayload")
    private Object decryptedPayload;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public Object getDecryptedPayload() {
        return decryptedPayload;
    }

    public void setDecryptedPayload(Object decryptedPayload) {
        this.decryptedPayload = decryptedPayload;
    }
}
