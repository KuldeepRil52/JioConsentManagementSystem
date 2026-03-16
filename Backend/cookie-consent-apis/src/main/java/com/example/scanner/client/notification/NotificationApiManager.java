package com.example.scanner.client.notification;

import com.example.scanner.util.RestApiManager;
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


    public <REQ, RES> ResponseEntity<REQ> gettriggerEvent(Map<String, String> headers, RES requestBody, Class<REQ> responseType) {
        return super.get(notificationServiceBaseUrl, triggerEventEndpoint, headers, responseType);
    }

    public <REQ, RES> ResponseEntity<REQ> posttriggerEvent(Map<String, String> headers, RES requestBody, Class<REQ> responseType) {
        return super.post(notificationServiceBaseUrl, triggerEventEndpoint, headers, requestBody, responseType);
    }
}
