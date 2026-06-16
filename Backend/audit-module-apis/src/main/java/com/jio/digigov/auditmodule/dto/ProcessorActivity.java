package com.jio.digigov.auditmodule.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class ProcessorActivity {

    private ObjectId id;
    @Schema(description = "Unique identifier for the processor activity", example = "pa12345")
    @JsonProperty("processorActivityId")
    private String processorActivityId;
    @Schema(description = "Name of the processor activity", example = "Data Archiving")
    @JsonProperty("activityName")
    private String activityName;
    @Schema(description = "Name of the associated data processor", example = "Storage Processor")
    @JsonProperty("processorName")
    private String processorName;
    @Schema(description = "ID of the associated data processor", example = "proc123")
    @JsonProperty("processorId")
    private String processorId;
    @Schema(description = "List of data categories involved in this activity")
    @JsonProperty("dataTypesList")
    private List<DataCategory> dataTypesList;
    @Schema(description = "Details about the processor activity", example = "Archiving data as per retention policy.")
    @JsonProperty("details")
    private String details;
    @Schema(description = "ID of the business associated with the processor activity", example = "yourBusinessId")
    @JsonProperty("businessId")
    private String businessId;
    @Schema(description = "Scope type of the processor activity", example = "GLOBAL", allowableValues = {"GLOBAL", "BUSINESS", "INDIVIDUAL"})
    @JsonProperty("scopeType")
    private String scopeType;
    @Schema(description = "Status of the processor activity", example = "ACTIVE", allowableValues = {"ACTIVE", "INACTIVE"})
    @JsonProperty("status")
    private String status;@Schema(description = "Processor Activity version", example = "1")
    @JsonProperty("version")
    private int version;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
