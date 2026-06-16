package com.jio.digigov.grievance.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.jio.digigov.grievance.client.VaultManager;
import com.jio.digigov.grievance.client.response.SignResponse;
import com.jio.digigov.grievance.config.LocalDateTypeAdapter;
import com.jio.digigov.grievance.dto.DocumentMeta;
import com.jio.digigov.grievance.dto.SupportingDoc;
import com.jio.digigov.grievance.dto.request.*;
import com.jio.digigov.grievance.dto.response.GrievanceListResponse;
import com.jio.digigov.grievance.dto.response.GrievanceResponse;
import com.jio.digigov.grievance.dto.response.PagedResponse;
import com.jio.digigov.grievance.entity.Grievance;
import com.jio.digigov.grievance.entity.GrievanceTemplate;
import com.jio.digigov.grievance.enumeration.GrievanceStatus;
import com.jio.digigov.grievance.enumeration.GrievanceUpdateStatus;
import com.jio.digigov.grievance.exception.*;
import com.jio.digigov.grievance.integration.audit.AuditEventService;
import com.jio.digigov.grievance.integration.notification.NotificationEventService;
import com.jio.digigov.grievance.mapper.GrievanceMapper;
import com.jio.digigov.grievance.service.DocumentService;
import com.jio.digigov.grievance.service.GrievanceService;
import com.jio.digigov.grievance.config.MultiTenantMongoConfig;
import com.jio.digigov.grievance.util.GrievanceQueryUtils;
import com.jio.digigov.grievance.util.TenantContextHolder;
import com.jio.digigov.grievance.util.TokenUtility;
import com.nimbusds.jose.shaded.gson.Gson;
import com.nimbusds.jose.shaded.gson.GsonBuilder;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Stream;

@Slf4j
@Service
@RequiredArgsConstructor
public class GrievanceServiceImpl implements GrievanceService {

    private final MultiTenantMongoConfig mongoConfig;
    private final ObjectMapper objectMapper;
    private final VaultManager vaultManager;

    @Autowired
    private DocumentService documentService;

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    TokenUtility tokenUtility;

    @Autowired
    private AuditEventService auditEventService;

    @Autowired
    private NotificationEventService notificationEventService;


    // -------------------- CREATE --------------------
    @Override
    public GrievanceResponse create(GrievanceCreateRequest req, String tenantId, String businessId,
                                    String grievanceTemplateId, String transactionId,
                                    HttpServletRequest servletRequest) {
        log.info("Creating grievance for tenant={}, business={}, templateId={}", tenantId, businessId, grievanceTemplateId);

        validateBusinessId(businessId);
        TenantContextHolder.setTenant(tenantId);
        TenantContextHolder.setBusinessId(businessId);

        try {
            MongoTemplate mongoTemplate = mongoConfig.getMongoTemplateForTenant(tenantId);

            // Fetch grievance template
            Query templateQuery = new Query(
                    Criteria.where("grievanceTemplateId").is(grievanceTemplateId)
                            .and("businessId").is(businessId)
                            .and("status").is("PUBLISHED")
            );
            GrievanceTemplate template = mongoTemplate.findOne(templateQuery, GrievanceTemplate.class);
            if (template == null) {
                throw new EntityNotFoundException("No Grievance Template found with ID: " + grievanceTemplateId + " for businessId: " + businessId);
            }

            // Validate request against template
            validateRequestAgainstTemplate(req, template, mongoTemplate);

            // Save supporting documents
            List<DocumentMeta> documentMetas = saveDocuments(req.getSupportingDocs(), businessId, tenantId);

            // Map request → entity
            Grievance grievance = GrievanceMapper.toEntity(req, businessId, documentMetas);
            grievance.setGrievanceTemplateId(grievanceTemplateId);

            // Convert grievance to Map (for Vault sign API)
            ObjectMapper mapper = new ObjectMapper();
            mapper.registerModule(new JavaTimeModule());
            Map<String, Object> grievanceMap = mapper.convertValue(grievance, Map.class);

            log.info("Grievance mapped to entity for tenant={} businessId={}", tenantId, businessId);
            log.info("Grievance mapped to entity for grievanceMap={}", grievanceMap);

            String grievanceJwtToken = generateGrievanceJwtToken(tenantId, businessId, grievance);

            grievance.setGrievanceJwtToken(grievanceJwtToken);

            // Save grievance
            Grievance saved = mongoTemplate.save(grievance);
            log.info("Saved grievance {} in tenant_db_{}", saved.getGrievanceId(), tenantId);

            // Trigger Notification and Audit
            notificationEventService.triggerNotification(saved, tenantId, businessId, saved.getGrievanceId());
            auditEventService.triggerAuditEvent(saved, tenantId, businessId, transactionId, servletRequest);

            return GrievanceMapper.toResponse(saved);

        } catch (EntityNotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to create grievance | tenant={} | business={} | error={}", tenantId, businessId, e.getMessage(), e);
            throw new BusinessException("GRIEVANCE_CREATION_FAILED", e.getMessage());
        } finally {
            TenantContextHolder.clear();
        }
    }

