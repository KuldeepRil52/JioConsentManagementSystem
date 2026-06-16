package com.example.scanner.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.Instant;

@Data
public class CookieUpdateResponse {

    private String transactionId;
    private String cookieName;
    private String category;
    private String description;
    private String domain;
    private String privacyPolicyUrl;
    @JsonFormat(shape = JsonFormat.Shape.STRING, timezone = "UTC")
    private Instant expires;
    private String provider; // NEW FIELD
    private boolean updated;
    private String message;

    public CookieUpdateResponse(String transactionId, String cookieName, String category,
                                String description, String domain, String privacyPolicyUrl,
                                Instant expires, String provider, boolean updated, String message) {
        this.transactionId = transactionId;
        this.cookieName = cookieName;
        this.category = category;
        this.description = description;
        this.domain = domain;
        this.privacyPolicyUrl = privacyPolicyUrl;
        this.expires = expires;
        this.provider = provider;
        this.updated = updated;
        this.message = message;
    }

    public static CookieUpdateResponse success(String transactionId, String cookieName,
                                               String category, String description, String domain,
                                               String privacyPolicyUrl, Instant expires, String provider) {
        return new CookieUpdateResponse(transactionId, cookieName, category, description,
                domain, privacyPolicyUrl, expires, provider, true, "Cookie updated successfully");
    }

}