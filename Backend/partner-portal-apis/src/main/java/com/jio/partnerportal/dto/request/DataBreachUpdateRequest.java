package com.jio.partnerportal.dto.request;

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
@Schema(description = "Request body for updating Data Breach Report (only status and remarks can be updated)")
public class DataBreachUpdateRequest {

    @JsonProperty("status")
    @Schema(description = "New status for the breach report")
    private DataBreachReport.BreachStatus status;

    @JsonProperty("remarks")
    @Schema(description = "Remarks for the status change", example = "Forwarded to municipal officer")
    private String remarks;
}

