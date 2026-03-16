package com.example.scanner.dto;

import java.util.Map;

import com.example.scanner.enums.LANGUAGE;
import com.example.scanner.enums.LayoutType;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
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
public class UiConfig {

    @Schema(
            description = "Logo image as Base64 encoded string (PNG/JPG/SVG)",
            example = "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAUA...",
            required = true
    )
    @NotBlank(message = "Logo is required and cannot be empty")
    private String logo;


    @Schema(
            description = "Theme configuration as JSON string",
            example = "{\"primaryColor\":\"#0066cc\",\"secondaryColor\":\"#ffffff\",\"fontFamily\":\"Arial\",\"fontSize\":\"14px\"}",
            required = true
    )
    @NotBlank(message = "Theme is required and cannot be empty")
    private String theme;

    @Schema(description = "Enable dark mode", example = "false")
    private boolean darkMode;

    @Schema(description = "Optimize for mobile devices", example = "true")
    private boolean mobileView;

    @Schema(description = "Enable parental control features", example = "false")
    private boolean parentalControl;

    @Schema(description = "Show data type in UI", example = "true")
    private boolean dataTypeToBeShown;

    @Schema(description = "Show data items in UI", example = "true")
    private boolean dataItemToBeShown;

    @Schema(description = "Show process activity names in UI", example = "true")
    private boolean processActivityNameToBeShown;

    @Schema(description = "Show processor names in UI", example = "true")
    private boolean processorNameToBeShown;

    @Schema(description = "Show validity period in UI", example = "true")
    private boolean validitytoBeShown;

    @Schema(description = "Layout type for consent banner", example = "MODAL")
    private LayoutType layoutType;

    @Schema(description = "Title for manage preferences section", example = "Manage Your Cookie Preferences")
    private String managePreferencesTitle;

    @Schema(description = "Description for manage preferences section", example = "Choose which categories of cookies you allow. You can adjust these anytime.")
    private String managePreferencesDescription;

    @Schema(description = "Button label configuration")
    @Valid
    private ButtonLabels buttonLabels;
    
    @Schema(description = "Language specific typography settings map",
            example = "{\"typographySettings\":{\"ENGLISH\":{\"fontFile\":\"base64EncodedFontFile\",\"fontSize\":12,\"fontWeight\":12,\"fontStyle\":\"italic\"},\"HINDI\":{\"fontFile\":\"base64EncodedFontFile\",\"fontSize\":12,\"fontWeight\":12,\"fontStyle\":\"bold\"}}}")
    @Valid
    private Map<LANGUAGE, LanguageTypographySettings> typographySettings;
}