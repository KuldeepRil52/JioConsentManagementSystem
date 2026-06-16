package com.jio.consent.dto.Request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.jio.consent.constant.ErrorCodes;
import com.jio.consent.dto.ConsentHandleRemarks;
import com.jio.consent.dto.CustomerIdentifiers;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
public class CreateHandleRequest {

    @NotBlank(message = ErrorCodes.JCMP1030)
    @Schema(description = "Name of the template", example = "UUID")
    private String templateId;
    
    @Schema(description = "Version of the template (optional - if not provided, latest version will be used)", example = "1")
    private Integer templateVersion;
    
    @NotNull(message = ErrorCodes.JCMP1032)
    @jakarta.validation.Valid
    @Schema(description = "Customer Details")
    private CustomerIdentifiers customerIdentifiers;
    
    @Schema(description = "Remarks for the consent handle", example = "DATA_FIDUCIARY")
    @Builder.Default
    private ConsentHandleRemarks remarks = ConsentHandleRemarks.DATA_FIDUCIARY;

}
