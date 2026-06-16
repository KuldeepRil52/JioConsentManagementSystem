package com.jio.digigov.notification.dto.request.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.Valid;
import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = "Request to update an event configuration")
public class UpdateEventConfigurationRequestDto {

    @Schema(description = "Description of the event configuration")
    private String description;

    @Valid
    @Schema(description = "Notification settings for different recipients")
    private NotificationsConfigDto notifications;

    @Schema(description = "Priority level", example = "HIGH", allowableValues = {"CRITICAL", "HIGH", "MEDIUM", "LOW"})
    private String priority;

    @Schema(description = "Whether the configuration is active", example = "true")
    private Boolean isActive;

    @Data
    @Schema(description = "Notification settings for different recipients")
    public static class NotificationsConfigDto {

        @Schema(description = "Data principal notification settings")
        private RecipientConfigDto dataPrincipal;

        @Schema(description = "Data fiduciary notification settings")
        private RecipientConfigDto dataFiduciary;

        @Schema(description = "Data processor notification settings")
        private RecipientConfigDto dataProcessor;

        @Schema(description = "Data Protection Officer (DPO) notification settings - Can only use channels (EMAIL)")
        private RecipientConfigDto dataProtectionOfficer;
    }

    @Data
    @Schema(description = "Recipient notification configuration")
    public static class RecipientConfigDto {

        @Schema(description = "Whether notifications are enabled for this recipient", example = "true")
        private Boolean enabled;

        @Schema(description = "Notification channels for data principal", example = "[\"SMS\", \"EMAIL\"]")
        private List<String> channels;

        @Schema(description = "Notification method for callbacks", example = "CALLBACK", allowableValues = {"CALLBACK"})
        private String method;
    }
}