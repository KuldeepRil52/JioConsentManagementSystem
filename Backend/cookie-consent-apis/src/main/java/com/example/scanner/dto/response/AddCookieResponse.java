package com.example.scanner.dto.response;

import lombok.Data;

@Data
public class AddCookieResponse {

    private String transactionId;
    private String name;
    private String domain;
    private String subdomainName;
    private String provider; // NEW FIELD
    private boolean added;
    private String message;

    public AddCookieResponse(String transactionId, String name, String domain,
                             String subdomainName, String provider, boolean added, String message) {
        this.transactionId = transactionId;
        this.name = name;
        this.domain = domain;
        this.subdomainName = subdomainName;
        this.provider = provider;
        this.added = added;
        this.message = message;
    }

    public static AddCookieResponse success(String transactionId, String name,
                                            String domain, String subdomainName, String provider) {
        return new AddCookieResponse(transactionId, name, domain, subdomainName,
                provider, true, "Cookie added successfully");
    }
}