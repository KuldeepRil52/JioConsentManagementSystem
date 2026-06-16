package com.example.scanner.dto.request;

import com.example.scanner.dto.*;
import com.example.scanner.enums.TemplateStatus;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "Request to update an existing template (creates new version)")
public class UpdateTemplateRequest {

    @Schema(description = "Updated template name", example = "Updated Cookie Consent Template v2")
    private String templateName;

    @Schema(description = "Updated template status")
    private TemplateStatus status;

    @Schema(description = "Updated multilingual configuration")
    private Multilingual multilingual;

    @Schema(description = "Updated UI configuration")
    private UiConfig uiConfig;

    @Schema(description = "Updated privacy policy document URL or content")
    private String privacyPolicyDocument;

    @Schema(description = "Updated privacy policy document metadata")
    private DocumentMeta documentMeta;

    @Schema(description = "Updated list of preferences")
    private List<Preference> preferences;

    // Validation method to ensure at least one field is provided for update
    public boolean hasUpdates() {
        return templateName != null ||
                status != null ||
                multilingual != null ||
                uiConfig != null ||
                privacyPolicyDocument != null ||
                documentMeta != null ||
                preferences != null;
    }
}