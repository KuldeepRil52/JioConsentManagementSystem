package com.jio.partnerportal.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.jio.partnerportal.entity.DataBreachReport;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Simplified response for create and update operations")
public class DataBreachReportSimpleResponse {

    @JsonProperty("incidentId")
    @Schema(description = "Unique incident identifier", example = "DB-2025-001")
    private String incidentId;

    @JsonProperty("status")
    @Schema(description = "Current status of the breach report", example = "INVESTIGATION")
    private DataBreachReport.BreachStatus status;
}

