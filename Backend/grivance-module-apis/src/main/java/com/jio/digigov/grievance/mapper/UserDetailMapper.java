package com.jio.digigov.grievance.mapper;

import com.jio.digigov.grievance.dto.request.UserDetailCreateRequest;
import com.jio.digigov.grievance.dto.request.UserDetailUpdateRequest;
import com.jio.digigov.grievance.dto.response.UserDetailResponse;
import com.jio.digigov.grievance.entity.UserDetail;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Mapper utility to convert between Entity and DTOs for UserDetail.
 */
public class UserDetailMapper {

    public static UserDetail toEntity(UserDetailCreateRequest req) {
        UserDetail entity = new UserDetail();
        entity.setUserDetailId(UUID.randomUUID().toString());
        entity.setName(req.getName());
        entity.setDescription(req.getDescription());
        return entity;
    }

    public static void updateEntity(UserDetail entity, UserDetailUpdateRequest req) {
        if (req.getName() != null) entity.setName(req.getName());
        if (req.getDescription() != null) entity.setDescription(req.getDescription());
        entity.setUpdatedAt(LocalDateTime.now());
    }

    public static UserDetailResponse toResponse(UserDetail entity) {
        return UserDetailResponse.builder()
                .userDetailId(entity.getUserDetailId())
                .name(entity.getName())
                .description(entity.getDescription())
                .scope(entity.getScope())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
