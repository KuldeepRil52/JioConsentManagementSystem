package com.jio.partnerportal.client.notification.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Response from notification service onboarding setup")
public class OnboardingSetupResponse {

    @Schema(description = "Success status", example = "true")
    @JsonProperty("success")
    private boolean success;

    @Schema(description = "Response message", example = "Onboarding setup completed successfully")
    @JsonProperty("message")
    private String message;

    @Schema(description = "Setup ID", example = "setup_123456789")
    @JsonProperty("setupId")
    private String setupId;

    @Schema(description = "Created templates count", example = "10")
    @JsonProperty("templatesCreated")
    private int templatesCreated;

    @Schema(description = "Created event configurations count", example = "10")
    @JsonProperty("eventConfigurationsCreated")
    private int eventConfigurationsCreated;

    @Schema(description = "Created master list entries count", example = "10")
    @JsonProperty("masterListEntriesCreated")
    private int masterListEntriesCreated;
}
