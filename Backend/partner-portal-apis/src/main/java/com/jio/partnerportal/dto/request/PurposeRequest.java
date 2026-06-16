package com.jio.partnerportal.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PurposeRequest {

    @Schema(description = "Code for the purpose", example = "MKTG_CAMPAIGN")
    @JsonProperty("purposeCode")
    private String purposeCode;
    @Schema(description = "Name of the purpose", example = "Marketing Campaign Data")
    @JsonProperty("purposeName")
    private String purposeName;
    @Schema(description = "Description of the purpose", example = "Collecting data for targeted marketing campaigns.")
    @JsonProperty("purposeDescription")
    private String purposeDescription;

}
