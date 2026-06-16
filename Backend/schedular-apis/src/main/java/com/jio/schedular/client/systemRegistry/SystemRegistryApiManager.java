package com.jio.schedular.client.systemRegistry;

import com.jio.schedular.utils.RestApiManager;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class SystemRegistryApiManager extends RestApiManager {

    @Value("${system-registry.service.base.url}")
    private String baseUrl;

    @Value("${system-registry.service.endpoints.consent-withdraw}")
    private String withdrawConsentEndpoint;

    public <T> ResponseEntity<T> withdrawConsent(
            String consentId,
            Map<String, String> headers,
            Class<T> responseType) {

        String endpoint = withdrawConsentEndpoint.replace("{consentId}", consentId);

        return super.post(
                baseUrl,
                endpoint,
                headers,
                null,           // no request body
                responseType
        );
    }
}