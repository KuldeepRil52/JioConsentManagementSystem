package com.jio.digigov.auditmodule.client.notification;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jio.digigov.auditmodule.client.notification.request.TriggerEventRequest;
import com.jio.digigov.auditmodule.client.notification.response.TriggerEventResponse;
import com.jio.digigov.auditmodule.dto.Consent;
import com.jio.digigov.auditmodule.dto.ConsentIntegrityEventPayload;
import com.jio.digigov.auditmodule.dto.CookieConsentDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;

import java.util.HashMap;
import java.util.Map;

@Component
@Slf4j
public class NotificationManager extends NotificationApiManager {

    private final ObjectMapper objectMapper;

    private static final String EVENT_TYPE_SUCCESS = "CONSENT_INTEGRITY_VERIFICATION_COMPLETED";
    private static final String EVENT_TYPE_FAILURE = "CONSENT_INTEGRITY_VERIFICATION_FAILED";
    private static final String RESOURCE_COSENT = "Consent Check";

    public NotificationManager(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * Shared private method to trigger notification events
     */
    private TriggerEventResponse triggerConsentCheckEvent(
            String tenantId,
            String businessId,
            Consent consent,
            String eventType,
            Map<String, Object> eventPayload) {           // <-- eventType now dynamic


        Map<String, Object> customerIdentifiers = Map.of(
                "type", consent.getCustomerIdentifiers().getType().toString(),
                "value", consent.getCustomerIdentifiers().getValue()
        );

        // Build event request
        TriggerEventRequest request = TriggerEventRequest.builder()
                .eventType(eventType)       // <-- SUCCESS or FAILURE
                .resource(RESOURCE_COSENT)
                .customerIdentifiers(customerIdentifiers)
                .eventPayload(eventPayload)
                .build();

        // Headers
        Map<String, String> headers = new HashMap<>();
        headers.put("X-Tenant-Id", tenantId);
        headers.put("X-Business-Id", businessId);
        headers.put("X-Transaction-Id", "ABCD1234567890EF1234567890");

        log.info("Trigger consent check event request : {}", request);

        try {
            ResponseEntity<TriggerEventResponse> response =
                    super.posttriggerEvernt(headers, request, TriggerEventResponse.class);


            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                log.info("Event '{}' sent successfully: tenant={}, business={}, status={}",
                        eventType, tenantId, businessId, response.getBody().getStatus());
                return response.getBody();
            } else {
                log.error("Failed to send event '{}' - HTTP Status: {}",
                        eventType, response.getStatusCode());
                throw new RuntimeException("Event failed: " + response.getStatusCode());
            }

        } catch (RestClientException e) {
            log.error("Exception while sending event '{}': tenant={}, business={}, error={}",
                    eventType, tenantId, businessId, e.getMessage(), e);
            throw new RuntimeException("Exception sending event", e);
        }
    }

    // ---------------- Public Async Methods ----------------

    @Async
    public TriggerEventResponse triggerConsentCheckEventForSucess(
            String tenantId,
            String businessId,
            Consent consent,
            ConsentIntegrityEventPayload consetEventPayload) {

        Map<String, Object> eventPayload = objectMapper.convertValue(consetEventPayload, Map.class);

        return triggerConsentCheckEvent(tenantId, businessId, consent, EVENT_TYPE_SUCCESS, eventPayload);
    }

    @Async
    public TriggerEventResponse triggerConsentCheckEventForFailure(
            String tenantId,
            String businessId,
            Consent consent,
            ConsentIntegrityEventPayload consetEventPayload) {

        Map<String, Object> eventPayload = objectMapper.convertValue(consetEventPayload, Map.class);

        return triggerConsentCheckEvent(tenantId, businessId, consent, EVENT_TYPE_FAILURE, eventPayload);
    }
}