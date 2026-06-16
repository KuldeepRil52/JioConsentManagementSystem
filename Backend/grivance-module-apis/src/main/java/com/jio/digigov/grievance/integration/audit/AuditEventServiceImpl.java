package com.jio.digigov.grievance.integration.audit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jio.digigov.grievance.config.NotificationConfig;
import com.jio.digigov.grievance.dto.request.AuditRequestDto;
import com.jio.digigov.grievance.entity.Grievance;
import com.jio.digigov.grievance.entity.GrievanceTemplate;
import com.jio.digigov.grievance.enumeration.ActorRole;
import com.jio.digigov.grievance.enumeration.GrievanceStatus;
import com.jio.digigov.grievance.util.IpAddressUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import jakarta.servlet.http.HttpServletRequest;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
public class AuditEventServiceImpl implements AuditEventService {

    private final NotificationConfig notificationConfig;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Autowired
    public AuditEventServiceImpl(NotificationConfig notificationConfig,
                                 RestTemplate restTemplate,
                                 ObjectMapper objectMapper) {
        this.notificationConfig = notificationConfig;
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    // -------------------- GRIEVANCE AUDIT --------------------
    @Override
    public void triggerAuditEvent(Grievance grievance, String tenantId, String businessId, String transactionId, HttpServletRequest req) {
        String auditUrl = notificationConfig.getAuditUrl();

        try {
            String clientIp = IpAddressUtil.getClientIp(req);

            // Context and extra data
            Map<String, Object> contextMap = new HashMap<>();
            contextMap.put("txnId", transactionId);
            contextMap.put("ipAddress", clientIp);

            Map<String, Object> extraMap = Map.of(
                    "legalEntityName", "",
                    "pan", ""
            );

            String actorRole = grievance.getStatus() == GrievanceStatus.NEW
                    ? ActorRole.DATA_PRINCIPAL.toString()
                    : ActorRole.DATA_FIDUCIARY.toString();

            AuditRequestDto auditDto = AuditRequestDto.builder()
                    .businessId(businessId)
                    .actor(AuditRequestDto.Actor.builder()
                            .id(grievance.getGrievanceId())
                            .role(actorRole)
                            .type("USER")
                            .build())
                    .group("GRIEVANCE")
                    .component("GRIEVANCE")
                    .actionType(grievance.getStatus())
                    .resource(AuditRequestDto.Resource.builder()
                            .type("GrievanceID")
                            .id(grievance.getGrievanceId())
                            .build())
                    .initiator(actorRole)
                    .context(contextMap)
                    .extra(extraMap)
                    .build();

            sendAuditEvent(auditUrl, auditDto, tenantId, businessId);

        } catch (Exception ex) {
            log.error("Failed to send audit event for grievance: {}", ex.getMessage(), ex);
        }
    }

    // -------------------- TEMPLATE AUDIT --------------------
    @Override
    public void triggerAuditEventForTemplate(GrievanceTemplate template, String tenantId, String businessId, String transactionId, HttpServletRequest req) {
        String auditUrl = notificationConfig.getAuditUrl();

        try {
            String clientIp = IpAddressUtil.getClientIp(req);

            Map<String, Object> contextMap = Map.of(
                    "txnId", transactionId,
                    "ipAddress", clientIp
            );

            Map<String, Object> extraMap = Map.of(
                    "legalEntityName", "",
                    "pan", ""
            );

            AuditRequestDto auditDto = AuditRequestDto.builder()
                    .businessId(businessId)
                    .actor(AuditRequestDto.Actor.builder()
                            .id(template.getGrievanceTemplateId())
                            .role(ActorRole.DATA_PRINCIPAL.toString())
                            .type("USER")
                            .build())
                    .group("GRIEVANCE")
                    .component("GRIEVANCE_TEMPLATE")
                    .actionType(template.getStatus()) // DRAFT, PUBLISHED
                    .resource(AuditRequestDto.Resource.builder()
                            .type("GrievanceTemplateID")
                            .id(template.getGrievanceTemplateId())
                            .build())
                    .initiator(ActorRole.DATA_PRINCIPAL.toString())
                    .context(contextMap)
                    .extra(extraMap)
                    .build();

            sendAuditEvent(auditUrl, auditDto, tenantId, businessId);

        } catch (Exception ex) {
            log.error("Failed to send audit event for template: {}", ex.getMessage(), ex);
        }
    }

    // -------------------- COMMON METHOD --------------------
    private void sendAuditEvent(String auditUrl, AuditRequestDto auditDto, String tenantId, String businessId) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-Tenant-ID", tenantId);
            headers.set("X-Business-ID", businessId);
            headers.set("X-Transaction-ID", UUID.randomUUID().toString());

            HttpEntity<AuditRequestDto> requestEntity = new HttpEntity<>(auditDto, headers);

            log.info("Sending Audit Event to {} => {}", auditUrl, objectMapper.writeValueAsString(auditDto));

            ResponseEntity<String> response = restTemplate.postForEntity(auditUrl, requestEntity, String.class);
            log.info("Audit event sent successfully: {}", response.getStatusCode());

        } catch (Exception ex) {
            log.error("Error while sending audit event: {}", ex.getMessage(), ex);
        }
    }
}