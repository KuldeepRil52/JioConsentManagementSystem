package com.jio.schedular.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jio.schedular.client.audit.AuditManager;
import com.jio.schedular.client.audit.request.Actor;
import com.jio.schedular.client.audit.request.AuditRequest;
import com.jio.schedular.client.audit.request.Context;
import com.jio.schedular.client.audit.request.Resource;
import com.jio.schedular.client.notification.NotificationManager;
import com.jio.schedular.client.vault.VaultManager;
import com.jio.schedular.client.vault.response.EncryptPayloadResponse;
import com.jio.schedular.constant.Constants;
import com.jio.schedular.dto.*;
import com.jio.schedular.enums.*;
import com.jio.schedular.entity.CookieConsent;
import com.jio.schedular.entity.SchedularStats;
import com.jio.schedular.entity.TenantRegistry;
import com.jio.schedular.exception.ConversionException;
import com.jio.schedular.exception.VaultEncryptionException;
import com.jio.schedular.repository.CookieConsentRepository;
import com.jio.schedular.repository.SchedularStatsRepository;
import com.jio.schedular.repository.TenantRegistryRepository;
import com.jio.schedular.utils.Utils;
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
 * Service to mark expired cookie consents as EXPIRED with statistics tracking
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CookieConsentExpiryService {

    private final CookieConsentRepository cookieConsentRepository;
    private final TenantRegistryRepository tenantRegistryRepository;
    private final SchedularStatsRepository statsRepository;
    private final AuditManager auditManager;
    private final VaultManager vaultManager;
    private final ObjectMapper objectMapper;
    private final Utils utils;
    private final NotificationManager notificationManager;

    public int markExpiredCookieConsents() {

        UUID runId = UUID.randomUUID();
        Instant start = Instant.now();

        log.info("[{}] Starting cookie consent expiry process | runId={}", JobName.COOKIE_CONSENT_EXPIRY_JOB, runId);

        List<TenantRegistry> activeTenants = tenantRegistryRepository.findByStatus(Status.ACTIVE);
        if (activeTenants == null || activeTenants.isEmpty()) {
            log.warn("No active tenants found for cookie consent expiry");
            return 0;
        }

        int totalExpired = 0;
        LocalDateTime now = LocalDateTime.now();

        for (TenantRegistry tenant : activeTenants) {
            String tenantId = tenant.getTenantId();
            Instant tenantStart = Instant.now();

            try {
                ThreadContext.put(Constants.TENANT_ID_HEADER, tenantId);

                // Find expired consents (NOT marking them directly)
                List<CookieConsent> expiredConsents = cookieConsentRepository.findExpiredConsents(now);

                if (expiredConsents == null || expiredConsents.isEmpty()) {
                    log.info("No expired consents found for tenant: {}", tenantId);
                    continue;
                }

                // Process each expired consent - Create new version with EXPIRED status
                int successCount = 0;
                List<com.jio.schedular.dto.Resource> resources = new ArrayList<>();

                for (CookieConsent expiredConsent : expiredConsents) {
                    try {
                        processConsentExpiry(expiredConsent, tenantId);
                        successCount++;

                        resources.add(com.jio.schedular.dto.Resource.builder()
                                .type("COOKIE_CONSENT_ID")
                                .id(expiredConsent.getConsentId())
                                .businessId(expiredConsent.getBusinessId())
                                .build());

                        // Audit logging
                        logCookieConsentExpiryAudit(runId, expiredConsent, ActionType.UPDATED);

                        try {
                            Map<String, Object> payload = new HashMap<>();
                            payload.put("expiryDate", expiredConsent.getEndDate());
                            payload.put("consentId", expiredConsent.getConsentId());
                            payload.put("consentHandleId",expiredConsent.getConsentHandleId());
                            payload.put("version",expiredConsent.getVersion());
                            payload.put("templateId",expiredConsent.getTemplateId());

                            notificationManager.initiateCookieConsentNotification(
                                    NotificationEvent.COOKIE_CONSENT_EXPIRED,
                                    tenantId,
                                    expiredConsent.getBusinessId() != null ? expiredConsent.getBusinessId() : null,
                                    expiredConsent.getCustomerIdentifiers() != null ? expiredConsent.getCustomerIdentifiers() : null,
                                    payload,
                                    expiredConsent.getLanguagePreferences() != null ? expiredConsent.getLanguagePreferences() : LANGUAGE.ENGLISH,
                                    expiredConsent.getConsentId()
                            );
                        } catch (Exception ne) {
                            log.error("Failed to send cookie consent expiry notification for consent {}: {}", expiredConsent.getConsentId(), ne.getMessage());
                        }
                    } catch (Exception e) {
                        log.error("Failed to process expiry for cookie consent: {} | error: {}",
                                expiredConsent.getConsentId(), e.getMessage(), e);
                    }
                }

                totalExpired += successCount;

                if (successCount > 0) {
                    log.info("Marked {} cookie consents as EXPIRED for tenant: {}", successCount, tenantId);
                }
                else {
                    log.info("No cookie consents were marked as EXPIRED for tenant: {}", tenantId);
                    resources.add(com.jio.schedular.dto.Resource.builder()
                            .type(Constants.TENANT_ID)
                            .id(tenantId)
                            .businessId(tenantId)
                            .build());
                }

                // Save SUCCESS stats
                long duration = Duration.between(tenantStart, Instant.now()).toMillis();
                statsRepository.saveTenantSummary(tenantId, SchedularStats.builder()
                        .runId(runId.toString())
                        .jobName(JobName.COOKIE_CONSENT_EXPIRY_JOB)
                        .group(Group.COOKIE_CONSENT)
                        .action(Action.COOKIE_CONSENT_EXPIRED)
                        .summaryType(SummaryType.TENANT_SUMMARY)
                        .status(SchedularStatus.SUCCESS)
                        .resources(resources)
                        .totalAffected(successCount)
                        .timestamp(LocalDateTime.now())
                        .durationMillis(duration)
                        .details(Map.of(
                                "operation", "versioned_expiry_with_encryption"
                        ))
                        .build());
            } catch (Exception e) {
                log.error("Error processing tenant: {}", tenantId, e);

                long duration = Duration.between(tenantStart, Instant.now()).toMillis();
                statsRepository.saveTenantSummary(tenantId, SchedularStats.builder()
                        .runId(runId.toString())
                        .jobName(JobName.COOKIE_CONSENT_EXPIRY_JOB)
                        .group(Group.COOKIE_CONSENT)
                        .action(Action.COOKIE_CONSENT_EXPIRED)
                        .summaryType(SummaryType.TENANT_SUMMARY)
                        .status(SchedularStatus.FAILED)
                        .errorCount(1)
                        .lastError(e.getMessage())
                        .timestamp(LocalDateTime.now())
                        .durationMillis(duration)
                        .details(Map.of(
                                "operation", "versioned_expiry_with_encryption"
                        ))
                        .build());

            } finally {
                ThreadContext.clearAll();
            }
        }

        long totalDuration = Duration.between(start, Instant.now()).toMillis();
        log.info("[{}] Completed | runId={} | expired={} | duration={}ms",
                JobName.COOKIE_CONSENT_EXPIRY_JOB, runId, totalExpired, totalDuration);

        return totalExpired;
    }

    /**
     * Process consent expiry - Create new version with EXPIRED status (Like REVOKE logic)
     * - Creates new version with incremented version
     * - Computes chain hash
     * - Encrypts the consent
     * - Marks old version as STALE + UPDATED
     */
    private void processConsentExpiry(CookieConsent activeConsent, String tenantId) throws Exception {
        log.info("Processing expiry for consent: {}", activeConsent.getConsentId());

        // Store original document ID and previous payload hash
        String originalId = activeConsent.getId();
        String previousPayloadHash = activeConsent.getPayloadHash();

        // Create new version by copying active consent
        CookieConsent newVersion = new CookieConsent(activeConsent);
        newVersion.setId(null);
        newVersion.setVersion(activeConsent.getVersion() + 1);
        newVersion.setStatus(CookieConsentStatus.EXPIRED);
        newVersion.setConsentStatus(VersionStatus.ACTIVE);
        newVersion.setStaleStatus(StaleStatus.NOT_STALE);
        newVersion.setCreatedAt(Instant.now());
        newVersion.setUpdatedAt(Instant.now());

        // Convert to JSON BEFORE encryption
        String consentJsonString;
        try {
            consentJsonString = objectMapper.writeValueAsString(newVersion);
        } catch (Exception e) {
            log.error("Failed to convert consent to JSON: {}", e.getMessage());
            throw new ConversionException("Failed to convert consent to JSON: " + e.getMessage());
        }

        // Compute new payload hash
        String newPayloadHash = null;
        try {
            if (consentJsonString != null) {
                newPayloadHash = utils.computeSHA256Hash(consentJsonString);
            }
        } catch (Exception e) {
            log.warn("Failed to compute payload hash: {}", e.getMessage());
        }
        newVersion.setPayloadHash(newPayloadHash);

        // CHAIN HASH: Combine previous hash + new hash
        String chainedHash = null;
        if (previousPayloadHash != null && newPayloadHash != null) {
            chainedHash = utils.computeSHA256Hash(previousPayloadHash + newPayloadHash);
        } else if (newPayloadHash != null) {
            chainedHash = newPayloadHash;
        }
        newVersion.setCurrentChainHash(chainedHash);
        // ENCRYPT
        generateConsentEncryption(newVersion, consentJsonString, tenantId);

        // Save new version
        cookieConsentRepository.saveToDatabase(newVersion, tenantId);
        log.info("Created new EXPIRED version for consent: {} | version: {}",
                newVersion.getConsentId(), newVersion.getVersion());

        // Mark old version as STALE and UPDATED
        if (originalId != null) {
            Optional<CookieConsent> optionalConsent = cookieConsentRepository.findById(originalId, tenantId);
            if (optionalConsent.isPresent()) {
                CookieConsent originalConsent = optionalConsent.get();
                originalConsent.setStaleStatus(StaleStatus.STALE);
                originalConsent.setConsentStatus(VersionStatus.UPDATED);
                originalConsent.setUpdatedAt(Instant.now());
                cookieConsentRepository.saveToDatabase(originalConsent, tenantId);
                log.info("Marked old version as STALE+UPDATED for consent: {} | version: {}",
                        originalConsent.getConsentId(), originalConsent.getVersion());
            }
        }

        log.info("Successfully processed expiry for consent: {}", activeConsent.getConsentId());
    }

    /**
     * Generate encryption for consent using Vault API
     */
    private void generateConsentEncryption(CookieConsent consent, String consentJsonString, String tenantId)
            throws Exception {
        try {
            String businessId = consent.getBusinessId();

            EncryptPayloadResponse encryptResponse = vaultManager.encryptPayload(
                    tenantId,
                    businessId,
                    "CookieConsent",
                    "CookieConsent",
                    consentJsonString
            );

            // SET ENCRYPTION fields from Vault API response
            consent.setEncryptionTime(encryptResponse.getCreatedTimeStamp());
            consent.setEncryptedReferenceId(encryptResponse.getReferenceId());
            consent.setEncryptedString(encryptResponse.getEncryptedString());

            log.info("Vault encryption successful for consent: {}", consent.getConsentId());

        } catch (Exception e) {
            log.error("Failed to encrypt consent with vault service: {}", e.getMessage(), e);
            throw new VaultEncryptionException("Failed to encrypt consent with vault service: " + e.getMessage());
        }
    }

    /**
     * Modular function to log cookie-consent audit events
     * Can be used in both create and update cookie-consent flows
     *
     * @param cookieConsent The cookie-consent affected
     * @param actionType The action type (UPDATED)
     */
    public void logCookieConsentExpiryAudit(UUID runId, CookieConsent cookieConsent, ActionType actionType) {
        try {
            String tenantId = ThreadContext.get(Constants.TENANT_ID_HEADER);

            Actor actor = Actor.builder()
                    .id(runId.toString())
                    .role(Constants.SCHEDULAR)
                    .type(Constants.RUN_ID)
                    .build();

            Resource resource = Resource.builder()
                    .type(Constants.CONSENT_ID)
                    .id(cookieConsent.getConsentId())
                    .build();

            Context context = Context.builder()
                    .ipAddress(ThreadContext.get(Constants.SOURCE_IP) != null && !ThreadContext.get(Constants.SOURCE_IP).equals("-")
                            ? ThreadContext.get(Constants.SOURCE_IP) : UUID.randomUUID().toString())
                    .txnId(ThreadContext.get(Constants.TXN_ID_THREAD_CONTEXT) != null && !ThreadContext.get(Constants.TXN_ID_THREAD_CONTEXT).equals("-")
                            ? ThreadContext.get(Constants.TXN_ID_THREAD_CONTEXT) : Constants.IP_ADDRESS)
                    .build();

            Map<String, Object> extra = new HashMap<>();
            extra.put(Constants.DATA, cookieConsent);

            AuditRequest auditRequest = AuditRequest.builder()
                    .actor(actor)
                    .businessId(cookieConsent.getBusinessId())
                    .group(String.valueOf(Group.COOKIE_CONSENT))
                    .component(AuditComponent.COOKIE_CONSENT_EXPIRED)
                    .actionType(actionType)
                    .resource(resource)
                    .initiator(Constants.SCHEDULAR)
                    .context(context)
                    .extra(extra)
                    .build();

            this.auditManager.logAudit(auditRequest, tenantId);
        } catch (Exception e) {
            log.error("Audit logging failed for run id: {}, consent id: {}, action: {}, error: {}",
                    runId.toString(), cookieConsent.getConsentId(), actionType, e.getMessage(), e);
        }
    }
}