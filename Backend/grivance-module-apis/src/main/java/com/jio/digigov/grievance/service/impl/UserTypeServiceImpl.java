package com.jio.digigov.grievance.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jio.digigov.grievance.config.MultiTenantMongoConfig;
import com.jio.digigov.grievance.dto.request.UserTypeUpdateRequest;
import com.jio.digigov.grievance.entity.UserType;
import com.jio.digigov.grievance.enumeration.ScopeLevel;
import com.jio.digigov.grievance.exception.BusinessException;
import com.jio.digigov.grievance.exception.ValidationException;
import com.jio.digigov.grievance.service.UserTypeService;
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

@Slf4j
@Service
@RequiredArgsConstructor
public class UserTypeServiceImpl implements UserTypeService {

    private final MultiTenantMongoConfig mongoConfig;
    private final ObjectMapper objectMapper;

    @Override
    public UserType create(UserType req, String tenantId, String businessId, ScopeLevel scopeLevel) {
        log.info("Creating UserType {} for tenant={}, business={}", req.getName(), tenantId, businessId);
        validateBusinessId(businessId);

        TenantContextHolder.setTenant(tenantId);
        TenantContextHolder.setBusinessId(businessId);

        try {
            MongoTemplate tenantMongoTemplate = mongoConfig.getMongoTemplateForTenant(tenantId);

            // Check if a UserType with the same name already exists for this tenant and business
            Query query = new Query().addCriteria(
                    Criteria.where("name").is(req.getName())
                            .and("businessId").is(businessId)
            );
            UserType existing = tenantMongoTemplate.findOne(query, UserType.class);
            if (existing != null) {
                throw new IllegalArgumentException("A UserType with name '" + req.getName() + "' already exists for this business.");
            }

            // Set metadata
            if (req.getCreatedAt() == null) req.setCreatedAt(LocalDateTime.now());
            req.setBusinessId(businessId);
            req.setScope(scopeLevel);
            req.setUpdatedAt(LocalDateTime.now());

            UserType saved = tenantMongoTemplate.save(req);
            log.info("Saved UserType {} in tenant_db_{}", saved.getUserTypeId(), tenantId);
            return saved;

        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to create UserType for tenant={}, business={}", tenantId, businessId, e);
            throw new BusinessException("USER_TYPE_CREATION_FAILED", e.getMessage());
        } finally {
            TenantContextHolder.clear();
        }
    }

    @Override
    public List<UserType> list(String tenantId, String businessId, ScopeLevel scopeLevel) {
        log.info("Listing UserTypes for tenant={}, business={} with scope={}", tenantId, businessId, scopeLevel);
        validateBusinessId(businessId);

        TenantContextHolder.setTenant(tenantId);
        TenantContextHolder.setBusinessId(businessId);

        try {
            MongoTemplate tenantMongoTemplate = mongoConfig.getMongoTemplateForTenant(tenantId);
            Query query;

            if (scopeLevel == ScopeLevel.TENANT) {
                // Fetch all records across all businesses for the tenant
                query = new Query(); // no criteria — fetch all
            } else {
                query = new Query().addCriteria(
                        new Criteria().orOperator(
                                Criteria.where("businessId").is(businessId), // business-level records
                                Criteria.where("scope").is(ScopeLevel.TENANT)     // tenant-level records (scope = TENANT)
                        )
                );
            }

            List<UserType> results = tenantMongoTemplate.find(query, UserType.class);
            log.info("Fetched {} UserTypes for tenant={} with scope={}", results.size(), tenantId, scopeLevel);
            return results;

        } finally {
            TenantContextHolder.clear();
        }
    }

    @Override
    public Optional<UserType> getById(String userTypeId, String tenantId, String businessId, ScopeLevel scopeLevel) {
        log.info("Fetching UserType {} for tenant={}, business={}", userTypeId, tenantId, businessId);
        validateBusinessId(businessId);

        TenantContextHolder.setTenant(tenantId);
        TenantContextHolder.setBusinessId(businessId);

        try {
            MongoTemplate tenantMongoTemplate = mongoConfig.getMongoTemplateForTenant(tenantId);
            Query query = new Query().addCriteria(
                    Criteria.where("userTypeId").is(userTypeId)
                            .and("businessId").is(businessId)
            );
            return Optional.ofNullable(tenantMongoTemplate.findOne(query, UserType.class));
        } finally {
            TenantContextHolder.clear();
        }
    }

