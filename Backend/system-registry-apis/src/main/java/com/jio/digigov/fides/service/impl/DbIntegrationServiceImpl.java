package com.jio.digigov.fides.service.impl;

import com.jio.digigov.fides.config.MultiTenantMongoConfig;
import com.jio.digigov.fides.constant.IntegrationConstants;
import com.jio.digigov.fides.dto.request.DbIntegrationCreateRequest;
import com.jio.digigov.fides.dto.request.DbIntegrationTestRequest;
import com.jio.digigov.fides.dto.request.DbIntegrationUpdateRequest;
import com.jio.digigov.fides.dto.response.DbIntegrationResponse;
import com.jio.digigov.fides.entity.DbIntegration;
import com.jio.digigov.fides.enumeration.ActionType;
import com.jio.digigov.fides.enumeration.AuditComponent;
import com.jio.digigov.fides.enumeration.Group;
import com.jio.digigov.fides.enumeration.Status;
import com.jio.digigov.fides.exception.BodyValidationException;
import com.jio.digigov.fides.integration.factory.DbConnectionTestFactory;
import com.jio.digigov.fides.integration.test.DbConnectionTester;
import com.jio.digigov.fides.mapper.DbIntegrationMapper;
import com.jio.digigov.fides.service.DbIntegrationService;
import com.jio.digigov.fides.util.HeaderValidationService;
import com.jio.digigov.fides.util.TenantContextHolder;
import com.jio.digigov.fides.util.TriggerAuditEvent;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class DbIntegrationServiceImpl implements DbIntegrationService {

    private final MultiTenantMongoConfig mongoConfig;
    private final HeaderValidationService headerValidationService;
    private final TriggerAuditEvent triggerAuditEvent;

    // ----------------------------------------------------------------
    // Supported DB Types
    // ----------------------------------------------------------------
    @Override
    public Map<String, Object> getSupportedDbTypes() {

        log.info("Fetching supported database types");

        return Map.of(
                "dbTypes", List.of(
                        Map.of(
                                "dbType", IntegrationConstants.DB_MONGODB,
                                "mandatoryFields", IntegrationConstants.MANDATORY_DB_FIELDS
                        ),
                        Map.of(
                                "dbType", IntegrationConstants.DB_MYSQL,
                                "mandatoryFields", IntegrationConstants.MANDATORY_DB_FIELDS
                        )
                )
        );
    }

    // ----------------------------------------------------------------
    // Create Integration
    // ----------------------------------------------------------------
    @Override
    public DbIntegrationResponse create(String tenantId, String businessId, DbIntegrationCreateRequest request, HttpServletRequest req) {

        log.info("Creating DB integration tenant={} business={}", tenantId, businessId);

        headerValidationService.validateTenantAndBusinessId(tenantId, businessId);

        log.info("Header validation passed for tenantId: {}, businessId: {}", tenantId, businessId);

        validateSystemIdExists(tenantId, request.getSystemId());
        validateDatasetIdExists(tenantId, request.getDatasetId());

        try {
            MongoTemplate template = mongoConfig.getMongoTemplateForTenant(tenantId);

            DbIntegration entity =
                    DbIntegrationMapper.toEntity(request, businessId);

            DbIntegration saved = template.save(entity);

            log.info("DB Integration created integrationId={}", saved.getIntegrationId());

            triggerAuditEvent.trigger(
                    saved.getIntegrationId(),
                    "INTEGRATION_ID",
                    Group.DB_INTEGRATION,
                    AuditComponent.DB_INTEGRATION,
                    ActionType.CREATED,
                    tenantId,
                    businessId,
                    req
            );

            return DbIntegrationMapper.toResponse(saved);

        } catch (Exception e) {
            log.error("Failed to create DB Integration", e);
            throw new RuntimeException("DB_INTEGRATION_CREATION_FAILED");
        } finally {
            TenantContextHolder.clear();
        }
    }

    // ----------------------------------------------------------------
    // List Integrations
    // ----------------------------------------------------------------
    @Override
    public List<DbIntegration> list(String tenantId, String businessId, String systemId) {

        log.info("Listing DB integrations tenant={} business={} systemId={}",
                tenantId, businessId, systemId);

        headerValidationService.validateTenantAndBusinessId(tenantId, businessId);

        Criteria criteria = Criteria.where("businessId").is(businessId)
                .and("isDeleted").is(false);

        if (systemId != null && !systemId.isBlank()) {
            criteria.and("systemId").is(systemId);
        }

        try {
            return mongoConfig.getMongoTemplateForTenant(tenantId)
                    .find(Query.query(criteria), DbIntegration.class);
        } finally {
            TenantContextHolder.clear();
        }
    }

    // ----------------------------------------------------------------
    // Get Integration By ID
    // ----------------------------------------------------------------
    @Override
    public DbIntegration getByIntegrationId(
            String tenantId, String businessId, String integrationId) {

        log.info("Fetching DB integration integrationId={} tenant={} business={}",
                integrationId, tenantId, businessId);

        headerValidationService.validateTenantAndBusinessId(tenantId, businessId);

        log.info("Header validation passed for tenantId: {}, businessId: {}", tenantId, businessId);

        try {
            DbIntegration integration = mongoConfig.getMongoTemplateForTenant(tenantId)
                    .findOne(
                            Query.query(
                                    Criteria.where("integrationId").is(integrationId)
                                            .and("businessId").is(businessId)
                                            .and("isDeleted").is(false)
                            ),
                            DbIntegration.class
                    );

            if (integration == null) {
                log.warn("Integration not found integrationId={}", integrationId);
                throw new RuntimeException("INTEGRATION_NOT_FOUND");
            }
            return integration;

        } finally {
            TenantContextHolder.clear();
        }
    }

    // ----------------------------------------------------------------
    // Update Integration By ID
    // ----------------------------------------------------------------

    @Override
    public DbIntegrationResponse update(String tenantId, String businessId, String integrationId, DbIntegrationUpdateRequest request,
            HttpServletRequest req
    ) {

        log.info("Updating DB integration integrationId={} tenant={} business={}",
                integrationId, tenantId, businessId);

        headerValidationService.validateTenantAndBusinessId(tenantId, businessId);

        DbIntegration existing =
                getByIntegrationId(tenantId, businessId, integrationId);

        validateSystemIdExists(tenantId, request.getSystemId());
        validateDatasetIdExists(tenantId, request.getDatasetId());

        try {
            // Update allowed fields
            existing.setDbType(request.getDbType());
            existing.setSystemId(request.getSystemId());
            existing.setDatasetId(request.getDatasetId());
            existing.setConnectionDetails(request.getConnectionDetails());
            existing.setStatus(request.getStatus() != null? Status.valueOf(request.getStatus()): Status.ACTIVE);
            existing.setUpdatedAt(LocalDateTime.now());

            DbIntegration saved =
                    mongoConfig.getMongoTemplateForTenant(tenantId).save(existing);

            // Trigger audit after successful update
            triggerAuditEvent.trigger(
                    saved.getIntegrationId(),
                    "INTEGRATION_ID",
                    Group.DB_INTEGRATION,
                    AuditComponent.DB_INTEGRATION,
                    ActionType.UPDATED,
                    tenantId,
                    businessId,
                    req
            );

            log.info("DB integration updated successfully integrationId={}", integrationId);

            return DbIntegrationMapper.toResponse(saved);

        } finally {
            TenantContextHolder.clear();
        }
    }

    // ----------------------------------------------------------------
    // Delete Integration (Soft Delete)
    // ----------------------------------------------------------------
    @Override
    public void delete(String tenantId, String businessId, String integrationId, HttpServletRequest req) {

        log.info("Deleting DB integration integrationId={}", integrationId);

        headerValidationService.validateTenantAndBusinessId(tenantId, businessId);

        log.info("Header validation passed for tenantId: {}, businessId: {}", tenantId, businessId);

        DbIntegration integration =
                getByIntegrationId(tenantId, businessId, integrationId);

        try {
            integration.setDeleted(true);
            integration.setStatus(Status.ACTIVE);
            integration.setUpdatedAt(LocalDateTime.now());

            DbIntegration saved = mongoConfig.getMongoTemplateForTenant(tenantId).save(integration);


            triggerAuditEvent.trigger(
                    saved.getIntegrationId(),
                    "INTEGRATION_ID",
                    Group.DB_INTEGRATION,
                    AuditComponent.DB_INTEGRATION,
                    ActionType.DELETED,
                    tenantId,
                    businessId,
                    req
            );
        } finally {
            TenantContextHolder.clear();
        }
    }

    // ----------------------------------------------------------------
    // Map Dataset
    // ----------------------------------------------------------------
    @Override
    public DbIntegration mapDataset(
            String tenantId, String businessId,
            String integrationId, String datasetId, HttpServletRequest req) {

        log.info("Mapping dataset={} to integration={}", datasetId, integrationId);

        headerValidationService.validateTenantAndBusinessId(tenantId, businessId);

        log.info("Header validation passed for tenantId: {}, businessId: {}", tenantId, businessId);

        DbIntegration integration =
                getByIntegrationId(tenantId, businessId, integrationId);

        try {
            integration.setDatasetId(datasetId);
            integration.setUpdatedAt(LocalDateTime.now());

            DbIntegration saved =
                    mongoConfig.getMongoTemplateForTenant(tenantId).save(integration);

            //  Trigger audit AFTER successful save
            triggerAuditEvent.trigger(
                    saved.getIntegrationId(),
                    "INTEGRATION_ID",
                    Group.DB_INTEGRATION,
                    AuditComponent.DB_INTEGRATION,
                    ActionType.UPDATED,
                    tenantId,
                    businessId,
                    req
            );

            log.info("Dataset mapped successfully integrationId={} datasetId={}",
                    integrationId, datasetId);

            return saved;
        } finally {
            TenantContextHolder.clear();
        }
    }

    // ----------------------------------------------------------------
    // Test DB Connection (REAL IMPLEMENTATION)
    // ----------------------------------------------------------------
    @Override
    public Map<String, Object> testConnection(DbIntegrationTestRequest request) {

        log.info("Testing DB connection dbType={}", request.getDbType());

        DbConnectionTester tester =
                DbConnectionTestFactory.getTester(request.getDbType());

        Map<String, Object> testResult =
                tester.test(request.getConnectionDetails());

        return Map.of(
                "success", testResult.getOrDefault("success", false),
                "message", testResult.getOrDefault("message", "Unknown"),
                "dbType", request.getDbType(),
                "testedAt", Instant.now()
        );
    }

    // ----------------------------------------------------------------
    // Count Integrations
    // ----------------------------------------------------------------
    @Override
    public Map<String, Object> count(String tenantId, String businessId) {

        log.info("Counting DB integrations tenant={} business={}", tenantId, businessId);

        headerValidationService.validateTenantAndBusinessId(tenantId, businessId);

        log.info("Header validation passed for tenantId: {}, businessId: {}", tenantId, businessId);

        try {
            MongoTemplate template = mongoConfig.getMongoTemplateForTenant(tenantId);

            long total = template.count(
                    Query.query(Criteria.where("businessId").is(businessId)),
                    DbIntegration.class
            );

            long active = template.count(
                    Query.query(
                            Criteria.where("businessId").is(businessId)
                                    .and("status").is(Status.ACTIVE)
                                    .and("isDeleted").is(false)
                    ),
                    DbIntegration.class
            );

            long inactive = template.count(
                    Query.query(
                            Criteria.where("businessId").is(businessId)
                                    .and("status").is(Status.INACTIVE)
                                    .and("isDeleted").is(false)
                    ),
                    DbIntegration.class
            );

            return Map.of(
                    "totalIntegrations", total,
                    "activeIntegrations", active,
                    "inactiveIntegrations", inactive
            );

        } finally {
            TenantContextHolder.clear();
        }
    }

    private void validateSystemIdExists(String tenantId, String systemId) {
        if (systemId == null || systemId.isBlank()) {
            return;
        }

        boolean exists = mongoConfig.getMongoTemplateForTenant(tenantId)
                .exists(
                        Query.query(
                                Criteria.where("_id").is(systemId)
                                        .and("status").is(Status.ACTIVE)
                                        .and("isDeleted").is(false)
                        ),
                        "system_register"
                );

        if (!exists) {
            throw new BodyValidationException("INVALID_OR_INACTIVE_SYSTEM_ID");
        }
    }

    private void validateDatasetIdExists(String tenantId, String datasetId) {
        if (datasetId == null || datasetId.isBlank()) {
            return;
        }

        boolean exists = mongoConfig.getMongoTemplateForTenant(tenantId)
                .exists(
                        Query.query(
                                Criteria.where("datasetId").is(datasetId)
                                        .and("status").is(Status.ACTIVE)
                                        .and("isDeleted").is(false)
                        ),
                        "dataset_registry"
                );

        if (!exists) {
            throw new BodyValidationException("INVALID_DATASET_ID");
        }
    }
}