    /**
     * Generate JWT token for grievance using Vault sign API.
     * Sets the grievanceJsonString on the grievance object.
     * Throws exception if Vault sign API fails.
     *
     * @param tenantId   The tenant identifier
     * @param businessId The business identifier
     * @param grievance  The grievance entity to generate token for
     * @return JWT token string
     */
    private String generateGrievanceJwtToken(String tenantId, String businessId, Grievance grievance) {
        try {
            if (tenantId == null || businessId == null) {
                throw new IllegalArgumentException("TenantId and BusinessId must not be null");
            }

            Map<String, Object> grievanceMap = objectMapper.convertValue(grievance, Map.class);
            grievanceMap.remove("id");
            grievanceMap.remove("createdAt");
            grievanceMap.remove("updatedAt");
            grievanceMap.remove("grievanceJwtToken");

            Gson gson = new GsonBuilder()
                    .registerTypeAdapter(LocalDateTime.class, new LocalDateTypeAdapter())
                    .create();

            String grievanceJsonString = gson.toJson(grievanceMap).replaceAll("\\s+", "");

            // Build JWT payload
            Map<String, Object> payload = new HashMap<>();
            payload.put("sub", grievanceJsonString);
            payload.put("iss", "GRIEVANCE");
            payload.put("iat", grievance.getCreatedAt().atZone(ZoneId.systemDefault()).toEpochSecond());
            payload.put("exp", grievance.getCreatedAt().plusDays(7).atZone(ZoneId.systemDefault()).toEpochSecond());

            log.info("Calling Vault sign API for grievanceId={} with payload", grievance.getGrievanceId());

            SignResponse signResponse = this.vaultManager.sign(tenantId, businessId, payload);
            log.info("Grievance sign response={}", signResponse);
            if (signResponse == null || signResponse.getJwt() == null || signResponse.getJwt().isEmpty()) {
                log.error("Vault sign API returned null/empty JWT for grievanceId={}", grievance.getGrievanceId());
                throw new IllegalStateException("Vault sign API failed to generate JWT token");
            }

            return signResponse.getJwt();

        } catch (Exception e) {
            log.error("Error generating JWT for grievanceId={}, error={}", grievance.getGrievanceId(), e.getMessage(), e);
            throw new RuntimeException("Failed to generate grievance JWT token: " + e.getMessage(), e);
        }
    }

