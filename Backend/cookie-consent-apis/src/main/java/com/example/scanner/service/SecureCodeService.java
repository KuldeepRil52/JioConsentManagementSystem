package com.example.scanner.service;

import com.example.scanner.constants.ErrorCodes;
import com.example.scanner.dto.request.SecureCodeRequest;
import com.example.scanner.dto.response.SecureCodeApiResponse;
import com.example.scanner.exception.ScannerException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Service
@RequiredArgsConstructor
@Slf4j
public class SecureCodeService {

    private final RestTemplate restTemplate;

    @Value("${secure.code.api.base-url:https://api.dscoe.jiolabs.com:8443}")
    private String secureCodeBaseUrl;

    @Value("${secure.code.api.endpoint:/secureCode/create}")
    private String secureCodeEndpoint;

    /**
     * Call external secure code API to generate secure code
     *
     * @param tenantId      Tenant ID from header
     * @param businessId    Business ID from header
     * @param identityValue Identity value from customerIdentifiers
     * @param identityType  Identity type from customerIdentifiers
     * @return SecureCodeApiResponse containing secureCode, identity, and expiry
     * @throws ScannerException if API call fails
     */
    public SecureCodeApiResponse createSecureCode(String tenantId, String businessId,
                                                  String identityValue, String identityType) throws ScannerException {
        try {
            String url = secureCodeBaseUrl + secureCodeEndpoint;

            // Prepare headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("tenant-id", tenantId);
            headers.set("business-id", businessId);

            // Prepare request body
            SecureCodeRequest requestBody = SecureCodeRequest.builder()
                    .identityValue(identityValue)
                    .identityType(identityType)
                    .build();

            HttpEntity<SecureCodeRequest> requestEntity = new HttpEntity<>(requestBody, headers);

            log.info("Calling secure code API: {} for identity: {}", url, identityValue);

            // Make API call
            ResponseEntity<SecureCodeApiResponse> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    requestEntity,
                    SecureCodeApiResponse.class
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                log.info("Secure code created successfully: {}", response.getBody().getSecureCode());
                return response.getBody();
            } else {
                log.error("Secure code API returned non-200 status: {}", response.getStatusCode());
                throw new ScannerException(
                        ErrorCodes.EXTERNAL_SOURCE_CODE_SERVICE_ERROR,
                        "Secure code API returned non-200 status.",
                        "Failed to create secure code - API returned status: " + response.getStatusCode()
                );
            }

        } catch (RestClientException e) {
            log.error("Error calling secure code API:");
            throw new ScannerException(
                    ErrorCodes.EXTERNAL_SOURCE_CODE_SERVICE_ERROR,
                    "Problem occurred in calling source code API.",
                    "Failed to create secure code: " + e.getMessage()
            );
        } catch (ScannerException e) {
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error while creating secure code:");
            throw new ScannerException(
                    ErrorCodes.INTERNAL_ERROR,
                    "Something went wrong while calling source code API.",
                    "Unexpected error while creating secure code: " + e.getMessage()
            );
        }
    }
}