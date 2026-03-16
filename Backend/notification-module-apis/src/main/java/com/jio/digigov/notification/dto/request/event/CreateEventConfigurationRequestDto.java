package com.jio.digigov.notification.dto.request.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = "Request to create an event configuration")
public class CreateEventConfigurationRequestDto {
    
    @NotBlank(message = "Event type is required")
    @Schema(description = "Event type identifier", example = "CONSENT_GRANTED", required = true)
    private String eventType;
    
    @Schema(description = "Description of the event configuration")
    private String description;
    
    @NotNull(message = "Notifications configuration is required")
    @Valid
    @Schema(description = "Notification settings for different recipients", required = true)
    private NotificationsConfigDto notifications;
    
    @Schema(description = "Priority level", example = "HIGH", allowableValues = {"CRITICAL", "HIGH", "MEDIUM", "LOW"})
    private String priority = "MEDIUM";
    
    @Schema(description = "Whether the configuration is active", example = "true")
    private Boolean isActive = true;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Notification settings for different recipient types. At least one recipient must be configured.")
    public static class NotificationsConfigDto {

        @Schema(description = "Data principal notification settings - Individual whose data is being processed. Can only use channels (SMS, EMAIL), methods will be ignored.")
        private RecipientConfigDto dataPrincipal;

        @Schema(description = "Data fiduciary notification settings - Organization responsible for data protection. Can only use methods (CALLBACK), channels will be ignored.")
        private RecipientConfigDto dataFiduciary;

        @Schema(description = "Data processor notification settings - Third-party entities processing the data. Can only use methods (CALLBACK), channels will be ignored.")
        private RecipientConfigDto dataProcessor;

        @Schema(description = "Data Protection Officer (DPO) notification settings - DPO responsible for handling privacy compliance and grievances. Can only use channels (EMAIL), methods will be ignored. DPO email is fetched from dpo_configurations collection.")
        private RecipientConfigDto dataProtectionOfficer;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Configuration for a specific recipient type defining notification preferences")
    public static class RecipientConfigDto {

        @Schema(description = "Whether notifications are enabled for this recipient", example = "true", defaultValue = "false")
        private Boolean enabled = false;

        @Schema(description = "List of notification channels to use",
                example = "[\"SMS\", \"EMAIL\"]",
                allowableValues = {"SMS", "EMAIL"})
        private List<String> channels;

        @Schema(description = "Notification delivery method",
                example = "CALLBACK",
                allowableValues = {"CALLBACK"},
                nullable = true)
        private String method;
    }
}