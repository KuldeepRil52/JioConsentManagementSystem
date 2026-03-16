package com.jio.partnerportal.client.notification;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jio.partnerportal.client.notification.request.OnboardingSetupRequest;
import com.jio.partnerportal.client.notification.request.TriggerEventRequest;
import com.jio.partnerportal.client.notification.response.OnboardingSetupResponse;
import com.jio.partnerportal.client.notification.response.TriggerEventResponse;
import com.jio.partnerportal.constant.ErrorCodes;
import com.jio.partnerportal.dto.IdentityType;
import com.jio.partnerportal.entity.DataBreachNotify;
import com.jio.partnerportal.entity.DataBreachReport;
import com.jio.partnerportal.entity.NotificationTrigger;
import com.jio.partnerportal.entity.NotificationTriggerCentral;
import com.jio.partnerportal.exception.PartnerPortalException;
import com.jio.partnerportal.multitenancy.TenantMongoTemplateProvider;
import com.jio.partnerportal.repository.NotificationTriggerRepository;
import com.jio.partnerportal.repository.NotificationTriggerCentralRepository;
import com.mongodb.client.result.UpdateResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;

@Component
@Slf4j
public class NotificationManager extends NotificationApiManager {

    @Autowired
    private TenantMongoTemplateProvider tenantMongoTemplateProvider;

    @Autowired
    private NotificationTriggerRepository notificationTriggerRepository;

    @Autowired
    private NotificationTriggerCentralRepository notificationTriggerCentralRepository;

    @Value("${notification.service.provider.type}")
    private String providerType;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    public NotificationManager() {
        super();
    }

    private void updateNotificationStatus(
            String tenantId,
            String notifyId,
            String consentId,
            DataBreachReport.NotificationStatus status,
            String reason) {

        log.info("Updating notifyId={} consentId={} with status={}", notifyId, consentId, status);

        MongoTemplate tenantMongoTemplate = tenantMongoTemplateProvider.getMongoTemplate(tenantId);

        Query query = new Query(Criteria.where("notifyId").is(notifyId)
                .and("consent.consentId").is(consentId));

        Update update = new Update()
                .set("status", status.name())
                .set("notifiedAt", LocalDateTime.now(ZoneOffset.UTC))
                .set("failureReason", reason);

        UpdateResult result = tenantMongoTemplate.updateFirst(query, update, "data_breach_notify");
        log.info("Updated tenant DB {} → Matched: {}, Modified: {}",
                tenantMongoTemplate.getDb().getName(),
                result.getMatchedCount(), result.getModifiedCount());
    }

    /**
     * Setup notification service onboarding with DLT mappings
     */
    public OnboardingSetupResponse setupNotificationOnboarding(
            String tenantId,
            String businessId,
            String scopeLevel) throws PartnerPortalException {

        try {
            Map<String, String> headers = createHeaders(tenantId, businessId, scopeLevel);

            log.info("Setting up notification onboarding for tenant: {}, business: {}", tenantId, businessId);

            // Create request body with temporary DLT data
            OnboardingSetupRequest request = createTemporaryOnboardingRequest();

            ResponseEntity<OnboardingSetupResponse> response = postOnboardingSetup(
                    headers,
                    request,
                    OnboardingSetupResponse.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                log.info("Notification onboarding setup completed successfully for tenant: {}", tenantId);
                return response.getBody();
            } else {
                log.error("Failed to setup notification onboarding. Status: {}", response.getStatusCode());
                throw new PartnerPortalException(ErrorCodes.JCMP0001);
            }

        } catch (RestClientException e) {
            log.error("Error calling notification service for onboarding setup: {}", e.getMessage());
            throw new PartnerPortalException(ErrorCodes.JCMP0001);
        }
    }

