package com.jio.schedular.client.notification;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jio.schedular.client.notification.request.TriggerEventRequest;
import com.jio.schedular.client.notification.response.TriggerEventResponse;
import com.jio.schedular.dto.CustomerIdentifiers;
import com.jio.schedular.enums.LANGUAGE;
import com.jio.schedular.enums.NotificationEvent;
import com.jio.schedular.enums.NotificationStatus;
import com.jio.schedular.entity.NotificationTrigger;
import com.jio.schedular.repository.NotificationTriggerRepository;
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

    @Async
    public void initiateConsentNotification(NotificationEvent eventType,
                                            String tenantId,
                                            String businessId,
                                            CustomerIdentifiers customerIdentifiers,
                                            List<String> dataProcessorIds,
                                            Object eventPayload,
                                            LANGUAGE language,
                                            String consentId) {
        try {
            triggerNotificationEvent(eventType, tenantId, businessId, customerIdentifiers, 
                    dataProcessorIds, eventPayload, language);
        } catch (Exception e) {
            log.error("Notification trigger failed for event: {}, consent id: {}, error: {}", 
                    eventType.getEventId(), consentId, e.getMessage());
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
            triggerNotificationEvent(notificationEvent,
                    tenantId,
                    businessId,
                    customerIdentifiers,
                    processorActivityIds,
                    eventPayload,
                    language);
        } catch (Exception e) {
            log.error("CONSENT_CREATED trigger failed for schedular handle id: {}, error: {}", consentHandleId, e.getMessage());
        }

    }

    @Async
    public void initiateGrievanceNotification(String grievanceStatus,
                                            String tenantId,
                                            String businessId,
                                            CustomerIdentifiers customerIdentifiers,
                                            Object eventPayload,
                                            LANGUAGE language,
                                            String grievanceId) {
        try {
            NotificationEvent eventType = NotificationEvent.valueOf("GRIEVANCE_" + grievanceStatus);
            triggerNotificationEvent(eventType, tenantId, businessId, customerIdentifiers, 
                    null, eventPayload, language);
        } catch (Exception e) {
            log.error("Notification trigger failed for grievance: {}, error: {}", grievanceId, e.getMessage());
        }
    }

    @Async
    public void initiateRetentionNotification(NotificationEvent eventType,
                                            String tenantId,
                                            CustomerIdentifiers customerIdentifiers,
                                            Object eventPayload,
                                            LANGUAGE language,
                                            String runId) {
        try {
            triggerNotificationEvent(eventType, tenantId, tenantId, customerIdentifiers, 
                    null, eventPayload, language);
        } catch (Exception e) {
            log.error("Notification trigger failed for event: {}, schedular run id: {}, error: {}", 
                    eventType.getEventId(), runId, e.getMessage());
        }
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

        // add logic to generate unique transaction id
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
    public void initiateCookieConsentNotification(NotificationEvent eventType,
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
            log.error("Notification trigger failed for event: {}, Cookie consent id: {}, error: {}",
                    eventType.getEventId(), consentId, e.getMessage());
        }
    }

    @Async
    public void initiateCookieConsentHandleNotification(NotificationEvent notificationEvent,
                                                  String tenantId,
                                                  String businessId,
                                                  CustomerIdentifiers customerIdentifiers,
                                                  Object eventPayload,
                                                  LANGUAGE language,
                                                  String consentHandleId) {

        try {
            triggerNotificationEvent(notificationEvent,
                    tenantId,
                    businessId,
                    customerIdentifiers,
                    null,
                    eventPayload,
                    language);
        } catch (Exception e) {
            log.error("CONSENT_CREATED trigger failed for schedular handle id: {}, error: {}", consentHandleId, e.getMessage());
        }

    }
}
