package com.example.scanner.client;

import com.example.scanner.dto.request.EncryptPayloadRequest;
import com.example.scanner.dto.request.VaultSignRequest;
import com.example.scanner.dto.request.VaultVerifyRequest;
import com.example.scanner.dto.response.EncryptPayloadResponse;
import com.example.scanner.dto.response.VaultSignResponse;
import com.example.scanner.dto.response.VaultVerifyResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class VaultClient {

    private final RestTemplate restTemplate;

    @Value("${vault.service.base-url}")
    private String vaultBaseUrl;

    @Value("${vault.service.sign-endpoint}")
    private String signEndpoint;

    @Value("${vault.service.verify-endpoint}")
    private String verifyEndpoint;

    @Value("${vault.service.endpoints.encryptPayload}")
    private String encryptPayload;

    public String signPayload(String jsonPayload, String tenantId, String businessId) {
        try {
            String url = vaultBaseUrl + signEndpoint;

            // Prepare request - vault expects payload as Map
            Map<String, Object> payload = new HashMap<>();
            payload.put("data", jsonPayload);

            VaultSignRequest request = VaultSignRequest.builder()
                    .payload(payload)
                    .build();

            // Set headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Tenant-Id", tenantId);
            headers.set("Business-Id", businessId);

            HttpEntity<VaultSignRequest> requestEntity = new HttpEntity<>(request, headers);

            log.debug("Calling vault sign API: {}", url);

            ResponseEntity<VaultSignResponse> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    requestEntity,
                    VaultSignResponse.class
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                String jwt = response.getBody().getJwt();
                log.info("Vault sign successful - Status: {}", response.getStatusCode());
                return jwt;
            } else {
                throw new RuntimeException("Vault API returned invalid response");
            }

        } catch (Exception e) {
            log.error("Vault sign API failed - Exception occurred ");
            throw new RuntimeException("Failed to sign payload: " + e);
        }
    }

    public VaultVerifyResponse verifyToken(String jwt, String tenantId, String businessId) {
        try {
            String url = vaultBaseUrl + verifyEndpoint;

            VaultVerifyRequest request = VaultVerifyRequest.builder()
                    .jwt(jwt)
                    .build();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Tenant-Id", tenantId);
            headers.set("Business-Id", businessId);

            HttpEntity<VaultVerifyRequest> requestEntity = new HttpEntity<>(request, headers);

            log.debug("Calling vault verify API: {}", url);

            ResponseEntity<VaultVerifyResponse> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    requestEntity,
                    VaultVerifyResponse.class
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                log.info("Vault verify successful - Valid: {}", response.getBody().isValid());
                return response.getBody();
            } else {
                throw new RuntimeException("Vault API returned invalid response");
            }

        } catch (Exception e) {
            log.error("Vault verify API failed - exception occurred");
            throw new RuntimeException("Failed to verify token: " + e);
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

            String url = vaultBaseUrl + encryptPayload;


            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Tenant-Id", tenantId);
            headers.set("Business-Id", businessId);
            headers.set("Data-Category-Type", dataCategoryType);
            headers.set("Data-Category-Value", dataCategoryValue);

            EncryptPayloadRequest request = EncryptPayloadRequest.builder()
                    .dataString(dataString)
                    .build();

            HttpEntity<EncryptPayloadRequest> requestEntity = new HttpEntity<>(request, headers);

            ResponseEntity<EncryptPayloadResponse> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    requestEntity,
                    EncryptPayloadResponse.class
            );
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                log.info("Payload encrypted successfully for tenant: {}, business: {}, referenceId: {}",
                        tenantId, businessId, response.getBody().getReferenceId());
                return response.getBody();
            } else {
                log.error("Failed to encrypt payload - HTTP Status: {}", response.getStatusCode());
                throw new RuntimeException("Failed to encrypt payload: " + response.getStatusCode());
            }
        } catch (RestClientException e) {
            throw new RuntimeException("Exception occurred while encrypting payload" + e);
        }
    }

}