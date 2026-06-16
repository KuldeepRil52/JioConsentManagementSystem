package com.example.scanner.dto.request;

import com.example.scanner.dto.DocumentMeta;
import com.example.scanner.dto.Multilingual;
import com.example.scanner.dto.Preference;
import com.example.scanner.dto.UiConfig;
import com.example.scanner.enums.TemplateStatus;
import com.fasterxml.jackson.annotation.JsonInclude;
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
@Schema(description = "Request to create template")
public class CreateTemplateRequest {

    @Schema(description = "Scan ID from completed cookie scan", example = "550e8400-XXXXXX.......", required = true)
    @NotBlank(message = "Scan ID is required and must be from a completed scan")
    private String scanId;

    @Schema(description = "Name of the template", example = "Template1")
    @NotBlank(message = "Template name is required and cannot be empty")
    private String templateName;

    @Schema(description = "List of preferences")
    @NotEmpty(message = "At least one preference is required")
    @Valid
    private List<Preference> preferences;

    @Schema(description = "Multilingual content")
    @NotNull(message = "Multilingual configuration is required")
    @Valid
    private Multilingual multilingual;

    @Schema(description = "UI configuration")
    @NotNull(message = "UI configuration is required")
    @Valid
    private UiConfig uiConfig;

    @Schema(description = "Privacy policy document (Base64)")
    private String privacyPolicyDocument;

    @Schema(description = "Privacy policy document metadata")
    private DocumentMeta documentMeta;

    @Schema(description = "Status of the template", example = "PUBLISHED")
    @NotNull(message = "Status is required")
    private TemplateStatus status;
}