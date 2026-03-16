package com.example.scanner.client;

import com.example.scanner.dto.request.AuditRequest;
import com.example.scanner.dto.response.AuditResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
@RequiredArgsConstructor
@Slf4j
public class AuditClient {

    private final RestTemplate restTemplate;

    @Value("${audit.service.base-url}")
    private String auditBaseUrl;

    @Value("${audit.service.endpoint}")
    private String auditEndpoint;

    public AuditResponse createAudit(AuditRequest auditRequest, String tenantId, String businessId, String transactionId) {
        try {
            String url = auditBaseUrl + auditEndpoint;

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-Tenant-ID", tenantId);
            headers.set("X-Business-ID", businessId);
            headers.set("X-Transaction-ID", transactionId);

            HttpEntity<AuditRequest> requestEntity = new HttpEntity<>(auditRequest, headers);

            log.debug("Sending audit log to: {} for action: {}", url, auditRequest.getActionType());

            ResponseEntity<AuditResponse> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    requestEntity,
                    AuditResponse.class
            );

            log.info("Audit logged: {} - Status: {}", auditRequest.getActionType(), response.getStatusCode());
            return response.getBody();

        } catch (Exception e) {
            log.error("Audit logging failed for action: {} ",
                    auditRequest.getActionType());
            return null;
        }
    }
}