package com.jio.digigov.grievance.client;

import com.jio.digigov.grievance.util.RestApiManager;
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

    public <REQ, RES> ResponseEntity<RES> sign(Map<String, String> headers, REQ requestBody, Class<RES> responseType) {
        return super.post(vaultServiceBaseUrl, signEndpoint, headers, requestBody, responseType);
    }
}