package com.jio.vault.dto;

public class EncryptRequest {

    private String base64Text;

    public EncryptRequest() {}

    public EncryptRequest(String base64Text) {
        this.base64Text = base64Text;
    }

    public String getBase64Text() {
        return base64Text;
    }

    public void setBase64Text(String base64Text) {
        this.base64Text = base64Text;
    }
}