    /**
     * Trigger a notification event
     */
    @Async
    public void triggerEvent(
            String tenantId,
            String businessId,
            String scopeLevel,
            TriggerEventRequest request) throws PartnerPortalException {

        String triggerId = UUID.randomUUID().toString();
        NotificationTrigger notificationTrigger = null;

        try {
            // Create notification trigger with PENDING status
            notificationTrigger = NotificationTrigger.builder()
                    .triggerId(triggerId)
                    .eventType(request.getEventType())
                    .resource(request.getResource())
                    .businessId(businessId)
                    .status(DataBreachReport.NotificationStatus.PENDING)
                    .eventPayload(request.getEventPayload())
                    .customerIdentifiers(request.getCustomerIdentifiers())
                    .dataProcessorsIds(request.getDataProcessorIds())
                    .build();

            // Save notification trigger before calling API
            notificationTriggerRepository.save(notificationTrigger, tenantId);

            Map<String, String> headers = createHeaders(tenantId, businessId, scopeLevel);

            log.info("Triggering notification event: {} for tenant: {}, business: {}, triggerId: {}",
                    request.getEventType(), tenantId, businessId, triggerId);

            ResponseEntity<TriggerEventResponse> response = postTriggerEvent(
                    headers,
                    request,
                    TriggerEventResponse.class
            );

            // Update notification trigger based on response
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                TriggerEventResponse responseBody = response.getBody();
                if (responseBody != null) {
                    notificationTrigger.setHttpStatus(response.getStatusCode().toString());
                    notificationTrigger.setNotificationEventId(responseBody.getEventId());
                    notificationTrigger.setStatus(DataBreachReport.NotificationStatus.SENT);

                    log.info("Notification event triggered successfully. Event ID: {}, triggerId: {}",
                            responseBody.getEventId(), triggerId);
                } else {
                    notificationTrigger.setHttpStatus(response.getStatusCode().toString());
                    notificationTrigger.setStatus(DataBreachReport.NotificationStatus.FAILED);
                    notificationTrigger.setErrorMessage("Response body is null");
                }
            } else {
                notificationTrigger.setHttpStatus(response.getStatusCode().toString());
                notificationTrigger.setStatus(DataBreachReport.NotificationStatus.FAILED);
                try {
                    notificationTrigger.setErrorMessage(objectMapper.writeValueAsString(response.getBody()));
                } catch (JsonProcessingException e) {
                    notificationTrigger.setErrorMessage("Failed to parse error response");
                }
            }

            // Save updated notification trigger
            notificationTriggerRepository.save(notificationTrigger, tenantId);

        } catch (RestClientException e) {
            log.error("Error calling notification service for event trigger: {}", e.getMessage());

            // Update notification trigger with error
            if (notificationTrigger != null) {
                notificationTrigger.setStatus(DataBreachReport.NotificationStatus.FAILED);
                notificationTrigger.setErrorMessage(e.getMessage());
                notificationTriggerRepository.save(notificationTrigger, tenantId);
            }
        }
    }

    /**
     * Trigger a notification event (Central - replaces NotificationEvent)
     * Saves to central database (not tenant-specific)
     */
    @Async
    public void triggerEventCentral(
            String tenantId,
            String businessId,
            String clientId,
            String scopeLevel,
            TriggerEventRequest request) throws PartnerPortalException {

        String triggerId = UUID.randomUUID().toString();
        NotificationTriggerCentral notificationTriggerCentral = null;

        try {
            // Create notification trigger central with PENDING status
            notificationTriggerCentral = NotificationTriggerCentral.builder()
                    .triggerId(triggerId)
                    .eventType(request.getEventType())
                    .resource(request.getResource())
                    .businessId(businessId)
                    .status(DataBreachReport.NotificationStatus.PENDING)
                    .eventPayload(request.getEventPayload())
                    .customerIdentifiers(request.getCustomerIdentifiers())
                    .dataProcessorsIds(request.getDataProcessorIds())
                    .build();

            // Save notification trigger central before calling API
            notificationTriggerCentralRepository.save(notificationTriggerCentral);

            Map<String, String> headers = createHeaders(tenantId, businessId, scopeLevel);

            log.info("Triggering central notification event: {} for tenant: {}, business: {}, triggerId: {}",
                    request.getEventType(), tenantId, businessId, triggerId);

            ResponseEntity<TriggerEventResponse> response = postSystemTriggerEvent(
                    headers,
                    request,
                    TriggerEventResponse.class
            );

            // Update notification trigger central based on response
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                TriggerEventResponse responseBody = response.getBody();
                if (responseBody != null) {
                    notificationTriggerCentral.setHttpStatus(response.getStatusCode().toString());
                    notificationTriggerCentral.setNotificationEventId(responseBody.getEventId());
                    notificationTriggerCentral.setStatus(DataBreachReport.NotificationStatus.SENT);

                    log.info("Central notification event triggered successfully. Event ID: {}, triggerId: {}",
                            responseBody.getEventId(), triggerId);
                } else {
                    notificationTriggerCentral.setHttpStatus(response.getStatusCode().toString());
                    notificationTriggerCentral.setStatus(DataBreachReport.NotificationStatus.FAILED);
                    notificationTriggerCentral.setErrorMessage("Response body is null");
                }
            } else {
                notificationTriggerCentral.setHttpStatus(response.getStatusCode().toString());
                notificationTriggerCentral.setStatus(DataBreachReport.NotificationStatus.FAILED);
                try {
                    notificationTriggerCentral.setErrorMessage(objectMapper.writeValueAsString(response.getBody()));
                } catch (JsonProcessingException e) {
                    notificationTriggerCentral.setErrorMessage("Failed to parse error response");
                }
            }

            // Save updated notification trigger central
            notificationTriggerCentralRepository.save(notificationTriggerCentral);

        } catch (RestClientException e) {
            log.error("Error calling notification service for central event trigger: {}", e.getMessage());

            // Update notification trigger central with error
            if (notificationTriggerCentral != null) {
                notificationTriggerCentral.setStatus(DataBreachReport.NotificationStatus.FAILED);
                notificationTriggerCentral.setErrorMessage(e.getMessage());
                notificationTriggerCentralRepository.save(notificationTriggerCentral);
            }
        }
    }

    /**
     * Async method to trigger notification events
     */
    @Async
    public void triggerEventAsync(
            String tenantId,
            String businessId,
            String scopeLevel,
            TriggerEventRequest request,
            String notifyId,
            String notifyGroupId,
            String consentId) {

        String triggerId = UUID.randomUUID().toString();
        NotificationTrigger notificationTrigger = null;

        try {
            // Create notification trigger with PENDING status
            notificationTrigger = NotificationTrigger.builder()
                    .triggerId(triggerId)
                    .eventType(request.getEventType())
                    .resource(request.getResource())
                    .businessId(businessId)
                    .status(DataBreachReport.NotificationStatus.PENDING)
                    .eventPayload(request.getEventPayload())
                    .customerIdentifiers(request.getCustomerIdentifiers())
                    .dataProcessorsIds(request.getDataProcessorIds())
                    .build();

            // Save notification trigger before calling API
            notificationTriggerRepository.save(notificationTrigger, tenantId);

            Map<String, String> headers = createHeaders(tenantId, businessId, scopeLevel);

            log.info("Triggering async notification event: {} for tenant: {}, business: {}, triggerId: {}",
                    request.getEventType(), tenantId, businessId, triggerId);

            ResponseEntity<TriggerEventResponse> response = postTriggerEvent(
                    headers,
                    request,
                    TriggerEventResponse.class
            );

            // Update notification trigger based on response
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                TriggerEventResponse responseBody = response.getBody();
                if (responseBody != null) {
                    notificationTrigger.setHttpStatus(response.getStatusCode().toString());
                    notificationTrigger.setNotificationEventId(responseBody.getEventId());
                    notificationTrigger.setStatus(DataBreachReport.NotificationStatus.SENT);

                    log.info("Async notification event triggered successfully. Event ID: {}, triggerId: {}",
                            responseBody.getEventId(), triggerId);

                    // If successful, mark notification as DISPATCHED
                    updateNotificationStatus(tenantId, notifyId, consentId, DataBreachReport.NotificationStatus.DISPATCHED, null);

                    // Check if all consents dispatched and mark as COMPLETED if yes
                    markNotificationIfAllDispatched(tenantId, notifyGroupId);
                } else {
                    notificationTrigger.setHttpStatus(response.getStatusCode().toString());
                    notificationTrigger.setStatus(DataBreachReport.NotificationStatus.FAILED);
                    notificationTrigger.setErrorMessage("Response body is null");

                    // If failed, mark as FAILED with reason
                    updateNotificationStatus(tenantId, notifyId, consentId, DataBreachReport.NotificationStatus.FAILED, "Response body is null");
                }
            } else {
                notificationTrigger.setHttpStatus(response.getStatusCode().toString());
                notificationTrigger.setStatus(DataBreachReport.NotificationStatus.FAILED);
                try {
                    String errorMsg = objectMapper.writeValueAsString(response.getBody());
                    notificationTrigger.setErrorMessage(errorMsg);
                } catch (JsonProcessingException e) {
                    notificationTrigger.setErrorMessage("Failed to parse error response");
                }

                // If failed, mark as FAILED with reason
                String errorMessage = notificationTrigger.getErrorMessage();
                updateNotificationStatus(tenantId, notifyId, consentId, DataBreachReport.NotificationStatus.FAILED, errorMessage);
            }

            // Save updated notification trigger
            notificationTriggerRepository.save(notificationTrigger, tenantId);

        } catch (RestClientException e) {
            log.error("Error calling notification service for async event trigger: {}", e.getMessage());

            // Update notification trigger with error
            if (notificationTrigger != null) {
                notificationTrigger.setStatus(DataBreachReport.NotificationStatus.FAILED);
                notificationTrigger.setErrorMessage(e.getMessage());
                notificationTriggerRepository.save(notificationTrigger, tenantId);
            }

            // If failed, mark as FAILED with reason
            updateNotificationStatus(tenantId, notifyId, consentId, DataBreachReport.NotificationStatus.FAILED, e.getMessage());

        } catch (Exception e) {
            log.error("Async notification event trigger failed tenant={} business={} event={} error={}",
                    tenantId, businessId, request.getEventType(), e.getMessage(), e);

            // Update notification trigger with error
            if (notificationTrigger != null) {
                notificationTrigger.setStatus(DataBreachReport.NotificationStatus.FAILED);
                notificationTrigger.setErrorMessage(e.getMessage());
                notificationTriggerRepository.save(notificationTrigger, tenantId);
            }

            // If failed, mark as FAILED with reason
            updateNotificationStatus(tenantId, notifyId, consentId, DataBreachReport.NotificationStatus.FAILED, e.getMessage());
        }
    }

    /**
     * Create temporary onboarding request with DLT mappings for testing
     */
    private OnboardingSetupRequest createTemporaryOnboardingRequest() {
        Map<String, OnboardingSetupRequest.DltMapping> eventDltMappings = new HashMap<>();

        // Generate random DLT IDs for each event type
        String[] eventTypes = {
                "CONSENT_REQUEST_PENDING", "CONSENT_REQUEST_RENEWAL", "CONSENT_REQUEST_EXPIRED",
                "CONSENT_PREFERENCE_EXPIRY", "CONSENT_CREATED", "CONSENT_UPDATED", "CONSENT_WITHDRAWN",
                "CONSENT_EXPIRED", "CONSENT_RENEWED", "GRIEVANCE_RAISED", "GRIEVANCE_INPROCESS",
                "GRIEVANCE_L1_ESCALATED", "GRIEVANCE_L2_ESCALATED", "GRIEVANCE_RESOLVED",
                "DATA_DELETED", "DATA_SHARED", "DATA_RETENTION_DURATION_EXPIRED", "LOG_RETENTION_DURATION_EXPIRED"
        };

        for (String eventType : eventTypes) {
            // Generate random 19-digit DLT Entity ID (starting with 1001)
            String dltEntityId = "1001" + generateRandomDigits(15);

            // Generate random 19-digit DLT Template ID (starting with 1207)
            String dltTemplateId = "1207" + generateRandomDigits(15);

            log.debug("Generated DLT mapping for {}: EntityId={}, TemplateId={}",
                    eventType, dltEntityId, dltTemplateId);

            eventDltMappings.put(eventType,
                    OnboardingSetupRequest.DltMapping.builder()
                            .dltEntityId(dltEntityId)
                            .dltTemplateId(dltTemplateId)
                            .build());
        }

        return OnboardingSetupRequest.builder()
                .createTemplates(true)
                .createEventConfigurations(true)
                .createMasterList(true)
                .description("Complete onboarding with dynamically generated DLT IDs")
                .eventDltMappings(eventDltMappings)
                .providerType(providerType)
                .build();
    }

    /**
     * Generate random digits of specified length
     */
    private String generateRandomDigits(int length) {
        SecureRandom secureRandom = new SecureRandom();

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            sb.append(secureRandom.nextInt(10));
        }
        return sb.toString();
    }

    /**
     * Create headers for notification service calls
     */
    private Map<String, String> createHeaders(String tenantId, String businessId, String scopeLevel) {
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        headers.put("X-Tenant-ID", tenantId);
        headers.put("X-Business-ID", businessId);
        headers.put("X-Scope-Level", scopeLevel);
        headers.put("X-Transaction-Id", UUID.randomUUID().toString());
        return headers;
    }

    /**
     * Checks if all consents under a notifyGroupId are DISPATCHED.
     * If yes, marks notify and incident as COMPLETED and updates channel counts.
     */
    private void markNotificationIfAllDispatched(String tenantId, String notifyGroupId) {
        try {
            MongoTemplate tenantMongo = tenantMongoTemplateProvider.getMongoTemplate(tenantId);
            log.info("Triggered notification events for data_breach_notify notifyGroupId={} tenant={}", notifyGroupId, tenantId);

            // Get all records for this notifyGroupId
            List<DataBreachNotify> notifyRecords = tenantMongo.find(
                    new Query(Criteria.where("notifyGroupId").is(notifyGroupId)),
                    DataBreachNotify.class,
                    "data_breach_notify"
            );

            if (notifyRecords == null || notifyRecords.isEmpty()) {
                log.warn("No data_breach_notify records found for notifyGroupId={} tenant={}", notifyGroupId, tenantId);
                return;
            }

            // Check if all consents under this notifyGroupId are DISPATCHED
            boolean allDone = notifyRecords.stream().allMatch(n -> "DISPATCHED".equalsIgnoreCase(n.getStatus()));

            log.info("Notification status check for notifyGroupId={} tenant={}: allDone={}", notifyGroupId, tenantId, allDone);

            if (!allDone) return;

            // Mark all those notify records as COMPLETED
            for (DataBreachNotify n : notifyRecords) {
                n.setStatus(String.valueOf(DataBreachReport.NotificationStatus.COMPLETED));
                tenantMongo.save(n, "data_breach_notify");
            }
            log.info("All consents dispatched for notifyGroupId={} tenant={}: marked COMPLETED", notifyGroupId, tenantId);

            // Find related incident
            Query incidentQuery = new Query(Criteria.where("notificationDetails.notifyGroupId").is(notifyGroupId));
            DataBreachReport incident = tenantMongo.findOne(incidentQuery, DataBreachReport.class, "data_breach_reports");

            if (incident == null || incident.getNotificationDetails() == null) {
                log.warn("No related data_breach_report found for notifyGroupId={} tenant={}", notifyGroupId, tenantId);
                return;
            }

            incident.getNotificationDetails().setStatus("COMPLETED");

            // Count channels across all notify records
            int emailCount = 0;
            int smsCount = 0;

            for (DataBreachNotify n : notifyRecords) {
                if (n.getConsent() != null && n.getConsent().getCustomerIdentifiers() != null) {
                    IdentityType type = n.getConsent().getCustomerIdentifiers().getType();
                    if (type == IdentityType.EMAIL) emailCount++;
                    else if (type == IdentityType.MOBILE) smsCount++;
                }
            }

            log.info("Notification counts for incident={} notifyGroupId={}: emails={}, sms={}",
                    incident.getIncidentId(), notifyGroupId, emailCount, smsCount);

            // Update channels
            List<DataBreachReport.NotificationDetails.NotificationChannel> channels = new ArrayList<>();
            channels.add(DataBreachReport.NotificationDetails.NotificationChannel.builder()
                    .notificationChannel(DataBreachReport.NotificationChannelType.EMAIL)
                    .notificationStatus(DataBreachReport.NotificationStatus.DISPATCHED)
                    .count(emailCount)
                    .build());
            channels.add(DataBreachReport.NotificationDetails.NotificationChannel.builder()
                    .notificationChannel(DataBreachReport.NotificationChannelType.SMS)
                    .notificationStatus(DataBreachReport.NotificationStatus.DISPATCHED)
                    .count(smsCount)
                    .build());

            incident.getNotificationDetails().setChannels(channels);
            incident.getNotificationDetails().setUpdatedAt(LocalDateTime.now());
            tenantMongo.save(incident, "data_breach_reports");

            log.info("Notification cycle completed for incident={} notifyGroupId={} (emails={}, sms={})",
                    incident.getIncidentId(), notifyGroupId, emailCount, smsCount);

        } catch (Exception e) {
            log.error("Error during all-dispatched check for notifyGroupId={} tenant={}: {}", notifyGroupId, tenantId, e.getMessage(), e);
        }
    }
}
