package com.jio.digigov.notification.service.audit.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jio.digigov.notification.dto.audit.AuditRequestDto;
import com.jio.digigov.notification.service.audit.AuditEventService;
import com.jio.digigov.notification.util.IpAddressUtil;
import com.jio.digigov.notification.util.ServerIpAddressUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Implementation of AuditEventService for sending audit events to central audit service.
 * All methods execute asynchronously to prevent blocking business operations.
 */
@Slf4j
@Service
public class AuditEventServiceImpl implements AuditEventService {

    @Value("${audit.url:}")
    private String auditUrl;

    @Value("${audit.enabled:true}")
    private boolean auditEnabled;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Autowired
    public AuditEventServiceImpl(RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    @Async("externalApiExecutor")
    public void auditTemplateOperation(
            String templateId,
            String actionType,
            String tenantId,
            String businessId,
            String transactionId,
            HttpServletRequest request) {

        // Skip audit for SYSTEM-level operations
        if (isSystemLevelOperation(tenantId, businessId)) {
            log.debug("Skipping audit for SYSTEM-level template operation: templateId={}", templateId);
            return;
        }

        try {
            String clientIp = IpAddressUtil.getClientIp(request);

            // Determine actor (DATA_FIDUCIARY with businessId, fallback to tenantId)
            String actorId = (businessId != null && !businessId.isEmpty()) ? businessId : tenantId;
            String initiator = "DATA_FIDUCIARY";

            Map<String, Object> context = new HashMap<>();
            context.put("txnId", transactionId);
            context.put("ipAddress", clientIp);

            Map<String, Object> extra = new HashMap<>();
            extra.put("legalEntityName", "");
            extra.put("pan", "");

            AuditRequestDto auditDto = AuditRequestDto.builder()
                    .businessId(businessId != null ? businessId : tenantId)
                    .actor(AuditRequestDto.Actor.builder()
                            .id(actorId)
                            .role("DATA_FIDUCIARY")
                            .type("USER")
                            .build())
                    .group("NOTIFICATION")
                    .component("NOTIFICATION_TEMPLATE")
                    .actionType(actionType)
                    .resource(AuditRequestDto.Resource.builder()
                            .type("TemplateID")
                            .id(templateId)
                            .build())
                    .initiator(initiator)
                    .context(context)
                    .extra(extra)
                    .build();

            sendAuditEvent(auditDto, tenantId, businessId, transactionId);

        } catch (Exception ex) {
            log.error("Failed to send audit event for template operation - templateId: {}, actionType: {}, error: {}",
                    templateId, actionType, ex.getMessage(), ex);
        }
    }

    @Override
    @Async("externalApiExecutor")
    public void auditTriggerEvent(
            String eventId,
            String eventStatus,
            String tenantId,
            String businessId,
            String transactionId,
            HttpServletRequest request,
            boolean isSystemTrigger) {

        // Skip audit for SYSTEM-level operations
        if (isSystemLevelOperation(tenantId, businessId)) {
            log.debug("Skipping audit for SYSTEM-level trigger event: eventId={}", eventId);
            return;
        }

        try {
            String clientIp = IpAddressUtil.getClientIp(request);

            // Determine actor based on trigger type
            String actorId;
            String actorRole;
            String actorType;

            if (isSystemTrigger) {
                actorId = "SYSTEM";
                actorRole = "SYSTEM";
                actorType = "SYSTEM";
            } else {
                actorId = (businessId != null && !businessId.isEmpty()) ? businessId : tenantId;
                actorRole = "DATA_FIDUCIARY";
                actorType = "USER";
            }

            String initiator = actorRole;

            Map<String, Object> context = new HashMap<>();
            context.put("txnId", transactionId);
            context.put("ipAddress", clientIp);

            Map<String, Object> extra = new HashMap<>();
            extra.put("legalEntityName", "");
            extra.put("pan", "");

            AuditRequestDto auditDto = AuditRequestDto.builder()
                    .businessId(businessId != null ? businessId : tenantId)
                    .actor(AuditRequestDto.Actor.builder()
                            .id(actorId)
                            .role(actorRole)
                            .type(actorType)
                            .build())
                    .group("NOTIFICATION")
                    .component("Trigger_Event")
                    .actionType(eventStatus)
                    .resource(AuditRequestDto.Resource.builder()
                            .type("EventId")
                            .id(eventId)
                            .build())
                    .initiator(initiator)
                    .context(context)
                    .extra(extra)
                    .build();

            sendAuditEvent(auditDto, tenantId, businessId, transactionId);

        } catch (Exception ex) {
            log.error("Failed to send audit event for trigger - eventId: {}, status: {}, error: {}",
                    eventId, eventStatus, ex.getMessage(), ex);
        }
    }

    @Override
    @Async("externalApiExecutor")
    public void auditOtpOperation(
            String eventId,
            String operation,
            String status,
            String tenantId,
            String businessId,
            String transactionId,
            HttpServletRequest request) {

        // Skip audit for SYSTEM-level operations
        if (isSystemLevelOperation(tenantId, businessId)) {
            log.debug("Skipping audit for SYSTEM-level OTP operation: eventId={}", eventId);
            return;
        }

        try {
            String clientIp = IpAddressUtil.getClientIp(request);
            String actionType = operation + "_" + status; // e.g., "INIT_SUCCESS", "VERIFY_FAILED"

            Map<String, Object> context = new HashMap<>();
            context.put("txnId", transactionId);
            context.put("ipAddress", clientIp);

            Map<String, Object> extra = new HashMap<>();
            extra.put("legalEntityName", "");
            extra.put("pan", "");

            AuditRequestDto auditDto = AuditRequestDto.builder()
                    .businessId(businessId != null ? businessId : tenantId)
                    .actor(AuditRequestDto.Actor.builder()
                            .id("SYSTEM")
                            .role("SYSTEM")
                            .type("SYSTEM")
                            .build())
                    .group("NOTIFICATION")
                    .component("OTP_SERVICE")
                    .actionType(actionType)
                    .resource(AuditRequestDto.Resource.builder()
                            .type("EventId")
                            .id(eventId)
                            .build())
                    .initiator("SYSTEM")
                    .context(context)
                    .extra(extra)
                    .build();

            sendAuditEvent(auditDto, tenantId, businessId, transactionId);

        } catch (Exception ex) {
            log.error("Failed to send audit event for OTP operation - eventId: {}, operation: {}, status: {}, error: {}",
                    eventId, operation, status, ex.getMessage(), ex);
        }
    }

    @Override
    @Async("externalApiExecutor")
    public void auditTenantOnboarding(
            String jobId,
            String status,
            String tenantId,
            String businessId,
            String transactionId) {

        // Skip audit for SYSTEM-level operations
        if (isSystemLevelOperation(tenantId, businessId)) {
            log.debug("Skipping audit for SYSTEM-level tenant onboarding: jobId={}", jobId);
            return;
        }

        try {
            String serverIp = ServerIpAddressUtil.getServerIp();
            Map<String, Object> context = new HashMap<>();
            context.put("txnId", transactionId);
            context.put("ipAddress", serverIp);

            Map<String, Object> extra = new HashMap<>();
            extra.put("legalEntityName", "");
            extra.put("pan", "");

            AuditRequestDto auditDto = AuditRequestDto.builder()
                    .businessId(businessId != null ? businessId : tenantId)
                    .actor(AuditRequestDto.Actor.builder()
                            .id("SYSTEM")
                            .role("SYSTEM")
                            .type("SYSTEM")
                            .build())
                    .group("NOTIFICATION")
                    .component("TENANT_ONBOARDING")
                    .actionType(status)
                    .resource(AuditRequestDto.Resource.builder()
                            .type("JobID")
                            .id(jobId)
                            .build())
                    .initiator("SYSTEM")
                    .context(context)
                    .extra(extra)
                    .build();

            sendAuditEvent(auditDto, tenantId, businessId, transactionId);

        } catch (Exception ex) {
            log.error("Failed to send audit event for tenant onboarding - jobId: {}, status: {}, error: {}",
                    jobId, status, ex.getMessage(), ex);
        }
    }

    @Override
    @Async("externalApiExecutor")
    public void auditNotificationDelivery(
            String notificationId,
            String status,
            String component,
            String tenantId,
            String businessId,
            String transactionId,
            String sourceIp,
            String correlationId) {

        // Skip audit for SYSTEM-level operations
        if (isSystemLevelOperation(tenantId, businessId)) {
            log.debug("Skipping audit for SYSTEM-level notification delivery: notificationId={}", notificationId);
            return;
        }

        try {
            Map<String, Object> context = new HashMap<>();
            context.put("txnId", transactionId);
            context.put("ipAddress", sourceIp != null ? sourceIp : "unknown");
            context.put("correlationId", correlationId);

            Map<String, Object> extra = new HashMap<>();
            extra.put("legalEntityName", "");
            extra.put("pan", "");

            AuditRequestDto auditDto = AuditRequestDto.builder()
                    .businessId(businessId != null ? businessId : tenantId)
                    .actor(AuditRequestDto.Actor.builder()
                            .id("SYSTEM")
                            .role("SYSTEM")
                            .type("SYSTEM")
                            .build())
                    .group("NOTIFICATION")
                    .component(component)
                    .actionType(status)
                    .resource(AuditRequestDto.Resource.builder()
                            .type("NotificationID")
                            .id(notificationId)
                            .build())
                    .initiator("SYSTEM")
                    .context(context)
                    .extra(extra)
                    .build();

            sendAuditEvent(auditDto, tenantId, businessId, transactionId);

        } catch (Exception ex) {
            log.error("Failed to send audit event for notification delivery - notificationId: {}, status: {}, component: {}, error: {}",
                    notificationId, status, component, ex.getMessage(), ex);
        }
    }

    /**
     * Common method to send audit event to central audit service.
     * Fire-and-forget pattern - errors are logged but not thrown.
     */
    private void sendAuditEvent(AuditRequestDto auditDto, String tenantId, String businessId, String transactionId) {
        if (!auditEnabled) {
            log.debug("Audit service is disabled, skipping audit event");
            return;
        }

        if (auditUrl == null || auditUrl.isEmpty()) {
            log.warn("Audit URL is not configured, skipping audit event");
            return;
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-Tenant-ID", tenantId != null ? tenantId : "");
            headers.set("X-Business-ID", businessId != null ? businessId : "");
            headers.set("X-Transaction-ID", transactionId != null ? transactionId : UUID.randomUUID().toString());

            HttpEntity<AuditRequestDto> requestEntity = new HttpEntity<>(auditDto, headers);

            log.info("Sending audit event to {} => Group: {}, Component: {}, ActionType: {}, Resource: {}",
                    auditUrl, auditDto.getGroup(), auditDto.getComponent(), auditDto.getActionType(),
                    auditDto.getResource().getId());
            log.debug("Audit event payload: {}", objectMapper.writeValueAsString(auditDto));

            ResponseEntity<String> response = restTemplate.postForEntity(auditUrl, requestEntity, String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("Audit event sent successfully - StatusCode: {}, Response: {}",
                        response.getStatusCode(), response.getBody());
            } else {
                log.warn("Audit event returned non-success status - StatusCode: {}, Response: {}",
                        response.getStatusCode(), response.getBody());
            }

        } catch (Exception ex) {
            // Fire-and-forget: Log error but don't throw exception
            log.error("Error sending audit event to central service - URL: {}, Error: {}",
                    auditUrl, ex.getMessage(), ex);
        }
    }

    /**
     * Checks if the operation is a SYSTEM-level operation.
     * SYSTEM-level operations should not trigger audit API calls.
     *
     * @param tenantId the tenant ID
     * @param businessId the business ID
     * @return true if both tenantId and businessId are "SYSTEM"
     */
    private boolean isSystemLevelOperation(String tenantId, String businessId) {
        return "SYSTEM".equals(tenantId) && "SYSTEM".equals(businessId);
    }
}
