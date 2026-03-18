package com.jio.partnerportal.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class GrievanceDetails {

    @Schema(description = "List of grievance types supported", example = "[\"ACCESS\", \"CORRECTION\"]")
    @JsonProperty("grievanceTypes")
    private List<GrievanceType> grievanceTypes;
    @Schema(description = "Endpoint URL for grievance submission", example = "https://privacy.example.com/grievance")
    @JsonProperty("endpointUrl")
    private String endpointUrl;
    @Schema(description = "List of intake methods for grievances", example = "[\"WEB_FORM\", \"MOBILE_APP\"]")
    @JsonProperty("intakeMethods")
    private List<IntakeMethods> intakeMethods;
    @Schema(description = "Workflow stages for grievance processing", example = "[\"New\", \"In Progress\", \"Resolved\"]")
    @JsonProperty("workflow")
    private List<String> workflow;
    @Schema(description = "SLA timeline for grievance resolution")
    @JsonProperty("slaTimeline")
    private Duration slaTimeline;
    @Schema(description = "Escalation policy timeline for grievances")
    @JsonProperty("escalationPolicy")
    private Duration escalationPolicy;
    @Schema(description = "Retention policy for grievance data")
    @JsonProperty("retentionPolicy")
    private Duration retentionPolicy;
    @Schema(description = "Communication configuration for various grievance stages")
    @JsonProperty("communicationConfig")
    private CommunicationConfig communicationConfig;

}
