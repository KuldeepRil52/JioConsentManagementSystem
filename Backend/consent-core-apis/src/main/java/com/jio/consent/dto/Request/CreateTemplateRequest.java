package com.jio.consent.dto.Request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.jio.consent.constant.ErrorCodes;
import com.jio.consent.dto.*;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class CreateTemplateRequest {

    @Schema(description = "Name of the template", example = "Template1")
    @NotBlank(message = ErrorCodes.JCMP1001)
    private String templateName;

    @Schema(description = "Business ID", example = "0c092ed7-e99d-4ef7-8b1f-a3898e788832")
    @NotBlank(message = ErrorCodes.JCMP1002)
    private String businessId;

    @Schema(description = "List of preferences")
    @NotEmpty(message = ErrorCodes.JCMP1003)
    @Valid
    private List<Preference> preferences;

    @Schema(description = "Multilingual content")
    @NotNull(message = ErrorCodes.JCMP1004)
    @Valid
    private Multilingual multilingual;

    @Schema(description = "UI configuration")
    @NotNull(message = ErrorCodes.JCMP1005)
    @Valid
    private UiConfig uiConfig;

    @Schema(description = "Privacy policy document (Base64)")
    @NotBlank(message = ErrorCodes.JCMP1050)
    private String privacyPolicyDocument;

    @Schema(description = "Privacy policy document metadata")
    @NotNull(message = ErrorCodes.JCMP1050)
    @Valid
    private DocumentMeta privacyPolicyDocumentMeta;

    @Schema(description = "Status of the template", example = "PUBLISHED", allowableValues = {"PUBLISHED", "DRAFT", "INACTIVE"})
    private TemplateStatus status;
    
    
}
