package com.jio.schedular.service;

import com.jio.schedular.client.audit.AuditManager;
import com.jio.schedular.client.audit.request.Actor;
import com.jio.schedular.client.audit.request.AuditRequest;
import com.jio.schedular.client.audit.request.Context;
import com.jio.schedular.client.audit.request.Resource;
import com.jio.schedular.constant.Constants;
import com.jio.schedular.dto.*;
import com.jio.schedular.enums.*;
import com.jio.schedular.entity.Consent;
import com.jio.schedular.entity.DpoConfiguration;
import com.jio.schedular.entity.SchedularStats;
import com.jio.schedular.entity.TenantRegistry;
import com.jio.schedular.multitenancy.TenantMongoTemplateProvider;
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
import java.util.stream.Collectors;

/**
 * Service that removes/archives consent artifacts older than configured retention period (value + unit)
 * per tenant.
 *
 * Behavior:
 * - Iterates active tenants in parallel (thread pool size configurable)
 * - Loads per-tenant artifact retention window (value + unit) from consent_configurations.configurationJson
 *   It tries several likely keys (artefactRetention, consentArtifactRetention, consentArtifactRetentionPeriod).
 * - Computes cutoff based on unit and value (DAYS, WEEKS, MONTHS, YEARS, HOURS)
 * - Selects consent documents where artifacts.createdAt <= cutoff and deletes them in batches
 * - Sends a per-tenant notification payload containing:
 *     consentArtifactRetention { value, unit }, consentExpiryDate, consentArtifactList { count, ids }
 * - Persists per-tenant SchedularStats
 */
@Service
@Slf4j
public class ConsentArtifactRetentionService {

    private final TenantRegistryRepository tenantRegistryRepository;
    private final TenantMongoTemplateProvider tenantMongoTemplateProvider;
    private final NotificationManager notificationManager;
    private final SchedularStatsRepository statsRepository;
    private ExecutorService executorService;
    private final AuditManager auditManager;

    @Value("${schedular.job.consent-artifact-retention.thread-pool-size:10}")
    private int threadPoolSize;

    /**
     * Fallback retention when tenant config is missing — interpreted as days.
     */
    @Value("${consent-artifact.default-retention-days:2}")
    private int defaultRetentionDays;

    @Autowired
    public ConsentArtifactRetentionService(TenantRegistryRepository tenantRegistryRepository,
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
        log.info("Initialized ConsentArtifactRetentionService with thread pool size: {}", threadPoolSize);
    }

