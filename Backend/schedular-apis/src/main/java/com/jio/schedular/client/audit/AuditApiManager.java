package com.jio.schedular.client.audit;

import com.jio.schedular.utils.RestApiManager;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Thin HTTP adapter mirroring NotificationApiManager shape.
 */
@Component
public class AuditApiManager extends RestApiManager {

    @Value("${audit.service.base.url}")
    private String auditServiceBaseUrl;

    @Value("${audit.service.endpoints}")
    private String triggerEventEndpoint;

    public <I, O> ResponseEntity<I> gettriggerEvent(Map<String, String> headers, O requestBody, Class<I> responseType) {
        return super.get(auditServiceBaseUrl, triggerEventEndpoint, headers, responseType);
    }

    public <I, O> ResponseEntity<I> posttriggerEvent(Map<String, String> headers, O requestBody, Class<I> responseType) {
        return super.post(auditServiceBaseUrl, triggerEventEndpoint, headers, requestBody, responseType);
    }
}