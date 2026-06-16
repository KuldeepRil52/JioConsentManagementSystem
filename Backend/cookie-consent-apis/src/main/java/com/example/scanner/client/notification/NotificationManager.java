package com.example.scanner.client.notification;

import com.example.scanner.dto.CustomerIdentifiers;
import com.example.scanner.dto.NotificationTrigger;
import com.example.scanner.dto.request.TriggerEventRequest;
import com.example.scanner.dto.response.TriggerEventResponse;
import com.example.scanner.enums.LANGUAGE;
import com.example.scanner.enums.NotificationEvent;
import com.example.scanner.enums.NotificationStatus;
import com.example.scanner.repository.NotificationTriggerRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@Slf4j
public class NotificationManager extends NotificationApiManager {

    NotificationTriggerRepository notificationTriggerRepository;

    @Autowired
    public NotificationManager(NotificationTriggerRepository notificationTriggerRepository) {
        this.notificationTriggerRepository = notificationTriggerRepository;
    }

    /**
     * Common logic for triggering notification events
     * 
     * @param eventType The notification event type
     * @param tenantId The tenant ID
     * @param businessId The business ID
     * @param customerIdentifiers Customer identifiers
     * @param dataProcessorIds Data processor IDs
     * @param eventPayload Event payload
     * @param language Language preference
     */
    private void triggerNotificationEvent(NotificationEvent eventType,
                                          String tenantId,
                                          String businessId,
                                          CustomerIdentifiers customerIdentifiers,
                                          List<String> dataProcessorIds,
                                          Object eventPayload,
                                          LANGUAGE language) {
        String eventId = eventType.getEventId();
        String resource = eventType.getResource();

        TriggerEventRequest request = TriggerEventRequest.builder()
                .eventType(eventId)
                .resource(resource)
                .dataProcessorIds(dataProcessorIds)
                .customerIdentifiers(customerIdentifiers)
                .build();

        if (language != null) {
            request.setLanguage(language);
        }

        if (eventPayload != null) {
            request.setEventPayload(eventPayload);
        }

        NotificationTrigger notificationTrigger = NotificationTrigger.builder()
                .eventType(eventId)
                .resource(resource)
                .businessId(businessId)
                .status(NotificationStatus.PENDING)
                .eventPayload(eventPayload)
                .customerIdentifiers(customerIdentifiers)
                .dataProcessorsIds(dataProcessorIds)
                .build();

        this.notificationTriggerRepository.save(notificationTrigger, tenantId);

        Map<String, String> headers = new HashMap<>();
        headers.put("X-Tenant-Id", tenantId);
        headers.put("X-Business-Id", businessId);

        //Todo: add logic to generate unique transaction id
        headers.put("X-Transaction-Id", "ABCD1234567890EF1234567890");

        ObjectMapper ob = new ObjectMapper();
        try {
            ResponseEntity<TriggerEventResponse> response = super.posttriggerEvent(headers, request, TriggerEventResponse.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                notificationTrigger.setHttpStatus(response.getStatusCode().toString());
                notificationTrigger.setNotificationEventId(response.getBody().getEventId());
                notificationTrigger.setStatus(NotificationStatus.SENT);
            } else {
                notificationTrigger.setHttpStatus(response.getStatusCode().toString());
                notificationTrigger.setStatus(NotificationStatus.FAILED);
                notificationTrigger.setErrorMessage(ob.writeValueAsString(response.getBody()));
            }
        } catch (RestClientException | JsonProcessingException e) {
            notificationTrigger.setStatus(NotificationStatus.FAILED);
            notificationTrigger.setErrorMessage(e.getMessage());
        } finally {
            this.notificationTriggerRepository.save(notificationTrigger, tenantId);
        }
    }

    @Async
    public void initiateCookieConsentHandleCreatedNotification(NotificationEvent eventType,
                                            String tenantId,
                                            String businessId,
                                            CustomerIdentifiers customerIdentifiers,
                                            Object eventPayload,
                                            LANGUAGE language,
                                            String consentHandleId) {
        try {
            triggerNotificationEvent(eventType, tenantId, businessId, customerIdentifiers,
                    null , eventPayload, language);
        } catch (Exception e) {
            log.error("Notification trigger failed for CookieConsentHandleCreatedNotification event: {}, Cookie consent id: {}",
                    eventType.getEventId(), consentHandleId);
        }
    }

    @Async
    public void initiateCookieConsentCreatedNotification(NotificationEvent eventType,
                                                               String tenantId,
                                                               String businessId,
                                                               CustomerIdentifiers customerIdentifiers,
                                                               Object eventPayload,
                                                               LANGUAGE language,
                                                               String consentId) {
        try {
            triggerNotificationEvent(eventType, tenantId, businessId, customerIdentifiers,
                    null , eventPayload, language);
        } catch (Exception e) {
            log.error("Notification trigger failed for CookieConsentCreatedNotification event: {}, Cookie consent id: {}",
                    eventType.getEventId(), consentId);
        }
    }

    @Async
    public void initiateNewCookieConsentVersionCreatedNotification(NotificationEvent eventType,
                                                         String tenantId,
                                                         String businessId,
                                                         CustomerIdentifiers customerIdentifiers,
                                                         Object eventPayload,
                                                         LANGUAGE language,
                                                         String consentId) {
        try {
            triggerNotificationEvent(eventType, tenantId, businessId, customerIdentifiers,
                    null , eventPayload, language);
        } catch (Exception e) {
            log.error("Notification trigger failed for NewCookieConsentVersionCreated event: {}, Cookie consent id: {}",
                    eventType.getEventId(), consentId);
        }
    }

    @Async
    public void initiateTokenValidationSuccessNotification(NotificationEvent eventType,
                                                                   String tenantId,
                                                                   String businessId,
                                                                   CustomerIdentifiers customerIdentifiers,
                                                                   Object eventPayload,
                                                                   LANGUAGE language,
                                                                   String consentId) {
        try {
            triggerNotificationEvent(eventType, tenantId, businessId, customerIdentifiers,
                    null , eventPayload, language);
        } catch (Exception e) {
            log.error("Notification trigger failed for TokenValidationSuccess event: {}, Cookie consent id: {}",
                    eventType.getEventId(), consentId);
        }
    }

    @Async
    public void initiateConsentRevokedNotification(NotificationEvent eventType,
                                                           String tenantId,
                                                           String businessId,
                                                           CustomerIdentifiers customerIdentifiers,
                                                           Object eventPayload,
                                                           LANGUAGE language,
                                                           String consentId) {
        try {
            triggerNotificationEvent(eventType, tenantId, businessId, customerIdentifiers,
                    null , eventPayload, language);
        } catch (Exception e) {
            log.error("Notification trigger failed for ConsentRevoked event: {}, Cookie consent id: {}",
                    eventType.getEventId(), consentId);
        }
    }
}
