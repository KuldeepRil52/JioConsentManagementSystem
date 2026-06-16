package com.jio.vault.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "vault")
public class VaultProperties {

    private String baseUrl;
    private String encryptUri;
    private String decryptUri;
    private String signUri;
    private String verifyUri;
    private String token;

    // Getters and setters
    public String getBaseUrl() { return baseUrl; }
    public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }

    public String getEncryptUri() { return encryptUri; }
    public void setEncryptUri(String encryptUri) { this.encryptUri = encryptUri; }

    public String getDecryptUri() { return decryptUri; }
    public void setDecryptUri(String decryptUri) { this.decryptUri = decryptUri; }

    public String getSignUri() { return signUri; }
    public void setSignUri(String signUri) { this.signUri = signUri; }

    public String getVerifyUri() { return verifyUri; }
    public void setVerifyUri(String verifyUri) { this.verifyUri = verifyUri; }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

}

