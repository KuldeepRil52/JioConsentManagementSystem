package com.jio.digigov.fides.mapper;

import com.jio.digigov.fides.dto.request.DbIntegrationCreateRequest;
import com.jio.digigov.fides.dto.response.DbIntegrationResponse;
import com.jio.digigov.fides.entity.DbIntegration;
import com.jio.digigov.fides.enumeration.Status;

import java.time.LocalDateTime;
import java.util.UUID;

import static com.jio.digigov.fides.constant.IntegrationConstants.*;

public class DbIntegrationMapper {

    private DbIntegrationMapper() {}

    public static DbIntegration toEntity(
            DbIntegrationCreateRequest request,
            String businessId
    ) {
        DbIntegration entity = new DbIntegration();
        entity.setIntegrationId(INTEGRATION_ID_PREFIX + UUID.randomUUID());
        entity.setSystemId(request.getSystemId());
        entity.setDatasetId(request.getDatasetId());
        entity.setDbType(request.getDbType());
        entity.setConnectionDetails(request.getConnectionDetails());
        entity.setBusinessId(businessId);
        entity.setStatus(Status.ACTIVE);
        entity.setDeleted(false);
        entity.setCreatedAt(LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());
        return entity;
    }

    public static DbIntegrationResponse toResponse(DbIntegration entity) {
        return DbIntegrationResponse.builder()
                .integrationId(entity.getIntegrationId())
                .systemId(entity.getSystemId())
                .businessId(entity.getBusinessId())
                .dbType(entity.getDbType())
                .connectionDetails(entity.getConnectionDetails())
                .datasetId(entity.getDatasetId())
                .status(entity.getStatus())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}