package com.jio.digigov.fides.service.impl;

import com.jio.digigov.fides.config.MultiTenantMongoConfig;
import com.jio.digigov.fides.dto.request.DatasetRequest;
import com.jio.digigov.fides.dto.response.DatasetCountResponse;
import com.jio.digigov.fides.dto.response.DatasetListResponse;
import com.jio.digigov.fides.dto.response.DatasetResponse;
import com.jio.digigov.fides.dto.response.DatasetUpdateResponse;
import com.jio.digigov.fides.entity.Dataset;
import com.jio.digigov.fides.enumeration.ActionType;
import com.jio.digigov.fides.enumeration.AuditComponent;
import com.jio.digigov.fides.enumeration.Group;
import com.jio.digigov.fides.enumeration.Status;
import com.jio.digigov.fides.service.DatasetService;
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
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class DatasetServiceImpl implements DatasetService {

    private final MultiTenantMongoConfig mongoConfig;
    private final HeaderValidationService headerValidationService;
    private final TriggerAuditEvent triggerAuditEvent;


    @Override
    public DatasetResponse create(DatasetRequest request, String tenantId, String businessId,  HttpServletRequest req) {
        
        log.info("Creating dataset for tenantId: {}, businessId: {}", tenantId, businessId);

        headerValidationService.validateTenantAndBusinessId(tenantId, businessId);

        log.info("Header validation passed for tenantId: {}, businessId: {}", tenantId, businessId);

        try {
            MongoTemplate template = mongoConfig.getMongoTemplateForTenant(tenantId);

            Dataset dataset = new Dataset();
            dataset.setDatasetId("DATASET-" + System.currentTimeMillis());
            dataset.setBusinessId(businessId);
            dataset.setDatasetYaml(request.getDatasetYaml());
            dataset.setVersion(1);
            dataset.setStatus(Status.ACTIVE);
            dataset.setCreatedAt(LocalDateTime.now());
            dataset.setUpdatedAt(LocalDateTime.now());
            dataset.setDeleted(false);

            Dataset saved = template.save(dataset);
            log.info("Saving dataset entity for tenantId: {}, businessId: {}", tenantId, businessId);

            triggerAuditEvent.trigger(
                    saved.getDatasetId(),
                    "DATASET_ID",
                    Group.DATASET,
                    AuditComponent.DATASET_REGISTRY,
                    ActionType.CREATED,
                    tenantId,
                    businessId,
                    req
            );

            return DatasetResponse.from(saved);

        } finally {
            TenantContextHolder.clear();
        }
    }

    @Override
    public DatasetListResponse list(String tenantId, String businessId) {
        log.info("Listing datasets for tenantId: {}, businessId: {}", tenantId, businessId);

        headerValidationService.validateTenantAndBusinessId(tenantId, businessId);

        log.info("Header validation passed for tenantId: {}, businessId: {}", tenantId, businessId);

        try {
            MongoTemplate template = mongoConfig.getMongoTemplateForTenant(tenantId);

            Query query = new Query(Criteria.where("businessId").is(businessId)
                    .and("isDeleted").is(false));

            List<DatasetResponse> list = template.find(query, Dataset.class)
                    .stream()
                    .map(DatasetResponse::from)
                    .toList();

            log.info("Found {} datasets for tenantId: {}, businessId: {}", list.size(), tenantId, businessId);

            return new DatasetListResponse(list.size(), list);
        } finally {
            TenantContextHolder.clear();
        }
    }

    @Override
    public DatasetResponse getById(String datasetId, String tenantId, String businessId) {

        log.info("Getting dataset by id: {} for tenantId: {}, businessId: {}", datasetId, tenantId, businessId);

        headerValidationService.validateTenantAndBusinessId(tenantId, businessId);

        log.info("Header validation passed for tenantId: {}, businessId: {}", tenantId, businessId);

        try {
            MongoTemplate template = mongoConfig.getMongoTemplateForTenant(tenantId);

            Query query = new Query(Criteria.where("datasetId").is(datasetId)
                    .and("businessId").is(businessId)
                    .and("isDeleted").is(false));
            

            Dataset entity = template.findOne(query, Dataset.class);
            if (entity == null) {
                throw new RuntimeException("Dataset not found");
            }

            log.info("Dataset found with id: {} for tenantId: {}, businessId: {}", datasetId, tenantId, businessId);

            return DatasetResponse.from(entity);
        } finally {
            TenantContextHolder.clear();
        }
    }

    @Override
    public DatasetUpdateResponse update(String datasetId, DatasetRequest request,
                                            String tenantId, String businessId, HttpServletRequest req) {
        log.info("Updating dataset with id: {} for tenantId: {}, businessId: {}", datasetId, tenantId, businessId);

        headerValidationService.validateTenantAndBusinessId(tenantId, businessId);

        log.info("Header validation passed for tenantId: {}, businessId: {}", tenantId, businessId);

        try {
            MongoTemplate template = mongoConfig.getMongoTemplateForTenant(tenantId);

            Query query = new Query(Criteria.where("datasetId").is(datasetId)
                    .and("businessId").is(businessId)
                    .and("isDeleted").is(false));

            Dataset existing = template.findOne(query, Dataset.class);
            if (existing == null) {
                throw new RuntimeException("Dataset not found");
            }

            existing.setDatasetYaml(request.getDatasetYaml());
            existing.setVersion(existing.getVersion() + 1);
            existing.setUpdatedAt(LocalDateTime.now());

            template.save(existing);

            log.info("Dataset updated with id: {} for tenantId: {}, businessId: {}", datasetId, tenantId, businessId);

            triggerAuditEvent.trigger(
                    existing.getDatasetId(),
                    "DATASET_ID",
                    Group.DATASET,
                    AuditComponent.DATASET_REGISTRY,
                    ActionType.UPDATED,
                    tenantId,
                    businessId,
                    req
            );

            return new DatasetUpdateResponse("Dataset updated successfully", DatasetResponse.from(existing));
        } finally {
            TenantContextHolder.clear();
        }
    }

    @Override
    public void delete(String datasetId, String tenantId, String businessId, HttpServletRequest req) {

        log.info("Deleting dataset with id: {} for tenantId: {}, businessId: {}", datasetId, tenantId, businessId);

        headerValidationService.validateTenantAndBusinessId(tenantId, businessId);

        log.info("Header validation passed for tenantId: {}, businessId: {}", tenantId, businessId);

        try {
            MongoTemplate template = mongoConfig.getMongoTemplateForTenant(tenantId);

            Query query = new Query(Criteria.where("datasetId").is(datasetId)
                    .and("businessId").is(businessId));

            Dataset existing = template.findOne(query, Dataset.class);
            if (existing == null) {
                throw new RuntimeException("Dataset not found");
            }
            
            existing.setDeleted(true);
            existing.setUpdatedAt(LocalDateTime.now());

            log.info("Marking dataset as deleted with id: {} for tenantId: {}, businessId: {}", datasetId, tenantId, businessId);

            template.save(existing);

            triggerAuditEvent.trigger(
                    existing.getDatasetId(),
                    "DATASET_REGISTRY",
                    Group.DATASET,
                    AuditComponent.DATASET_REGISTRY,
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
    public DatasetCountResponse getDatasetCounts(String tenantId, String businessId) {

        log.info("Getting dataset counts for tenantId: {}, businessId: {}", tenantId, businessId);

        headerValidationService.validateTenantAndBusinessId(tenantId, businessId);

        log.info("Header validation passed for tenantId: {}, businessId: {}", tenantId, businessId);

        try {
            MongoTemplate mongoTemplate = mongoConfig.getMongoTemplateForTenant(tenantId);

            Query totalQuery = new Query().addCriteria(
                    Criteria.where("businessId").is(businessId)
                            .and("isDeleted").is(false)
            );
            int totalDatasets = (int) mongoTemplate.count(totalQuery, Dataset.class);
            log.debug("Total Datasets count query executed for tenantId: {}, businessId: {}", tenantId, businessId);

            Query activeQuery = new Query().addCriteria(
                    Criteria.where("businessId").is(businessId)
                            .and("status").is("ACTIVE")
                            .and("isDeleted").is(false)
            );
            int activeDatasets = (int) mongoTemplate.count(activeQuery, Dataset.class);
            log.debug("Active Datasets count query executed for tenantId: {}, businessId: {}", tenantId, businessId);

            Query inactiveQuery = new Query().addCriteria(
                    Criteria.where("businessId").is(businessId)
                            .and("status").is("INACTIVE")
                            .and("isDeleted").is(false)
            );
            int inactiveDatasets = (int) mongoTemplate.count(inactiveQuery, Dataset.class);
            log.debug("Inactive Datasets count query executed for tenantId: {}, businessId: {}", tenantId, businessId);

            DatasetCountResponse response = new DatasetCountResponse();
            response.setTotalDatasets(totalDatasets);
            response.setActiveDatasets(activeDatasets);
            response.setInactiveDatasets(inactiveDatasets);

            log.info("Dataset counts retrieved for tenantId: {}, businessId: {}", tenantId, businessId);

            return response;

        } finally {
            TenantContextHolder.clear();
        }
    }
}