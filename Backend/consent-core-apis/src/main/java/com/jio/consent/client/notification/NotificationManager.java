package com.jio.consent.client.notification;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jio.consent.client.notification.request.TriggerEventRequest;
import com.jio.consent.client.notification.response.TriggerEventResponse;
import com.jio.consent.constant.Constants;
import com.jio.consent.dto.CustomerIdentifiers;
import com.jio.consent.dto.LANGUAGE;
import com.jio.consent.dto.NotificationEvent;
import com.jio.consent.dto.NotificationStatus;
import com.jio.consent.entity.NotificationTrigger;
import com.jio.consent.repository.NotificationTriggerRepository;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.ThreadContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
@Slf4j
public class NotificationManager extends NotificationApiManager {

    NotificationTriggerRepository notificationTriggerRepository;

    @Autowired
    public NotificationManager(NotificationTriggerRepository notificationTriggerRepository) {
        this.notificationTriggerRepository = notificationTriggerRepository;
    }

    @Async
    public void triggerConsentEvent(NotificationEvent eventType,
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
                .triggerId(UUID.randomUUID().toString())
                .eventType(eventId)
                .resource(resource)
                .businessId(businessId)
                .status(com.jio.consent.dto.NotificationStatus.PENDING)
                .eventPayload(eventPayload)
                .customerIdentifiers(customerIdentifiers)
                .dataProcessorsIds(dataProcessorIds)
                .build();

        notificationTrigger = this.notificationTriggerRepository.save(notificationTrigger, tenantId);

        Map<String, String> headers = new HashMap<>();
        headers.put("X-Tenant-Id", tenantId);
        headers.put("X-Business-Id", businessId);

        //Todo: add logic to generate unique transaction id
        headers.put("X-Transaction-Id", "ABCD1234567890EF1234567890");


        ObjectMapper ob = new ObjectMapper();
        try {
            ResponseEntity<TriggerEventResponse> response = super.posttriggerEvernt(headers, request, TriggerEventResponse.class);
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
    public void initiateConsentHandleNotification(NotificationEvent notificationEvent,
                                                  String tenantId,
                                                  String businessId,
                                                  CustomerIdentifiers customerIdentifiers,
                                                  List<String> processorActivityIds,
                                                  Object eventPayload,
                                                  LANGUAGE language,
                                                  String consentHandleId) {

        try {
            triggerConsentEvent(notificationEvent,
                    tenantId,
                    businessId,
                    customerIdentifiers,
                    processorActivityIds,
                    eventPayload,
                    language);
        } catch (Exception e) {
            log.error("CONSENT_CREATED trigger failed for consent handle id: {}, error: {}", consentHandleId, e.getMessage());
        }

    }
}
