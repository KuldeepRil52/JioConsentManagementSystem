package com.jio.digigov.notification.service.registry.impl;

import com.jio.digigov.notification.config.SystemRegistryProperties;
import com.jio.digigov.notification.dto.registry.ConsentWithdrawRequestDto;
import com.jio.digigov.notification.service.registry.ConsentWithdrawService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

/**
 * Implementation of ConsentWithdrawService.
 *
 * Calls the System Registry consent withdraw API before DF callbacks for
 * CONSENT_EXPIRED and CONSENT_WITHDRAWN events. The API saves withdrawal_data
 * directly to the notification_events collection.
 *
 * Error handling follows fire-and-forget pattern - errors are logged
 * but do not block the DF callback processing.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ConsentWithdrawServiceImpl implements ConsentWithdrawService {

    private final SystemRegistryProperties systemRegistryProperties;
    private final RestTemplate restTemplate;

    @Override
    public void withdrawConsent(String consentId, String eventId, String tenantId, String businessId) {
        if (consentId == null || consentId.isEmpty()) {
            log.warn("Cannot call withdraw API - consentId is null or empty for eventId: {}", eventId);
            return;
        }

        try {
            // Build the URL with consentId
            String url = buildWithdrawUrl(consentId);

            // Create request headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-Tenant-ID", tenantId);
            headers.set("X-Business-ID", businessId);

            // Create request body
            ConsentWithdrawRequestDto requestDto = ConsentWithdrawRequestDto.builder()
                    .eventId(eventId)
                    .build();

            HttpEntity<ConsentWithdrawRequestDto> requestEntity = new HttpEntity<>(requestDto, headers);

            log.info("Calling consent withdraw API: url={}, eventId={}, consentId={}, tenantId={}, businessId={}",
                    url, eventId, consentId, tenantId, businessId);

            // Make the API call
            ResponseEntity<String> response = restTemplate.postForEntity(url, requestEntity, String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("Consent withdraw API call successful: eventId={}, consentId={}, status={}",
                        eventId, consentId, response.getStatusCode());
            } else {
                log.warn("Consent withdraw API returned non-success status: eventId={}, consentId={}, status={}",
                        eventId, consentId, response.getStatusCode());
            }

        } catch (Exception e) {
            // Fire-and-forget: Log error but don't throw to allow callback processing to continue
            log.error("Error calling consent withdraw API: eventId={}, consentId={}, error={}",
                    eventId, consentId, e.getMessage(), e);
        }
    }

    /**
     * Builds the withdraw API URL with the consent ID.
     *
     * @param consentId The consent ID to include in the URL
     * @return The full URL for the withdraw API call
     */
    private String buildWithdrawUrl(String consentId) {
        String baseUrl = systemRegistryProperties.getBaseUrl();
        String endpointTemplate = systemRegistryProperties.getEndpoints().getConsentWithdraw();

        String endpoint = endpointTemplate.replace("{consentId}", consentId);

        return baseUrl + endpoint;
    }
}
