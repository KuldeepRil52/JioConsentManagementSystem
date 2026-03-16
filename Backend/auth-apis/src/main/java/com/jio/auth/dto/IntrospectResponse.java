package com.jio.auth.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
        "active",
        "sub",
        "iss",
        "aud",
        "scope",
        "clientId",
        "username",
        "tokenType",
        "exp",
        "iat",
        "nbf",
        "jti",
        "accessToken"
})
public class IntrospectResponse {

    private boolean active;
    private String scope;
    private String clientId;
    private String username;
    private String tokenType;
    private Long exp;
    private Long iat;
    private Long nbf;
    private String sub;
    private String aud;
    private String iss;
    private String jti;
    private String accessToken;

    private IntrospectResponse(Builder builder) {
        this.active = builder.active;
        this.scope = builder.scope;
        this.clientId = builder.clientId;
        this.username = builder.username;
        this.tokenType = builder.tokenType;
        this.exp = builder.exp;
        this.iat = builder.iat;
        this.nbf = builder.nbf;
        this.sub = builder.sub;
        this.aud = builder.aud;
        this.iss = builder.iss;
        this.jti = builder.jti;
        this.accessToken = builder.accessToken;
    }
    public boolean isActive() { return active; }
    public String getScope() { return scope; }
    public String getClientId() { return clientId; }
    public String getUsername() { return username; }
    public String getTokenType() { return tokenType; }
    public Long getExp() { return exp; }
    public Long getIat() { return iat; }
    public Long getNbf() { return nbf; }
    public String getSub() { return sub; }
    public String getAud() { return aud; }
    public String getIss() { return iss; }
    public String getJti() { return jti; }
    public String getAccessToken() { return accessToken; }


    public static class Builder {
        private boolean active;
        private String scope;
        private String clientId;
        private String username;
        private String tokenType;
        private Long exp;
        private Long iat;
        private Long nbf;
        private String sub;
        private String aud;
        private String iss;
        private String jti;
        private String accessToken;

        public Builder active(boolean active) { this.active = active; return this; }
        public Builder scope(String scope) { this.scope = scope; return this; }
        public Builder clientId(String clientId) { this.clientId = clientId; return this; }
        public Builder username(String username) { this.username = username; return this; }
        public Builder tokenType(String tokenType) { this.tokenType = tokenType; return this; }
        public Builder exp(Long exp) { this.exp = exp; return this; }
        public Builder iat(Long iat) { this.iat = iat; return this; }
        public Builder nbf(Long nbf) { this.nbf = nbf; return this; }
        public Builder sub(String sub) { this.sub = sub; return this; }
        public Builder aud(String aud) { this.aud = aud; return this; }
        public Builder iss(String iss) { this.iss = iss; return this; }
        public Builder jti(String jti) { this.jti = jti; return this; }
        public Builder accessToken(String accessToken) { this.accessToken = accessToken; return this; }

        public IntrospectResponse build() {
            return new IntrospectResponse(this);
        }
    }
}
