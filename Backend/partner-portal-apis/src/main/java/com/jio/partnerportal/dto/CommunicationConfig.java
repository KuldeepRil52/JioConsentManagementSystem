package com.jio.partnerportal.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class
CommunicationConfig {

    @Schema(description = "SMS", example = "true", allowableValues = {"true", "false"})
    @JsonProperty("sms")
    private boolean sms;

    @Schema(description = "EMAIL", example = "true", allowableValues = {"true", "false"})
    @JsonProperty("email")
    private boolean email;

}