    @PreDestroy
    public void shutdown() {
        if (executorService != null) {
            log.info("Shutting down ConsentArtifactRetentionService executor");
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
     * Entry point — processes all active tenants.
     *
     * @param batchSize number of consent documents to fetch/delete per page
     * @return total number of consent documents removed across tenants
     */
    public int processConsentArtifactRetention(int batchSize) {
        log.info("Starting ConsentArtifactRetentionService - batchSize: {}", batchSize);

        UUID runId = UUID.randomUUID();
        Instant start = Instant.now();
        int totalProcessed = 0;

        List<TenantRegistry> activeTenants = tenantRegistryRepository.findByStatus(Status.ACTIVE);
        if (activeTenants == null || activeTenants.isEmpty()) {
            log.warn("No active tenants found for ConsentArtifactRetentionService");
            return 0;
        }

        List<Callable<TenantResult>> tasks = activeTenants.stream()
                .map(t -> (Callable<TenantResult>) () -> processTenantRetention(t, batchSize))
                .collect(Collectors.toList());

        try {
            List<Future<TenantResult>> futures = executorService.invokeAll(tasks);

            for (int i = 0; i < futures.size(); i++) {
                TenantRegistry tenant = activeTenants.get(i);
                String tenantId = tenant.getTenantId();

                try {
                    TenantResult result = futures.get(i).get();
                    int processed = result.processed();
                    totalProcessed += processed;

                    long duration = Duration.between(start, Instant.now()).toMillis();
                    List<com.jio.schedular.dto.Resource> resources;
                    Set<String> consentIds = new HashSet<>();

                    if(result.resources().isEmpty()) {
                        log.info("No consent artifacts found for tenant: {}", tenantId);
                        resources = List.of(createResource(Constants.TENANT_ID, tenantId, tenantId));
                    }
                    else {
                        log.info("Tenant: {} processed {} consent artifacts for retention", tenantId, processed);
                        resources = result.resources().stream()
                            .map(consent -> createResource(Constants.CONSENT_ID, consent.getConsentId(), consent.getBusinessId()))
                            .toList();
                    
                        consentIds = result.resources().stream()
                            .map(Consent::getConsentId)
                            .collect(Collectors.toSet());
                    }

                    log.info("Saving ConsentArtifactRetention stats for tenant: {} | processed={} | duration={}ms",
                            tenantId, processed, duration);

                    // Save tenant summary stats
                    statsRepository.saveTenantSummary(tenantId, SchedularStats.builder()
                            .runId(runId.toString())
                            .jobName(JobName.CONSENT_ARTIFACT_RETENTION_JOB)
                            .group(Group.CONSENT)
                            .action(Action.CONSENT_ARTIFACT_RETENTION)
                            .summaryType(SummaryType.TENANT_SUMMARY)
                            .status(SchedularStatus.SUCCESS)
                            .resources(resources)
                            .totalAffected(processed)
                            .timestamp(LocalDateTime.now())
                            .durationMillis(duration)
                            .details(Map.of("batchSize", batchSize,
                                            "retentionValue", result.retentionWindow().value(),
                                            "retentionUnit", result.retentionWindow().unit()))
                            .build());

                    DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");

                    ZonedDateTime ist = result.cutoff()        // LocalDateTime
                            .atZone(ZoneId.of("UTC"))
                            .withZoneSameInstant(ZoneId.of("Asia/Kolkata"));

                    // Build notification payload with requested values
                    Map<String, Object> payload = new HashMap<>();
                    payload.put("retentionPeriodValue", result.retentionWindow().value());
                    payload.put("retentionPeriodUnit", result.retentionWindow().unit());
                    payload.put("expiryDate", ist.format(fmt));
                    payload.put("expiredRecordsCount", processed);
                    payload.put("processedIds", consentIds);

                    log.info("Finding DPO configurations for tenant: {}", tenantId);

                    CustomerIdentifiers customerIdentifiers = findDpoConfigurationsForTenant(tenantId);

                    log.debug("Customer Identifier value for tenant {}: {}", tenantId, customerIdentifiers.getValue());
                    log.info("Initiating retention notification for tenant: {} | payload={}", tenantId, payload);

                    notificationManager.initiateRetentionNotification(
                            NotificationEvent.RETENTION_CONSENT_ARTIFACT_EXPIRED,
                            tenantId,
                            customerIdentifiers,
                            payload,
                            LANGUAGE.ENGLISH,
                            runId.toString()
                    );

                    // Log audit event for consent artifact retention
                    logConsentArtifactRetentionAudit(runId, consentIds, ActionType.NOTIFICATION_SENT); // change action type if needed

                } catch (ExecutionException | InterruptedException e) {
                    long duration = Duration.between(start, Instant.now()).toMillis();
                    statsRepository.saveTenantSummary(tenantId, SchedularStats.builder()
                            .runId(runId.toString())
                            .jobName(JobName.CONSENT_ARTIFACT_RETENTION_JOB)
                            .group(Group.CONSENT)
                            .action(Action.CONSENT_ARTIFACT_RETENTION)
                            .summaryType(SummaryType.TENANT_SUMMARY)
                            .status(SchedularStatus.FAILED)
                            .errorCount(1)
                            .lastError(e.getMessage())
                            .timestamp(LocalDateTime.now())
                            .durationMillis(duration)
                            .details(Map.of("batchSize", batchSize))
                            .build());
                    log.error("Tenant {} failed ConsentArtifactRetention: {}", tenantId, e.getMessage(), e);
                    if (e instanceof InterruptedException) Thread.currentThread().interrupt();
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("ConsentArtifactRetentionService interrupted", e);
        }

        log.info("ConsentArtifactRetentionService completed | runId={} | processed={}", runId, totalProcessed);
        return totalProcessed;
    }

    /**
     * Process retention for a single tenant.
     * Loads retention window (value + unit), computes cutoff, pages through consents with artifacts older than cutoff,
     * deletes them and returns processed ids/count.
     */
    private TenantResult processTenantRetention(TenantRegistry tenant, int batchSize) {
        String tenantId = tenant.getTenantId();
        ThreadContext.put(Constants.TENANT_ID_HEADER, tenantId);
        log.info("Processing consent artifact retention for tenant: {}", tenantId);

        try {
            MongoTemplate mongoTemplate = tenantMongoTemplateProvider.getMongoTemplate(tenantId);

            // Load retention window (value + unit) from tenant config
            RetentionWindow window = loadTenantRetentionWindow(mongoTemplate);

            LocalDateTime cutoff = computeCutoff(LocalDateTime.now(), window.unit(), window.value());

            int processed = 0;
            int page = 0;
            boolean hasMore = true;
            Set<Consent> processedConsents = new HashSet<>();

            while (hasMore) {
                Pageable pageable = PageRequest.of(page, Math.max(1, batchSize));

                // Query documents that have status as EXPIRED and updatedAt <= cutoff
                Criteria criteria = Criteria.where("status").is(String.valueOf(ConsentStatus.EXPIRED))
                                            .and("staleStatus").is(String.valueOf(StaleStatus.NOT_STALE))
                                            .and("updatedAt").lte(cutoff);
                Query query = new Query(criteria).with(pageable);

                List<Consent> consents = mongoTemplate.find(query, Consent.class, "consents");
                consents = consents.stream()
                    .filter(consent -> !notificationEventExists(consent, mongoTemplate))
                    .toList();
                if (consents == null || consents.isEmpty()) {
                    break;
                }

                processed += consents.size();
                processedConsents.addAll(consents);

                if (consents.size() < batchSize) hasMore = false;
                else page++;
            }

            log.info("Tenant {} consent artifact retention complete. Processed: {}", tenantId, processed);
            return new TenantResult(processed, processedConsents, window, cutoff);

        } catch (Exception e) {
            log.error("Error processing consent artifact retention for tenant {}: {}", tenantId, e.getMessage(), e);
            return new TenantResult(0, Set.of(), new RetentionWindow(defaultRetentionDays, "DAYS"), LocalDateTime.now());
        } finally {
            ThreadContext.clearAll();
        }
    }

    private boolean notificationEventExists(Consent consent, MongoTemplate mongoTemplate) {

        return mongoTemplate.exists(
                Query.query(
                    Criteria.where("event_type").is("RETENTION_CONSENT_ARTIFACT_EXPIRED")
                            .and("event_payload.processedIds").is(consent.getConsentId()) //add conditions as/when required
                ),
                NotificationEvent.class,
                "notification_events"
        );
    }

    /**
     * Attempts to read a retention window object { value, unit } from retention configuration documents.
     * falls back to defaultRetentionDays in DAYS.
     */
    private RetentionWindow loadTenantRetentionWindow(MongoTemplate mongoTemplate) {
        try {
            Query q = new Query(Criteria.where("retentions.consent_artifact_retention").exists(true));
            Document cfg = mongoTemplate.findOne(q, Document.class, Constants.RETENTION_CONFIG);

            if (cfg != null) {
                Document retentions = (Document) cfg.get("retentions");
                if (retentions != null) {
                    String key = "consent_artifact_retention";
                    Object obj = retentions.get(key);

                    if (obj instanceof Document doc) {
                        Integer value = doc.getInteger("value");
                        String unit = doc.getString("unit");

                        if (value != null && value > 0 && unit != null && !unit.isBlank()) {
                            log.debug("Found tenant retention config {}: {} {}", key, value, unit);
                            return new RetentionWindow(value, unit);
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Failed to load tenant retention window: {}. Using fallback {} DAYS",
                    e.getMessage(), defaultRetentionDays);
        }

        return new RetentionWindow(defaultRetentionDays, "DAYS");
    }


    /**
     * Compute cutoff by subtracting value units from now.
     */
    private LocalDateTime computeCutoff(LocalDateTime now, String unit, int value) {
        if (unit == null) unit = "DAYS";
        switch (unit.toUpperCase()) {
            case "DAYS", "DAY" -> { return now.minusDays(value); }
            case "WEEKS", "WEEK" -> { return now.minusWeeks(value); }
            case "MONTHS", "MONTH" -> { return now.minusMonths(value); }
            case "YEARS", "YEAR" -> { return now.minusYears(value); }
            case "HOURS", "HOUR" -> { return now.minusHours(value); }
            default -> { return now.minusDays(value); }
        }
    }

    private com.jio.schedular.dto.Resource createResource(String type, String id, String businessId) {
        return com.jio.schedular.dto.Resource.builder().type(type).id(id).businessId(businessId).build();
    }

    private CustomerIdentifiers findDpoConfigurationsForTenant(String tenantId) {
        MongoTemplate mongoTemplate = tenantMongoTemplateProvider.getMongoTemplate(tenantId);

        // Fetch the active config
        Query query = new Query();
        query.addCriteria(Criteria.where("businessId").is(tenantId));

        DpoConfiguration config = mongoTemplate.findOne(query, DpoConfiguration.class, "dpo_configurations");

        if (config == null || config.getConfigurationJson() == null) {
            return null; // or throw error based on your requirement
        }

        Map<String, Object> json = config.getConfigurationJson();

        String mobile = json.get("mobile") != null ? json.get("mobile").toString().trim() : null;
        String email  = json.get("email") != null ? json.get("email").toString().trim() : null;

        log.debug("DPO Configuration for tenant {}: mobile={}, email={}", tenantId, mobile, email);

        // Priority: mobile -> email
        if (email != null && !email.isEmpty()) {
            return CustomerIdentifiers.builder()
                    .type(IdentityType.EMAIL)
                    .value(email)
                    .build();
        }

        if (mobile != null && !mobile.isEmpty()) {
            return CustomerIdentifiers.builder()
                    .type(IdentityType.MOBILE)
                    .value(mobile)
                    .build();
        }

        // should never happen (your data guarantees one will be present)
        return null;
    }

    private record RetentionWindow(int value, String unit) {}
    private record TenantResult(int processed, Set<Consent> resources, RetentionWindow retentionWindow, LocalDateTime cutoff) {}
    
    /**
     * Modular function to log consent artifact retention audit events
     * Can be used in both create and update consent artifact retention flows
     *
     * @param consentIds The set of consent Ids affected
     * @param actionType The action type (DELETE) //NOTIFICATION_SENT for now
     */
    public void logConsentArtifactRetentionAudit(UUID runId, Set<String> consentIds, ActionType actionType) {
        try {
            String tenantId = ThreadContext.get(Constants.TENANT_ID_HEADER);

            Actor actor = Actor.builder()
                    .id(runId.toString())
                    .role(Constants.SCHEDULAR)
                    .type(Constants.RUN_ID)
                    .build();

            Resource resource = Resource.builder()
                    .type(Constants.TENANT_ID)
                    .id(tenantId)
                    .build();

            Context context = Context.builder()
                    .ipAddress(ThreadContext.get(Constants.SOURCE_IP) != null && !ThreadContext.get(Constants.SOURCE_IP).equals("-")
                            ? ThreadContext.get(Constants.SOURCE_IP) : UUID.randomUUID().toString())
                    .txnId(ThreadContext.get(Constants.TXN_ID_THREAD_CONTEXT) != null && !ThreadContext.get(Constants.TXN_ID_THREAD_CONTEXT).equals("-")
                            ? ThreadContext.get(Constants.TXN_ID_THREAD_CONTEXT) : Constants.IP_ADDRESS)
                    .build();

            Map<String, Object> extra = new HashMap<>();
            
            extra.put(Constants.DATA, consentIds);

            AuditRequest auditRequest = AuditRequest.builder()
                    .actor(actor)
                    .businessId(tenantId)
                    .group(String.valueOf(Group.CONSENT))
                    .component(AuditComponent.CONSENT_ARTIFACT_RETENTION)
                    .actionType(actionType)
                    .resource(resource)
                    .initiator(Constants.SCHEDULAR)
                    .context(context)
                    .extra(extra)
                    .build();

            this.auditManager.logAudit(auditRequest, tenantId);
        } catch (Exception e) {
            log.error("Audit logging failed for run id: {}, action: {}, error: {}", 
                    runId.toString(), actionType, e.getMessage(), e);
        }
    }
}