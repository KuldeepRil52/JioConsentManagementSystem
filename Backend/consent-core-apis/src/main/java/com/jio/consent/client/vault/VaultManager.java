package com.jio.consent.client.vault;

import com.jio.consent.client.vault.request.EncryptPayloadRequest;
import com.jio.consent.client.vault.request.SignRequest;
import com.jio.consent.client.vault.request.VerifyRequest;
import com.jio.consent.client.vault.response.EncryptPayloadResponse;
import com.jio.consent.client.vault.response.SignResponse;
import com.jio.consent.client.vault.response.VerifyResponse;
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

    /**
     * Verify a JWT token and return the payload
     *
     * @param tenantId Tenant ID
     * @param businessId Business ID
     * @param jwt JWT token to verify
     * @return VerifyResponse containing validation result and payload
     */
    public VerifyResponse verify(String tenantId, String businessId, String jwt) {
        try {
            Map<String, String> headers = new HashMap<>();
            headers.put("Tenant-ID", tenantId);
            headers.put("Business-Id", businessId);
            headers.put("Content-Type", MediaType.APPLICATION_JSON_VALUE);

            VerifyRequest request = VerifyRequest.builder()
                    .jwt(jwt)
                    .build();

            ResponseEntity<VerifyResponse> response = super.verify(headers, request, VerifyResponse.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                log.info("JWT verified successfully for tenant: {}, business: {}, valid: {}", 
                        tenantId, businessId, response.getBody().getValid());
                return response.getBody();
            } else {
                log.error("Failed to verify JWT - HTTP Status: {}", response.getStatusCode());
                throw new RuntimeException("Failed to verify JWT: " + response.getStatusCode());
            }
        } catch (RestClientException e) {
            log.error("Exception occurred while verifying JWT for tenant: {}, business: {}, error: {}", 
                    tenantId, businessId, e.getMessage(), e);
            throw new RuntimeException("Exception occurred while verifying JWT", e);
        }
    }

    /**
     * Encrypt a payload
     *
     * @param tenantId Tenant ID
     * @param businessId Business ID
     * @param dataCategoryType Data category type (e.g., "consent")
     * @param dataCategoryValue Data category value
     * @param dataString Data string to encrypt
     * @return EncryptPayloadResponse containing encrypted data
     */
    public EncryptPayloadResponse encryptPayload(String tenantId, String businessId, 
                                                  String dataCategoryType, String dataCategoryValue,
                                                  String dataString) {
        try {
            Map<String, String> headers = new HashMap<>();
            headers.put("Tenant-Id", tenantId);
            headers.put("Business-Id", businessId);
            headers.put("Data-Category-Type", dataCategoryType);
            headers.put("Data-Category-Value", dataCategoryValue);
            headers.put("Content-Type", MediaType.APPLICATION_JSON_VALUE);

            EncryptPayloadRequest request = EncryptPayloadRequest.builder()
                    .dataString(dataString)
                    .build();

            ResponseEntity<EncryptPayloadResponse> response = super.encryptPayload(headers, request, EncryptPayloadResponse.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                log.info("Payload encrypted successfully for tenant: {}, business: {}, referenceId: {}", 
                        tenantId, businessId, response.getBody().getReferenceId());
                return response.getBody();
            } else {
                log.error("Failed to encrypt payload - HTTP Status: {}", response.getStatusCode());
                throw new RuntimeException("Failed to encrypt payload: " + response.getStatusCode());
            }
        } catch (RestClientException e) {
            log.error("Exception occurred while encrypting payload for tenant: {}, business: {}, error: {}", 
                    tenantId, businessId, e.getMessage(), e);
            throw new RuntimeException("Exception occurred while encrypting payload", e);
        }
    }
}

