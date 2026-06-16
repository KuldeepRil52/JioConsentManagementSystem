package com.jio.consent.client.registry;

import com.jio.consent.utils.RestApiManager;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class RegistryApiManager extends RestApiManager {

    @Value("${registry.service.base.url}")
    private String registryServiceBaseUrl;

    @Value("${registry.service.endpoints.withdraw-consent}")
    private String withdrawConsentEndpoint;

    public <RES> ResponseEntity<RES> withdrawConsent(Map<String, String> headers, String consentId, Class<RES> responseType) {
        String endpoint = withdrawConsentEndpoint.replace("{consentId}", consentId);
        return super.post(registryServiceBaseUrl, endpoint, headers, null, responseType);
    }
}

