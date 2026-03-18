package com.jio.partnerportal.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.jio.partnerportal.dto.Duration;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;

@Document(collection = "ropa_records")
@Data
@Builder
@EqualsAndHashCode(callSuper = false)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class RopaRecord extends AbstractEntity {

    @Id
    @JsonProperty("id")
    private ObjectId id;
    
    @Schema(description = "Unique identifier for the ROPA record", example = "a1b2c3d4-e5f6-7890-1234-567890abcdef")
    @JsonProperty("ropaId")
    private String ropaId;
    
    @Indexed
    @Schema(description = "ID of the business associated with the ROPA record", example = "yourBusinessId")
    @JsonProperty("businessId")
    private String businessId;

    @Schema(description = "Processing activity ID", example = "a1b2c3d4-e5f6-7890-1234-567890abcdef")
    @JsonProperty("processingActivityId")
    private String processingActivityId;

    @Schema(description = "Purpose for processing ID", example = "a1b2c3d4-e5f6-7890-1234-567890abcdef")
    @JsonProperty("purposeForProcessingId")
    private String purposeForProcessingId;

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

}
