package com.jio.digigov.grievance.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jio.digigov.grievance.config.MultiTenantMongoConfig;
import com.jio.digigov.grievance.dto.request.UserDetailUpdateRequest;
import com.jio.digigov.grievance.entity.UserDetail;
import com.jio.digigov.grievance.enumeration.ScopeLevel;
import com.jio.digigov.grievance.exception.BusinessException;
import com.jio.digigov.grievance.exception.ValidationException;
import com.jio.digigov.grievance.service.UserDetailService;
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
public class UserDetailServiceImpl implements UserDetailService {

    private final MultiTenantMongoConfig mongoConfig;
    private final ObjectMapper objectMapper;

    @Override
    public UserDetail create(UserDetail req, String tenantId, String businessId, ScopeLevel scopeLevel) {
        log.info("Creating UserDetail {} for tenant={}, business={}", req.getName(), tenantId, businessId);
        validateBusinessId(businessId);

        TenantContextHolder.setTenant(tenantId);
        TenantContextHolder.setBusinessId(businessId);

        try {
            MongoTemplate mongoTemplate = mongoConfig.getMongoTemplateForTenant(tenantId);

            // Check for existing UserDetail with same name
            Query query = new Query().addCriteria(
                    Criteria.where("name").is(req.getName())
                            .and("businessId").is(businessId)
            );
            UserDetail existing = mongoTemplate.findOne(query, UserDetail.class);
            if (existing != null) {
                throw new IllegalArgumentException(
                        "A UserDetail with name '" + req.getName() + "' already exists for this business"
                );
            }

            // Set metadata
            if (req.getCreatedAt() == null) req.setCreatedAt(LocalDateTime.now());
            req.setBusinessId(businessId);
            req.setScope(scopeLevel);
            req.setUpdatedAt(LocalDateTime.now());

            UserDetail saved = mongoTemplate.save(req);
            log.info("Saved UserDetail {} in tenant_db_{}", saved.getUserDetailId(), tenantId);
            return saved;
        } catch (IllegalArgumentException e) {
            throw e; // propagate duplicate name error to controller
        } catch (Exception e) {
            log.error("Failed to create UserDetail for tenant={}, business={}", tenantId, businessId, e);
            throw new BusinessException("USER_DETAIL_CREATION_FAILED", e.getMessage());
        } finally {
            TenantContextHolder.clear();
        }
    }

    @Override
    public List<UserDetail> list(String tenantId, String businessId, ScopeLevel scopeLevel) {
        log.info("Listing UserDetails for tenant={}, business={} with scope={}", tenantId, businessId, scopeLevel);
        validateBusinessId(businessId);

        TenantContextHolder.setTenant(tenantId);
        TenantContextHolder.setBusinessId(businessId);

        try {
            MongoTemplate mongoTemplate = mongoConfig.getMongoTemplateForTenant(tenantId);
            Query query;

            if (scopeLevel == ScopeLevel.TENANT) {
                // TENANT → fetch all user details across all businesses
                query = new Query();
            } else {
                query = new Query().addCriteria(
                        new Criteria().orOperator(
                                Criteria.where("businessId").is(businessId), // business-level records
                                Criteria.where("scope").is(ScopeLevel.TENANT)     // tenant-level records (scope = TENANT)
                        )
                );
            }

            List<UserDetail> results = mongoTemplate.find(query, UserDetail.class);
            log.info("Fetched {} UserDetails for tenant={} scope={}", results.size(), tenantId, scopeLevel);
            return results;

        } finally {
            TenantContextHolder.clear();
        }
    }

