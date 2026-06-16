package com.jio.vault.dto;

public class VerifySignRequest {
    private String input;      // Base64-encoded original text
    private String signature;  // Base64-encoded signature with optional vault:v1 prefix

    public String getInput() {
        return input;
    }

    public void setInput(String input) {
        this.input = input;
    }

    public String getSignature() {
        return signature;
    }

    public void setSignature(String signature) {
        this.signature = signature;
    }
}

