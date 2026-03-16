package com.jio.schedular.service;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.jio.schedular.client.audit.AuditManager;
import com.jio.schedular.client.notification.NotificationManager;
import com.jio.schedular.constant.Constants;
import com.jio.schedular.dto.*;
import com.jio.schedular.enums.*;
import com.jio.schedular.entity.SchedularStats;
import com.jio.schedular.entity.TenantRegistry;
import com.jio.schedular.multitenancy.TenantMongoTemplateProvider;
import com.jio.schedular.repository.SchedularStatsRepository;
import com.jio.schedular.repository.TenantRegistryRepository;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.ThreadContext;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

import java.security.MessageDigest;
import java.time.*;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.*;

/**
 * Service to escalate grievances:
 *  - NEW -> L1_ESCALATED when createdAt is older than escalationPolicy
 *  - INPROCESS -> L2_ESCALATED when updatedAt is older than escalationPolicy
 *
 * After updating status in tenant Mongo, this service delegates:
 *  - notification via NotificationManager
 *  - audit via AuditManager
 *
 * Uses _id-based iteration to avoid skip-based pagination.
 */
@Service
@Slf4j
public class GrievanceEscalationService {

    private final TenantRegistryRepository tenantRegistryRepository;
    private final TenantMongoTemplateProvider tenantMongoTemplateProvider;
    private final NotificationManager notificationManager;
    private final AuditManager auditManager;
    private final SchedularStatsRepository statsRepository;
    private ExecutorService executorService;
    private final ObjectMapper objectMapper = new ObjectMapper()
        .registerModule(new JavaTimeModule())
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);


    @Value("${schedular.job.grievance-escalation.thread-pool-size:10}")
    private int threadPoolSize;

    @Value("${grievance.default-escalation-days:2}")
    private int defaultEscalationDays;

    @Autowired
    public GrievanceEscalationService(TenantRegistryRepository tenantRegistryRepository,
                                      TenantMongoTemplateProvider tenantMongoTemplateProvider,
                                      NotificationManager notificationManager,
                                      AuditManager auditManager,
                                      SchedularStatsRepository statsRepository) {
        this.tenantRegistryRepository = tenantRegistryRepository;
        this.tenantMongoTemplateProvider = tenantMongoTemplateProvider;
        this.notificationManager = notificationManager;
        this.auditManager = auditManager;
        this.statsRepository = statsRepository;
    }

    @PostConstruct
    public void init() {
        this.executorService = Executors.newFixedThreadPool(Math.max(1, threadPoolSize));
        log.info("Initialized GrievanceEscalationService thread-pool size={}", threadPoolSize);
    }

    @PreDestroy
    public void shutdown() {
        if (executorService != null) {
            log.info("Shutting down GrievanceEscalationService executor");
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

    private record EscalationPolicy(int value, String unit) {}
    private record TenantResult(int escalatedCount, Set<Document> grievancesList) {}

    @Transactional
    public int escalateGrievances(int batchSize) {

        UUID runId = UUID.randomUUID();
        Instant start = Instant.now();

        log.info("[{}] Starting grievance escalation | runId={}", JobName.GRIEVANCE_ESCALATION_JOB, runId);

        int totalEscalated = 0;

        List<TenantRegistry> tenants = tenantRegistryRepository.findByStatus(Status.ACTIVE);
        if (tenants == null || tenants.isEmpty()) {
            log.warn("No active tenants found for grievance escalation");
            return 0;
        }

        for (TenantRegistry tenant : tenants) {
            String tenantId = tenant.getTenantId();
            try {
                TenantResult result = processTenant(tenantId, batchSize);
                int escalatedCount = result.escalatedCount();
                List<Resource> tenantResources;

                if(result.grievancesList().isEmpty()) {
                    log.info("No consent artifacts found for tenant: {}", tenantId);
                    tenantResources = List.of(createResource(Constants.TENANT_ID, tenantId, tenantId));
                }
                else {
                    log.info("Tenant: {} processed {} consent artifacts for retention", tenantId, escalatedCount);
                    tenantResources = result.grievancesList().stream()
                        .map(grievance -> createResource(Constants.GRIEVANCE_ID, grievance.getString(Constants.GRIEVANCE_ID_1), grievance.getString(Constants.BUSINESS_ID)))
                        .toList();
                }

                long duration = Duration.between(start, Instant.now()).toMillis();
                totalEscalated += escalatedCount;

                statsRepository.saveTenantSummary(tenantId, SchedularStats.builder()
                        .runId(runId.toString())
                        .jobName(JobName.GRIEVANCE_ESCALATION_JOB)
                        .action(Action.GRIEVANCE_ESCALATION)
                        .group(Group.GRIEVANCE)
                        .summaryType(SummaryType.TENANT_SUMMARY)
                        .status(SchedularStatus.SUCCESS)
                        .totalAffected(escalatedCount)
                        .timestamp(LocalDateTime.now())
                        .durationMillis(duration)
                        .resources(tenantResources)
                        .details(Map.of(
                                "batchSize", batchSize
                        ))
                        .build());

            } catch (Exception ex) {
                long duration = Duration.between(start, Instant.now()).toMillis();
                statsRepository.saveTenantSummary(tenantId, SchedularStats.builder()
                        .runId(runId.toString())
                        .jobName(JobName.GRIEVANCE_ESCALATION_JOB)
                        .action(Action.GRIEVANCE_ESCALATION)
                        .group(Group.GRIEVANCE)
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

        log.info("[{}] Completed | runId={} | escalated={}", JobName.GRIEVANCE_ESCALATION_JOB, runId, totalEscalated);
        return totalEscalated;
    }

    private TenantResult processTenant(String tenantId, int batchSize) {
        log.info("Start escalation for tenant={}", tenantId);
        try {
            ThreadContext.put(Constants.TENANT_ID_HEADER, tenantId);
            Set<Document> grievancesList = new HashSet<>();

            EscalationPolicy policy = fetchEscalationPolicy(tenantId);
            if (policy == null) policy = new EscalationPolicy(defaultEscalationDays, "DAYS");

            Date cutoffDate = computeCutoffDate(policy.unit(), policy.value());
            if (cutoffDate == null) cutoffDate = computeCutoffDate("DAYS", defaultEscalationDays);
            log.info("Tenant {} cutoff (UTC Date) = {}", tenantId, cutoffDate);

            int escalatedCount = 0;

            // NEW → L1_ESCALATED
            escalatedCount += escalateByCriteria(tenantId, batchSize,
                    Criteria.where(Constants.STATUS).is(String.valueOf(GrievanceStatus.NEW)).and(Constants.CREATED_AT).lte(cutoffDate),
                    GrievanceStatus.NEW, GrievanceStatus.L1_ESCALATED, grievancesList);

            // INPROCESS → L2_ESCALATED
            escalatedCount += escalateByCriteria(tenantId, batchSize,
                    Criteria.where(Constants.STATUS).is(String.valueOf(GrievanceStatus.INPROCESS)).and(Constants.UPDATED_AT).lte(cutoffDate),
                    GrievanceStatus.INPROCESS, GrievanceStatus.L2_ESCALATED, grievancesList);

            log.info("Completed tenant {} escalation. EscalatedCount={} grievancesList={}", tenantId, escalatedCount, grievancesList.size());
            return new TenantResult(escalatedCount, grievancesList);

        } catch (Exception e) {
            log.error("Error processing tenant {} escalation", tenantId, e);
            return new TenantResult(0, Set.of());
        } finally {
            ThreadContext.clearAll();
        }
    }

    private EscalationPolicy fetchEscalationPolicy(String tenantId) {
        try {
            MongoTemplate mongoTemplate = tenantMongoTemplateProvider.getMongoTemplate(tenantId);
            Query q = new Query(Criteria.where(Constants.CONFIGURATION_JSON).exists(true));
            Document cfg = mongoTemplate.findOne(q, Document.class, "grievance_configurations");
            if (cfg == null) return null;

            Object confObj = cfg.get(Constants.CONFIGURATION_JSON);
            if (confObj instanceof Document) {
                Document conf = (Document) confObj;
                Object esc = conf.get("escalationPolicy");
                if (esc instanceof Document) {
                    Document escDoc = (Document) esc;
                    Integer v = null;
                    String u = null;
                    Object vv = escDoc.get("value");
                    if (vv instanceof Number) v = ((Number) vv).intValue();
                    else if (vv instanceof String)
                    {
                        try { 
                            v = Integer.parseInt((String) vv); 
                        } catch (Exception ignore) {}
                    }
                    Object uu = escDoc.get("unit");
                    if (uu != null) u = uu.toString();
                    if (v != null && v > 0 && u != null) return new EscalationPolicy(v, u);
                }
            } else if (confObj instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String,Object> conf = (Map<String,Object>) confObj;
                Object esc = conf.get("escalationPolicy");
                if (esc instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String,Object> escMap = (Map<String,Object>) esc;
                    Integer v = null;
                    String u = null;
                    Object vv = escMap.get("value");
                    if (vv instanceof Number)
                        v = ((Number) vv).intValue();
                    else if (vv instanceof String)
                    {
                        try {
                            v = Integer.parseInt((String) vv); 
                        } catch (Exception ignore) {}
                    }
                    Object uu = escMap.get("unit");
                    if (uu != null) u = uu.toString();
                    if (v != null && v > 0 && u != null)
                        return new EscalationPolicy(v, u);
                }
            }
        } catch (Exception e) {
            log.warn("Failed to fetch escalationPolicy for tenant {}: {}", tenantId, e.getMessage());
        }
        return null;
    }

    private int escalateByCriteria(String tenantId, int batchSize, Criteria criteria, GrievanceStatus currentStatus, GrievanceStatus targetStatus, Set<Document> grievancesList) {
        MongoTemplate mongoTemplate = tenantMongoTemplateProvider.getMongoTemplate(tenantId);
        ObjectId lastId = null;
        int escalated = 0;

        while (true) {
            Query q = new Query(criteria);
            if (lastId != null) q.addCriteria(Criteria.where("_id").gt(lastId));
            q.with(Sort.by(Sort.Direction.ASC, "_id"));
            q.limit(Math.max(1, batchSize));

            List<Document> grievances = mongoTemplate.find(q, Document.class, "grievances");
            if (grievances == null || grievances.isEmpty()) break;

            for (Document g : grievances) {
                try {
                    ObjectId oid = g.getObjectId("_id");
                    String grievanceId = g.getString(Constants.GRIEVANCE_ID_1);
                    String businessId = g.getString(Constants.BUSINESS_ID);
                    if (grievanceId == null) grievanceId = (oid != null) ? oid.toHexString() : null;
                    if (grievanceId == null) continue;

                    boolean ok = updateGrievanceInMongoAndNotify(mongoTemplate, g, currentStatus, targetStatus, tenantId, businessId, grievanceId);
                    if (ok) {
                        escalated++;
                        grievancesList.add(g);
                    }
                    lastId = (oid != null) ? oid : lastId;
                } catch (Exception e) {
                    log.error("Failed to process grievance doc (tenant={}) doc={} error={}", tenantId,
                            g == null ? "null" : g.toJson(), e.getMessage(), e);
                }
            }

            if (grievances.size() < batchSize) break;
        }

        return escalated;
    }

    private boolean updateGrievanceInMongoAndNotify(MongoTemplate mongoTemplate,
                                                    Document doc,
                                                    GrievanceStatus currentStatus,
                                                    GrievanceStatus newStatus,
                                                    String tenantId,
                                                    String businessId,
                                                    String grievanceId) {
        try {
            // update status and updatedAt (UTC)
            doc.put(Constants.STATUS, newStatus);
            doc.put(Constants.UPDATED_AT, Date.from(ZonedDateTime.now(ZoneOffset.UTC).toInstant()));

            // --- Append to resolutionRemarks ---
            List<Document> remarksDocs = doc.getList("resolutionRemarks", Document.class);

            if (remarksDocs == null) {
                remarksDocs = new ArrayList<>();
            }

            // Add new entry
            OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
            StatusTimeline timelineEntry = StatusTimeline.builder()
                    .status(newStatus)
                    .timestamp(now)
                    .remark("Status auto-updated to " + newStatus + " by Scheduler")
                    .updatedAt(now)
                    .build();

            Document timelineDoc = objectMapper.convertValue(timelineEntry, Document.class);

            if (timelineEntry.getTimestamp() != null) {
                timelineDoc.put("timestamp", Date.from(timelineEntry.getTimestamp().toInstant()));
            }
            if (timelineEntry.getUpdatedAt() != null) {
                timelineDoc.put(Constants.UPDATED_AT, Date.from(timelineEntry.getUpdatedAt().toInstant()));
            }

            remarksDocs.add(timelineDoc);

            // Now reassign only once, with the full combined list
            doc.put("resolutionRemarks", remarksDocs);

            // --- Append to history ---
            List<Map> history = doc.getList("history", Map.class);
            if (history == null) {
                history = new ArrayList<>();
            }

            Map<String, Object> historyEntry = new LinkedHashMap<>();
            historyEntry.put("previousStatus", currentStatus);
            historyEntry.put("newStatus", newStatus);
            historyEntry.put(Constants.UPDATED_AT, Date.from(ZonedDateTime.now(ZoneOffset.UTC).toInstant()));
            historyEntry.put("remarks", "Status auto-updated to " + newStatus + " by Scheduler");

            history.add(historyEntry);
            doc.put("history", history);

            log.debug("Added history entry for grievanceId={} from {} → {}", grievanceId, currentStatus, newStatus);

            // persist
            mongoTemplate.save(doc, "grievances");
            log.info("Grievance updated in mongo | grievanceId={} tenant={} status={}", grievanceId, tenantId, newStatus);

            // Prepare payload
            Map<String, Object> eventPayload = objectMapper.convertValue(doc, Map.class);
            eventPayload.put(Constants.GRIEVANCE_ID_1, doc.getString(Constants.GRIEVANCE_ID_1));

            Map<String, Boolean> commConfig = getCommunicationConfig(tenantId);
            boolean allowSms = commConfig.getOrDefault("sms", true);
            boolean allowEmail = commConfig.getOrDefault(Constants.EMAIL, true);

            // Extract both identifiers if available
            List<CustomerIdentifiers> identifiers = extractCustomerIdentifiers(doc);

            // Send notification for each available identifier
            for (CustomerIdentifiers ci : identifiers) {
                boolean shouldSend = switch (ci.getType()) {
                    case MOBILE -> allowSms;
                    case EMAIL -> allowEmail;
                    default -> false;
                };

                if (!shouldSend) {
                    log.info("Skipping notification for grievance={} via {} (disabled in config)", grievanceId, ci.getType());
                    continue;
                }

                try {
                    notificationManager.initiateGrievanceNotification(
                            String.valueOf(newStatus), tenantId, businessId, ci, eventPayload, LANGUAGE.ENGLISH, grievanceId
                    );
                    log.info("Notification sent for grievance={} via {}", grievanceId, ci.getType());
                } catch (Exception ex) {
                    log.error("Notification failed for grievance={} via {}: {}", grievanceId, ci.getType(), ex.getMessage());
                }
            }

            // delegate audit event to AuditManager
            try {
                Map<String, Object> auditDto = new LinkedHashMap<>();

                auditDto.put(Constants.BUSINESS_ID, businessId);
                auditDto.put("actionType", ActionType.UPDATED);
                auditDto.put("group", String.valueOf(Group.GRIEVANCE));
                auditDto.put("component", AuditComponent.GRIEVANCE_ESCALATION);
                auditDto.put("initiator", Constants.SCHEDULAR);
                auditDto.put(Constants.STATUS, "SUCCESS");

                auditDto.put("resource", Map.of(
                        "type", Constants.GRIEVANCE_ID,
                        "id", grievanceId
                ));

                auditDto.put("actor", Map.of(
                        "id", grievanceId,
                        "role", Constants.DATA_PRINCIPAL,
                        "type", Constants.USER
                ));

                auditDto.put("context", Map.of(
                        "txnId", UUID.randomUUID().toString(),
                        "ipAddress", "192.168.1.1"
                ));

                auditDto.put("extra", Map.of(
                        "legalEntityName", "XYZ Pvt Ltd",
                        "triggeredBy", "Grievance Escalation Schedular",
                        "pan", "ABCDE1234F"
                ));

                // Add timestamp
                String timestamp = ZonedDateTime.now(ZoneOffset.UTC)
                        .truncatedTo(ChronoUnit.SECONDS)
                        .toString();
                auditDto.put("timestamp", timestamp);

                // Compute SHA-256 hash (optional but nice to have)
                String hash = computePayloadHash(auditDto);
                auditDto.put("auditHash", hash);

                auditManager.sendAudit(auditDto, tenantId, businessId);
                log.info("AuditManager triggered for grievance={}", grievanceId);

            } catch (Exception ex) {
                log.error("AuditManager failed for grievance {}: {}", grievanceId, ex.getMessage(), ex);
            }
            return true;

        } catch (Exception e) {
            log.error("Failed to update grievance {} in mongo (tenant={}): {}", grievanceId, tenantId, e.getMessage(), e);
            return false;
        }
    }

    private Date computeCutoffDate(String unit, int value) {
        if (unit == null || value <= 0) return null;
        ZonedDateTime nowUtc = ZonedDateTime.now(ZoneOffset.UTC);
        ZonedDateTime cutoff;
        switch (unit.toUpperCase()) {
            case "DAYS": case "DAY": cutoff = nowUtc.minus(value, ChronoUnit.DAYS); break;
            case "WEEKS": case "WEEK": cutoff = nowUtc.minus(value * 7L, ChronoUnit.DAYS); break;
            case "MONTHS": case "MONTH": cutoff = nowUtc.minus(value, ChronoUnit.MONTHS); break;
            case "YEARS": case "YEAR": cutoff = nowUtc.minus(value, ChronoUnit.YEARS); break;
            case "HOURS": case "HOUR": cutoff = nowUtc.minus(value, ChronoUnit.HOURS); break;
            default: return null;
        }
        return Date.from(cutoff.toInstant());
    }

    /**
     * Extracts phone and email from userDetails, returns list of both if available.
     */
    private List<CustomerIdentifiers> extractCustomerIdentifiers(Document doc) {
        List<CustomerIdentifiers> identifiers = new ArrayList<>();
        Map<String, Object> userDetails = doc.get("userDetails", Map.class);

        if (userDetails != null) {
            for (Map.Entry<String, Object> e : userDetails.entrySet()) {
                String key = e.getKey().toLowerCase();
                Object val = e.getValue();
                if (val == null) continue;

                if (key.contains("mobile") || key.contains("phone")) {
                    identifiers.add(CustomerIdentifiers.builder()
                            .type(IdentityType.MOBILE)
                            .value(String.valueOf(val))
                            .build());
                }
                if (key.contains(Constants.EMAIL)) {
                    identifiers.add(CustomerIdentifiers.builder()
                            .type(IdentityType.EMAIL)
                            .value(String.valueOf(val))
                            .build());
                }
            }
        }

        // Fallback: empty list means no identifiers found
        if (identifiers.isEmpty()) {
            identifiers.add(CustomerIdentifiers.builder()
                    .type(null)
                    .value("")
                    .build());
        }

        return identifiers;
    }

    private Map<String, Boolean> getCommunicationConfig(String tenantId) {
        try {
            MongoTemplate mongoTemplate = tenantMongoTemplateProvider.getMongoTemplate(tenantId);
            Query q = new Query(Criteria.where(Constants.CONFIGURATION_JSON).exists(true));
            Document cfg = mongoTemplate.findOne(q, Document.class, "grievance_configurations");
            if (cfg == null) return Map.of("sms", true, Constants.EMAIL, true); // default if missing

            Object confObj = cfg.get(Constants.CONFIGURATION_JSON);
            if (confObj instanceof Document conf) {
                Object comm = conf.get("communicationConfig");
                if (comm instanceof Document commDoc) {
                    Boolean sms = commDoc.getBoolean("sms", true);
                    Boolean email = commDoc.getBoolean(Constants.EMAIL, true);
                    return Map.of("sms", sms, Constants.EMAIL, email);
                }
            }
        } catch (Exception e) {
            log.warn("Failed to fetch communicationConfig for tenant {}: {}", tenantId, e.getMessage());
        }
        return Map.of("sms", true, Constants.EMAIL, true); // fallback default
    }

        /**
     * Computes SHA-256 hash of the payload.
     */
    private String computePayloadHash(Map<String, Object> payload) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            ObjectMapper mapper = new ObjectMapper();
            byte[] encodedhash = digest.digest(mapper.writeValueAsBytes(payload));
            return Base64.getEncoder().encodeToString(encodedhash);
        } catch (Exception e) {
            log.warn("Failed to compute audit payload hash");
            log.debug("Underlying exception when computing payload hash: {}", e.getMessage(), e);
            return "";
        }
    }

    private Resource createResource(String type, String id, String businessId) {
        return Resource.builder()
            .type(type)
            .id(id)
            .businessId(businessId)
            .build();
    }
}