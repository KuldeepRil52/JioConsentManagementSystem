package com.example.scanner.service;

import com.example.scanner.client.AuditClient;
import com.example.scanner.constants.AuditConstants;
import com.example.scanner.dto.Actor;
import com.example.scanner.dto.request.AuditRequest;
import com.example.scanner.dto.Resource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import jakarta.servlet.http.HttpServletRequest;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuditService {

    private final AuditClient auditClient;

    public void logAudit(String tenantId, String businessId, String component, String actionType,
                         String initiator, String resourceType, String resourceId, String actorId,
                         Map<String, Object> context, String status) {
        try {

            Actor actor = Actor.builder()
                    .id(actorId != null? actorId : tenantId)
                    .role("initiator")
                    .type(AuditConstants.ACTOR_TYPE_SYSTEM)
                    .build();

            Resource resource = Resource.builder()
                    .type(resourceType)
                    .id(resourceId != null ? resourceId : "resource-" + UUID.randomUUID()) // ID
                    .build();

            String txnId = UUID.randomUUID().toString();
            log.info("Initiating audit log with - " + txnId);
            context.put("txnId", txnId);

            AuditRequest auditRequest = AuditRequest.builder()
                    .tenantId(tenantId)
                    .businessId(businessId != null ? businessId :  UUID.randomUUID().toString())
                    .actor(actor)
                    .group(AuditConstants.GROUP_COOKIE_CONSENT)
                    .component(component)
                    .actionType(actionType)
                    .initiator(initiator)
                    .resource(resource)
                    .context(context)
                    .status(status)
                    .timestamp(Instant.now().toString())
                    .build();

            auditClient.createAudit(auditRequest, tenantId, businessId, txnId);

        } catch (Exception e) {
            log.error("Audit logging failed");
        }
    }

    // ========================================
    // COOKIE SCAN METHODS (3) - Pass null for businessId
    // ========================================
    public void logCookieScanInitiated(String tenantId, String scanId) {
        Map<String, Object> context = new HashMap<>();
        context.put("ipAddress", getClientIpAddress());
        logAudit(tenantId,
                null,
                AuditConstants.COMPONENT_COOKIE_SCAN,
                AuditConstants.ACTION_SCAN_INITIATED,
                AuditConstants.INITIATOR_DF,
                AuditConstants.RESOURCE_COOKIE_SCAN_ID,
                scanId,
                null,
                context,
                AuditConstants.ACTION_SCAN_INITIATED);
    }

    public void logCookieScanStarted(String tenantId, String scanId) {
        Map<String, Object> context = new HashMap<>();
        context.put("ipAddress", getClientIpAddress());
        logAudit(tenantId,
                null,
                AuditConstants.COMPONENT_COOKIE_SCAN,
                AuditConstants.ACTION_SCAN_STARTED,
                AuditConstants.INITIATOR_DF,
                AuditConstants.RESOURCE_COOKIE_SCAN_ID,
                scanId,
                null,
                context,
                AuditConstants.ACTION_SCAN_STARTED );
    }

    public void logCookieScanFailed(String tenantId, String scanId) {
        Map<String, Object> context = new HashMap<>();
        context.put("ipAddress", getClientIpAddress());
        logAudit(tenantId, null, AuditConstants.COMPONENT_COOKIE_SCAN,
                AuditConstants.ACTION_SCAN_FAILED, AuditConstants.INITIATOR_DF,
                AuditConstants.RESOURCE_COOKIE_SCAN_ID, scanId, null, context, AuditConstants.ACTION_SCAN_FAILED);
    }

    // ========================================
    // TEMPLATE METHODS (4) - businessId parameter added
    // ========================================
    public void logTemplateCreationInitiated(String tenantId, String businessId) {
        Map<String, Object> context = new HashMap<>();
        context.put("ipAddress", getClientIpAddress());
        logAudit(tenantId, businessId, AuditConstants.COMPONENT_TEMPLATE_CREATION,
                AuditConstants.ACTION_TEMPLATE_CREATION_INITIATED, AuditConstants.INITIATOR_DF,
                AuditConstants.RESOURCE_COOKIE_TEMPLATE_ID, "Pending Creation", null, context,
                AuditConstants.ACTION_TEMPLATE_CREATION_INITIATED);

    }

    public void logTemplateCreated(String tenantId, String businessId, String templateId) {
        Map<String, Object> context = new HashMap<>();
        context.put("ipAddress", getClientIpAddress());
        logAudit(tenantId, businessId, AuditConstants.COMPONENT_TEMPLATE_CREATION,
                AuditConstants.ACTION_TEMPLATE_CREATED, AuditConstants.INITIATOR_DF,
                AuditConstants.RESOURCE_COOKIE_TEMPLATE_ID, templateId, null, context,
                AuditConstants.ACTION_TEMPLATE_CREATED);
    }

    public void logTemplateUpdateInitiated(String tenantId, String businessId, String templateId) {
        Map<String, Object> context = new HashMap<>();
        context.put("ipAddress", getClientIpAddress());
        logAudit(tenantId, businessId, AuditConstants.COMPONENT_TEMPLATE_UPDATE,
                AuditConstants.ACTION_TEMPLATE_UPDATE_INITIATED, AuditConstants.INITIATOR_DF,
                AuditConstants.RESOURCE_COOKIE_TEMPLATE_ID, templateId, null, context,AuditConstants.ACTION_TEMPLATE_UPDATE_INITIATED );
    }

    public void logNewTemplateVersionCreated(String tenantId, String businessId, String templateId) {
        Map<String, Object> context = new HashMap<>();
        context.put("ipAddress", getClientIpAddress());
        logAudit(tenantId, businessId, AuditConstants.COMPONENT_TEMPLATE_UPDATE,
                AuditConstants.ACTION_NEW_TEMPLATE_VERSION_CREATED, AuditConstants.INITIATOR_DF,
                AuditConstants.ACTION_NEW_TEMPLATE_VERSION_CREATED_ID, templateId, null, context,
                AuditConstants.ACTION_NEW_TEMPLATE_VERSION_CREATED);
    }

    // ========================================
    // CONSENT HANDLE METHODS (2) - businessId parameter added
    // ========================================
    public void logConsentHandleCreationInitiated(String tenantId, String businessId, String handleId) {
        Map<String, Object> context = new HashMap<>();
        context.put("ipAddress", getClientIpAddress());
        logAudit(tenantId, businessId, AuditConstants.COMPONENT_CONSENT_HANDLE,
                AuditConstants.ACTION_CONSENT_HANDLE_CREATION_INITIATED, AuditConstants.INITIATOR_DF,
                AuditConstants.RESOURCE_CONSENT_HANDLE, handleId, null, context,
                AuditConstants.ACTION_CONSENT_HANDLE_CREATION_INITIATED);
    }

    public void logConsentHandleCreated(String tenantId, String businessId, String actorId ,String handleId) {
        Map<String, Object> context = new HashMap<>();
        context.put("ipAddress", getClientIpAddress());
        logAudit(tenantId, businessId, AuditConstants.COMPONENT_CONSENT_HANDLE,
                AuditConstants.ACTION_CONSENT_HANDLE_CREATED, AuditConstants.INITIATOR_DF,
                AuditConstants.RESOURCE_CONSENT_HANDLE_ID, handleId, actorId,context, AuditConstants.ACTION_CONSENT_HANDLE_CREATED);
    }

    // ========================================
    // CONSENT METHODS (7) - businessId parameter added
    // ========================================
    public void logConsentCreationInitiated(String tenantId, String businessId, String actorId) {
        Map<String, Object> context = new HashMap<>();
        context.put("ipAddress", getClientIpAddress());
        logAudit(tenantId, businessId, AuditConstants.COMPONENT_CONSENT_CREATION,
                AuditConstants.ACTION_CONSENT_CREATION_INITIATED, AuditConstants.INITIATOR_USER,
                AuditConstants.RESOURCE_CONSENT, "Initiated", actorId, context,AuditConstants.ACTION_CONSENT_CREATION_INITIATED);
    }

    public void logConsentCreated(String tenantId, String businessId, String consentId, String actorId) {
        Map<String, Object> context = new HashMap<>();
        context.put("ipAddress", getClientIpAddress());
        logAudit(tenantId, businessId, AuditConstants.COMPONENT_CONSENT_CREATION,
                AuditConstants.ACTION_CONSENT_CREATED, AuditConstants.INITIATOR_USER,
                AuditConstants.ACTION_CONSENT_ID, consentId, actorId, context, AuditConstants.ACTION_CONSENT_CREATED);
    }

    public void logConsentHandleMarkedUsed(String tenantId, String businessId, String handleId, String actorId) {
        Map<String, Object> context = new HashMap<>();
        context.put("ipAddress", getClientIpAddress());
        logAudit(tenantId, businessId, AuditConstants.COMPONENT_CONSENT_CREATION,
                AuditConstants.ACTION_CONSENT_HANDLE_MARKED_USED, AuditConstants.INITIATOR_USER,
                AuditConstants.RESOURCE_CONSENT_HANDLE_ID, handleId, actorId, context, AuditConstants.ACTION_CONSENT_HANDLE_MARKED_USED);
    }

    public void logConsentUpdateInitiated(String tenantId, String businessId, String consentId, String actorId) {
        Map<String, Object> context = new HashMap<>();
        context.put("ipAddress", getClientIpAddress());
        logAudit(tenantId, businessId, AuditConstants.COMPONENT_CONSENT_UPDATE,
                AuditConstants.ACTION_CONSENT_UPDATE_INITIATED, AuditConstants.INITIATOR_USER,
                AuditConstants.RESOURCE_CONSENT, consentId, actorId, context, AuditConstants.ACTION_CONSENT_UPDATE_INITIATED);
    }

    public void logNewConsentVersionCreated(String tenantId, String businessId, String consentId, String actorId) {
        Map<String, Object> context = new HashMap<>();
        context.put("ipAddress", getClientIpAddress());
        logAudit(tenantId, businessId, AuditConstants.COMPONENT_CONSENT_UPDATE,
                AuditConstants.ACTION_NEW_CONSENT_VERSION_CREATED, AuditConstants.INITIATOR_USER,
                AuditConstants.RESOURCE_CONSENT, consentId, actorId, context, AuditConstants.ACTION_NEW_CONSENT_VERSION_CREATED);
    }

    public void logOldConsentVersionMarkedUpdated(String tenantId, String businessId, String consentId, String actorId) {
        Map<String, Object> context = new HashMap<>();
        context.put("ipAddress", getClientIpAddress());
        logAudit(tenantId, businessId, AuditConstants.COMPONENT_CONSENT_UPDATE,
                AuditConstants.ACTION_OLD_CONSENT_VERSION_MARKED_UPDATED, AuditConstants.INITIATOR_USER,
                AuditConstants.RESOURCE_CONSENT, consentId, actorId, context, AuditConstants.ACTION_OLD_CONSENT_VERSION_MARKED_UPDATED);
    }

    public void logConsentHandleMarkedUsedAfterUpdate(String tenantId, String businessId, String handleId, String actorId) {
        Map<String, Object> context = new HashMap<>();
        context.put("ipAddress", getClientIpAddress());
        logAudit(tenantId, businessId, AuditConstants.COMPONENT_CONSENT_UPDATE,
                AuditConstants.ACTION_CONSENT_HANDLE_MARKED_USED_AFTER_UPDATE, AuditConstants.INITIATOR_DF,
                AuditConstants.RESOURCE_CONSENT_HANDLE, handleId, actorId, context, AuditConstants.ACTION_CONSENT_HANDLE_MARKED_USED_AFTER_UPDATE);
    }

    public void logConsentRevoked(String tenantId, String businessId, String consentId, String actorId) {
        Map<String, Object> context = new HashMap<>();
        context.put("ipAddress", getClientIpAddress());
        logAudit(tenantId, businessId, AuditConstants.COMPONENT_CONSENT_UPDATE,
                AuditConstants.ACTION_CONSENT_REVOKED, AuditConstants.INITIATOR_USER,
                AuditConstants.RESOURCE_CONSENT, consentId, actorId, context, AuditConstants.ACTION_CONSENT_REVOKED);
    }

    // ========================================
    // TOKEN METHODS (4) - businessId parameter added
    // ========================================
    public void logTokenVerificationInitiated(String tenantId, String businessId, String tokenId) {
        Map<String, Object> context = new HashMap<>();
        context.put("ipAddress", getClientIpAddress());
        logAudit(tenantId, businessId, AuditConstants.COMPONENT_TOKEN_VERIFICATION,
                AuditConstants.ACTION_TOKEN_VERIFICATION_INITIATED, AuditConstants.INITIATOR_DP,
                AuditConstants.RESOURCE_TOKEN, tokenId, null, context, AuditConstants.ACTION_TOKEN_VERIFICATION_INITIATED);
    }

    public void logTokenSignatureVerified(String tenantId, String businessId, String tokenId) {
        Map<String, Object> context = new HashMap<>();
        context.put("ipAddress", getClientIpAddress());
        logAudit(tenantId, businessId, AuditConstants.COMPONENT_TOKEN_VERIFICATION,
                AuditConstants.ACTION_TOKEN_SIGNATURE_VERIFIED, AuditConstants.INITIATOR_DP,
                AuditConstants.RESOURCE_TOKEN, tokenId, null, context, AuditConstants.ACTION_TOKEN_SIGNATURE_VERIFIED);
    }

    public void logTokenValidationSuccess(String tenantId, String businessId, String tokenId) {
        Map<String, Object> context = new HashMap<>();
        context.put("ipAddress", getClientIpAddress());
        logAudit(tenantId, businessId, AuditConstants.COMPONENT_TOKEN_VERIFICATION,
                AuditConstants.ACTION_TOKEN_VALIDATION_SUCCESS, AuditConstants.INITIATOR_DP,
                AuditConstants.RESOURCE_TOKEN, tokenId, null, context, AuditConstants.ACTION_TOKEN_VALIDATION_SUCCESS);
    }

    public void logTokenValidationFailed(String tenantId, String businessId, String tokenId) {
        Map<String, Object> context = new HashMap<>();
        context.put("ipAddress", getClientIpAddress());
        logAudit(tenantId, businessId, AuditConstants.COMPONENT_TOKEN_VERIFICATION,
                AuditConstants.ACTION_TOKEN_VALIDATION_FAILED, AuditConstants.INITIATOR_DP,
                AuditConstants.RESOURCE_TOKEN, tokenId, null, context, AuditConstants.ACTION_TOKEN_VALIDATION_FAILED);
    }


    private String getClientIpAddress() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes)
                    RequestContextHolder.currentRequestAttributes();
            HttpServletRequest request = attributes.getRequest();

            // Check for proxy headers first
            String ip = request.getHeader("X-Forwarded-For");
            if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
                ip = request.getHeader("X-Real-IP");
            }
            if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
                ip = request.getRemoteAddr();
            }

            // If multiple IPs in X-Forwarded-For, take the first one (client IP)
            if (ip != null && ip.contains(",")) {
                ip = ip.split(",")[0].trim();
            }

            return ip != null ? ip : "UNKNOWN";
        } catch (Exception e) {
            log.warn("Unable to get client IP address, likely async/background thread");
            return "SYSTEM";
        }
    }
}