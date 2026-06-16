package com.jio.digigov.notification.dto.response.event;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@Schema(description = "Event configuration response")
public class EventConfigurationResponseDto {

    @Schema(description = "Configuration ID", example = "EC_20240120_001")
    private String configId;

    @Schema(description = "Business ID", example = "BUS_001")
    private String businessId;

    @Schema(description = "Event type", example = "CONSENT_GRANTED")
    private String eventType;

    @Schema(description = "Notification settings for different recipients")
    private NotificationsConfigDto notifications;

    @Schema(description = "Priority level", example = "HIGH")
    private String priority;

    @Schema(description = "Whether the configuration is active", example = "true")
    private Boolean isActive;

    @Schema(description = "Creation timestamp")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;

    @Schema(description = "Last update timestamp")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime updatedAt;

    @Data
    @Builder
    @Schema(description = "Notification settings for different recipients")
    public static class NotificationsConfigDto {

        @Schema(description = "Data principal notification settings")
        private RecipientConfigDto dataPrincipal;

        @Schema(description = "Data fiduciary notification settings")
        private RecipientConfigDto dataFiduciary;

        @Schema(description = "Data processor notification settings")
        private RecipientConfigDto dataProcessor;

        @Schema(description = "Data Protection Officer (DPO) notification settings")
        private RecipientConfigDto dataProtectionOfficer;
    }

    @Data
    @Builder
    @Schema(description = "Recipient notification configuration")
    public static class RecipientConfigDto {

        @Schema(description = "Whether notifications are enabled for this recipient", example = "true")
        private Boolean enabled;

        @Schema(description = "Notification channels", example = "[\"SMS\", \"EMAIL\"]")
        private List<String> channels;

        @Schema(description = "Notification method", example = "CALLBACK")
        private String method;
    }
}