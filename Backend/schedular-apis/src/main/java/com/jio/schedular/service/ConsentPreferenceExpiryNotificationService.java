package com.jio.schedular.service;

import com.jio.schedular.client.audit.AuditManager;
import com.jio.schedular.client.audit.request.Actor;
import com.jio.schedular.client.audit.request.AuditRequest;
import com.jio.schedular.client.audit.request.Context;
import com.jio.schedular.client.audit.request.Resource;
import com.jio.schedular.constant.Constants;
import com.jio.schedular.dto.*;
import com.jio.schedular.entity.Consent;
import com.jio.schedular.entity.SchedularStats;
import com.jio.schedular.entity.TenantRegistry;
import com.jio.schedular.enums.*;
import com.jio.schedular.multitenancy.TenantMongoTemplateProvider;
import com.jio.schedular.repository.ConsentRepository;
import com.jio.schedular.repository.SchedularStatsRepository;
import com.jio.schedular.repository.TenantRegistryRepository;
import com.jio.schedular.client.notification.NotificationManager;

import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.ThreadContext;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

import java.time.*;
import java.time.Duration;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * Service that checks active consents for each tenant and sends notifications
 * for preferences whose endDate falls within the configured autoRenewalReminderPeriod.
 *
 * Only preferences with:
 *  - isAutoRenew = true
 *  - preferenceStatus = ACCEPTED
 * are considered.
 */
@Service
@Slf4j
public class ConsentPreferenceExpiryNotificationService {

    private final TenantRegistryRepository tenantRegistryRepository;
    private final TenantMongoTemplateProvider tenantMongoTemplateProvider;
    private final NotificationManager notificationManager;
    private final SchedularStatsRepository statsRepository;
    private ExecutorService executorService;
    private final AuditManager auditManager;

    @Value("${schedular.job.consent-preference-expiry.thread-pool-size:10}")
    private int threadPoolSize;

    /** Fallback default reminder period if tenant config not found. */
    @Value("${consent.default-reminder-days:30}")
    private int defaultReminderDays;

    @Value("${consent-preference-expiry.portal-url:}")
    private String portalUrl;

    @Autowired
    public ConsentPreferenceExpiryNotificationService(TenantRegistryRepository tenantRegistryRepository,
                                                      TenantMongoTemplateProvider tenantMongoTemplateProvider,
                                                      NotificationManager notificationManager,
                                                      SchedularStatsRepository statsRepository,
                                                      AuditManager auditManager) {
        this.tenantRegistryRepository = tenantRegistryRepository;
        this.tenantMongoTemplateProvider = tenantMongoTemplateProvider;
        this.notificationManager = notificationManager;
        this.statsRepository = statsRepository;
        this.auditManager = auditManager;
    }

    @PostConstruct
    public void init() {
        this.executorService = Executors.newFixedThreadPool(threadPoolSize);
        log.info("Initialized ConsentPreferenceExpiryNotificationService with thread pool size: {}", threadPoolSize);
    }

