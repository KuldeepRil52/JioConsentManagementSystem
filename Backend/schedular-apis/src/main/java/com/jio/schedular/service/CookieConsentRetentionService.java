package com.jio.schedular.service;

import com.jio.schedular.client.audit.AuditManager;
import com.jio.schedular.client.audit.request.Actor;
import com.jio.schedular.client.audit.request.AuditRequest;
import com.jio.schedular.client.audit.request.Context;
import com.jio.schedular.client.audit.request.Resource;
import com.jio.schedular.constant.Constants;
import com.jio.schedular.dto.*;
import com.jio.schedular.enums.*;
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
 * Service that removes cookie consent documents older than configured retention days per tenant.
 * - Iterates active tenants (parallelized)
 * - Deletes old cookie consent documents in batches
 * - Sends per-tenant notification summary and writes stats
 */
@Service
@Slf4j
public class CookieConsentRetentionService {

    private final TenantRegistryRepository tenantRegistryRepository;
    private final TenantMongoTemplateProvider tenantMongoTemplateProvider;
    private final NotificationManager notificationManager;
    private final SchedularStatsRepository statsRepository;
    private ExecutorService executorService;
    private final AuditManager auditManager;

    @Value("${schedular.job.cookie-consent-retention.thread-pool-size:10}")
    private int threadPoolSize;

    @Value("${cookie-consent.default-retention-days:2}")
    private int defaultRetentionDays;

    @Autowired
    public CookieConsentRetentionService(TenantRegistryRepository tenantRegistryRepository,
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
        log.info("Initialized CookieConsentRetentionService with thread pool size: {}", threadPoolSize);
    }