    private void validateRequestAgainstTemplate(GrievanceCreateRequest req, GrievanceTemplate template, MongoTemplate mongoTemplate) {
        // 1️ Validate Grievance Type
        boolean grievanceTypeExists = template.getMultilingual().getGrievanceInformation().stream()
                .anyMatch(info -> info.getGrievanceType().equalsIgnoreCase(req.getGrievanceType()));
        if (!grievanceTypeExists) {
            throw new BusinessException(
                    "INVALID_GRIEVANCE_TYPE",
                    String.format("Grievance type '%s' is not allowed in this template.", req.getGrievanceType())
            );
        }

        // 2 Validate Grievance Type Details
        boolean grievanceTypeDetailsExists = template.getMultilingual().getGrievanceInformation().stream()
                .anyMatch(info -> info.getGrievanceItems().stream()
                        .anyMatch(item -> item.equalsIgnoreCase(req.getGrievanceDetail())));
        if (!grievanceTypeDetailsExists) {
            throw new BusinessException(
                    "INVALID_GRIEVANCE_TYPE_DETAIL",
                    String.format("Grievance type detail '%s' is not allowed in this template.", req.getGrievanceDetail())
            );
        }

        // 3️ Validate User Type
        boolean userTypeExists = template.getMultilingual().getUserInformation().stream()
                .anyMatch(info -> info.getUserType().contains(req.getUserType()));
        if (!userTypeExists) {
            throw new BusinessException(
                    "INVALID_USER_TYPE",
                    String.format("User type '%s' is not allowed in this template.", req.getUserType())
            );
        }

        // 4️ Validate User Details keys against template userItems
        ObjectMapper mapper = new ObjectMapper();
        Map<String, Object> userDetailsMap = mapper.convertValue(req.getUserDetails(), Map.class); // dynamic map

        List<String> allowedKeys = template.getMultilingual().getUserInformation().stream()
                .flatMap(info -> info.getUserItems().stream())
                .map(String::toLowerCase) // normalize for comparison
                .toList();

        for (String key : userDetailsMap.keySet()) {
            log.info("Validating userDetails key: {}", key);
            if (!allowedKeys.contains(key.toLowerCase())) {
                throw new BusinessException("INVALID_USER_FIELD",
                        "Field '" + key + "' is not allowed as per grievance template.");
            }
        }

        // 5 Validate description if required by template
        if (Boolean.TRUE.equals(template.getMultilingual().getDescriptionCheck())
                && (req.getGrievanceDescription() == null || req.getGrievanceDescription().isEmpty())) {
            throw new BusinessException("DESCRIPTION_REQUIRED", "Grievance description is mandatory as per the template.");
        }

        // 6 Validate supportingDocs if required by template
        if (Boolean.TRUE.equals(template.getMultilingual().getUploadFiles())
                && (req.getSupportingDocs() == null || req.getSupportingDocs().isEmpty())) {
            throw new BusinessException("DOCUMENT_REQUIRED", "Grievance document is mandatory as per the template.");
        }
    }

    // -------------------- SEARCH --------------------
    @Override
    public PagedResponse<GrievanceListResponse> search(Map<String, String> params,
                                                       String tenantId,
                                                       String businessId,
                                                       Integer page,
                                                       Integer size) {

        GrievanceQueryUtils.validateBusinessId(businessId);

        TenantContextHolder.setTenant(tenantId);
        TenantContextHolder.setBusinessId(businessId);

        try {
            MongoTemplate mongo = mongoConfig.getMongoTemplateForTenant(tenantId);

            // 1. Always enforce businessId
            params.put("businessId", businessId);

            // 2. Build FULL dynamic + nested query
            Query query = GrievanceQueryUtils.buildQuery(params);

            long total = mongo.count(query, Grievance.class);

            List<Grievance> result;

            // 3. Handle pagination
            if (page != null && size != null && size > 0) {
                int pageIndex = Math.max(0, page - 1);

                Pageable pageable = PageRequest.of(pageIndex, size);
                query.with(pageable);

                result = mongo.find(query, Grievance.class);

                return PagedResponse.<GrievanceListResponse>builder()
                        .total(total)
                        .page(page)
                        .size(size)
                        .data(result.stream().map(GrievanceMapper::toListResponse).toList())
                        .build();
            }

            // 4. No pagination → return all
            result = mongo.find(query, Grievance.class);

            return PagedResponse.<GrievanceListResponse>builder()
                    .total(result.size())
                    .page(1)
                    .size(result.size())
                    .data(result.stream().map(GrievanceMapper::toListResponse).toList())
                    .build();

        } finally {
            TenantContextHolder.clear();
        }
    }

