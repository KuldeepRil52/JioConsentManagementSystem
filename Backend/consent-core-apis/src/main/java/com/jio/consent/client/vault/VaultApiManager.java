package com.jio.consent.client.vault;

import com.jio.consent.utils.RestApiManager;
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

    @Value("${vault.service.endpoints.verify}")
    private String verifyEndpoint;

    @Value("${vault.service.endpoints.encryptPayload}")
    private String encryptPayloadEndpoint;

    public <REQ, RES> ResponseEntity<RES> sign(Map<String, String> headers, REQ requestBody, Class<RES> responseType) {
        return super.post(vaultServiceBaseUrl, signEndpoint, headers, requestBody, responseType);
    }

    public <REQ, RES> ResponseEntity<RES> verify(Map<String, String> headers, REQ requestBody, Class<RES> responseType) {
        return super.post(vaultServiceBaseUrl, verifyEndpoint, headers, requestBody, responseType);
    }

    public <REQ, RES> ResponseEntity<RES> encryptPayload(Map<String, String> headers, REQ requestBody, Class<RES> responseType) {
        return super.post(vaultServiceBaseUrl, encryptPayloadEndpoint, headers, requestBody, responseType);
    }
}

