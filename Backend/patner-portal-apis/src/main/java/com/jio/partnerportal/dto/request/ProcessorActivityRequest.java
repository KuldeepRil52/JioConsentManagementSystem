package com.jio.partnerportal.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.jio.partnerportal.dto.DataCatagory;
import com.jio.partnerportal.dto.Status;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ProcessorActivityRequest {

    @Schema(description = "Name of the processor activity", example = "Data Transformation")
    @JsonProperty("activityName")
    private String activityName;
    @Schema(description = "Name of the associated data processor", example = "Analytics Processor")
    @JsonProperty("processorName")
    private String processorName;
    @Schema(description = "ID of the associated data processor", example = "a1b2c3d4-e5f6-7890-1234-567890abcdef")
    @JsonProperty("processorId")
    private String processorId;
    @Schema(description = "List of data categories involved in this activity")
    @JsonProperty("dataTypeList")
    private List<DataCatagory> dataTypeList;
    @Schema(description = "Details about the processor activity", example = "Transforms raw data into structured format.")
    @JsonProperty("details")
    private String details;
    @Schema(description = "Status of the processor activity", example = "ACTIVE", allowableValues = {"ACTIVE", "INACTIVE"})
    @JsonProperty("status")
    private Status status;

}
