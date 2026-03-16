package com.jio.digigov.grievance.mapper;

import com.jio.digigov.grievance.dto.request.GrievanceTypeCreateRequest;
import com.jio.digigov.grievance.dto.request.GrievanceTypeUpdateRequest;
import com.jio.digigov.grievance.dto.response.GrievanceTypeResponse;
import com.jio.digigov.grievance.entity.GrievanceType;

import java.time.Instant;
import java.util.UUID;

/**
 * Mapper utility for converting between GrievanceType entity and DTOs.
 */
public class GrievanceTypeMapper {

    public static GrievanceType toEntity(GrievanceTypeCreateRequest req) {
        GrievanceType entity = new GrievanceType();
        entity.setGrievanceTypeId(UUID.randomUUID().toString());
        entity.setGrievanceType(req.getGrievanceType());
        entity.setGrievanceItem(req.getGrievanceItem());
        entity.setDescription(req.getDescription());
        return entity;
    }

    public static void updateEntity(GrievanceType entity, GrievanceTypeUpdateRequest req) {
        if (req.getGrievanceType() != null) entity.setGrievanceType(req.getGrievanceType());
        if (req.getGrievanceItem() != null) entity.setGrievanceItem(req.getGrievanceItem());
        if (req.getDescription() != null) entity.setDescription(req.getDescription());
        entity.setUpdatedAt(entity.getUpdatedAt());
    }

    public static GrievanceTypeResponse toResponse(GrievanceType entity) {
        return GrievanceTypeResponse.builder()
                .grievanceTypeId(entity.getGrievanceTypeId())
                .grievanceType(entity.getGrievanceType())
                .grievanceItem(entity.getGrievanceItem())
                .description(entity.getDescription())
                .scope(entity.getScope())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
