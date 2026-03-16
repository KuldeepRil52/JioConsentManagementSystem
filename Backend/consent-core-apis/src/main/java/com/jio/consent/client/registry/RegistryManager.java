package com.jio.consent.client.registry;

import com.jio.consent.client.registry.response.WithdrawConsentResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;

import java.util.HashMap;
import java.util.Map;

@Component
@Slf4j
public class RegistryManager extends RegistryApiManager {

    /**
     * Withdraw a consent by consent ID
     *
     * @param tenantId Tenant ID
     * @param businessId Business ID
     * @param consentId Consent ID to withdraw
     * @return WithdrawConsentResponse containing the withdrawal status
     */
    public WithdrawConsentResponse withdrawConsent(String tenantId, String businessId, String consentId) {
        try {
            Map<String, String> headers = new HashMap<>();
            headers.put("X-Tenant-Id", tenantId);
            headers.put("X-Business-Id", businessId);
            headers.put("Content-Type", MediaType.APPLICATION_JSON_VALUE);

            ResponseEntity<WithdrawConsentResponse> response = super.withdrawConsent(headers, consentId, WithdrawConsentResponse.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                log.info("Consent withdrawn successfully for tenant: {}, business: {}, consentId: {}, status: {}", 
                        tenantId, businessId, consentId, response.getBody().getStatus());
                return response.getBody();
            } else {
                log.error("Failed to withdraw consent - HTTP Status: {}", response.getStatusCode());
                throw new RuntimeException("Failed to withdraw consent: " + response.getStatusCode());
            }
        } catch (RestClientException e) {
            log.error("Exception occurred while withdrawing consent for tenant: {}, business: {}, consentId: {}, error: {}", 
                    tenantId, businessId, consentId, e.getMessage(), e);
            throw new RuntimeException("Exception occurred while withdrawing consent", e);
        }
    }
}


