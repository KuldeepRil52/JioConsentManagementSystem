package com.jio.digigov.grievance.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jio.digigov.grievance.config.MultiTenantMongoConfig;
import com.jio.digigov.grievance.dto.request.GrievanceTypeUpdateRequest;
import com.jio.digigov.grievance.entity.GrievanceType;
import com.jio.digigov.grievance.enumeration.ScopeLevel;
import com.jio.digigov.grievance.exception.BusinessException;
import com.jio.digigov.grievance.exception.ValidationException;
import com.jio.digigov.grievance.service.GrievanceTypeService;
import com.jio.digigov.grievance.util.TenantContextHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;

@Slf4j
@Service
@RequiredArgsConstructor
public class GrievanceTypeServiceImpl implements GrievanceTypeService {

    private final MultiTenantMongoConfig mongoConfig;
    private final ObjectMapper objectMapper;

    @Override
    public GrievanceType create(GrievanceType req, String tenantId, String businessId, ScopeLevel scopeLevel) {
        log.info("Creating GrievanceType '{}' for tenant={}, business={}", req.getGrievanceType(), tenantId, businessId);

        validateBusinessId(businessId);

        TenantContextHolder.setTenant(tenantId);
        TenantContextHolder.setBusinessId(businessId);

        try {
            MongoTemplate mongoTemplate = mongoConfig.getMongoTemplateForTenant(tenantId);

            // Check for existing GrievanceType with same name
            Query query = new Query().addCriteria(
                    Criteria.where("grievanceType").is(req.getGrievanceType())
                            .and("businessId").is(businessId)
            );
            GrievanceType existing = mongoTemplate.findOne(query, GrievanceType.class);
            if (existing != null) {
                throw new IllegalArgumentException(
                        "A GrievanceType with name '" + req.getGrievanceType() + "' already exists for this business"
                );
            }

            // Set metadata
            if (req.getCreatedAt() == null) req.setCreatedAt(LocalDateTime.now());
            req.setBusinessId(businessId);
            req.setScope(scopeLevel);
            req.setUpdatedAt(LocalDateTime.now());

            GrievanceType saved = mongoTemplate.save(req);
            log.info("Saved GrievanceType {} in tenant_db_{}", saved.getGrievanceTypeId(), tenantId);
            return saved;
        } catch (IllegalArgumentException e) {
            throw e; // propagate duplicate name error to controller
        } catch (Exception e) {
            log.error("Failed to create GrievanceType for tenant={}, business={}", tenantId, businessId, e);
            throw new BusinessException("GRIEVANCE_TYPE_CREATION_FAILED", e.getMessage());
        } finally {
            TenantContextHolder.clear();
        }
    }

    @Override
    public List<GrievanceType> list(String tenantId, String businessId, ScopeLevel scopeLevel) {
        log.info("Listing GrievanceTypes for tenant={}, business={} with scope={}", tenantId, businessId, scopeLevel);
        validateBusinessId(businessId);

        TenantContextHolder.setTenant(tenantId);
        TenantContextHolder.setBusinessId(businessId);

        try {
            MongoTemplate tenantMongoTemplate = mongoConfig.getMongoTemplateForTenant(tenantId);
            Query query;

            if (scopeLevel == ScopeLevel.TENANT) {
                // TENANT scope → fetch all grievance types across all businesses
                query = new Query();
            } else {
                query = new Query().addCriteria(
                        new Criteria().orOperator(
                                Criteria.where("businessId").is(businessId), // business-level records
                                Criteria.where("scope").is(ScopeLevel.TENANT)     // tenant-level records (scope = TENANT)
                        )
                );
            }

            List<GrievanceType> results = tenantMongoTemplate.find(query, GrievanceType.class);
            log.info("Fetched {} GrievanceTypes for tenant={} scope={}", results.size(), tenantId, scopeLevel);
            return results;

        } finally {
            TenantContextHolder.clear();
        }
    }

    @Override
    public Optional<GrievanceType> getById(String grievanceTypeId, String tenantId, String businessId) {
        log.info("Fetching GrievanceType {} for tenant={}, business={}", grievanceTypeId, tenantId, businessId);
        validateBusinessId(businessId);

        TenantContextHolder.setTenant(tenantId);
        TenantContextHolder.setBusinessId(businessId);

        try {
            MongoTemplate tenantMongoTemplate = mongoConfig.getMongoTemplateForTenant(tenantId);
            Query q = new Query().addCriteria(
                    Criteria.where("grievanceTypeId").is(grievanceTypeId)
                            .and("businessId").is(businessId)
            );
            return Optional.ofNullable(tenantMongoTemplate.findOne(q, GrievanceType.class));
        } finally {
            TenantContextHolder.clear();
        }
    }