    @PreDestroy
    public void shutdown() {
        if (executorService != null) {
            log.info("Shutting down ConsentPreferenceExpiryNotificationService executor");
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

    /**
     * Entry point — checks all tenants and sends preference expiry reminders.
     * @param batchSize number of consents to fetch per batch
     * @return total number of notifications sent
     */
    public int sendPreferenceExpiryReminders(int batchSize) {
        log.info("Starting preference expiry reminder job, batchSize: {}", batchSize);

        UUID runId = UUID.randomUUID();
        Instant start = Instant.now();
        int totalReminded = 0;

        List<TenantRegistry> activeTenants =
                tenantRegistryRepository.findByStatus(Status.ACTIVE);

        if (activeTenants == null || activeTenants.isEmpty()) {
            log.warn("No active tenants found for reminder job");
            return 0;
        }

        LocalDateTime now = LocalDateTime.now();

        for (TenantRegistry tenant : activeTenants) {
            String tenantId = tenant.getTenantId();
            try {
                TenantReminderResult result = processTenantReminders(tenantId, batchSize, now, runId);
                int remindedCount = result.notificationsSent();
                totalReminded += remindedCount;
                List<com.jio.schedular.dto.Resource> tenantResources;

                if(result.affectedConsents().isEmpty()) {
                    log.info("No consent affected for tenant: {}", tenantId);
                    tenantResources = List.of(createResource(Constants.TENANT_ID, tenantId, tenantId));
                } else {
                    log.info("Total consents affected for tenant {}: {}", tenantId, result.affectedConsents().size());
                    tenantResources = result.affectedConsents().stream()
                        .map(consent -> createResource(Constants.CONSENT_ID, consent.getConsentId(), consent.getBusinessId()))
                        .toList();
                }

                long duration = Duration.between(start, Instant.now()).toMillis();

                statsRepository.saveTenantSummary(tenantId, SchedularStats.builder()
                        .runId(runId.toString())
                        .jobName(JobName.CONSENT_PREFERENCE_EXPIRY_NOTIFICATION_JOB)
                        .group(Group.CONSENT)
                        .action(Action.CONSENT_PREFERENCE_EXPIRY_REMINDER)
                        .summaryType(SummaryType.TENANT_SUMMARY)
                        .status(SchedularStatus.SUCCESS)
                        .resources(tenantResources)
                        .totalAffected(remindedCount)
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
                        .jobName(JobName.CONSENT_PREFERENCE_EXPIRY_NOTIFICATION_JOB)
                        .group(Group.CONSENT)
                        .action(Action.CONSENT_PREFERENCE_EXPIRY_REMINDER)
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

        log.info("[{}] Completed | runId={} | reminded={}", JobName.CONSENT_PREFERENCE_EXPIRY_NOTIFICATION_JOB, runId, totalReminded);
        return totalReminded;
    }

    /**
     * Process a single tenant — read config, then fetch and process consents.
     */
    private TenantReminderResult processTenantReminders(String tenantId, int batchSize, LocalDateTime now, UUID runId) {
        ThreadContext.put(Constants.TENANT_ID_HEADER, tenantId);
        log.info("Processing reminders for tenant: {}", tenantId);

        try {
            MongoTemplate mongoTemplate = tenantMongoTemplateProvider.getMongoTemplate(tenantId);

            // Get autoRenewalReminderPeriod for this tenant
            ReminderWindow reminderWindow = loadReminderWindowFromConfig(mongoTemplate);
            LocalDateTime upperBound = computeUpperBound(now, reminderWindow.unit(), reminderWindow.value());

            int tenantNotifications = 0;
            int page = 0;
            boolean hasMore = true;
            Set<Consent> tenantConsents = new HashSet<>();

            while (hasMore) {
                Pageable pageable = PageRequest.of(page, Math.max(1, batchSize));
                List<Consent> consents = findConsentsWithinWindow(mongoTemplate, now, upperBound, pageable);

                if (consents == null || consents.isEmpty()) {
                    break;
                }

                TenantReminderResult batchResult = processConsentBatch(consents, tenantId, now, upperBound, runId, mongoTemplate);
                tenantNotifications += batchResult.notificationsSent();
                tenantConsents.addAll(batchResult.affectedConsents());

                if (consents.size() < batchSize) hasMore = false;
                else page++;
            }

            log.info("Tenant {} reminders complete. Notifications sent: {}", tenantId, tenantNotifications);
            return new TenantReminderResult(tenantNotifications, tenantConsents);

        } catch (Exception e) {
            log.error("Error processing reminders for tenant {}: {}", tenantId, e.getMessage(), e);
            return new TenantReminderResult(0, Set.of());
        } finally {
            ThreadContext.clearAll();
        }
    }

    /**
     * Load autoRenewalReminderPeriod config from consent_configurations.
     * If not found, fallback to defaultReminderDays.
     */
    private ReminderWindow loadReminderWindowFromConfig(MongoTemplate mongoTemplate) {
        try {
            Query query = new Query(Criteria.where("configurationJson.autoRenewalReminderPeriod").exists(true));
            Document cfg = mongoTemplate.findOne(query, Document.class, "consent_configurations");

            if (cfg != null) {
                Document conf = (Document) cfg.get("configurationJson");
                if (conf != null) {
                    Document reminder = (Document) conf.get("autoRenewalReminderPeriod");
                    if (reminder != null) {
                        Integer value = reminder.getInteger("value");
                        String unit = reminder.getString("unit");

                        if (value != null && value > 0 && unit != null && !unit.isBlank()) {
                            log.debug("Found autoRenewalReminderPeriod: {} {}", value, unit);
                            return new ReminderWindow(value, unit);
                        }
                    }
                }
            }

            // No valid config found, fallback
            log.warn("autoRenewalReminderPeriod not found. Using default: {} DAYS", defaultReminderDays);
            return new ReminderWindow(defaultReminderDays, "DAYS");

        } catch (Exception e) {
            log.error("Failed to load autoRenewalReminderPeriod: {}. Using default {} DAYS",
                    e.getMessage(), defaultReminderDays);
            return new ReminderWindow(defaultReminderDays, "DAYS");
        }
    }

    /**
     * Query only consents whose preference endDate falls within [now, upperBound].
     */
    private List<Consent> findConsentsWithinWindow(MongoTemplate mongoTemplate,
                                                   LocalDateTime now,
                                                   LocalDateTime upperBound,
                                                   Pageable pageable) {
        Criteria criteria = new Criteria()
                .and("status").is(ConsentStatus.ACTIVE)
                .and("staleStatus").is(StaleStatus.NOT_STALE)
                .and("preferences.endDate").gte(now).lte(upperBound);

        Query query = new Query(criteria).with(pageable);
        return mongoTemplate.find(query, Consent.class);
    }

    /**
     * For each consent, check its preferences and send reminders where required.
     */
    private TenantReminderResult processConsentBatch(List<Consent> consents, String tenantId,
                                    LocalDateTime now, LocalDateTime upperBound, UUID runId, MongoTemplate mongoTemplate) {
        int count = 0;
        Set<Consent> affectedConsents = new HashSet<>();

        for (Consent consent : consents) {
            if (consent == null || consent.getPreferences() == null) continue;

            boolean anyPreferenceReminded = false;

            for (HandlePreference pref : consent.getPreferences()) {
                try {
                    if (pref == null) continue;
                    if (!Boolean.TRUE.equals(pref.isAutoRenew())) continue;
                    if (!PreferenceStatus.ACCEPTED.equals(pref.getPreferenceStatus())) continue;

                    LocalDateTime endDate = pref.getEndDate();
                    if (endDate == null) continue;

                    if (!endDate.isBefore(now) && !endDate.isAfter(upperBound) && !notificationEventExists(consent, pref, mongoTemplate)) {
                        sendNotification(tenantId, consent, pref, endDate);
                        logConsentPreferenceExpiryNotificationAudit(runId, pref, consent, ActionType.NOTIFICATION_SENT);
                        count++;
                        anyPreferenceReminded = true;
                    }
                } catch (Exception e) {
                    log.error("Error processing preference {} in consent {}: {}",
                            pref.getPreferenceId(), consent.getId(), e.getMessage(), e);
                }
            }
            if (anyPreferenceReminded && consent.getConsentId() != null) {
                affectedConsents.add(consent);
            }
        }
        return new TenantReminderResult(count, affectedConsents);
    }

    /**
     * Sends the expiry notification using NotificationManager.
     */
    private void sendNotification(String tenantId, Consent consent,
                                  HandlePreference pref, LocalDateTime endDate) {
        
        // Extract processor IDs from processor activities in consent preferences
        List<String> processorIds = pref.getProcessorActivityList().stream()
            .filter(pa -> pa.getProcessActivityInfo() != null && pa.getProcessActivityInfo().getProcessorId() != null)
            .map(pa -> pa.getProcessActivityInfo().getProcessorId())
            .distinct()
            .collect(Collectors.toList());

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");

        ZonedDateTime ist = endDate          // LocalDateTime
                .atZone(ZoneId.of("UTC"))
                .withZoneSameInstant(ZoneId.of("Asia/Kolkata"));

        Map<String, Object> payload = new HashMap<>();
        payload.put("preferenceId", pref.getPreferenceId());
        payload.put("expiryDate", ist.format(fmt));
        payload.put("consentId", consent.getConsentId());
        payload.put("portalUrl", portalUrl);

        notificationManager.initiateConsentNotification(
                NotificationEvent.CONSENT_PREFERENCE_EXPIRY,
                tenantId,
                consent.getBusinessId() != null ? consent.getBusinessId() : null,
                consent.getCustomerIdentifiers() != null ? consent.getCustomerIdentifiers() : null,
                processorIds,
                payload,
                consent.getLanguagePreferences() != null ? consent.getLanguagePreferences() : LANGUAGE.ENGLISH,
                consent.getConsentId()
        );
    }

    /**
     * Add days/weeks/months/years to now based on unit and value.
     */
    private LocalDateTime computeUpperBound(LocalDateTime now, String unit, int value) {
        return switch (unit.toUpperCase()) {
            case "DAYS", "DAY" -> now.plusDays(value);
            case "WEEKS", "WEEK" -> now.plusWeeks(value);
            case "MONTHS", "MONTH" -> now.plusMonths(value);
            case "YEARS", "YEAR" -> now.plusYears(value);
            case "HOURS", "HOUR" -> now.plusHours(value);
            default -> now;
        };
    }

    private com.jio.schedular.dto.Resource createResource(String type, String id, String businessId) {
        return com.jio.schedular.dto.Resource.builder()
            .type(type)
            .id(id)
            .businessId(businessId)
            .build();
    }

    private boolean notificationEventExists(Consent consent,HandlePreference pref, MongoTemplate mongoTemplate) {
        
        return mongoTemplate.exists(
                Query.query(
                    Criteria.where("event_type").is("CONSENT_PREFERENCE_EXPIRY")
                            .and("event_payload.consentId").is(consent.getId())
                            .and("event_payload.preferenceId").is(pref.getPreferenceId())
                ),
                NotificationEvent.class,
                "notification_events"
        );
    }

    /**
     * Simple immutable holder for reminder period values.
     */
    private record ReminderWindow(int value, String unit) {}

    private record TenantReminderResult(int notificationsSent, Set<Consent> affectedConsents) {}

    /**
     * Modular function to log preference expiry notification events
     *
     * @param preference The preference affected
     * @param consent The consent to which the affected preference belongs to
     * @param actionType The action type (NOTIFICATION_SENT)
     */
    public void logConsentPreferenceExpiryNotificationAudit(UUID runId, HandlePreference preference, Consent consent, ActionType actionType) {
        try {
            String tenantId = ThreadContext.get(Constants.TENANT_ID_HEADER);

            Actor actor = Actor.builder()
                    .id(runId.toString())
                    .role(Constants.SCHEDULAR)
                    .type(Constants.RUN_ID)
                    .build();

            Resource resource = Resource.builder()
                    .type(Constants.PREFERENCE_ID)
                    .id(preference.getPreferenceId())
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
                    .component(AuditComponent.CONSENT_PREFERENCE_EXPIRY_REMINDER)
                    .actionType(actionType)
                    .resource(resource)
                    .initiator(Constants.SCHEDULAR)
                    .context(context)
                    .extra(extra)
                    .build();

            this.auditManager.logAudit(auditRequest, tenantId);
        } catch (Exception e) {
            log.error("Audit logging failed for run id: {}, preference id: {}, consent id: {}, action: {}, error: {}", 
                    runId.toString(), preference.getPreferenceId(), consent.getConsentId(), actionType, e.getMessage(), e);
        }
    }
}