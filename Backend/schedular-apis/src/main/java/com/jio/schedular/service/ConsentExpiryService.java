package com.jio.schedular.service;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.jio.schedular.client.audit.AuditManager;
import com.jio.schedular.client.audit.request.Actor;
import com.jio.schedular.client.audit.request.AuditRequest;
import com.jio.schedular.client.audit.request.Context;
import com.jio.schedular.client.audit.request.Resource;
import com.jio.schedular.client.vault.VaultManager;
import com.jio.schedular.client.vault.response.EncryptPayloadResponse;
import com.jio.schedular.client.systemRegistry.SystemRegistryManager;
import com.jio.schedular.config.LocalDateTypeAdapter;
import com.jio.schedular.constant.Constants;
import com.jio.schedular.dto.*;
import com.jio.schedular.enums.*;
import com.jio.schedular.entity.Consent;
import com.jio.schedular.entity.SchedularStats;
import com.jio.schedular.entity.TenantRegistry;
import com.jio.schedular.multitenancy.TenantMongoTemplateProvider;
import com.jio.schedular.repository.ConsentRepository;
import com.jio.schedular.repository.SchedularStatsRepository;
import com.jio.schedular.repository.TenantRegistryRepository;
import com.jio.schedular.client.notification.NotificationManager;
import com.jio.schedular.utils.Utils;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.ThreadContext;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

import java.time.Instant;
import java.time.*;
import java.time.Duration;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.stream.Collectors;

/**
 * Service class for expiring consents and their preferences across tenants.
 *
 * Approach:
 * - Multi-tenant parallel processing
 * - Fetch-modify-save per consent (uses repository.save for persistence)
 *
 * Behaviour:
 * - If a preference is ACCEPTED and its endDate < today => mark preference EXPIRED
 * - If after updating, all preferences are EXPIRED or NOTACCEPTED => mark consent EXPIRED and send notification
 * - If consent had all preferences NOTACCEPTED from start, it will be expired when preference(s) endDate passes
 */
@Service
@Slf4j
public class ConsentExpiryService {

    private final Object dataHashLock = new Object();

    private final ConsentRepository consentRepository;
    private final TenantRegistryRepository tenantRegistryRepository;
    private final TenantMongoTemplateProvider tenantMongoTemplateProvider;
    private final NotificationManager notificationManager;
    private ExecutorService executorService;
    private final SchedularStatsRepository statsRepository;
    private final AuditManager auditManager;
    private final Utils utils;
    private final VaultManager vaultManager;
    private final SystemRegistryManager systemRegistryManager;

    @Value("${schedular.job.consent-expiry.thread-pool-size:10}")
    private int threadPoolSize;

    @Autowired
    public ConsentExpiryService(ConsentRepository consentRepository,
                                TenantRegistryRepository tenantRegistryRepository,
                                TenantMongoTemplateProvider tenantMongoTemplateProvider,
                                NotificationManager notificationManager,
                                SchedularStatsRepository statsRepository,
                                AuditManager auditManager,
                                Utils utils,
                                VaultManager vaultManager,
                                SystemRegistryManager systemRegistryManager) {
        this.consentRepository = consentRepository;
        this.tenantRegistryRepository = tenantRegistryRepository;
        this.tenantMongoTemplateProvider = tenantMongoTemplateProvider;
        this.notificationManager = notificationManager;
        this.statsRepository = statsRepository;
        this.utils = utils;
        this.vaultManager = vaultManager;
        this.auditManager = auditManager;
        this.systemRegistryManager = systemRegistryManager;
    }

    @PostConstruct
    public void init() {
        this.executorService = Executors.newFixedThreadPool(threadPoolSize);
        log.info("Initialized ConsentExpiryService with thread pool size: {}", threadPoolSize);
    }

