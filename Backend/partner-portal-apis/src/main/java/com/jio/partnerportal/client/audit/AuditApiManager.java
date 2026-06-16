package com.jio.partnerportal.client.audit;

import com.jio.partnerportal.util.RestApiManager;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class AuditApiManager extends RestApiManager {

    @Value("${audit.service.base.url}")
    private String auditServiceBaseUrl;

    @Value("${audit.service.endpoints.audit}")
    private String auditEndpoint;

    public <REQ, RES> ResponseEntity<REQ> postAudit(Map<String, String> headers, RES requestBody, Class<REQ> responseType) {
        return super.post(auditServiceBaseUrl, auditEndpoint, headers, requestBody, responseType);
    }

}

