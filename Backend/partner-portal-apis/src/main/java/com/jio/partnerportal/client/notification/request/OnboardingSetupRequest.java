package com.jio.partnerportal.client.notification.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request for notification service onboarding setup")
public class OnboardingSetupRequest {

    @Schema(description = "Whether to create templates", example = "true")
    @JsonProperty("createTemplates")
    private boolean createTemplates;

    @Schema(description = "Whether to create event configurations", example = "true")
    @JsonProperty("createEventConfigurations")
    private boolean createEventConfigurations;

    @Schema(description = "Whether to create master list", example = "true")
    @JsonProperty("createMasterList")
    private boolean createMasterList;

    @Schema(description = "Description of the onboarding setup", example = "Complete onboarding with dynamically generated DLT IDs")
    @JsonProperty("description")
    private String description;

    @Schema(description = "Event DLT mappings for different notification events")
    @JsonProperty("eventDltMappings")
    private Map<String, DltMapping> eventDltMappings;

    @Schema(description = "Provider type for notification service", example = "DIGIGOV")
    @JsonProperty("providerType")
    private String providerType;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "DLT mapping for an event")
    public static class DltMapping {
        @Schema(description = "DLT Entity ID", example = "1001141761762155642")
        @JsonProperty("dltEntityId")
        private String dltEntityId;

        @Schema(description = "DLT Template ID", example = "1207171761762155642")
        @JsonProperty("dltTemplateId")
        private String dltTemplateId;
    }
}
