package com.jio.digigov.notification.mapper;

import com.jio.digigov.notification.dto.request.dpo.CreateDpoConfigurationRequestDto;
import com.jio.digigov.notification.dto.request.dpo.UpdateDpoConfigurationRequestDto;
import com.jio.digigov.notification.dto.response.dpo.DpoConfigurationResponseDto;
import com.jio.digigov.notification.entity.DpoConfiguration;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Mapper for converting between DPO Configuration entities and DTOs.
 *
 * @author DPDP Notification Team
 * @version 1.0
 * @since 2025-10-24
 */
@Component
public class DpoConfigurationMapper {

    /**
     * Converts CreateDpoConfigurationRequestDto to DpoConfiguration entity.
     *
     * @param request The create request DTO
     * @return DpoConfiguration entity
     */
    public DpoConfiguration toEntity(CreateDpoConfigurationRequestDto request) {
        DpoConfiguration.ConfigurationJson configJson = DpoConfiguration.ConfigurationJson.builder()
                .name(request.getConfigurationJson().getName())
                .email(request.getConfigurationJson().getEmail())
                .mobile(request.getConfigurationJson().getMobile())
                .address(request.getConfigurationJson().getAddress())
                .build();

        return DpoConfiguration.builder()
                .configurationJson(configJson)
                .build();
    }

    /**
     * Updates existing DpoConfiguration entity with values from UpdateDpoConfigurationRequestDto.
     *
     * @param entity The existing entity to update
     * @param request The update request DTO
     */
    public void updateEntity(DpoConfiguration entity, UpdateDpoConfigurationRequestDto request) {
        if (request.getConfigurationJson() != null) {
            DpoConfiguration.ConfigurationJson configJson = DpoConfiguration.ConfigurationJson.builder()
                    .name(request.getConfigurationJson().getName())
                    .email(request.getConfigurationJson().getEmail())
                    .mobile(request.getConfigurationJson().getMobile())
                    .address(request.getConfigurationJson().getAddress())
                    .build();

            entity.setConfigurationJson(configJson);
        }

        entity.setUpdatedAt(LocalDateTime.now());
    }

    /**
     * Converts DpoConfiguration entity to DpoConfigurationResponseDto.
     *
     * @param entity The entity
     * @return Response DTO
     */
    public DpoConfigurationResponseDto toResponse(DpoConfiguration entity) {
        DpoConfigurationResponseDto.ConfigurationJsonDto configJsonDto =
                DpoConfigurationResponseDto.ConfigurationJsonDto.builder()
                .name(entity.getConfigurationJson().getName())
                .email(entity.getConfigurationJson().getEmail())
                .mobile(entity.getConfigurationJson().getMobile())
                .address(entity.getConfigurationJson().getAddress())
                .build();

        return DpoConfigurationResponseDto.builder()
                .id(entity.getId())
                .configurationJson(configJsonDto)
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
