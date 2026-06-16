package com.jio.schedular.service;

import com.jio.schedular.client.audit.AuditManager;
import com.jio.schedular.client.audit.request.Actor;
import com.jio.schedular.client.audit.request.AuditRequest;
import com.jio.schedular.client.audit.request.Context;
import com.jio.schedular.client.audit.request.Resource;
import com.jio.schedular.client.notification.NotificationManager;
import com.jio.schedular.constant.Constants;
import com.jio.schedular.dto.*;
import com.jio.schedular.enums.*;
import com.jio.schedular.entity.CookieConsentHandle;
import com.jio.schedular.entity.SchedularStats;
import com.jio.schedular.entity.TenantRegistry;
import com.jio.schedular.repository.CookieConsentHandleRepository;
import com.jio.schedular.repository.SchedularStatsRepository;
import com.jio.schedular.repository.TenantRegistryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.ThreadContext;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service to handle cookie consent handle expiry logic with statistics tracking
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CookieConsentHandleExpiryService {

    private final CookieConsentHandleRepository cookieConsentHandleRepository;
    private final TenantRegistryRepository tenantRegistryRepository;
    private final SchedularStatsRepository statsRepository;
    private final AuditManager auditManager;
    private final NotificationManager notificationManager;

    public int markExpiredCookieConsentHandles() {

        UUID runId = UUID.randomUUID();
        Instant start = Instant.now();

        log.info("[{}] Starting cookie consent handle expiry process | runId={}", JobName.COOKIE_CONSENT_HANDLE_EXPIRY_JOB, runId);

        List<TenantRegistry> activeTenants = tenantRegistryRepository.findByStatus(Status.ACTIVE);
        if (activeTenants == null || activeTenants.isEmpty()) {
            log.warn("No active tenants found for cookie consent handle expiry");
            return 0;
        }

        int totalExpired = 0;
        Instant now = Instant.now();

        for (TenantRegistry tenant : activeTenants) {
            String tenantId = tenant.getTenantId();
            Instant tenantStart = Instant.now();

            try {
                ThreadContext.put(Constants.TENANT_ID_HEADER, tenantId);

                List<CookieConsentHandle> expiredHandles = cookieConsentHandleRepository.findExpiredPendingHandles(now);

                int expiredCount = 0;
                List<com.jio.schedular.dto.Resource> resources = new ArrayList<>();

                if (expiredHandles != null && !expiredHandles.isEmpty()) {
                    List<String> handleIds = expiredHandles.stream()
                            .map(CookieConsentHandle::getId)
                            .collect(Collectors.toList());

                    // Bulk update - FIRST expire them in DB
                    expiredCount = cookieConsentHandleRepository.markHandlesAsExpired(handleIds, now);
                    totalExpired += expiredCount;

                    log.info("Marked {} cookie consent handles as EXPIRED for tenant: {}", expiredCount, tenantId);

                    if (expiredCount > 0) {
                        expiredHandles.stream().forEach(cookieConsentHandle ->
                                logCookieConsentHandleExpiryAudit(runId, cookieConsentHandle, ActionType.UPDATED)
                        );
                        expiredHandles.stream().forEach(cookieConsentHandle ->
                                triggerExpiryNotification(cookieConsentHandle, tenantId)
                        );

                        resources = expiredHandles.stream()
                                .map(handle -> com.jio.schedular.dto.Resource.builder()
                                        .type("CONSENT_HANDLE_ID")
                                        .id(handle.getId())
                                        .businessId(handle.getBusinessId())
                                        .build())
                                .collect(Collectors.toList());
                    }
                }

                if (expiredCount == 0) {
                        log.info("No cookie consent handles to expire for tenant: {}", tenantId);
                        resources.add(com.jio.schedular.dto.Resource.builder()
                                .type(Constants.TENANT_ID)
                                .id(tenantId)
                                .businessId(tenantId)
                                .build());
                }
                long duration = Duration.between(tenantStart, Instant.now()).toMillis();
                statsRepository.saveTenantSummary(tenantId, SchedularStats.builder()
                        .runId(runId.toString())
                        .jobName(JobName.COOKIE_CONSENT_HANDLE_EXPIRY_JOB)
                        .group(Group.COOKIE_CONSENT_HANDLE)
                        .action(Action.COOKIE_CONSENT_HANDLE_EXPIRED)
                        .summaryType(SummaryType.TENANT_SUMMARY)
                        .status(SchedularStatus.SUCCESS)
                        .resources(resources)
                        .totalAffected(expiredCount)
                        .timestamp(LocalDateTime.now())
                        .durationMillis(duration)
                        .details(Map.of(
                                "operation", "bulk_update"
                        ))
                        .build());
            } catch (Exception e) {
                log.error("Error processing tenant: {}", tenantId, e);

                // Save FAILED stats
                long duration = Duration.between(tenantStart, Instant.now()).toMillis();
                statsRepository.saveTenantSummary(tenantId, SchedularStats.builder()
                        .runId(runId.toString())
                        .jobName(JobName.COOKIE_CONSENT_HANDLE_EXPIRY_JOB)
                        .group(Group.COOKIE_CONSENT_HANDLE)
                        .action(Action.COOKIE_CONSENT_HANDLE_EXPIRED)
                        .summaryType(SummaryType.TENANT_SUMMARY)
                        .status(SchedularStatus.FAILED)
                        .errorCount(1)
                        .lastError(e.getMessage())
                        .timestamp(LocalDateTime.now())
                        .durationMillis(duration)
                        .details(Map.of(
                                "operation", "bulk_update"
                        ))
                        .build());

            } finally {
                ThreadContext.clearAll();
            }
        }

        long totalDuration = Duration.between(start, Instant.now()).toMillis();
        log.info("[{}] Completed | runId={} | expired={} | duration={}ms",
                JobName.COOKIE_CONSENT_HANDLE_EXPIRY_JOB, runId, totalExpired, totalDuration);

        return totalExpired;
    }

    /**
     * Modular function to log cookie-consent handle audit events
     * Can be used in both create and update cookie-consent handle flows
     *
     * @param cookieConsentHandle The cookie-consent handle affected
     * @param actionType The action type (UPDATED)
     */
    public void logCookieConsentHandleExpiryAudit(UUID runId, CookieConsentHandle cookieConsentHandle, ActionType actionType) {
        try {
            String tenantId = ThreadContext.get(Constants.TENANT_ID_HEADER);

            Actor actor = Actor.builder()
                    .id(runId.toString())
                    .role(Constants.SCHEDULAR)
                    .type(Constants.RUN_ID)
                    .build();

            Resource resource = Resource.builder()
                    .type(Constants.CONSENT_HANDLE_ID)
                    .id(cookieConsentHandle.getConsentHandleId())
                    .build();

            Context context = Context.builder()
                    .ipAddress(ThreadContext.get(Constants.SOURCE_IP) != null && !ThreadContext.get(Constants.SOURCE_IP).equals("-")
                            ? ThreadContext.get(Constants.SOURCE_IP) : UUID.randomUUID().toString())
                    .txnId(ThreadContext.get(Constants.TXN_ID_THREAD_CONTEXT) != null && !ThreadContext.get(Constants.TXN_ID_THREAD_CONTEXT).equals("-")
                            ? ThreadContext.get(Constants.TXN_ID_THREAD_CONTEXT) : Constants.IP_ADDRESS)
                    .build();

            Map<String, Object> extra = new HashMap<>();
            
            extra.put(Constants.DATA, cookieConsentHandle);

            AuditRequest auditRequest = AuditRequest.builder()
                    .actor(actor)
                    .businessId(cookieConsentHandle.getBusinessId())
                    .group(String.valueOf(Group.COOKIE_CONSENT_HANDLE))
                    .component(AuditComponent.COOKIE_CONSENT_HANDLE_EXPIRED)
                    .actionType(actionType)
                    .resource(resource)
                    .initiator(Constants.SCHEDULAR)
                    .context(context)
                    .extra(extra)
                    .build();

            this.auditManager.logAudit(auditRequest, tenantId);
        } catch (Exception e) {
            log.error("Audit logging failed for run id: {}, consent handle id: {}, action: {}, error: {}", 
                    runId.toString(), cookieConsentHandle.getConsentHandleId(), actionType, e.getMessage(), e);
        }
    }

    private void triggerExpiryNotification(CookieConsentHandle cookieCnsentHandle, String tenantId) {
        try {
            CustomerIdentifiers customerIdentifiers = cookieCnsentHandle.getCustomerIdentifiers();
            String businessId = cookieCnsentHandle.getBusinessId();

            Map<String, Object> payload = new HashMap<>();
            payload.put("expiresAt", cookieCnsentHandle.getExpiresAt());
            payload.put("consentHandleId", cookieCnsentHandle.getConsentHandleId());
            payload.put("templateVersion",cookieCnsentHandle.getTemplateVersion());

            if (customerIdentifiers != null) {
                notificationManager.initiateConsentHandleNotification(
                        NotificationEvent.CONSENT_REQUEST_EXPIRED,
                        tenantId,
                        businessId,
                        customerIdentifiers,
                        null,
                        payload,
                        LANGUAGE.ENGLISH,
                        cookieCnsentHandle.getConsentHandleId()
                );

                log.debug("Triggered COOKIE_CONSENT_REQUEST_EXPIRED notification for cookie consent handle: {}",
                        cookieCnsentHandle.getConsentHandleId());
            } else {
                log.warn("Cannot trigger notification - customer identifiers are null for cookie consent handle: {}",
                        cookieCnsentHandle.getConsentHandleId());
            }
        } catch (Exception e) {
            log.error("Error triggering notification for cookie consent handle: {}", cookieCnsentHandle.getConsentHandleId(), e);
        }
    }
}