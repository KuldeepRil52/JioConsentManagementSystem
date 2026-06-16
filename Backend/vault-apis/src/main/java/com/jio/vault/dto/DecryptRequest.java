package com.jio.vault.dto;

public class DecryptRequest {
    private String ciphertext;

    public DecryptRequest() {}

    public DecryptRequest(String ciphertext) {
        this.ciphertext = ciphertext;
    }

    public String getCiphertext() {
        return ciphertext;
    }

    public void setCiphertext(String ciphertext) {
        this.ciphertext = ciphertext;
    }
}
