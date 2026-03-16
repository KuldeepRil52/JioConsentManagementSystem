package com.jio.digigov.grievance.client;

import com.jio.digigov.grievance.client.request.SignRequest;
import com.jio.digigov.grievance.client.response.SignResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;

import java.util.HashMap;
import java.util.Map;

@Component
@Slf4j
public class VaultManager extends VaultApiManager {

    /**
     * Sign a payload and generate a JWT token
     *
     * @param tenantId Tenant ID
     * @param businessId Business ID
     * @param payload Payload to sign
     * @return SignResponse containing the JWT token
     */
    public SignResponse sign(String tenantId, String businessId, Map<String, Object> payload) {
        try {
            Map<String, String> headers = new HashMap<>();
            headers.put("Tenant-Id", tenantId);
            headers.put("Business-Id", businessId);
            headers.put("Content-Type", MediaType.APPLICATION_JSON_VALUE);

            SignRequest request = SignRequest.builder()
                    .payload(payload)
                    .build();

            ResponseEntity<SignResponse> response = super.sign(headers, request, SignResponse.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                log.info("JWT signed successfully for tenant: {}, business: {}", tenantId, businessId);
                return response.getBody();
            } else {
                log.error("Failed to sign JWT - HTTP Status: {}", response.getStatusCode());
                throw new RuntimeException("Failed to sign JWT: " + response.getStatusCode());
            }
        } catch (RestClientException e) {
            log.error("Exception occurred while signing JWT for tenant: {}, business: {}, error: {}",
                    tenantId, businessId, e.getMessage(), e);
            throw new RuntimeException("Exception occurred while signing JWT", e);
        }
    }
}
