package com.jio.partnerportal.client.vault;

import com.jio.partnerportal.util.RestApiManager;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class VaultApiManager extends RestApiManager {

    @Value("${vault.service.base.url}")
    private String vaultBaseUrl;

    @Value("${vault.service.endpoints.onboard-key}")
    private String onboardKeyEndpoint;

    @Value("${vault.service.endpoints.sign}")
    private String signEndpoint;

    public <REQ, RES> ResponseEntity<REQ> postOnboardKey(Map<String, String> headers, RES requestBody, Class<REQ> responseType) {
        return super.post(vaultBaseUrl, onboardKeyEndpoint, headers, requestBody, responseType);
    }

    public <REQ, RES> ResponseEntity<REQ> postSign(Map<String, String> headers, RES requestBody, Class<REQ> responseType) {
        return super.post(vaultBaseUrl, signEndpoint, headers, requestBody, responseType);
    }

}

