package com.jio.digigov.notification.dto.request.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = "Request to create event configuration")
public class CreateEventConfigRequestDto {
    
    @NotBlank(message = "Event type is required")
    @Schema(description = "Event type for the configuration", example = "CONSENT_GRANTED", required = true)
    private String eventType;
    
    @NotNull(message = "Notification settings are required")
    @Valid
    @Schema(description = "Notification configuration settings", required = true)
    private NotificationSettingsDto notifications;
    
    @Schema(description = "Event priority level", example = "HIGH")
    private String priority = "MEDIUM";
    
    @Schema(description = "Whether configuration is active", example = "true")
    private Boolean isActive = true;
    
    @Data
    @Schema(description = "Notification settings for different recipient types")
    public static class NotificationSettingsDto {
        
        @Valid
        @Schema(description = "Data Principal notification settings")
        private DataPrincipalSettingsDto dataPrincipal;
        
        @Valid
        @Schema(description = "Data Fiduciary notification settings")
        private DataFiduciarySettingsDto dataFiduciary;
        
        @Valid
        @Schema(description = "Data Processor notification settings")
        private DataProcessorSettingsDto dataProcessor;
    }
    
    @Data
    @Schema(description = "Data Principal notification configuration")
    public static class DataPrincipalSettingsDto {
        
        @Schema(description = "Enable notifications to data principal", example = "true")
        private Boolean enabled = false;
        
        @Schema(description = "List of enabled notification channels")
        private List<ChannelSettingDto> channels;
    }
    
    @Data
    @Schema(description = "Data Fiduciary notification configuration")
    public static class DataFiduciarySettingsDto {
        
        @Schema(description = "Enable notifications to data fiduciary", example = "true")
        private Boolean enabled = false;
        
        @Schema(description = "Notification method", example = "CALLBACK")
        private String method = "CALLBACK";
    }
    
    @Data
    @Schema(description = "Data Processor notification configuration")
    public static class DataProcessorSettingsDto {
        
        @Schema(description = "Enable notifications to data processors", example = "true")
        private Boolean enabled = false;
        
        @Schema(description = "Notification method", example = "CALLBACK")
        private String method = "CALLBACK";
    }
    
    @Data
    @Schema(description = "Channel setting configuration")
    public static class ChannelSettingDto {
        
        @NotBlank(message = "Channel name is required")
        @Schema(description = "Channel name", example = "SMS", allowableValues = {"SMS", "EMAIL"})
        private String channel;
        
        @Schema(description = "Enable this channel", example = "true")
        private Boolean enabled = true;
    }
}