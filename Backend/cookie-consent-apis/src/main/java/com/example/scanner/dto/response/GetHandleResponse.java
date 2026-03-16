package com.example.scanner.dto.response;

import com.example.scanner.dto.CustomerIdentifiers;
import com.example.scanner.dto.Multilingual;
import com.example.scanner.dto.UiConfig;
import com.example.scanner.enums.ConsentHandleStatus;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Consent handle details with associated template information")
public class GetHandleResponse {
    @Schema(
            description = "Consent handle ID",
            example = "handle_123e4567-......."
    )
    private String consentHandleId;

    @Schema(
            description = "Template ID associated with this handle",
            example = "tpl_123e4567-e89b-1XXXX..."
    )
    private String templateId;

    @Schema(
            description = "Template name",
            example = "Main Website Cookie Template"
    )
    private String templateName;

    @Schema(
            description = "Template version number",
            example = "1"
    )
    private int templateVersion;

    @Schema(
            description = "URL associated with the consent handle",
            example = "https://example.com"
    )
    private String url;

    @Schema(
            description = "Business ID",
            example = "eyXXX.EXAMPLE-TOKEN-NOT-REAL.xxxXXX-e9XXX...."
    )
    private String businessId;

    @Schema(
            description = "Multilingual content configuration",
            implementation = Multilingual.class
    )
    private Multilingual multilingual;

    @Schema(description = "UI configuration for consent banner",
            implementation = UiConfig.class)
    private UiConfig uiConfig;

    @Schema(
            description = "List of preference configurations with associated cookies",
            implementation = PreferenceWithCookies.class
    )
    private List<PreferenceWithCookies> preferences;

    @Schema(
            description = "Customer identification details",
            implementation = CustomerIdentifiers.class
    )
    private CustomerIdentifiers customerIdentifiers;

    @Schema(
            description = "Current status of the handle",
            example = "ACTIVE"
    )
    private ConsentHandleStatus status;

}