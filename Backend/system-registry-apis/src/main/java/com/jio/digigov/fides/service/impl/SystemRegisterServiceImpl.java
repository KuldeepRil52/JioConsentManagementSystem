package com.jio.digigov.fides.service.impl;

import com.jio.digigov.fides.config.MultiTenantMongoConfig;
import com.jio.digigov.fides.dto.request.SystemRegisterRequest;
import com.jio.digigov.fides.dto.response.SystemCountResponse;
import com.jio.digigov.fides.dto.response.SystemRegisterListResponse;
import com.jio.digigov.fides.dto.response.SystemRegisterResponse;
import com.jio.digigov.fides.dto.response.SystemRegisterUpdateResponse;
import com.jio.digigov.fides.entity.SystemRegister;
import com.jio.digigov.fides.enumeration.ActionType;
import com.jio.digigov.fides.enumeration.AuditComponent;
import com.jio.digigov.fides.enumeration.Group;
import com.jio.digigov.fides.enumeration.Status;
import com.jio.digigov.fides.service.SystemRegisterService;
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

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SystemRegisterServiceImpl implements SystemRegisterService {

    private final MultiTenantMongoConfig mongoConfig;
    private final HeaderValidationService headerValidationService;
    private final TriggerAuditEvent triggerAuditEvent;


    @Override
    public SystemRegisterResponse create(String tenantId, String businessId, SystemRegisterRequest request, HttpServletRequest req) {

        headerValidationService.validateTenantAndBusinessId(tenantId, businessId);

        try {
            MongoTemplate mongoTemplate = mongoConfig.getMongoTemplateForTenant(tenantId);

            SystemRegister entity = new SystemRegister();
            entity.setBusinessId(businessId);
            entity.setSystemUniqueId("SYS-" + UUID.randomUUID());
            entity.setSystemName(request.getSystemName());
            entity.setDescription(request.getDescription());
            entity.setStatus(
                    request.getStatus() != null
                            ? Status.valueOf(request.getStatus())
                            : Status.ACTIVE
            );
            entity.setCreatedAt(LocalDateTime.now());
            entity.setUpdatedAt(LocalDateTime.now());
            entity.setDeleted(false);

            SystemRegister saved = mongoTemplate.save(entity);

            //  AUDIT — CREATE
            triggerAuditEvent.trigger(
                    saved.getId(),
                    "SYSTEM_ID",
                    Group.SYSTEM,
                    AuditComponent.SYSTEM_REGISTRY,
                    ActionType.CREATED,
                    tenantId,
                    businessId,
                    req
            );

            return SystemRegisterResponse.from(saved);

        } finally {
            TenantContextHolder.clear();
        }
    }

    @Override
    public SystemRegisterListResponse findAll(String tenantId, String businessId) {
        log.info("Listing systems for tenantId: {}, businessId: {}", tenantId, businessId);

        headerValidationService.validateTenantAndBusinessId(tenantId, businessId);

        log.info("Header validation passed for tenantId: {}, businessId: {}", tenantId, businessId);

        try {
            MongoTemplate mongoTemplate = mongoConfig.getMongoTemplateForTenant(tenantId);

            Query query = new Query().addCriteria(
                    Criteria.where("businessId").is(businessId)
                            .and("isDeleted").is(false)
            );
            List<SystemRegisterResponse> systems = mongoTemplate.find(query, SystemRegister.class)
                    .stream()
                    .map(SystemRegisterResponse::from)
                    .collect(Collectors.toList());
            
            log.info("Found {} systems for tenantId: {}, businessId: {}", systems.size(), tenantId, businessId);

            return new SystemRegisterListResponse(systems.size(), systems);

        } finally {
            TenantContextHolder.clear();
        }
    }

    @Override
    public SystemRegisterResponse findById(String tenantId, String businessId, String id) {

        log.info("Getting system by id: {} for tenantId: {}, businessId: {}", id, tenantId, businessId);

        headerValidationService.validateTenantAndBusinessId(tenantId, businessId);

        log.info("Header validation passed for tenantId: {}, businessId: {}", tenantId, businessId);

        try {
            MongoTemplate mongoTemplate = mongoConfig.getMongoTemplateForTenant(tenantId);

            Query query = new Query().addCriteria(
                    Criteria.where("_id").is(id)
                            .and("businessId").is(businessId)
                            .and("isDeleted").is(false)
            );

            SystemRegister entity = mongoTemplate.findOne(query, SystemRegister.class);
            if (entity == null) {
                throw new RuntimeException("System not found");
            }

            log.info("System found with id: {} for tenantId: {}, businessId: {}", id, tenantId, businessId);

            return SystemRegisterResponse.from(entity);
        } finally {
            TenantContextHolder.clear();
        }
    }

    @Override
    public SystemRegisterUpdateResponse update(
            String tenantId,
            String businessId,
            String id,
            SystemRegisterRequest request,
            HttpServletRequest req) {

        headerValidationService.validateTenantAndBusinessId(tenantId, businessId);

        try {
            MongoTemplate mongoTemplate = mongoConfig.getMongoTemplateForTenant(tenantId);

            Query query = Query.query(
                    Criteria.where("_id").is(id)
                            .and("businessId").is(businessId)
                            .and("isDeleted").is(false)
            );

            SystemRegister entity = mongoTemplate.findOne(query, SystemRegister.class);
            if (entity == null) {
                throw new RuntimeException("System not found");
            }

            entity.setSystemName(request.getSystemName());
            entity.setDescription(request.getDescription());
            entity.setStatus(Status.valueOf(request.getStatus()));
            entity.setUpdatedAt(LocalDateTime.now());

            mongoTemplate.save(entity);

            //  AUDIT — UPDATE
            triggerAuditEvent.trigger(
                    entity.getId(),
                    "SYSTEM_ID",
                    Group.SYSTEM,
                    AuditComponent.SYSTEM_REGISTRY,
                    ActionType.UPDATED,
                    tenantId,
                    businessId,
                    req
            );

            return new SystemRegisterUpdateResponse(
                    "System updated successfully",
                    SystemRegisterResponse.from(entity)
            );

        } finally {
            TenantContextHolder.clear();
        }
    }

    @Override
    public void delete(String tenantId, String businessId, String id, HttpServletRequest req) {

        headerValidationService.validateTenantAndBusinessId(tenantId, businessId);

        try {
            MongoTemplate mongoTemplate = mongoConfig.getMongoTemplateForTenant(tenantId);

            Query query = Query.query(
                    Criteria.where("_id").is(id)
                            .and("businessId").is(businessId)
                            .and("isDeleted").is(false)
            );

            SystemRegister entity = mongoTemplate.findOne(query, SystemRegister.class);
            if (entity == null) {
                throw new RuntimeException("System not found");
            }

            entity.setDeleted(true);
            entity.setUpdatedAt(LocalDateTime.now());
            mongoTemplate.save(entity);

            //  AUDIT — DELETE
            triggerAuditEvent.trigger(
                    entity.getId(),
                    "SYSTEM_ID",
                    Group.SYSTEM,
                    AuditComponent.SYSTEM_REGISTRY,
                    ActionType.DELETED,
                    tenantId,
                    businessId,
                    req
            );

        } finally {
            TenantContextHolder.clear();
        }
    }


    @Override
    public SystemCountResponse getSystemCounts(String tenantId, String businessId) {

        log.info("Getting system counts for tenantId: {}, businessId: {}", tenantId, businessId);

        headerValidationService.validateTenantAndBusinessId(tenantId, businessId);

        log.info("Header validation passed for tenantId: {}, businessId: {}", tenantId, businessId);

        try {
            MongoTemplate mongoTemplate = mongoConfig.getMongoTemplateForTenant(tenantId);

            Query totalQuery = new Query().addCriteria(
                    Criteria.where("businessId").is(businessId)
                            .and("isDeleted").is(false)
            );
            int totalSystems = (int) mongoTemplate.count(totalQuery, SystemRegister.class);
            log.debug("Total systems count query executed for tenantId: {}, businessId: {}", tenantId, businessId);

            Query activeQuery = new Query().addCriteria(
                    Criteria.where("businessId").is(businessId)
                            .and("status").is("ACTIVE")
                            .and("isDeleted").is(false)
            );
            int activeSystems = (int) mongoTemplate.count(activeQuery, SystemRegister.class);
            log.debug("Active systems count query executed for tenantId: {}, businessId: {}", tenantId, businessId);

            Query inactiveQuery = new Query().addCriteria(
                    Criteria.where("businessId").is(businessId)
                            .and("status").is("INACTIVE")
                            .and("isDeleted").is(false)
            );
            int inactiveSystems = (int) mongoTemplate.count(inactiveQuery, SystemRegister.class);
            log.debug("Inactive systems count query executed for tenantId: {}, businessId: {}", tenantId, businessId);

            SystemCountResponse response = new SystemCountResponse();
            response.setTotalSystems(totalSystems);
            response.setActiveSystems(activeSystems);
            response.setInactiveSystems(inactiveSystems);

            log.info("System counts retrieved for tenantId: {}, businessId: {}", tenantId, businessId);

            return response;

        } finally {
            TenantContextHolder.clear();
        }
    }
}
