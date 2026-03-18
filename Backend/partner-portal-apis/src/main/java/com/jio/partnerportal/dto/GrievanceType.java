package com.jio.partnerportal.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public enum GrievanceType {

    @Schema(description = "Grievance related to access requests")
    ACCESS,
    @Schema(description = "Grievance related to correction requests")
    CORRECTION,
    @Schema(description = "Grievance related to deletion requests")
    DELETION,
    @Schema(description = "Grievance related to objection requests")
    OBJECTION;
}
