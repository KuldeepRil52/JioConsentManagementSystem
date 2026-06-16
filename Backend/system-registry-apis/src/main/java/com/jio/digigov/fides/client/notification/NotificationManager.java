package com.jio.digigov.fides.client.notification;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jio.digigov.fides.client.notification.request.MissedNotificationRequest;
import com.jio.digigov.fides.client.notification.request.TriggerEventRequest;
import com.jio.digigov.fides.client.notification.response.MissedNotificationResponse;
import com.jio.digigov.fides.client.notification.response.TriggerEventResponse;
import com.jio.digigov.fides.dto.CustomerIdentifiers;
import com.jio.digigov.fides.enumeration.LANGUAGE;
import com.jio.digigov.fides.enumeration.NotificationEvent;
import com.jio.digigov.fides.enumeration.NotificationStatus;
import com.jio.digigov.fides.entity.NotificationTrigger;
import com.jio.digigov.fides.repository.NotificationTriggerRepository;
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
                                            String jobId) {
        try {
            triggerNotificationEvent(eventType, tenantId, businessId, customerIdentifiers, 
                    dataProcessorIds, eventPayload, language);
        } catch (Exception e) {
            log.error("Notification trigger failed for event: {}, job id: {}, error: {}", 
                    eventType.getEventId(), jobId, e.getMessage());
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

    public void handleMissedNotification(String tenantId,
                                      String businessId,
                                      String recipientType,
                                      String recipientId,
                                      String notificationId) {

        // Build request body
        MissedNotificationRequest request = new MissedNotificationRequest();
        request.setStatus("DELETED");
        // request.setAcknowledgementId(acknowledgementId);
        request.setRemark("DELETED");

        // Prepare headers
        Map<String, String> headers = new HashMap<>();
        headers.put("tenant-id", tenantId);
        headers.put("business-id", businessId);
        headers.put("requestor-type", recipientType);
        headers.put("data-processor-id", recipientId);
        headers.put("Accept", "application/json");
        headers.put("Content-Type", "application/json");

        try {
            ResponseEntity<MissedNotificationResponse> response =
                    super.putMissedNotification(
                            headers,
                            request,
                            MissedNotificationResponse.class,
                            notificationId
                    );

            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("Missed notification [{}] marked as DELETED", notificationId);
            } else {
                log.error("Failed to update missed notification [{}], status={}",
                        notificationId,
                        response.getStatusCode());
            }

        } catch (RestClientException e) {
            log.error("Error while calling missed notification API for notificationId={}",
                    notificationId, e);
        }
    }
}