    @PreDestroy
    public void shutdown() {
        if (executorService != null) {
            log.info("Shutting down ConsentExpiryService executor");
            executorService.shutdown();
            try {
                if (!executorService.awaitTermination(30, TimeUnit.SECONDS)) {
                    executorService.shutdownNow();
                }
            } catch (InterruptedException e) {
                executorService.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }

    private record TenantResult(int expiredCount, Set<Consent> affectedConsents) {}

    /**
     * Expires consents (and preferences where configured) across active tenants.
     *
     * @param batchSize number of consents to fetch per batch per tenant
     * @return total number of consents marked expired in this run
     */
    @Transactional
    public int expireConsentsAndPreferences(int batchSize) {

        UUID runId = UUID.randomUUID();
        Instant start = Instant.now();
        int totalExpired = 0;

        log.info("Starting multi-tenant consent expiry - batchSize: {}", batchSize);

        List<TenantRegistry> activeTenants = tenantRegistryRepository.findByStatus(Status.ACTIVE);
        if (activeTenants == null || activeTenants.isEmpty()) {
            log.warn("No active tenants found for consent expiry");
            return 0;
        }

        LocalDate today = LocalDate.now();

        for (TenantRegistry tenant : activeTenants) {
            String tenantId = tenant.getTenantId();
            try {
                TenantResult result = processTenantConsents(tenantId, batchSize, today, runId);
                int expiredCount = result.expiredCount();
                Set<Consent> affectedConsents = result.affectedConsents();
                List<com.jio.schedular.dto.Resource> tenantResources;

                totalExpired += expiredCount;

                if(affectedConsents.isEmpty()) {
                    log.info("No consent expired for tenant: {}", tenantId);
                    tenantResources = List.of(createResource(Constants.TENANT_ID, tenantId, tenantId));
                } else {
                    log.info("Total consents expired for tenant {}: {}", tenantId, expiredCount);
                    tenantResources = affectedConsents.stream()
                            .map(consent -> createResource(Constants.CONSENT_ID, consent.getConsentId(), consent.getBusinessId()))
                            .toList();
                }

                long duration = Duration.between(start, Instant.now()).toMillis();

                statsRepository.saveTenantSummary(tenantId, SchedularStats.builder()
                        .runId(runId.toString())
                        .jobName(JobName.CONSENT_EXPIRY_JOB)
                        .group(Group.CONSENT)
                        .action(Action.CONSENT_EXPIRED)
                        .summaryType(SummaryType.TENANT_SUMMARY)
                        .status(SchedularStatus.SUCCESS)
                        .resources(tenantResources)
                        .totalAffected(expiredCount)
                        .timestamp(LocalDateTime.now())
                        .durationMillis(duration)
                        .details(Map.of(
                                "batchSize", batchSize
                        ))
                        .build());

            } catch (Exception ex) {
                long duration = Duration.between(start, Instant.now()).toMillis();
                statsRepository.saveTenantSummary(tenantId, SchedularStats.builder()
                        .runId(runId.toString())
                        .jobName(JobName.CONSENT_EXPIRY_JOB)
                        .group(Group.CONSENT)
                        .action(Action.CONSENT_EXPIRED)
                        .summaryType(SummaryType.TENANT_SUMMARY)
                        .status(SchedularStatus.FAILED)
                        .errorCount(1)
                        .lastError(ex.getMessage())
                        .timestamp(LocalDateTime.now())
                        .durationMillis(duration)
                        .details(Map.of(
                                "batchSize", batchSize
                        ))
                        .build());
            }
        }

        log.info("[{}] Completed | runId={} | expired={}", JobName.CONSENT_EXPIRY_JOB, runId, totalExpired);
        return totalExpired;
    }

    /**
     * Processes consents for a tenant in pages.
     */
    private TenantResult processTenantConsents(String tenantId, int batchSize, LocalDate today, UUID runId) {
        log.info("Processing consents for tenant: {}", tenantId);
        try {
            ThreadContext.put(Constants.TENANT_ID_HEADER, tenantId);
            int tenantExpired = 0;
            int page = 0;
            boolean hasMore = true;
            Set<Consent> affectedConsents = new HashSet<>();

            while (hasMore) {
                Pageable pageable = PageRequest.of(page, Math.max(1, batchSize));
                List<Consent> candidates = findCandidateConsentsForTenant(tenantId, today, pageable);
                log.debug("Candidate consents fetched for tenant {}: {}", tenantId, candidates == null ? 0 : candidates.size());

                if (candidates == null || candidates.isEmpty()) {
                    break;
                }

                TenantResult batchResult = processConsentBatch(candidates, tenantId, today, runId);
                tenantExpired += batchResult.expiredCount();
                affectedConsents.addAll(batchResult.affectedConsents());

                if (candidates.size() < batchSize) {
                    hasMore = false;
                } else {
                    page++;
                }
            }

            log.info("Completed processing for tenant: {}. Expired consents: {}", tenantId, tenantExpired);
            return new TenantResult(tenantExpired, affectedConsents);

        } catch (Exception e) {
            log.error("Error processing consents for tenant: {}", tenantId, e);
            return new TenantResult(0, Set.of());
        } finally {
            ThreadContext.clearAll();
        }
    }

    /**
     * Finds candidate consents for expiry in a tenant.
     * Criteria: consent.status = ACTIVE and at least one preference.endDate < today
     *
     */
    private List<Consent> findCandidateConsentsForTenant(String tenantId, LocalDate today, Pageable pageable) {
        MongoTemplate mongoTemplate = tenantMongoTemplateProvider.getMongoTemplate(tenantId);

        Criteria criteria = new Criteria();
        criteria.and(Constants.STATUS).is(ConsentStatus.ACTIVE);
        criteria.and(Constants.STALE_STATUS).is(StaleStatus.NOT_STALE);
        criteria.and("preferences.endDate").lt(today);

        Query query = new Query(criteria);
        query.with(pageable);

        return mongoTemplate.find(query, Consent.class);
    }

    /**
     * Process a batch of consents: expire preferences if needed, and expire consent when all prefs are notaccepted/expired.
     */
    private TenantResult processConsentBatch(List<Consent> consents, String tenantId, LocalDate today, UUID runId) {

        log.info("[ConsentBatch] Starting consent batch processing. tenantId={}, runId={}, size={}",
                tenantId, runId, consents != null ? consents.size() : 0);

        int expiredCount = 0;
        Set<Consent> affectedConsents = new HashSet<>();

        for (Consent consent : consents) {
            Consent currentConsent = new Consent(consent);

            log.debug("[ConsentProcess] Processing consentId={} tenantId={} runId={}",
                    currentConsent.getConsentId(), tenantId, runId);

            try {

                ObjectId originalId = consent.getId();
                currentConsent.setId(null);

                boolean anyChange = false;
                List<HandlePreference> prefs = currentConsent.getPreferences();

                // ---------- Preference Processing ----------
                if (prefs != null && !prefs.isEmpty()) {

                    log.debug("[PrefProcess] Consent {} has {} preferences", currentConsent.getConsentId(), prefs.size());

                    for (HandlePreference p : prefs) {

                        log.debug("[PrefProcess] Checking preferenceId={} status={} endDate={}",
                                p.getPreferenceId(), p.getPreferenceStatus(), p.getEndDate());

                        if (p.getPreferenceStatus() != null
                                && PreferenceStatus.ACCEPTED.equals(p.getPreferenceStatus())
                                && p.getEndDate() != null
                                && p.getEndDate().isBefore(today.atStartOfDay())) {

                            log.info("[PrefExpired] Preference {} of consent {} is expired",
                                    p.getPreferenceId(), currentConsent.getConsentId());

                            p.setPreferenceStatus(PreferenceStatus.EXPIRED);
                            anyChange = true;
                        }
                    }
                }

                // ---------- Decision: Should Consent Be Expired? ----------
                boolean allPrefsExpiredOrNotAccepted =
                        (prefs == null || prefs.isEmpty()) ||
                                prefs.stream().allMatch(p ->
                                        p.getPreferenceStatus() == PreferenceStatus.EXPIRED ||
                                                p.getPreferenceStatus() == PreferenceStatus.NOTACCEPTED);

                log.debug("[ConsentCheck] Consent {} - allPrefsExpiredOrNotAccepted={}",
                        currentConsent.getConsentId(), allPrefsExpiredOrNotAccepted);

                boolean allNotAccepted = prefs != null && !prefs.isEmpty() &&
                        prefs.stream().allMatch(p -> PreferenceStatus.NOTACCEPTED.equals(p.getPreferenceStatus()));

                boolean allEndDatesPast = !allNotAccepted ||
                        prefs.stream().allMatch(p ->
                                p.getEndDate() != null && p.getEndDate().toLocalDate().isBefore(today));

                log.debug("[ConsentCheck] Consent {} - allEndDatesPast={}",
                        currentConsent.getConsentId(), allEndDatesPast);

                if (allPrefsExpiredOrNotAccepted && allEndDatesPast
                        && !ConsentStatus.EXPIRED.equals(currentConsent.getStatus())) {

                    log.info("[ConsentExpired] Marking consent {} expired for tenant {}",
                            currentConsent.getConsentId(), tenantId);

                    currentConsent.setStatus(ConsentStatus.EXPIRED);
                    currentConsent.setUpdatedAt(LocalDateTime.now());
                    currentConsent.setEndDate(LocalDateTime.now());
                    currentConsent.setStaleStatus(StaleStatus.NOT_STALE);
                    anyChange = true;
                    expiredCount++;
                    affectedConsents.add(currentConsent);

                    logConsentExpiryAudit(runId, currentConsent, ActionType.UPDATED);

                    // ---------- Notification ----------
                    try {
                        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");

                        ZonedDateTime ist = currentConsent.getEndDate()
                                .atZone(ZoneId.of("UTC"))
                                .withZoneSameInstant(ZoneId.of("Asia/Kolkata"));

                        List<String> allProcessorIds =
                                (consent.getPreferences() != null ? consent.getPreferences() : List.<HandlePreference>of())
                                        .stream()
                                        .filter(p -> p.getProcessorActivityList() != null)
                                        .flatMap(p -> p.getProcessorActivityList().stream())
                                        .filter(pa -> pa.getProcessActivityInfo() != null &&
                                                pa.getProcessActivityInfo().getProcessorId() != null)
                                        .map(pa -> pa.getProcessActivityInfo().getProcessorId())
                                        .distinct()
                                        .collect(Collectors.toList());

                        Map<String, Object> payload = new HashMap<>();
                        payload.put("expiryDate", ist.format(fmt));
                        payload.put("consentId", currentConsent.getConsentId());

                        log.info("[Notification] Sending expiry notification for consent {} (tenant={})",
                                currentConsent.getConsentId(), tenantId);

                        notificationManager.initiateConsentNotification(
                                NotificationEvent.CONSENT_EXPIRED,
                                tenantId,
                                currentConsent.getBusinessId() != null ? currentConsent.getBusinessId() : null,
                                currentConsent.getCustomerIdentifiers() != null ? currentConsent.getCustomerIdentifiers() : null,
                                allProcessorIds,
                                payload,
                                currentConsent.getLanguagePreferences() != null ? currentConsent.getLanguagePreferences() : LANGUAGE.ENGLISH,
                                currentConsent.getConsentId()
                        );        
                    } catch (Exception ne) {
                        log.error("[NotificationError] Failed to send expiry notification for consent {}: {}",
                                currentConsent.getConsentId(), ne.getMessage());
                    }

                    // ---------- System Registry Consent Withdraw ----------
                    try {
                        log.info("[SystemRegistry] Initiating consent withdraw for consent {} (tenant={})",
                                currentConsent.getConsentId(), tenantId);

                        systemRegistryManager.withdrawConsent(
                                currentConsent.getConsentId(),
                                tenantId,
                                currentConsent.getBusinessId() != null ? currentConsent.getBusinessId() : tenantId
                        );

                    } catch (Exception sre) {
                        log.error("[SystemRegistryError] Failed to trigger consent withdrawal for consentId {}: {}",
                                currentConsent.getConsentId(), sre.getMessage());
                    }
                }

                // ---------- Changes to Save? ----------
                if (anyChange) {

                    log.info("[ConsentUpdate] Updating consent {} for tenant {}",
                            currentConsent.getConsentId(), tenantId);

                    generateConsentJwtToken(currentConsent);
                    log.debug("[ConsentUpdate] Generated JWT token for consent {}", currentConsent.getConsentId());

                    // Compute SHA-256 hash of consentJsonString and store as payloadHash
                    String consentJsonStringHash = null;
                    if (currentConsent.getConsentJsonString() != null) {
                        consentJsonStringHash = utils.computeSHA256Hash(currentConsent.getConsentJsonString());
                        currentConsent.setPayloadHash(consentJsonStringHash);

                        log.debug("[Hash] Computed payloadHash for consent {} = {}", currentConsent.getConsentId(),
                                consentJsonStringHash);
                    }

                    // ---------- Hash Chain ----------
                    synchronized (dataHashLock) {

                        log.debug("[HashChain] Computing chain hash for consent {}", currentConsent.getConsentId());

                        Consent latestConsent = consentRepository.findLatestByCreatedAt();
                        String prevHash = (latestConsent != null ? latestConsent.getCurrentChainHash() : null);

                        String currentChainHash;
                        if (prevHash != null && consentJsonStringHash != null) {
                            currentChainHash = utils.computeSHA256Hash(prevHash + consentJsonStringHash);
                        } else if (consentJsonStringHash != null) {
                            currentChainHash = consentJsonStringHash;
                        } else {
                            currentChainHash = null;
                        }
                        currentConsent.setCurrentChainHash(currentChainHash);

                        log.info("[HashChain] Updated chain hash for consent {} -> {}",
                                currentConsent.getConsentId(), currentChainHash);

                        consentRepository.save(currentConsent);
                    }

                    log.debug("[ConsentSave] Saved new version of consent {} for tenant {}",
                            currentConsent.getConsentId(), tenantId);

                    // ---------- Mark original as stale ----------
                    if (originalId != null && consent != null) {
                        log.info("[ConsentStale] Marking original consent {} as STALE", originalId);
                        consent.setStaleStatus(StaleStatus.STALE);
                        consentRepository.save(consent);
                    }
                }

            } catch (Exception e) {
                log.error("[ConsentError] Error processing consent {} for tenant {}: {}",
                        currentConsent.getConsentId(), tenantId, e.getMessage(), e);
            }
        }

        log.info("[ConsentBatch] Completed processing. tenantId={} runId={} expiredCount={}",
                tenantId, runId, expiredCount);

        return new TenantResult(expiredCount, affectedConsents);
    }

    private com.jio.schedular.dto.Resource createResource(String type, String id, String businessId) {
        return com.jio.schedular.dto.Resource.builder()
            .type(type)
            .id(id)
            .businessId(businessId)
            .build();
    }

    /**
     * Generate JWT token for consent using Vault sign API
     * Also sets the consentJsonString and encryptedReferenceId fields on the consent object
     *
     * @param consent The consent entity to generate token for
     * @return JWT token string
     * 
     */
    private String generateConsentJwtToken(Consent consent) {
        Consent tempConsent = new Consent(consent);
        Gson gson = new GsonBuilder()
                .registerTypeAdapter(LocalDateTime.class, new LocalDateTypeAdapter())
                .disableHtmlEscaping()
                .create();
        String consentJsonString = gson.toJson(tempConsent).replaceAll("\\s+", "");

        // Store consentJsonString in the consent entity
        consent.setConsentJsonString(consentJsonString);

        // Encrypt the consentJsonString using Vault encryptPayload API
        String tenantId = ThreadContext.get(Constants.TENANT_ID_HEADER);
        String businessId = consent.getBusinessId();
        try {
            EncryptPayloadResponse encryptResponse = vaultManager.encryptPayload(
                    tenantId,
                    businessId,
                    "Consent",
                    "Consent",
                    consentJsonString
            );
            // Store encryptedReferenceId in the consent entity
            consent.setEncryptedReferenceId(encryptResponse.getReferenceId());
        } catch (Exception e) {
            log.error("Failed to encrypt consent payload for consent id: {}, error: {}",
                    consent.getConsentId(), e.getMessage(), e);
            // Continue with JWT generation even if encryption fails
        }

        // Build payload for Vault sign API
        Map<String, Object> payload = new HashMap<>();
        payload.put("sub", consentJsonString);
        payload.put("iss", "JIO CONSENT");
        payload.put("iat", consent.getStartDate().atZone(ZoneId.systemDefault()).toEpochSecond());
        payload.put("exp", consent.getEndDate().atZone(ZoneId.systemDefault()).toEpochSecond());

        return vaultManager.sign(tenantId, businessId, payload).getJwt();
    }

    /**
     * Modular function to log consent audit events
     * Can be used in both create and update consent flows
     *
     * @param consent The consent affected
     * @param actionType The action type (UPDATED)
     */
    public void logConsentExpiryAudit(UUID runId, Consent consent, ActionType actionType) {
        try {
            String tenantId = ThreadContext.get(Constants.TENANT_ID_HEADER);

            Actor actor = Actor.builder()
                    .id(runId.toString())
                    .role(Constants.SCHEDULAR)
                    .type(Constants.RUN_ID)
                    .build();

            Resource resource = Resource.builder()
                    .type(Constants.CONSENT_ID)
                    .id(consent.getConsentId())
                    .build();

            Context context = Context.builder()
                    .ipAddress(ThreadContext.get(Constants.SOURCE_IP) != null && !ThreadContext.get(Constants.SOURCE_IP).equals("-")
                            ? ThreadContext.get(Constants.SOURCE_IP) : UUID.randomUUID().toString())
                    .txnId(ThreadContext.get(Constants.TXN_ID_THREAD_CONTEXT) != null && !ThreadContext.get(Constants.TXN_ID_THREAD_CONTEXT).equals("-")
                            ? ThreadContext.get(Constants.TXN_ID_THREAD_CONTEXT) : Constants.IP_ADDRESS)
                    .build();

            Map<String, Object> extra = new HashMap<>();
            
            extra.put(Constants.DATA, consent);

            AuditRequest auditRequest = AuditRequest.builder()
                    .actor(actor)
                    .businessId(consent.getBusinessId())
                    .group(String.valueOf(Group.CONSENT))
                    .component(AuditComponent.CONSENT_EXPIRED)
                    .actionType(actionType)
                    .resource(resource)
                    .initiator(Constants.SCHEDULAR)
                    .context(context)
                    .extra(extra)
                    .build();

            this.auditManager.logAudit(auditRequest, tenantId);
        } catch (Exception e) {
            log.error("Audit logging failed for run id: {}, consent id: {}, action: {}, error: {}", 
                    runId.toString(), consent.getConsentId(), actionType, e.getMessage(), e);
        }
    }
}