    @Override
    public Optional<UserDetail> getById(String userDetailId, String tenantId, String businessId) {
        log.info("Fetching UserDetail {} for tenant={}, business={}", userDetailId, tenantId, businessId);
        validateBusinessId(businessId);

        TenantContextHolder.setTenant(tenantId);
        TenantContextHolder.setBusinessId(businessId);

        try {
            MongoTemplate mongoTemplate = mongoConfig.getMongoTemplateForTenant(tenantId);
            Query query = new Query().addCriteria(
                    Criteria.where("userDetailId").is(userDetailId)
                            .and("businessId").is(businessId)
            );
            return Optional.ofNullable(mongoTemplate.findOne(query, UserDetail.class));
        } finally {
            TenantContextHolder.clear();
        }
    }

    @Override
    public Optional<UserDetail> update(String userDetailId, UserDetailUpdateRequest updateReq, String tenantId, String businessId) {
        log.info("Updating UserDetail {} for tenant={}, business={}", userDetailId, tenantId, businessId);
        validateBusinessId(businessId);

        TenantContextHolder.setTenant(tenantId);
        TenantContextHolder.setBusinessId(businessId);

        try {
            MongoTemplate mongoTemplate = mongoConfig.getMongoTemplateForTenant(tenantId);
            Query query = new Query().addCriteria(
                    Criteria.where("userDetailId").is(userDetailId)
                            .and("businessId").is(businessId)
            );

            UserDetail existing = mongoTemplate.findOne(query, UserDetail.class);
            if (existing == null) return Optional.empty();

            // Validate and update 'name'
            if (updateReq.getName() != null) {
                if (updateReq.getName().trim().isEmpty()) {
                    throw new IllegalArgumentException("UserDetail name must not be empty.");
                }

                if (!updateReq.getName().equals(existing.getName())) {
                    Query duplicateQuery = new Query().addCriteria(
                            Criteria.where("name").is(updateReq.getName())
                                    .and("businessId").is(businessId)
                                    .and("userDetailId").ne(userDetailId)
                    );
                    UserDetail duplicate = mongoTemplate.findOne(duplicateQuery, UserDetail.class);
                    if (duplicate != null) {
                        throw new IllegalArgumentException(
                                "A UserDetail with name '" + updateReq.getName() + "' already exists for this business."
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
            UserDetail saved = mongoTemplate.save(existing);
            return Optional.of(saved);

        } finally {
            TenantContextHolder.clear();
        }
    }

    @Override
    public boolean delete(String userDetailId, String tenantId, String businessId) {
        log.info("Deleting UserDetail {} for tenant={}, business={}", userDetailId, tenantId, businessId);
        validateBusinessId(businessId);

        TenantContextHolder.setTenant(tenantId);
        TenantContextHolder.setBusinessId(businessId);

        try {
            MongoTemplate mongoTemplate = mongoConfig.getMongoTemplateForTenant(tenantId);
            Query query = new Query().addCriteria(
                    Criteria.where("userDetailId").is(userDetailId)
                            .and("businessId").is(businessId)
            );

            UserDetail existing = mongoTemplate.findOne(query, UserDetail.class);
            if (existing == null) return false;

            mongoTemplate.remove(query, UserDetail.class);
            log.info("Deleted UserDetail {} from tenant_db_{}", userDetailId, tenantId);
            return true;
        } finally {
            TenantContextHolder.clear();
        }
    }

    @Override
    public long count(String tenantId, String businessId) {
        log.info("Counting UserDetails for tenant={}, business={}", tenantId, businessId);
        validateBusinessId(businessId);

        TenantContextHolder.setTenant(tenantId);
        TenantContextHolder.setBusinessId(businessId);

        try {
            MongoTemplate mongoTemplate = mongoConfig.getMongoTemplateForTenant(tenantId);
            Query query = new Query().addCriteria(Criteria.where("businessId").is(businessId));
            long total = mongoTemplate.count(query, UserDetail.class);
            log.info("Total UserDetails in tenant_db_{}: {}", tenantId, total);
            return total;
        } finally {
            TenantContextHolder.clear();
        }
    }

    private void validateBusinessId(String businessId) {
        if (businessId == null || businessId.trim().isEmpty()) {
            throw new ValidationException("X-Business-Id header is required for user-detail operations");
        }
    }
}