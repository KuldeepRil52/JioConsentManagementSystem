package com.example.scanner.dto.response;

import com.example.scanner.dto.Multilingual;
import com.example.scanner.dto.UiConfig;
import com.example.scanner.dto.DocumentMeta;
import com.example.scanner.enums.TemplateStatus;
import com.example.scanner.enums.VersionStatus;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Template response with scanned cookies")
public class TemplateWithCookiesResponse {

    @Schema(description = "Document ID")
    private String id;

    @Schema(description = "Template ID")
    private String templateId;

    @Schema(description = "Scan ID")
    private String scanId;

    @Schema(description = "Template name")
    private String templateName;

    @Schema(description = "Business ID")
    private String businessId;

    @Schema(description = "Template status")
    private TemplateStatus status;

    @Schema(description = "Template version status")
    private VersionStatus templateStatus;

    @Schema(description = "Multilingual content")
    private Multilingual multilingual;

    @Schema(description = "UI configuration")
    private UiConfig uiConfig;

    @Schema(description = "Document metadata")
    private DocumentMeta documentMeta;

    @Schema(description = "Privacy policy document")
    private String privacyPolicyDocument;

    @Schema(description = "Preferences with scanned cookies")
    private List<PreferenceWithCookies> preferencesWithCookies;

    @Schema(description = "Template version")
    private Integer version;

    @Schema(description = "Created timestamp")
    @JsonFormat(shape = JsonFormat.Shape.STRING, timezone = "UTC")
    private Instant createdAt;

    @Schema(description = "Updated timestamp")
    @JsonFormat(shape = JsonFormat.Shape.STRING, timezone = "UTC")
    private Instant updatedAt;
}