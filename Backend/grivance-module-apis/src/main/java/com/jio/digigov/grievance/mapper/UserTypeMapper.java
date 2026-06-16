package com.jio.digigov.grievance.mapper;

import com.jio.digigov.grievance.dto.request.UserTypeCreateRequest;
import com.jio.digigov.grievance.dto.request.UserTypeUpdateRequest;
import com.jio.digigov.grievance.dto.response.UserTypeResponse;
import com.jio.digigov.grievance.entity.UserType;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Mapper utility to convert between Entity and DTOs for UserType.
 */
public class UserTypeMapper {

    public static UserType toEntity(UserTypeCreateRequest req) {
        UserType entity = new UserType();
        entity.setUserTypeId(UUID.randomUUID().toString());
        entity.setName(req.getName());
        entity.setDescription(req.getDescription());
        return entity;
    }

    public static void updateEntity(UserType entity, UserTypeUpdateRequest req) {
        if (req.getName() != null) entity.setName(req.getName());
        if (req.getDescription() != null) entity.setDescription(req.getDescription());
        entity.setUpdatedAt(LocalDateTime.now());
    }

    public static UserTypeResponse toResponse(UserType entity) {
        return UserTypeResponse.builder()
                .userTypeId(entity.getUserTypeId())
                .name(entity.getName())
                .description(entity.getDescription())
                .scope(entity.getScope())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
