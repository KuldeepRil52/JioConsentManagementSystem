package com.jio.vault.dto;

public class EncryptResponse {

    private String keyId;
    private String ciphertext;

    public EncryptResponse() {}

    public EncryptResponse(String keyId, String ciphertext) {
        this.keyId = keyId;
        this.ciphertext = ciphertext;
    }

    public String getKeyId() {
        return keyId;
    }

    public void setKeyId(String keyId) {
        this.keyId = keyId;
    }

    public String getCiphertext() {
        return ciphertext;
    }

    public void setCiphertext(String ciphertext) {
        this.ciphertext = ciphertext;
    }
}