    @PreDestroy
    public void shutdown() {
        if (executorService != null) {
            log.info("Shutting down CookieConsentRetentionService executor");
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

    public int processCookieConsentRetention(int batchSize) {
        log.info("Starting CookieConsentRetentionService - batchSize: {}", batchSize);

        UUID runId = UUID.randomUUID();
        Instant start = Instant.now();
        int totalProcessed = 0;

        List<TenantRegistry> activeTenants = tenantRegistryRepository.findByStatus(Status.ACTIVE);
        if (activeTenants == null || activeTenants.isEmpty()) {
            log.warn("No active tenants found for CookieConsentRetentionService");
            return 0;
        }

        List<Callable<TenantResult>> tasks = activeTenants.stream()
                .map(t -> (Callable<TenantResult>) () -> processTenantCookieRetention(t.getTenantId(), batchSize))
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
                    Set<String> cookieConsentIds = new HashSet<>();

                    if(result.resources().isEmpty()) {
                        log.info("No cookie consent artifacts found for tenant: {}", tenantId);
                        resources = List.of(createResource(Constants.TENANT_ID, tenantId, tenantId));
                    }
                    else {
                        log.info("Tenant: {} processed {} cookie consent artifacts for retention", tenantId, processed);
                        resources = result.resources().stream()
                            .map(doc -> createResource("COOKIE_CONSENT_ID", doc.getString(Constants.COOKIE_CONSENT_ID), doc.getString("businessId")))  // change as per actual fields
                            .toList();
                    
                        cookieConsentIds = result.resources().stream()
                            .map(doc -> doc.getString(Constants.COOKIE_CONSENT_ID))   // change as per actual field
                            .collect(Collectors.toSet());
                    }

                    log.info("Saving cookieConsentRetention stats for tenant: {} | processed={} | duration={}ms",
                            tenantId, processed, duration);

                    statsRepository.saveTenantSummary(tenantId, SchedularStats.builder()
                            .runId(runId.toString())
                            .jobName(JobName.COOKIE_CONSENT_RETENTION_JOB)
                            .group(Group.CONSENT) // assuming Group.CONSENT is appropriate
                            .action(Action.COOKIE_CONSENT_RETENTION)
                            .summaryType(SummaryType.TENANT_SUMMARY)
                            .status(SchedularStatus.SUCCESS)
                            .resources(resources)
                            .totalAffected(processed)
                            .timestamp(LocalDateTime.now())
                            .durationMillis(duration)
                            .details(Map.of("batchSize", batchSize))
                            .build());
                    DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");

                    ZonedDateTime ist = result.cutoff()        // LocalDateTime
                            .atZone(ZoneId.of("UTC"))
                            .withZoneSameInstant(ZoneId.of("Asia/Kolkata"));

                    Map<String, Object> payload = new HashMap<>();
                    payload.put("retentionPeriodValue", result.retentionWindow().value());
                    payload.put("retentionPeriodUnit", result.retentionWindow().unit());
                    payload.put("expiryDate", ist.format(fmt));
                    payload.put("expiredRecordsCount", processed);
                    payload.put("processedIds", cookieConsentIds);

                    log.info("Finding DPO configurations for tenant: {}", tenantId);

                    CustomerIdentifiers customerIdentifiers = findDpoConfigurationsForTenant(tenantId);

                    log.info("Initiating retention notification for tenant: {} | payload={}", tenantId, payload);

                    notificationManager.initiateRetentionNotification(
                            NotificationEvent.RETENTION_COOKIE_CONSENT_EXPIRED,
                            tenantId,
                            customerIdentifiers,
                            payload,
                            LANGUAGE.ENGLISH,
                            runId.toString()
                    );

                    // Log audit event for cookie consent retention
                    logCookieConsentRetentionAudit(runId, cookieConsentIds, ActionType.NOTIFICATION_SENT);

                } catch (ExecutionException | InterruptedException e) {
                    long duration = Duration.between(start, Instant.now()).toMillis();
                    statsRepository.saveTenantSummary(tenantId, SchedularStats.builder()
                            .runId(runId.toString())
                            .jobName(JobName.COOKIE_CONSENT_RETENTION_JOB)
                            .group(Group.CONSENT)   // assuming Group.CONSENT is appropriate
                            .action(Action.COOKIE_CONSENT_RETENTION)
                            .summaryType(SummaryType.TENANT_SUMMARY)
                            .status(SchedularStatus.FAILED)
                            .errorCount(1)
                            .lastError(e.getMessage())
                            .timestamp(LocalDateTime.now())
                            .durationMillis(duration)
                            .details(Map.of("batchSize", batchSize))
                            .build());
                    log.error("Tenant {} failed CookieConsentRetention: {}", tenantId, e.getMessage(), e);
                    if (e instanceof InterruptedException) Thread.currentThread().interrupt();
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("CookieConsentRetentionService interrupted", e);
        }

        log.info("CookieConsentRetentionService completed | runId={} | processed={}", runId, totalProcessed);
        return totalProcessed;
    }

    private TenantResult processTenantCookieRetention(String tenantId, int batchSize) {
        ThreadContext.put(Constants.TENANT_ID_HEADER, tenantId);
        log.info("Processing cookie consent retention for tenant: {}", tenantId);
        try {
            MongoTemplate mongoTemplate = tenantMongoTemplateProvider.getMongoTemplate(tenantId);

            // Load retention window (value + unit) from tenant config
            RetentionWindow window = loadTenantRetentionWindow(mongoTemplate);

            LocalDateTime cutoff = computeCutoff(LocalDateTime.now(), window.unit(), window.value());

            int processed = 0;
            int page = 0;
            boolean hasMore = true;
            Set<Document> processedDocs = new HashSet<>();

            while (hasMore) {
                Pageable pageable = PageRequest.of(page, Math.max(1, batchSize));
                Criteria criteria = Criteria.where("updatedAt").lte(cutoff); // change field if needed
                Query query = new Query(criteria).with(pageable);

                List<Document> docs = mongoTemplate.find(query, Document.class, "cookie_consents"); // change collection name if needed
                docs = docs.stream()
                    .filter(doc -> !notificationEventExists(doc, mongoTemplate))
                    .toList();
                if (docs == null || docs.isEmpty()) {
                    break;
                }

                processed += docs.size();
                processedDocs.addAll(docs);

                if (docs.size() < batchSize) hasMore = false;
                else page++;
            }

            log.info("Tenant {} cookie consent retention complete. Processed: {}", tenantId, processed);
            return new TenantResult(processed, processedDocs, window, cutoff);

        } catch (Exception e) {
            log.error("Error processing cookie consent retention for tenant {}: {}", tenantId, e.getMessage(), e);
            return new TenantResult(0, Set.of(), new RetentionWindow(defaultRetentionDays, "DAYS"), LocalDateTime.now());
        } finally {
            ThreadContext.clearAll();
        }
    }

    private boolean notificationEventExists(Document doc, MongoTemplate mongoTemplate) {

        return mongoTemplate.exists(
                Query.query(
                    Criteria.where("event_type").is("RETENTION_COOKIE_CONSENT_EXPIRED")
                            .and("event_payload.processedIds").is(doc.getString(Constants.COOKIE_CONSENT_ID)) //add conditions as/when required
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
            Query q = new Query(Criteria.where("retentions.cookie_consent_artifact_retention").exists(true));
            Document cfg = mongoTemplate.findOne(q, Document.class, Constants.RETENTION_CONFIG);

            if (cfg != null) {
                Document retentions = (Document) cfg.get("retentions");
                if (retentions != null) {
                    String key = "cookie_consent_artifact_retention";
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

        // Priority: mobile -> email
        if (mobile != null && !mobile.isEmpty()) {
            return CustomerIdentifiers.builder()
                    .type(IdentityType.MOBILE)
                    .value(mobile)
                    .build();
        }

        if (email != null && !email.isEmpty()) {
            return CustomerIdentifiers.builder()
                    .type(IdentityType.EMAIL)
                    .value(email)
                    .build();
        }

        // should never happen (your data guarantees one will be present)
        return null;
    }

    private record RetentionWindow(int value, String unit) {}
    private record TenantResult(int processed, Set<Document> resources, RetentionWindow retentionWindow, LocalDateTime cutoff) {}
    
    /**
     * Modular function to log cookie consent retention audit events
     * Can be used in both create and update cookie consent retention flows
     *
     * @param cookieConsentIds The set of cookie consent Ids affected
     * @param actionType The action type (DELETE) //NOTIFICATION_SENT for now
     */
    public void logCookieConsentRetentionAudit(UUID runId, Set<String> cookieConsentIds, ActionType actionType) {
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
            
            extra.put(Constants.DATA, cookieConsentIds);

            AuditRequest auditRequest = AuditRequest.builder()
                    .actor(actor)
                    .businessId(tenantId)
                    .group(String.valueOf(Group.CONSENT))
                    .component(AuditComponent.COOKIE_CONSENT_RETENTION)
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