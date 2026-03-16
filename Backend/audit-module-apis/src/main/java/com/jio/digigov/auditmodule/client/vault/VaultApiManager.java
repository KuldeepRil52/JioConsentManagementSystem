package com.jio.digigov.auditmodule.client.vault;

import com.jio.digigov.auditmodule.util.RestApiManager;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class VaultApiManager extends RestApiManager {

    @Value("${vault.service.base.url}")
    private String vaultServiceBaseUrl;

    @Value("${vault.service.endpoints.encryptPayload}")
    private String encryptPayloadEndpoint;

    @Value("${vault.service.endpoints.decryptPayload}")
    private String decryptPayloadEndpoint;

    public <REQ, RES> ResponseEntity<RES> encryptPayload(
            Map<String, String> headers,
            REQ body,
            Class<RES> responseType
    ) {
        return super.post(vaultServiceBaseUrl, encryptPayloadEndpoint, headers, body, responseType);
    }

    public <RES> ResponseEntity<RES> decryptPayload(
            Map<String, String> headers,
            Class<RES> responseType
    ) {
        return super.get(vaultServiceBaseUrl, decryptPayloadEndpoint, headers, responseType);
    }
}