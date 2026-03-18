package com.jio.partnerportal.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ConsentDetails {

    @Schema(description = "Indicates if Digilocker integration is enabled", example = "true")
    @JsonProperty("isDigilockerIntegration")
    private boolean isDigilockerIntegration;
    @Schema(description = "Indicates if Apaar integration is enabled", example = "false")
    @JsonProperty("isApaarIntegration")
    private boolean isApaarIntegration;
    @Schema(description = "Preferred language for consent communication", example = "en-US")
    @JsonProperty("preferredLanguage")
    private String preferredLanguage;
    @Schema(description = "Duration for artefact retention")
    @JsonProperty("artefactRetention")
    private Duration artefactRetention;
    @Schema(description = "Duration for log retention")
    @JsonProperty("logRetention")
    private Duration logRetention;
    @Schema(description = "Indicates if multilingual support is enabled", example = "true")
    @JsonProperty("isMultilingualSupport")
    private boolean isMultilingualSupport;
    @Schema(description = "Indicates if blockchain is enabled", example = "false")
    @JsonProperty("isBlockchainEnabled")
    @Builder.Default
    private boolean isBlockchainEnabled = false;
    @Schema(description = "Duration for auto-renewal reminder period")
    @JsonProperty("autoRenewalReminderPeriod")
    private Duration autoRenewalReminderPeriod;

}
