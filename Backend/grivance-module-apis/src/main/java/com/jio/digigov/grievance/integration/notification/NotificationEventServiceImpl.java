package com.jio.digigov.grievance.integration.notification;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jio.digigov.grievance.config.NotificationConfig;
import com.jio.digigov.grievance.dto.GrievanceConfigurationDTO;
import com.jio.digigov.grievance.dto.request.NotificationEventRequest;
import com.jio.digigov.grievance.entity.Grievance;
import com.jio.digigov.grievance.enumeration.GrievanceStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

/**
 * Implementation of NotificationEventService.
 * Responsible for triggering the external notification API for grievance events.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class NotificationEventServiceImpl implements NotificationEventService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final MongoTemplate mongoTemplate;
    private final NotificationConfig notificationConfig;

    @Override
    public void triggerNotification(Grievance grievance, String tenantId, String businessId, String grievanceId) {
        try {
            String notificationUrl = notificationConfig.getTriggerUrl();

            // -------------------- Fetch Configuration --------------------
            Query query = new Query();
            GrievanceConfigurationDTO configuration =
                    mongoTemplate.findOne(query, GrievanceConfigurationDTO.class, "grievance_configurations");

            // Determine event type based on grievance status
            String eventType = (grievance.getStatus() == GrievanceStatus.NEW)
                    ? "GRIEVANCE_RAISED"
                    : "GRIEVANCE_" + grievance.getStatus();

            boolean sendSms = false;
            boolean sendEmail = false;

            if (configuration != null &&
                    configuration.getConfigurationJson() != null &&
                    configuration.getConfigurationJson().getCommunicationConfig() != null) {
                sendSms = configuration.getConfigurationJson().getCommunicationConfig().isSms();
                sendEmail = configuration.getConfigurationJson().getCommunicationConfig().isEmail();
            } else {
                sendEmail = true; // Default to email if config is missing
                sendSms = true;
            }

            // -------------------- Determine Customer Identifier --------------------
            String identifierType = "UNKNOWN";
            String identifierValue = "";

            if (grievance.getUserDetails() != null && !grievance.getUserDetails().isEmpty()) {
                Map<String, Object> userDetails = grievance.getUserDetails();

                Optional<Map.Entry<String, Object>> mobileEntry = userDetails.entrySet().stream()
                        .filter(e -> e.getKey().toLowerCase().contains("mobile")
                                || e.getKey().toLowerCase().contains("phone")
                                || e.getKey().toLowerCase().contains("contact"))
                        .findFirst();

                Optional<Map.Entry<String, Object>> emailEntry = userDetails.entrySet().stream()
                        .filter(e -> e.getKey().toLowerCase().contains("email"))
                        .findFirst();

                if (sendEmail && emailEntry.isPresent()) {
                    identifierType = "EMAIL";
                    identifierValue = String.valueOf(emailEntry.get().getValue());
                }  else if (sendSms && mobileEntry.isPresent()) {
                    identifierType = "MOBILE";
                    identifierValue = String.valueOf(mobileEntry.get().getValue());
                } else {
                    log.warn("Notification config enabled but no valid contact found for grievanceId={}", grievanceId);
                }
            }

            // -------------------- Build Notification Payload --------------------
            Map<String, Object> eventPayload = objectMapper.convertValue(grievance, Map.class);
            eventPayload.put("createdAt", grievance.getCreatedAt() != null ? grievance.getCreatedAt().toString() : null);
            eventPayload.put("updatedAt", grievance.getUpdatedAt() != null ? grievance.getUpdatedAt().toString() : null);

            Map<String, Object> customerIdentifiers = Map.of(
                    "type", identifierType,
                    "value", identifierValue
            );

            NotificationEventRequest notificationRequest = NotificationEventRequest.builder()
                    .eventType(eventType)
                    .resource("grievance")
                    .source("grievance_service")
                    .customerIdentifiers(customerIdentifiers)
                    .eventPayload(eventPayload)
                    .build();

            // -------------------- HTTP Call --------------------
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-Tenant-Id", tenantId);
            headers.set("X-Business-Id", businessId);
            headers.set("X-Transaction-Id", UUID.randomUUID().toString());

            HttpEntity<NotificationEventRequest> requestEntity = new HttpEntity<>(notificationRequest, headers);

            log.info("Triggering Notification API at {} with payload: {}", notificationUrl,
                    objectMapper.writeValueAsString(notificationRequest));

            ResponseEntity<String> response = restTemplate.postForEntity(notificationUrl, requestEntity, String.class);

            log.info("Notification API Response | Status: {}, Body: {}",
                    response.getStatusCode(), response.getBody());

        } catch (Exception ex) {
            log.error("Failed to trigger Notification API: {}", ex.getMessage(), ex);
        }
    }
}
