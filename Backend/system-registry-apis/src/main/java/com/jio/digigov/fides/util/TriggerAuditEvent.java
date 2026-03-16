package com.jio.digigov.fides.util;

import com.jio.digigov.fides.client.audit.AuditManager;
import com.jio.digigov.fides.client.audit.request.*;
import com.jio.digigov.fides.enumeration.ActionType;
import com.jio.digigov.fides.enumeration.AuditComponent;
import com.jio.digigov.fides.enumeration.Group;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.ThreadContext;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class TriggerAuditEvent {

    private final AuditManager auditManager;

    public void trigger(
            String resourceId,
            String resourceType,
            Group group,
            AuditComponent component,
            ActionType actionType,
            String tenantId,
            String businessId,
            HttpServletRequest request
    ) {
        try {

            AuditRequestDto auditPayload = buildAuditPayload(
                    resourceId, resourceType, group, component, actionType, businessId, request
            );
            log.info("Triggering audit event payload={} tenantId={} businessId={}",
                    auditPayload, tenantId, businessId);

            auditManager.sendAudit(auditPayload, tenantId, businessId);

            log.info("Audit triggered successfully component={} action={} resourceId={}",
                    component, actionType, resourceId);

        } catch (Exception ex) {
            log.error("Audit trigger failed resourceId={} action={}",
                    resourceId, actionType, ex);
        }
    }


    private AuditRequestDto buildAuditPayload(
            String resourceId,
            String resourceType,
            Group group,
            AuditComponent component,
            ActionType actionType,
            String businessId,
            HttpServletRequest req
    ) {

        String clientIp = getClientIp(req);

        Map<String, Object> contextMap = Map.of(
                "txnId", getTxnId(),
                "ipAddress", clientIp
        );

        Map<String, Object> extraMap = Map.of(
                "legalEntityName", "",
                "pan", ""
        );

        return AuditRequestDto.builder()
                .businessId(businessId)
                .actor(AuditRequestDto.Actor.builder()
                        .id(resourceId)
                        .role("SYSTEM")
                        .type("resourceType")
                        .build())
                .group(group.toString())
                .component(component.toString())
                .actionType(actionType) // DRAFT, PUBLISHED
                .resource(AuditRequestDto.Resource.builder()
                        .type(resourceType)
                        .id(resourceId)
                        .build())
                .initiator("SYSTEM")
                .context(contextMap)
                .extra(extraMap)
                .build();
    }

    private String getTxnId() {
        return ThreadContext.get("X-Transaction-ID") != null
                ? ThreadContext.get("X-Transaction-ID")
                : UUID.randomUUID().toString();
    }

    public String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }

        // Convert IPv6 localhost to IPv4 localhost
        if ("0:0:0:0:0:0:0:1".equals(ip)) {
            ip = "127.0.0.1";
        }

        // In case of multiple IPs (proxies), pick the first one
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0];
        }

        return ip;
    }
}