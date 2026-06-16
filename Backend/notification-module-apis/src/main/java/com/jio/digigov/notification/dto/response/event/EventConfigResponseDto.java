package com.jio.digigov.notification.dto.response.event;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@Schema(description = "Event configuration response")
public class EventConfigResponseDto {
    
    @Schema(description = "Configuration ID", example = "EC_20240120_001")
    private String configId;
    
    @Schema(description = "Event type", example = "CONSENT_GRANTED")
    private String eventType;
    
    @Schema(description = "Business ID", example = "business_001")
    private String businessId;
    
    @Schema(description = "Scope level", example = "BUSINESS")
    private String scopeLevel;
    
    @Schema(description = "Notification configuration settings")
    private NotificationSettingsResponse notifications;
    
    @Schema(description = "Event priority", example = "HIGH")
    private String priority;
    
    @Schema(description = "Whether configuration is active", example = "true")
    private Boolean isActive;
    
    @Schema(description = "Creation timestamp")
    private LocalDateTime createdAt;

    @Schema(description = "Last update timestamp")
    private LocalDateTime updatedAt;

    @Data
    @Builder
    @Schema(description = "Notification settings response")
    public static class NotificationSettingsResponse {
        
        @Schema(description = "Data Principal settings")
        private DataPrincipalSettingsResponse dataPrincipal;
        
        @Schema(description = "Data Fiduciary settings")
        private DataFiduciarySettingsResponse dataFiduciary;
        
        @Schema(description = "Data Processor settings")
        private DataProcessorSettingsResponse dataProcessor;
    }
    
    @Data
    @Builder
    @Schema(description = "Data Principal settings response")
    public static class DataPrincipalSettingsResponse {
        
        @Schema(description = "Whether notifications are enabled", example = "true")
        private Boolean enabled;
        
        @Schema(description = "Enabled channels")
        private List<ChannelSettingResponse> channels;
    }
    
    @Data
    @Builder
    @Schema(description = "Data Fiduciary settings response")
    public static class DataFiduciarySettingsResponse {
        
        @Schema(description = "Whether notifications are enabled", example = "true")
        private Boolean enabled;
        
        @Schema(description = "Notification method", example = "CALLBACK")
        private String method;
    }
    
    @Data
    @Builder
    @Schema(description = "Data Processor settings response")
    public static class DataProcessorSettingsResponse {
        
        @Schema(description = "Whether notifications are enabled", example = "true")
        private Boolean enabled;
        
        @Schema(description = "Notification method", example = "CALLBACK")
        private String method;
    }
    
    @Data
    @Builder
    @Schema(description = "Channel setting response")
    public static class ChannelSettingResponse {
        
        @Schema(description = "Channel name", example = "SMS")
        private String channel;
        
        @Schema(description = "Whether channel is enabled", example = "true")
        private Boolean enabled;
    }
}