package com.jio.digigov.auditmodule.client.vault;

import com.jio.digigov.auditmodule.client.vault.request.EncryptPayloadRequest;
import com.jio.digigov.auditmodule.client.vault.response.DecryptPayloadResponse;
import com.jio.digigov.auditmodule.client.vault.response.EncryptPayloadResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
public class VaultManager extends VaultApiManager {

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

            ResponseEntity<EncryptPayloadResponse> response =
                    super.encryptPayload(headers, request, EncryptPayloadResponse.class);

            return response.getBody();

        } catch (RestClientException e) {
            log.error("Encrypt API error: {}", e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }


    public DecryptPayloadResponse decryptPayload(
            String tenantId,
            String businessId,
            String referenceId
    ) {

        try {
            Map<String, String> headers = new HashMap<>();
            headers.put("Tenant-Id", tenantId);
            headers.put("Business-Id", businessId);
            headers.put("Reference-Id", referenceId);

            log.info("Calling decryptPayload for refId={}", referenceId);

            ResponseEntity<DecryptPayloadResponse> response =
                    super.decryptPayload(headers, DecryptPayloadResponse.class);

            log.info("Decrypt Response Status: {}", response.getStatusCode());
            log.info("Decrypt Response Body: {}", response.getBody());

            return response.getBody();

        } catch (RestClientException e) {
            log.error("Decrypt API error: {}", e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }
}