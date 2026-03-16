package com.wso2wrapper.credentials_generation.dto.response;

public class KeyResponse {

    private String consumerKey;
    private String consumerSecret;
    private String keyType;
    private String keyState;
    private TokenResponse token; // Nested token info if present

    // No-args constructor
    public KeyResponse() {}

    // All-args constructor
    public KeyResponse(String consumerKey, String consumerSecret, String keyType, String keyState, TokenResponse token) {
        this.consumerKey = consumerKey;
        this.consumerSecret = consumerSecret;
        this.keyType = keyType;
        this.keyState = keyState;
        this.token = token;
    }

    // Getters
    public String getConsumerKey() {
        return consumerKey;
    }

    public String getConsumerSecret() {
        return consumerSecret;
    }

    public String getKeyType() {
        return keyType;
    }

    public String getKeyState() {
        return keyState;
    }

    public TokenResponse getToken() {
        return token;
    }

    // Setters
    public void setConsumerKey(String consumerKey) {
        this.consumerKey = consumerKey;
    }

    public void setConsumerSecret(String consumerSecret) {
        this.consumerSecret = consumerSecret;
    }

    public void setKeyType(String keyType) {
        this.keyType = keyType;
    }

    public void setKeyState(String keyState) {
        this.keyState = keyState;
    }

    public void setToken(TokenResponse token) {
        this.token = token;
    }
}
