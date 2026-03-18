package com.jio.partnerportal.dto.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.jio.partnerportal.entity.DataBreachReport;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request body for creating Data Breach Report")
public class DataBreachReportRequest {

    @JsonProperty("incidentDetails")
    @Schema(description = "Incident details")
    private IncidentDetails incidentDetails;

    @JsonProperty("dataInvolved")
    @Schema(description = "Data involved in the breach")
    private DataInvolved dataInvolved;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Incident details")
    public static class IncidentDetails {
        @Schema(description = "Date and time when the breach was discovered", example = "2025-10-30T10:00:00Z")
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
        private LocalDateTime discoveryDateTime;

        @Schema(description = "Date and time when the breach occurred", example = "2025-10-29T22:00:00Z")
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
        private LocalDateTime occurrenceDateTime;

        @Schema(description = "Type of breach")
        private DataBreachReport.BreachType breachType;

        @Schema(description = "Brief description of the breach", example = "Email with user data sent to unintended recipient")
        private String briefDescription;

        @Schema(description = "List of affected systems or services", example = "[\"Consent API\", \"Consent API1\"]")
        private List<String> affectedSystemOrService;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Data involved in the breach")
    public static class DataInvolved {
        @Schema(description = "Categories of personal data involved", example = "[\"Name\", \"Contact Info\", \"Consent Logs\"]")
        private List<String> personalDataCategories;

        @Schema(description = "Number of affected data principals", example = "500")
        private Integer affectedDataPrincipalsCount;

        @Schema(description = "Whether the data was encrypted or protected", example = "true")
        private Boolean dataEncryptedOrProtected;

        @Schema(description = "Description of potential impact", example = "Minor risk of unauthorized disclosure.")
        private String potentialImpactDescription;
    }
}
