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
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * Service that archives/moves grievances older than configured escalation/retention from "grievances"
 * to "grievances_archives".
 * - Iterates active tenants (parallelized)
 * - For each batch: finds grievances matching retention criteria, inserts them into archive collection,
 *   and removes them from the original collection.
 * - Sends per-tenant notification summary and writes stats
 */
@Service
@Slf4j
public class GrievanceRetentionService {

    private final TenantRegistryRepository tenantRegistryRepository;
    private final TenantMongoTemplateProvider tenantMongoTemplateProvider;
    private final NotificationManager notificationManager;
    private final SchedularStatsRepository statsRepository;
    private ExecutorService executorService;
    private final AuditManager auditManager;

    @Value("${schedular.job.grievance-retention.thread-pool-size:10}")
    private int threadPoolSize;

    @Value("${grievance.default-retention-days:2}")
    private int defaultRetentionDays;

    @Autowired
    public GrievanceRetentionService(TenantRegistryRepository tenantRegistryRepository,
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
        log.info("Initialized GrievanceRetentionService with thread pool size: {}", threadPoolSize);
    }

    @PreDestroy
    public void shutdown() {
        if (executorService != null) {
            log.info("Shutting down GrievanceRetentionService executor");
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

    public int processGrievanceRetention(int batchSize) {
        log.info("Starting GrievanceRetentionService - batchSize: {}", batchSize);

        UUID runId = UUID.randomUUID();
        Instant start = Instant.now();
        int totalProcessed = 0;

        List<TenantRegistry> activeTenants = tenantRegistryRepository.findByStatus(Status.ACTIVE);
        if (activeTenants == null || activeTenants.isEmpty()) {
            log.warn("No active tenants found for GrievanceRetentionService");
            return 0;
        }

        List<Callable<TenantResult>> tasks = activeTenants.stream()
                .map(t -> (Callable<TenantResult>) () -> processTenantGrievanceRetention(t.getTenantId(), batchSize))
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
                    Set<String> grievanceIds = new HashSet<>();

                    if(result.resources().isEmpty()) {
                        log.info("No grievances found for retention in tenant: {}", tenantId);
                        resources = List.of(createResource(Constants.TENANT_ID, tenantId, tenantId));
                    }
                    else {
                        log.info("Tenant: {} processed {} grievances for retention", tenantId, processed);
                        resources = result.resources().stream()
                            .map(doc -> createResource("GRIEVANCE_ID", doc.getString(Constants.GRIEVANCE_ID_1), doc.getString("businessId")))  // change as per actual fields
                            .toList();
                    
                        grievanceIds = result.resources().stream()
                            .map(doc -> doc.getString(Constants.GRIEVANCE_ID_1))
                            .collect(Collectors.toSet());
                    }
                    
                    log.info("Saving GrievanceRetention stats for tenant: {} | processed={} | duration={}ms",
                            tenantId, processed, duration);

                    statsRepository.saveTenantSummary(tenantId, SchedularStats.builder()
                            .runId(runId.toString())
                            .jobName(JobName.GRIEVANCE_RETENTION_JOB)
                            .group(Group.GRIEVANCE)
                            .action(Action.GRIEVANCE_RETENTION)
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

                    log.info("Finding DPO configurations for tenant: {}", tenantId);

                    CustomerIdentifiers customerIdentifiers = findDpoConfigurationsForTenant(tenantId);

                    log.info("Initiating retention notification for tenant: {} | payload={}", tenantId, payload);

                    notificationManager.initiateRetentionNotification(
                            NotificationEvent.RETENTION_GRIEVANCE_EXPIRED,
                            tenantId,
                            customerIdentifiers,
                            payload,
                            LANGUAGE.ENGLISH,
                            runId.toString()
                    );

                    // Log audit event for grievance retention
                    logGrievanceRetentionAudit(runId, grievanceIds, ActionType.ARCHIVED);

                } catch (ExecutionException | InterruptedException e) {
                    long duration = Duration.between(start, Instant.now()).toMillis();
                    statsRepository.saveTenantSummary(tenantId, SchedularStats.builder()
                            .runId(runId.toString())
                            .jobName(JobName.GRIEVANCE_RETENTION_JOB)
                            .group(Group.GRIEVANCE)
                            .action(Action.GRIEVANCE_RETENTION)
                            .summaryType(SummaryType.TENANT_SUMMARY)
                            .status(SchedularStatus.FAILED)
                            .errorCount(1)
                            .lastError(e.getMessage())
                            .timestamp(LocalDateTime.now())
                            .durationMillis(duration)
                            .details(Map.of("batchSize", batchSize))
                            .build());
                    log.error("Tenant {} failed GrievanceRetention: {}", tenantId, e.getMessage(), e);
                    if (e instanceof InterruptedException) Thread.currentThread().interrupt();
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("GrievanceRetentionService interrupted", e);
        }

        log.info("GrievanceRetentionService completed | runId={} | processed={}", runId, totalProcessed);
        return totalProcessed;
    }

    /**
     * Moves grievances older than retention cutoff to grievances_archives.
     */
    private TenantResult processTenantGrievanceRetention(String tenantId, int batchSize) {
        ThreadContext.put(Constants.TENANT_ID_HEADER, tenantId);
        log.info("Processing grievance retention for tenant: {}", tenantId);
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
                // Consider grievances resolved/closed before cutoff
                Criteria criteria = Criteria.where("status").is(GrievanceStatus.RESOLVED).and("updatedAt").lte(cutoff);
                Query query = new Query(criteria).with(pageable);

                List<Document> grievances = mongoTemplate.find(query, Document.class, "grievances");
                if (grievances == null || grievances.isEmpty()) {
                    break;
                }

                // Insert into archive collection
                List<Document> toArchive = grievances.stream().map(d -> {
                    // optional: add archival metadata
                    Document copy = new Document(d);
                    copy.put("archivedAt", LocalDateTime.now().toString());
                    return copy;
                }).collect(Collectors.toList());

                mongoTemplate.insert(toArchive, "grievances_archives");

                // Remove originals by _id
                List<Object> objectIds = grievances.stream().map(d -> d.getObjectId("_id")).collect(Collectors.toList());
                Query deleteQuery = new Query(Criteria.where("_id").in(objectIds));
                mongoTemplate.remove(deleteQuery, "grievances");

                processed += grievances.size();
                processedDocs.addAll(grievances);

                if (grievances.size() < batchSize) hasMore = false;
                else page++;
            }

            log.info("Tenant {} grievance retention complete. Moved: {}", tenantId, processed);
            return new TenantResult(processed, processedDocs, window, cutoff);

        } catch (Exception e) {
            log.error("Error processing grievance retention for tenant {}: {}", tenantId, e.getMessage(), e);
            return new TenantResult(0, Set.of(), new RetentionWindow(defaultRetentionDays, "DAYS"), LocalDateTime.now());
        } finally {
            ThreadContext.clearAll();
        }
    }

    /**
     * Attempts to read a retention window object { value, unit } from retention configuration documents.
     * falls back to defaultRetentionDays in DAYS.
     */
    private RetentionWindow loadTenantRetentionWindow(MongoTemplate mongoTemplate) {
        try {
            Query q = new Query(Criteria.where("retentions.grievance_retention").exists(true));
            Document cfg = mongoTemplate.findOne(q, Document.class, Constants.RETENTION_CONFIG);

            if (cfg != null) {
                Document retentions = (Document) cfg.get("retentions");
                if (retentions != null) {
                    String key = "grievance_retention";
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
     * Modular function to log grievance retention audit events
     * Can be used in both create and update grievance retention flows
     *
     * @param grievanceIds The grievance Ids affected
     * @param actionType The action type (ARCHIVED)
     */
    public void logGrievanceRetentionAudit(UUID runId, Set<String> grievanceIds, ActionType actionType) {
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
            
            extra.put(Constants.DATA, grievanceIds);

            AuditRequest auditRequest = AuditRequest.builder()
                    .actor(actor)
                    .businessId(tenantId)
                    .group(String.valueOf(Group.GRIEVANCE))
                    .component(AuditComponent.GRIEVANCE_RETENTION)
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