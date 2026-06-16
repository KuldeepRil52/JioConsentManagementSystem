package com.jio.schedular.client.vault;

import com.jio.schedular.utils.RestApiManager;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class VaultApiManager extends RestApiManager {

    @Value("${vault.service.base.url}")
    private String vaultServiceBaseUrl;

    @Value("${vault.service.endpoints.sign}")
    private String signEndpoint;

    @Value("${vault.service.endpoints.encryptPayload}")
    private String encryptPayloadEndpoint;

    public <I, O> ResponseEntity<O> sign(Map<String, String> headers, I requestBody, Class<O> responseType) {
        return super.post(vaultServiceBaseUrl, signEndpoint, headers, requestBody, responseType);
    }

    public <I, O> ResponseEntity<O> encryptPayload(Map<String, String> headers, I requestBody, Class<O> responseType) {
        return super.post(vaultServiceBaseUrl, encryptPayloadEndpoint, headers, requestBody, responseType);
    }
}