    // -------------------- LIST --------------------
    @Override
    public PagedResponse<GrievanceListResponse> list(Integer page, Integer size, String tenantId, String businessId) {
        log.info("List grievances for tenant: {}, business: {}", tenantId, businessId);

        validateBusinessId(businessId);

        TenantContextHolder.setTenant(tenantId);
        TenantContextHolder.setBusinessId(businessId);

        try {
            MongoTemplate tenantMongoTemplate = mongoConfig.getMongoTemplateForTenant(tenantId);
            Query query = new Query().addCriteria(Criteria.where("businessId").is(businessId));

            long total = tenantMongoTemplate.count(query, Grievance.class);
            List<Grievance> list;

            //  If page & size are provided, apply pagination
            if (page != null && size != null && size > 0) {
                int pageIndex = Math.max(0, page - 1);
                Pageable pageable = PageRequest.of(pageIndex, size);
                query.with(pageable);
                list = tenantMongoTemplate.find(query, Grievance.class);

                log.info("Paged grievance list: page={} size={} total={}", page, size, total);

                return PagedResponse.<GrievanceListResponse>builder()
                        .total(total)
                        .page(page)
                        .size(size)
                        .data(list.stream().map(GrievanceMapper::toListResponse).toList())
                        .build();
            } else {
                //  No pagination → return all results
                list = tenantMongoTemplate.find(query, Grievance.class);
                log.info("Full grievance list fetched (no pagination), total={}", list.size());

                return PagedResponse.<GrievanceListResponse>builder()
                        .total(list.size())
                        .page(1)
                        .size(list.size())
                        .data(list.stream().map(GrievanceMapper::toListResponse).toList())
                        .build();
            }

        } finally {
            TenantContextHolder.clear();
        }
    }

    // -------------------- GET BY ID --------------------
    @Override
    public Optional<Grievance> getById(String grievanceId, String tenantId, String businessId) {
        log.info("Get grievance by id={} for tenant: {}, business: {}", grievanceId, tenantId, businessId);

        validateBusinessId(businessId);

        TenantContextHolder.setTenant(tenantId);
        TenantContextHolder.setBusinessId(businessId);

        try {
            MongoTemplate tenantMongoTemplate = mongoConfig.getMongoTemplateForTenant(tenantId);

            Query q = new Query().addCriteria(
                    Criteria.where("grievanceId").is(grievanceId)
                            .and("businessId").is(businessId)
            );

            Grievance found = tenantMongoTemplate.findOne(q, Grievance.class);
            return Optional.ofNullable(found);
        } finally {
            TenantContextHolder.clear();
        }
    }

    @Override
    public long countByFilters(Map<String, String> params, String tenantId, String businessId) {
        validateBusinessId(businessId);
        TenantContextHolder.setTenant(tenantId);
        TenantContextHolder.setBusinessId(businessId);

        try {
            MongoTemplate tenantMongoTemplate = mongoConfig.getMongoTemplateForTenant(tenantId);

            // Base criteria
            Criteria baseCriteria = Criteria.where("businessId").is(businessId);

            // Dynamic criteria
            List<Criteria> dynamicCriteriaList = new ArrayList<>();
            for (Map.Entry<String, String> entry : params.entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue();
                if (value == null || value.isBlank()) continue;
                if ("tenantId".equalsIgnoreCase(key) || "businessId".equalsIgnoreCase(key)) continue;

                // Case-insensitive partial match
                dynamicCriteriaList.add(Criteria.where(key).regex(value, "i"));
            }

            Criteria finalCriteria = dynamicCriteriaList.isEmpty()
                    ? baseCriteria
                    : new Criteria().andOperator(Stream.concat(dynamicCriteriaList.stream(), Stream.of(baseCriteria))
                    .toArray(Criteria[]::new));

            Query query = new Query(finalCriteria);

            return tenantMongoTemplate.count(query, Grievance.class);
        } finally {
            TenantContextHolder.clear();
        }
    }


