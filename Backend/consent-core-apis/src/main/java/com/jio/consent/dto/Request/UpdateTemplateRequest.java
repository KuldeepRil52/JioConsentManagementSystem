package com.jio.consent.dto.Request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.jio.consent.constant.ErrorCodes;
import com.jio.consent.dto.DocumentMeta;
import com.jio.consent.dto.Multilingual;
import com.jio.consent.dto.Preference;
import com.jio.consent.dto.TemplateStatus;
import com.jio.consent.dto.UiConfig;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
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
public class UpdateTemplateRequest {

    @NotNull(message = ErrorCodes.JCMP1004)
    @Valid
    private Multilingual multilingual;
    
    @Schema(description = "Flag to indicate if uiConfig is modified", example = "false")
    @JsonProperty("isUiConfigModified")
    @Builder.Default
    private boolean isUiConfigModified = false;
    
    @NotNull(message = ErrorCodes.JCMP1005)
    @Valid
    private UiConfig uiConfig;
    
    @NotEmpty(message = ErrorCodes.JCMP1003)
    @Valid
    private List<Preference> preferences;
    
    @NotNull(message = ErrorCodes.JCMP1006)
    private TemplateStatus status;
    
    @Schema(description = "Flag to indicate if privacy policy document is modified", example = "false")
    @JsonProperty("isPrivacyPolicyDocumentModified")
    @Builder.Default
    private boolean isPrivacyPolicyDocumentModified = false;
    
    @Schema(description = "Privacy policy document (Base64)", example = "abc")
    private String privacyPolicyDocument;
    
    @Schema(description = "Privacy policy document metadata")
    @Valid
    private DocumentMeta privacyPolicyDocumentMeta;

    @Schema(description = "Flag to indicate if logo is modified", example = "false")
    @JsonProperty("isLogoModified")
    @Builder.Default
    private boolean isLogoModified = false;

    @AssertTrue(message = ErrorCodes.JCMP1050)
    private boolean isValidPrivacyPolicyDocumentFields() {
        // If isPrivacyPolicyDocumentModified is true, then privacyPolicyDocument and privacyPolicyDocumentMeta must be provided
        if (isPrivacyPolicyDocumentModified) {
            return privacyPolicyDocument != null && !privacyPolicyDocument.trim().isEmpty() &&
                   privacyPolicyDocumentMeta != null;
        }
        return true; // If isPrivacyPolicyDocumentModified is false, validation passes
    }

}
