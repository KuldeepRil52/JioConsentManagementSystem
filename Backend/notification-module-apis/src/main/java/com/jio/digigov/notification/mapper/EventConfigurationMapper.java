package com.jio.digigov.notification.mapper;

import com.jio.digigov.notification.dto.request.event.CreateEventConfigurationRequestDto;
import com.jio.digigov.notification.dto.request.event.UpdateEventConfigurationRequestDto;
import com.jio.digigov.notification.dto.response.event.EventConfigurationResponseDto;
import com.jio.digigov.notification.entity.event.EventConfiguration;
import com.jio.digigov.notification.enums.EventPriority;
import com.jio.digigov.notification.enums.ScopeLevel;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.UUID;

@Component
public class EventConfigurationMapper {

    public EventConfiguration toEntity(CreateEventConfigurationRequestDto request, String businessId) {
        return EventConfiguration.builder()
                .configId(generateConfigId())
                .businessId(businessId)
                .scopeLevel(ScopeLevel.BUSINESS)
                .eventType(request.getEventType())
                .notifications(mapNotificationsConfig(request.getNotifications()))
                .priority(EventPriority.valueOf(request.getPriority()))
                .isActive(request.getIsActive() != null ? request.getIsActive() : true)
                .build();
    }

    public void updateEntity(EventConfiguration config, UpdateEventConfigurationRequestDto request) {
        if (request.getNotifications() != null) {
            config.setNotifications(mapNotificationsConfig(request.getNotifications()));
        }
        if (request.getPriority() != null) {
            config.setPriority(EventPriority.valueOf(request.getPriority()));
        }
        if (request.getIsActive() != null) {
            config.setIsActive(request.getIsActive());
        }
        config.setUpdatedAt(LocalDateTime.now());
    }

    public void updateEntity(EventConfiguration config, CreateEventConfigurationRequestDto request) {
        if (request.getNotifications() != null) {
            config.setNotifications(mapNotificationsConfig(request.getNotifications()));
        }
        if (request.getPriority() != null) {
            config.setPriority(EventPriority.valueOf(request.getPriority()));
        }
        if (request.getIsActive() != null) {
            config.setIsActive(request.getIsActive());
        }
        config.setUpdatedAt(LocalDateTime.now());
    }

    public EventConfigurationResponseDto toResponse(EventConfiguration config) {
        return EventConfigurationResponseDto.builder()
                .configId(config.getConfigId())
                .businessId(config.getBusinessId())
                .eventType(config.getEventType())
                .notifications(mapNotificationsConfigToResponse(config.getNotifications()))
                .priority(config.getPriority().name())
                .isActive(config.getIsActive())
                .createdAt(config.getCreatedAt())
                .updatedAt(config.getUpdatedAt())
                .build();
    }

    private EventConfiguration.NotificationsConfig mapNotificationsConfig(
            CreateEventConfigurationRequestDto.NotificationsConfigDto dto) {
        return EventConfiguration.NotificationsConfig.builder()
                .dataPrincipal(mapRecipientSettings(dto.getDataPrincipal()))
                .dataFiduciary(mapRecipientSettings(dto.getDataFiduciary()))
                .dataProcessor(mapRecipientSettings(dto.getDataProcessor()))
                .dataProtectionOfficer(mapRecipientSettings(dto.getDataProtectionOfficer()))
                .build();
    }

    private EventConfiguration.NotificationsConfig mapNotificationsConfig(
            UpdateEventConfigurationRequestDto.NotificationsConfigDto dto) {
        return EventConfiguration.NotificationsConfig.builder()
                .dataPrincipal(mapRecipientSettings(dto.getDataPrincipal()))
                .dataFiduciary(mapRecipientSettings(dto.getDataFiduciary()))
                .dataProcessor(mapRecipientSettings(dto.getDataProcessor()))
                .dataProtectionOfficer(mapRecipientSettings(dto.getDataProtectionOfficer()))
                .build();
    }

    private EventConfiguration.RecipientSettings mapRecipientSettings(
            CreateEventConfigurationRequestDto.RecipientConfigDto dto) {
        if (dto == null) {
            return null;
        }
        return EventConfiguration.RecipientSettings.builder()
                .enabled(dto.getEnabled() != null ? dto.getEnabled() : false)
                .channels(dto.getChannels())
                .method(dto.getMethod())
                .build();
    }

    private EventConfiguration.RecipientSettings mapRecipientSettings(
            UpdateEventConfigurationRequestDto.RecipientConfigDto dto) {
        if (dto == null) {
            return null;
        }
        return EventConfiguration.RecipientSettings.builder()
                .enabled(dto.getEnabled() != null ? dto.getEnabled() : false)
                .channels(dto.getChannels())
                .method(dto.getMethod())
                .build();
    }

    private EventConfigurationResponseDto.NotificationsConfigDto mapNotificationsConfigToResponse(
            EventConfiguration.NotificationsConfig config) {
        return EventConfigurationResponseDto.NotificationsConfigDto.builder()
                .dataPrincipal(mapRecipientSettingsToResponse(config.getDataPrincipal()))
                .dataFiduciary(mapRecipientSettingsToResponse(config.getDataFiduciary()))
                .dataProcessor(mapRecipientSettingsToResponse(config.getDataProcessor()))
                .dataProtectionOfficer(mapRecipientSettingsToResponse(config.getDataProtectionOfficer()))
                .build();
    }

    private EventConfigurationResponseDto.RecipientConfigDto mapRecipientSettingsToResponse(
            EventConfiguration.RecipientSettings settings) {
        if (settings == null) {
            return null;
        }
        return EventConfigurationResponseDto.RecipientConfigDto.builder()
                .enabled(settings.getEnabled())
                .channels(settings.getChannels())
                .method(settings.getMethod())
                .build();
    }

    private String generateConfigId() {
        return "EC_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}