    @Override
    public Grievance update(String grievanceId, GrievanceUpdateRequest updates, String tenantId,
                            String businessId, String transactionId, HttpServletRequest servletRequest) {
        log.info("Update grievance {} for tenant: {}, business: {}", grievanceId, tenantId, businessId);

        validateBusinessId(businessId);

        TenantContextHolder.setTenant(tenantId);
        TenantContextHolder.setBusinessId(businessId);

        try {
            MongoTemplate tenantMongoTemplate = mongoConfig.getMongoTemplateForTenant(tenantId);

            Query query = new Query().addCriteria(
                    Criteria.where("grievanceId").is(grievanceId)
                            .and("businessId").is(businessId)
            );

            Grievance existing = tenantMongoTemplate.findOne(query, Grievance.class);
            if (existing == null) {
                throw new ResourceNotFoundException("Grievance not found with ID: " + grievanceId);
            }

            // Only allow INPROCESS or RESOLVED through update API
            if (updates.getStatus() != null) {
                GrievanceStatus currentStatus = existing.getStatus();
                GrievanceUpdateStatus newStatus = updates.getStatus();

                if (newStatus != GrievanceUpdateStatus.INPROCESS && newStatus != GrievanceUpdateStatus.RESOLVED) {
                    throw new InvalidRequestException("Invalid status update: only INPROCESS or RESOLVED are allowed via API");
                }

                if (!isValidTransition(currentStatus, newStatus)) {
                    throw new InvalidRequestException(String.format(
                            "Invalid status transition: cannot move from %s to %s",
                            currentStatus, newStatus
                    ));
                }

                // Add history entry before updating
                if (!currentStatus.name().equals(newStatus.name())) {
                    Map<String, Object> historyEntry = new LinkedHashMap<>();
                    historyEntry.put("previousStatus", currentStatus.name());
                    historyEntry.put("newStatus", newStatus.name());
                    historyEntry.put("previousJwtToken", existing.getGrievanceJwtToken());
                    historyEntry.put("updatedAt", LocalDateTime.now());
                    historyEntry.put("remarks", updates.getResolutionRemark() != null
                            ? updates.getResolutionRemark()
                            : "Status updated");

                    existing.getHistory().add(historyEntry);
                    log.info("Added history entry for grievanceId={} from {} → {}", grievanceId, currentStatus, newStatus);
                }

                // Update main status
                existing.setStatus(GrievanceStatus.valueOf(newStatus.name()));
            }

            // Apply remarks and other updates
            GrievanceMapper.applyUpdates(existing, updates);

            String grievanceJwtToken = generateGrievanceJwtToken(tenantId, businessId, existing);

            existing.setGrievanceJwtToken(grievanceJwtToken);

            // Save updated grievance
            Grievance saved = tenantMongoTemplate.save(existing);
            log.info("Grievance updated successfully | grievanceId={}", grievanceId);

            // Trigger Notification API
            notificationEventService.triggerNotification(saved, tenantId, businessId, saved.getGrievanceId());

            // Trigger Audit Event
            auditEventService.triggerAuditEvent(saved, tenantId, businessId, transactionId, servletRequest);

            return saved;

        } catch (InvalidRequestException | ResourceNotFoundException e) {
            // Allow controller to map these correctly (400 or 404)
            throw e;

        } catch (Exception e) {
            // Catch unexpected exceptions and wrap as BusinessException
            log.error("Failed to update grievance | grievanceId={} | error={}", grievanceId, e.getMessage(), e);
            throw new BusinessException("GRV-500", "Internal Server Error: " + e.getMessage(), e);
        } finally {
            TenantContextHolder.clear();
        }
    }

    /**
     * Validates allowed status transitions through the Update API.
     * Scheduler-based transitions (like escalations) are handled elsewhere.
     */
    private boolean isValidTransition(GrievanceStatus current, GrievanceUpdateStatus next) {

        // Map current DB status → allowed next update API status
        Map<GrievanceStatus, List<GrievanceUpdateStatus>> allowedTransitions = new HashMap<>();
        allowedTransitions.put(GrievanceStatus.NEW, Collections.singletonList(GrievanceUpdateStatus.INPROCESS));
        allowedTransitions.put(GrievanceStatus.L1_ESCALATED, Collections.singletonList(GrievanceUpdateStatus.INPROCESS));
        allowedTransitions.put(GrievanceStatus.INPROCESS, Collections.singletonList(GrievanceUpdateStatus.RESOLVED));
        allowedTransitions.put(GrievanceStatus.L2_ESCALATED, Collections.singletonList(GrievanceUpdateStatus.RESOLVED));
        allowedTransitions.put(GrievanceStatus.RESOLVED, Collections.emptyList()); // no further update

        List<GrievanceUpdateStatus> validNext = allowedTransitions.getOrDefault(current, Collections.emptyList());
        return validNext.contains(next);
    }