    @Override
    public Optional<GrievanceType> update(String grievanceTypeId, GrievanceTypeUpdateRequest updateRequest,
                                          String tenantId, String businessId) {
        log.info("Updating GrievanceType {} for tenant={}, business={}", grievanceTypeId, tenantId, businessId);
        validateBusinessId(businessId);

        TenantContextHolder.setTenant(tenantId);
        TenantContextHolder.setBusinessId(businessId);

        try {
            MongoTemplate tenantMongoTemplate = mongoConfig.getMongoTemplateForTenant(tenantId);

            // Fetch the existing GrievanceType
            Query q = new Query().addCriteria(
                    Criteria.where("grievanceTypeId").is(grievanceTypeId)
                            .and("businessId").is(businessId)
            );
            GrievanceType existing = tenantMongoTemplate.findOne(q, GrievanceType.class);
            if (existing == null) return Optional.empty();

            // Check if the updated name already exists in another record
            if (updateRequest.getGrievanceType() != null &&
                    !updateRequest.getGrievanceType().equalsIgnoreCase(existing.getGrievanceType())) {

                Query duplicateCheckQuery = new Query().addCriteria(
                        Criteria.where("grievanceType").is(updateRequest.getGrievanceType())
                                .and("businessId").is(businessId)
                                .and("grievanceTypeId").ne(grievanceTypeId) // exclude current record
                );

                GrievanceType duplicate = tenantMongoTemplate.findOne(duplicateCheckQuery, GrievanceType.class);
                if (duplicate != null) {
                    throw new IllegalArgumentException(
                            "A GrievanceType with name '" + updateRequest.getGrievanceType() + "' already exists for this business"
                    );
                }
            }

            // Apply updates
            try {
                objectMapper.updateValue(existing, updateRequest);
            } catch (Exception e) {
                log.warn("Failed to apply updates via ObjectMapper: {}", e.getMessage());
            }

            existing.setUpdatedAt(LocalDateTime.now());
            GrievanceType saved = tenantMongoTemplate.save(existing);
            return Optional.of(saved);

        } finally {
            TenantContextHolder.clear();
        }
    }

    @Override
    public boolean delete(String grievanceTypeId, String tenantId, String businessId) {
        log.info("Deleting GrievanceType {} for tenant={}, business={}", grievanceTypeId, tenantId, businessId);
        validateBusinessId(businessId);

        TenantContextHolder.setTenant(tenantId);
        TenantContextHolder.setBusinessId(businessId);

        try {
            MongoTemplate tenantMongoTemplate = mongoConfig.getMongoTemplateForTenant(tenantId);
            Query q = new Query().addCriteria(
                    Criteria.where("grievanceTypeId").is(grievanceTypeId)
                            .and("businessId").is(businessId)
            );

            GrievanceType existing = tenantMongoTemplate.findOne(q, GrievanceType.class);
            if (existing == null) return false;

            tenantMongoTemplate.remove(q, GrievanceType.class);
            log.info("Deleted GrievanceType {} from tenant_db_{}", grievanceTypeId, tenantId);
            return true;
        } finally {
            TenantContextHolder.clear();
        }
    }

    @Override
    public long count(String tenantId, String businessId) {
        log.info("Counting GrievanceTypes for tenant={}, business={}", tenantId, businessId);
        validateBusinessId(businessId);

        TenantContextHolder.setTenant(tenantId);
        TenantContextHolder.setBusinessId(businessId);

        try {
            MongoTemplate tenantMongoTemplate = mongoConfig.getMongoTemplateForTenant(tenantId);
            Query q = new Query().addCriteria(Criteria.where("businessId").is(businessId));
            long total = tenantMongoTemplate.count(q, GrievanceType.class);
            log.info("Total GrievanceTypes in tenant_db_{}: {}", tenantId, total);
            return total;
        } finally {
            TenantContextHolder.clear();
        }
    }

    private void validateBusinessId(String businessId) {
        if (businessId == null || businessId.trim().isEmpty()) {
            throw new ValidationException("X-Business-Id header is required for grievance-type operations");
        }
    }
}