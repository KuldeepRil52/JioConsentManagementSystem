package com.jio.digigov.fides.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.jio.digigov.fides.constant.ErrorCodes;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
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
    @NotBlank(message = ErrorCodes.JCMP1019)
    private String description;

    @Schema(description = "Label", example = "Required")
    @NotBlank(message = ErrorCodes.JCMP1020)
    private String label;

    @Schema(description = "Rights text", example = "Towithdrawyourconsent,exerciseyourrights,orfilecomplaints...")
    @NotBlank(message = ErrorCodes.JCMP1021)
    private String rightsText;

    @Schema(description = "Permission text", example = "Byclicking'Allowall'...")
    @NotBlank(message = ErrorCodes.JCMP1022)
    private String permissionText;

}
