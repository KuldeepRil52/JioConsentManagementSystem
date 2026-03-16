package com.jio.consent.client.auth;

import com.jio.consent.client.auth.request.CreateSecureCodeRequest;
import com.jio.consent.client.auth.response.CreateSecureCodeResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;

import java.util.HashMap;
import java.util.Map;

@Component
@Slf4j
public class AuthManager extends AuthApiManager {

    /**
     * Create a secure code for authentication
     *
     * @param tenantId Tenant ID (passed as header)
     * @param businessId Business ID (passed as header)
     * @param identityValue Identity value (e.g., mobile number)
     * @param identityType Identity type (e.g., MOBILE)
     * @return CreateSecureCodeResponse containing the secure code, identity and expiry
     */
    public CreateSecureCodeResponse createSecureCode(String tenantId, String businessId, 
                                                      String identityValue, String identityType) {
        try {
            Map<String, String> headers = new HashMap<>();
            headers.put("Tenant-Id", tenantId);
            headers.put("Business-Id", businessId);
            headers.put("Content-Type", MediaType.APPLICATION_JSON_VALUE);

            CreateSecureCodeRequest request = CreateSecureCodeRequest.builder()
                    .identityValue(identityValue)
                    .identityType(identityType)
                    .build();

            ResponseEntity<CreateSecureCodeResponse> response = super.createSecureCode(headers, request, CreateSecureCodeResponse.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                log.info("Secure code created successfully for tenant: {}, business: {}, identity: {}", 
                        tenantId, businessId, identityValue);
                return response.getBody();
            } else {
                log.error("Failed to create secure code - HTTP Status: {}", response.getStatusCode());
                throw new RuntimeException("Failed to create secure code: " + response.getStatusCode());
            }
        } catch (RestClientException e) {
            log.error("Exception occurred while creating secure code for tenant: {}, business: {}, identity: {}, error: {}", 
                    tenantId, businessId, identityValue, e.getMessage(), e);
            throw new RuntimeException("Exception occurred while creating secure code", e);
        }
    }
}

