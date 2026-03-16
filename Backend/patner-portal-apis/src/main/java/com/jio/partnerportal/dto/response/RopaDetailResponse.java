package com.jio.partnerportal.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.jio.partnerportal.dto.Duration;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class RopaDetailResponse {

    @Schema(description = "MongoDB Object ID", example = "507f1f77bcf86cd799439011")
    @JsonProperty("id")
    private String id;

    @Schema(description = "ROPA record ID", example = "c3d4e5f6-g7h8-9012-3456-789012cdefgh")
    @JsonProperty("ropaId")
    private String ropaId;

    @Schema(description = "Business ID", example = "d4e5f6g7-h8i9-0123-4567-890123defghi")
    @JsonProperty("businessId")
    private String businessId;

    @Schema(description = "Process overview details")
    @JsonProperty("processOverview")
    private ProcessOverview processOverview;

    @Schema(description = "Categories of special nature data", example = "[\"Marital Relations\", \"Physical disabilities\"]")
    @JsonProperty("categoriesOfSpecialNature")
    private List<String> categoriesOfSpecialNature;

    @Schema(description = "Source of personal data", example = "[\"Recruitment agencies\"]")
    @JsonProperty("sourceOfPersonalData")
    private List<String> sourceOfPersonalData;

    @Schema(description = "Category of individual", example = "[\"Employment Candidates\"]")
    @JsonProperty("categoryOfIndividual")
    private List<String> categoryOfIndividual;

    @Schema(description = "Activity reason", example = "Explicit consent")
    @JsonProperty("activityReason")
    private String activityReason;

    @Schema(description = "Additional condition", example = "Employment")
    @JsonProperty("additionalCondition")
    private String additionalCondition;

    @Schema(description = "Case or purpose for exemption", example = "N/A")
    @JsonProperty("caseOrPurposeForExemption")
    private String caseOrPurposeForExemption;

    @Schema(description = "DPIA reference", example = "DPIA#00xx")
    @JsonProperty("dpiaReference")
    private String dpiaReference;

    @Schema(description = "Link or document reference")
    @JsonProperty("linkOrDocumentRef")
    private String linkOrDocumentRef;

    @Schema(description = "Business functions shared with", example = "[\"HR/Recruitment\"]")
    @JsonProperty("businessFunctionsSharedWith")
    private List<String> businessFunctionsSharedWith;

    @Schema(description = "Geographical locations", example = "[\"India\"]")
    @JsonProperty("geographicalLocations")
    private List<String> geographicalLocations;

    @Schema(description = "Third parties shared with", example = "[\"Recruitment agency [Name]\"]")
    @JsonProperty("thirdPartiesSharedWith")
    private List<String> thirdPartiesSharedWith;

    @Schema(description = "Contract references", example = "[\"Contract#00xx\"]")
    @JsonProperty("contractReferences")
    private List<String> contractReferences;

    @Schema(description = "Cross border flow", example = "false")
    @JsonProperty("crossBorderFlow")
    private boolean crossBorderFlow;

    @Schema(description = "Restricted transfer safeguards", example = "N/A")
    @JsonProperty("restrictedTransferSafeguards")
    private String restrictedTransferSafeguards;

    @Schema(description = "Administrative precautions", example = "Contractual obligations")
    @JsonProperty("administrativePrecautions")
    private String administrativePrecautions;

    @Schema(description = "Financial precautions", example = "Vendor audits")
    @JsonProperty("financialPrecautions")
    private String financialPrecautions;

    @Schema(description = "Technical precautions", example = "Internal data storage and retention, strict access control")
    @JsonProperty("technicalPrecautions")
    private String technicalPrecautions;

    @Schema(description = "Retention period")
    @JsonProperty("retentionPeriod")
    private Duration retentionPeriod;

    @Schema(description = "Storage location", example = "Shared drives and emails")
    @JsonProperty("storageLocation")
    private String storageLocation;

    @Schema(description = "Breach documentation", example = "N/A")
    @JsonProperty("breachDocumentation")
    private String breachDocumentation;

    @Schema(description = "Last breach date")
    @JsonProperty("lastBreachDate")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS")
    private LocalDateTime lastBreachDate;

    @Schema(description = "Breach summary")
    @JsonProperty("breachSummary")
    private String breachSummary;

    @Schema(description = "Created date")
    @JsonProperty("createdAt")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS")
    private LocalDateTime createdAt;

    @Schema(description = "Updated date")
    @JsonProperty("updatedAt")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS")
    private LocalDateTime updatedAt;

    @Data
    @Builder
    public static class ProcessOverview {
        @Schema(description = "Business function", example = "Human Resources")
        @JsonProperty("businessFunction")
        private String businessFunction;

        @Schema(description = "Department", example = "Human Resources")
        @JsonProperty("department")
        private String department;

        @Schema(description = "Process owner details")
        @JsonProperty("processOwner")
        private ProcessOwner processOwner;

        @Schema(description = "Processing activity name", example = "Recruitment")
        @JsonProperty("processingActivityName")
        private String processingActivityName;

        @Schema(description = "Purpose for processing", example = "To hire and recruit new employees")
        @JsonProperty("purposeForProcessing")
        private String purposeForProcessing;
    }

    @Data
    @Builder
    public static class ProcessOwner {
        @Schema(description = "Name", example = "John Doe")
        @JsonProperty("name")
        private String name;

        @Schema(description = "Mobile number", example = "+91-9876543210")
        @JsonProperty("mobile")
        private String mobile;

        @Schema(description = "Email address", example = "john.doe@company.com")
        @JsonProperty("email")
        private String email;
    }
}
