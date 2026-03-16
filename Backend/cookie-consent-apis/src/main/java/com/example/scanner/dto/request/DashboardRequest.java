package com.example.scanner.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
@Schema(description = "Dashboard API request parameters")
public class DashboardRequest {

    @Schema(description = "Template ID (optional)")
    private String templateID;

    @Schema(description = "Scan ID (optional)")
    private String scanID;

    @Schema(description = "Template version (optional)")
    private Integer version;

    @Schema(description = "Start date filter (optional)")
    private LocalDateTime startDate;

    @Schema(description = "End date filter (optional)")
    private LocalDateTime endDate;

    @Schema(description = "Business ID (optional)")
    private String businessId;
}