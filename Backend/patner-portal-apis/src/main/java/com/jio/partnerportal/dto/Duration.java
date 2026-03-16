package com.jio.partnerportal.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class Duration {

    @Schema(description = "Value of the duration", example = "5")
    @JsonProperty("value")
    int value;
    @Schema(description = "Unit of the duration")
    @JsonProperty("unit")
    Period unit;
}
