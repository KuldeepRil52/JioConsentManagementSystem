package com.jio.vault.dto;

import java.util.UUID;

public class ClientKeyDto {
    private String keyId;
    private String publicKeyPem;

    public ClientKeyDto() {}

    public ClientKeyDto(String keyId, String publicKeyPem) {
        this.keyId = keyId;
        this.publicKeyPem = publicKeyPem;
    }

    public String getKeyId() {
        return keyId;
    }

    public void setKeyId(String keyId) {
        this.keyId = keyId;
    }

    public String getPublicKeyPem() {
        return publicKeyPem;
    }

    public void setPublicKeyPem(String publicKeyPem) {
        this.publicKeyPem = publicKeyPem;
    }
}
