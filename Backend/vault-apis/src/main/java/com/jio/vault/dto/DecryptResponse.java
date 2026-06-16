package com.jio.vault.dto;

public class DecryptResponse {
    private String base64Text;
    private String keyId;

    public DecryptResponse() {}

    public DecryptResponse(String base64Text, String keyId) {
        this.base64Text = base64Text;
        this.keyId = keyId;
    }

    public String getBase64Text() {
        return base64Text;
    }

    public void setBase64Text(String plaintext) {
        this.base64Text = plaintext;
    }

    public String getKeyId() {
        return keyId;
    }

    public void setKeyId(String keyId) {
        this.keyId = keyId;
    }
}