    @Override
    public Optional<UserType> update(String userTypeId, UserTypeUpdateRequest updateReq, String tenantId, String businessId) {
        log.info("Updating UserType {} for tenant={}, business={}", userTypeId, tenantId, businessId);
        validateBusinessId(businessId);

        TenantContextHolder.setTenant(tenantId);
        TenantContextHolder.setBusinessId(businessId);

        try {
            MongoTemplate tenantMongoTemplate = mongoConfig.getMongoTemplateForTenant(tenantId);
            Query query = new Query().addCriteria(
                    Criteria.where("userTypeId").is(userTypeId)
                            .and("businessId").is(businessId)
            );

            UserType existing = tenantMongoTemplate.findOne(query, UserType.class);
            if (existing == null) return Optional.empty();

            // Validate name (non-null and non-empty)
            if (updateReq.getName() != null) {
                if (updateReq.getName().trim().isEmpty()) {
                    throw new IllegalArgumentException("UserType name must not be empty.");
                }

                // Check for duplicate only if name changed
                if (!updateReq.getName().equals(existing.getName())) {
                    Query duplicateQuery = new Query().addCriteria(
                            Criteria.where("name").is(updateReq.getName())
                                    .and("businessId").is(businessId)
                                    .and("userTypeId").ne(userTypeId)
                    );
                    UserType duplicate = tenantMongoTemplate.findOne(duplicateQuery, UserType.class);
                    if (duplicate != null) {
                        throw new IllegalArgumentException(
                                "A UserType with name '" + updateReq.getName() + "' already exists for this business."
                        );
                    }
                    existing.setName(updateReq.getName());
                }
            }

            // Update description if provided (can be empty)
            if (updateReq.getDescription() != null) {
                existing.setDescription(updateReq.getDescription());
            }

            existing.setUpdatedAt(LocalDateTime.now());
            UserType saved = tenantMongoTemplate.save(existing);
            return Optional.of(saved);

        } finally {
            TenantContextHolder.clear();
        }
    }

    @Override
    public boolean delete(String userTypeId, String tenantId, String businessId) {
        log.info("Deleting UserType {} for tenant={}, business={}", userTypeId, tenantId, businessId);
        validateBusinessId(businessId);

        TenantContextHolder.setTenant(tenantId);
        TenantContextHolder.setBusinessId(businessId);

        try {
            MongoTemplate tenantMongoTemplate = mongoConfig.getMongoTemplateForTenant(tenantId);
            Query query = new Query().addCriteria(
                    Criteria.where("userTypeId").is(userTypeId)
                            .and("businessId").is(businessId)
            );

            UserType existing = tenantMongoTemplate.findOne(query, UserType.class);
            if (existing == null) return false;

            tenantMongoTemplate.remove(query, UserType.class);
            log.info("Deleted UserType {} from tenant_db_{}", userTypeId, tenantId);
            return true;
        } finally {
            TenantContextHolder.clear();
        }
    }

    @Override
    public long count(String tenantId, String businessId) {
        log.info("Counting UserTypes for tenant={}, business={}", tenantId, businessId);
        validateBusinessId(businessId);

        TenantContextHolder.setTenant(tenantId);
        TenantContextHolder.setBusinessId(businessId);

        try {
            MongoTemplate tenantMongoTemplate = mongoConfig.getMongoTemplateForTenant(tenantId);
            Query query = new Query().addCriteria(Criteria.where("businessId").is(businessId));
            long total = tenantMongoTemplate.count(query, UserType.class);
            log.info("Total UserTypes in tenant_db_{}: {}", tenantId, total);
            return total;
        } finally {
            TenantContextHolder.clear();
        }
    }

    private void validateBusinessId(String businessId) {
        if (businessId == null || businessId.trim().isEmpty()) {
            throw new ValidationException("X-Business-Id header is required for user-type operations");
        }
    }
}