    // -------------------- DELETE --------------------
    @Override
    public boolean delete(String grievanceId, String tenantId, String businessId) {
        log.info("Delete grievance {} for tenant: {}, business: {}", grievanceId, tenantId, businessId);

        validateBusinessId(businessId);

        TenantContextHolder.setTenant(tenantId);
        TenantContextHolder.setBusinessId(businessId);

        try {
            MongoTemplate tenantMongoTemplate = mongoConfig.getMongoTemplateForTenant(tenantId);

            Query q = new Query().addCriteria(
                    Criteria.where("grievanceId").is(grievanceId)
                            .and("businessId").is(businessId)
            );

            Grievance existing = tenantMongoTemplate.findOne(q, Grievance.class);
            if (existing == null) {
                return false;
            }

            tenantMongoTemplate.remove(q, Grievance.class);
            log.info("Deleted grievance {} from tenant_db_{}", grievanceId, tenantId);
            return true;
        } finally {
            TenantContextHolder.clear();
        }
    }

    // -------------------- HELPERS --------------------
    private void validateBusinessId(String businessId) {
        if (businessId == null || businessId.trim().isEmpty()) {
            throw new ValidationException("X-Business-Id header is required for grievance operations");
        }
    }

    private List<DocumentMeta> saveDocuments(List<SupportingDoc> supportingDocs, String businessId, String tenantId) {
        List<DocumentMeta> documentMetas = new ArrayList<>();
        if (supportingDocs != null) {
            for (SupportingDoc docReq : supportingDocs) {
                // Detect content type from extension
                String contentType = "application/octet-stream";
                String fileName = docReq.getDocName();
                if (fileName != null && fileName.contains(".")) {
                    String ext = fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase();
                    contentType = switch (ext) {
                        case "png" -> "image/png";
                        case "jpg", "jpeg" -> "image/jpeg";
                        case "gif" -> "image/gif";
                        case "pdf" -> "application/pdf";
                        case "doc" -> "application/msword";
                        case "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
                        case "xls" -> "application/vnd.ms-excel";
                        case "xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
                        case "txt" -> "text/plain";
                        default -> "application/octet-stream";
                    };
                }

                DocumentMeta meta = documentService.saveBase64Document(
                        docReq.getDoc(),
                        fileName,
                        contentType,
                        businessId,
                        tenantId,
                        null
                );
                documentMetas.add(meta);
            }
        }
        return documentMetas;
    }

    @Override
    public Grievance updateFeedback(String grievanceId, int feedback, String tenantId, String businessId) {
        log.info("Updating feedback for grievanceId={} | tenantId={} | businessId={} | feedback={}",
                grievanceId, tenantId, businessId, feedback);

        validateBusinessId(businessId);

        TenantContextHolder.setTenant(tenantId);
        TenantContextHolder.setBusinessId(businessId);

        try {
            // 🔹 Get MongoTemplate for the tenant
            MongoTemplate tenantMongoTemplate = mongoConfig.getMongoTemplateForTenant(tenantId);

            // 🔹 Fetch existing grievance
            Query query = new Query(Criteria.where("grievanceId").is(grievanceId)
                    .and("businessId").is(businessId));
            Grievance grievance = tenantMongoTemplate.findOne(query, Grievance.class);

            if (grievance == null) {
                log.warn("No grievance found for grievanceId={} under tenant={} and business={}",
                        grievanceId, tenantId, businessId);
                throw new ResourceNotFoundException("Grievance not found for ID: " + grievanceId);
            }

            // 🔹 Validate feedback range (should be 1–5)
            if (feedback < 1 || feedback > 5) {
                log.warn("Invalid feedback={} for grievanceId={}. Allowed range is 1–5.", feedback, grievanceId);
                throw new ValidationException("Feedback value must be between 1 and 5.");
            }

            //  Update feedback field (initially 0)
            grievance.setFeedback(feedback);
            grievance.setUpdatedAt(LocalDateTime.now());

            //  Save grievance with updated feedback
            Grievance updated = tenantMongoTemplate.save(grievance);
            log.info("Feedback successfully updated | grievanceId={} | feedback={}", grievanceId, feedback);

            return updated;

        } catch (ValidationException | ResourceNotFoundException e) {
            log.error("Feedback update failed for grievanceId={} | reason={}", grievanceId, e.getMessage());
            throw e;

        } catch (Exception e) {
            log.error("Unexpected error while updating feedback for grievanceId={} | error={}", grievanceId, e.getMessage(), e);
            throw new BusinessException("FEEDBACK_UPDATE_FAILED", "An unexpected error occurred while updating feedback.");
        } finally {
            TenantContextHolder.clear();
        }
    }
}
