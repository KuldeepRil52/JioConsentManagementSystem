package com.jio.digigov.auditmodule.client.notification;

import com.jio.digigov.auditmodule.util.RestApiManager;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.ResponseEntity;

import java.util.Map;

@Configuration
public class NotificationApiManager extends RestApiManager {

    @Value("${notification.service.base.url}")
    private String notificationServiceBaseUrl;

    @Value("${notification.service.endpoints.trigger-event}")
    private String triggerEventEndpoint;

    public <REQ, RES> ResponseEntity<REQ> gettriggerEvernt(Map<String, String> headers, RES requestBody, Class<REQ> responseType) {
        return super.get(notificationServiceBaseUrl, triggerEventEndpoint, headers, responseType);
    }

    public <REQ, RES> ResponseEntity<REQ> posttriggerEvernt(Map<String, String> headers, RES requestBody, Class<REQ> responseType) {
        return super.post(notificationServiceBaseUrl, triggerEventEndpoint, headers, requestBody, responseType);
    }
}