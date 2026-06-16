package com.jio.digigov.auditmodule.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class LanguageSpecificContent {

    @Schema(description = "Description", example = "WhileusingJioMeet,youractivitiescreatedatawhichwillbeused...")
    private String description;

    @Schema(description = "Label", example = "Required")
    private String label;

    @Schema(description = "Rights text", example = "Towithdrawyourconsent,exerciseyourrights,orfilecomplaints...")
    private String rightsText;

    @Schema(description = "Permission text", example = "Byclicking'Allowall'...")
    private String permissionText;

}
