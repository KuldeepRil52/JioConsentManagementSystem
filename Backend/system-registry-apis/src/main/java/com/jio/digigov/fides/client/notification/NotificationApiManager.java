package com.jio.digigov.fides.client.notification;

import com.jio.digigov.fides.utils.RestApiManager;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class NotificationApiManager extends RestApiManager {

    @Value("${notification.service.base.url}")
    private String notificationServiceBaseUrl;

    @Value("${notification.service.endpoints.trigger-event}")
    private String triggerEventEndpoint;

    @Value("${notification.service.endpoints.missed-notification}")
    private String missedNotificationEndpoint;

    public <I, O> ResponseEntity<I> gettriggerEvent(Map<String, String> headers, O requestBody, Class<I> responseType) {
        return super.get(notificationServiceBaseUrl, triggerEventEndpoint, headers, responseType);
    }

    public <I, O> ResponseEntity<I> posttriggerEvent(Map<String, String> headers, O requestBody, Class<I> responseType) {
        return super.post(notificationServiceBaseUrl, triggerEventEndpoint, headers, requestBody, responseType);
    }

    public <I, O> ResponseEntity<I> putMissedNotification(Map<String, String> headers, O requestBody, Class<I> responseType, String notificationId) {
        // Build endpoint dynamically
        String endpoint = missedNotificationEndpoint.replace("{notificationId}", notificationId);
        return super.put(notificationServiceBaseUrl, endpoint, headers, requestBody, responseType);
    }
}
