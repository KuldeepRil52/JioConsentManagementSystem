package com.jio.auth.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "revoked_tokens")
public class RevokedToken {

    @Id
    private String jti;        // store only JWT ID
    private Instant revokedAt; // when token was revoked
    private Instant expiry;    // TTL handled by Mongo index

    public RevokedToken() {}

    public RevokedToken(String jti, Instant revokedAt, Instant expiry) {
        this.jti = jti;
        this.revokedAt = revokedAt;
        this.expiry = expiry;
    }

    public String getJti() { return jti; }
    public void setJti(String jti) { this.jti = jti; }

    public Instant getRevokedAt() { return revokedAt; }
    public void setRevokedAt(Instant revokedAt) { this.revokedAt = revokedAt; }

    public Instant getExpiry() { return expiry; }
    public void setExpiry(Instant expiry) { this.expiry = expiry; }
}
