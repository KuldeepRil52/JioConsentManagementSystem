package com.jio.digigov.notification.service.dpo.impl;

import com.jio.digigov.notification.dto.request.dpo.CreateDpoConfigurationRequestDto;
import com.jio.digigov.notification.dto.request.dpo.UpdateDpoConfigurationRequestDto;
import com.jio.digigov.notification.dto.response.dpo.DpoConfigurationResponseDto;
import com.jio.digigov.notification.entity.DpoConfiguration;
import com.jio.digigov.notification.exception.BusinessException;
import com.jio.digigov.notification.exception.ResourceNotFoundException;
import com.jio.digigov.notification.mapper.DpoConfigurationMapper;
import com.jio.digigov.notification.repository.DpoConfigurationRepository;
import com.jio.digigov.notification.service.dpo.DpoConfigurationService;
import com.jio.digigov.notification.util.MongoTemplateProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Implementation of DpoConfigurationService for managing DPO configurations.
 *
 * <p>This service enforces the constraint that only one DPO configuration can exist
 * per tenant. All operations are tenant-scoped.</p>
 *
 * @author DPDP Notification Team
 * @version 1.0
 * @since 2025-10-24
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DpoConfigurationServiceImpl implements DpoConfigurationService {

    private final DpoConfigurationRepository dpoConfigurationRepository;
    private final DpoConfigurationMapper dpoConfigurationMapper;
    private final MongoTemplateProvider mongoTemplateProvider;

    @Override
    @Transactional
    public DpoConfigurationResponseDto createDpoConfiguration(String tenantId, String businessId, CreateDpoConfigurationRequestDto request) {
        log.info("Creating DPO configuration for tenant: {}, businessId: {}", tenantId, businessId);

        MongoTemplate mongoTemplate = mongoTemplateProvider.getTemplate(tenantId);

        // Create entity
        DpoConfiguration entity = dpoConfigurationMapper.toEntity(request);

        // Generate unique configId
        entity.setConfigId(java.util.UUID.randomUUID().toString());

        // Set scope level and businessId
        if (businessId != null && !businessId.trim().isEmpty()) {
            entity.setBusinessId(businessId);
            entity.setScopeLevel("BUSINESS");
            log.info("Creating BUSINESS-scoped DPO configuration for businessId: {}", businessId);
        } else {
            entity.setBusinessId(null);
            entity.setScopeLevel("TENANT");
            log.info("Creating TENANT-scoped DPO configuration");
        }

        // Save to database
        DpoConfiguration saved = dpoConfigurationRepository.save(entity, mongoTemplate);

        log.info("Successfully created DPO configuration - configId: {}, scope: {}, email: {}",
                saved.getConfigId(), saved.getScopeLevel(), saved.getConfigurationJson().getEmail());

        return dpoConfigurationMapper.toResponse(saved);
    }

    @Override
    public DpoConfigurationResponseDto getDpoConfiguration(String tenantId) {
        log.info("Retrieving DPO configuration for tenant: {}", tenantId);

        MongoTemplate mongoTemplate = mongoTemplateProvider.getTemplate(tenantId);

        Optional<DpoConfiguration> configOpt = dpoConfigurationRepository.findDpoConfiguration(mongoTemplate);

        if (configOpt.isEmpty()) {
            log.error("DPO configuration not found for tenant: {}", tenantId);
            throw new ResourceNotFoundException("DPO configuration not found for tenant: " + tenantId);
        }

        return dpoConfigurationMapper.toResponse(configOpt.get());
    }

    @Override
    @Transactional
    public DpoConfigurationResponseDto updateDpoConfiguration(String tenantId, UpdateDpoConfigurationRequestDto request) {
        log.info("Updating DPO configuration for tenant: {}", tenantId);

        MongoTemplate mongoTemplate = mongoTemplateProvider.getTemplate(tenantId);

        // Fetch existing configuration
        Optional<DpoConfiguration> configOpt = dpoConfigurationRepository.findDpoConfiguration(mongoTemplate);

        if (configOpt.isEmpty()) {
            log.error("DPO configuration not found for tenant: {}", tenantId);
            throw new ResourceNotFoundException("DPO configuration not found for tenant: " + tenantId +
                    ". Please create a configuration first.");
        }

        DpoConfiguration existingConfig = configOpt.get();

        // Update entity
        dpoConfigurationMapper.updateEntity(existingConfig, request);

        // Save updated entity
        DpoConfiguration updated = dpoConfigurationRepository.save(existingConfig, mongoTemplate);

        log.info("Successfully updated DPO configuration for tenant: {}, email: {}",
                tenantId, updated.getConfigurationJson().getEmail());

        return dpoConfigurationMapper.toResponse(updated);
    }

    @Override
    @Transactional
    public void deleteDpoConfiguration(String tenantId) {
        log.info("Deleting DPO configuration for tenant: {}", tenantId);

        MongoTemplate mongoTemplate = mongoTemplateProvider.getTemplate(tenantId);

        // Check if configuration exists
        if (!dpoConfigurationRepository.exists(mongoTemplate)) {
            log.error("DPO configuration not found for tenant: {}", tenantId);
            throw new ResourceNotFoundException("DPO configuration not found for tenant: " + tenantId);
        }

        // Delete the configuration
        long deletedCount = dpoConfigurationRepository.delete(mongoTemplate);

        if (deletedCount > 0) {
            log.info("Successfully deleted DPO configuration for tenant: {}", tenantId);
        } else {
            log.error("Failed to delete DPO configuration for tenant: {}", tenantId);
            throw new BusinessException("DPO_CONFIG_DELETE_FAILED",
                    "Failed to delete DPO configuration for tenant: " + tenantId);
        }
    }

    @Override
    public boolean existsDpoConfiguration(String tenantId) {
        log.debug("Checking if DPO configuration exists for tenant: {}", tenantId);

        MongoTemplate mongoTemplate = mongoTemplateProvider.getTemplate(tenantId);

        return dpoConfigurationRepository.exists(mongoTemplate);
    }
}
