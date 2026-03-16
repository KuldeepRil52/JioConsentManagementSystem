package com.jio.consent.dto;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.jio.consent.constant.ErrorCodes;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
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
public class UiConfig {

    @Schema(description = "Logo in Base64", example = "Base64ofPNG")
    @NotBlank(message = ErrorCodes.JCMP1014)
    private String logo;

    @Schema(description = "Logo document metadata")
    private DocumentMeta logoMeta;

    @Schema(description = "Theme", example = "eyrjtrhfggftrg")
    @NotBlank(message = ErrorCodes.JCMP1015)
    private String theme;

    @Schema(description = "Dark mode enabled", example = "false")
    @Builder.Default
    private boolean darkMode = false;
    @Schema(description = "Mobile view enabled", example = "true")
    @Builder.Default
    private boolean mobileView = false;

    @Schema(description = "Parental control enabled", example = "false")
    @Builder.Default
    private boolean parentalControl = false;

    @Schema(description = "Show data type", example = "true")
    @Builder.Default
    private boolean dataTypeToBeShown = false;

    @Schema(description = "Show data item", example = "true")
    @Builder.Default
    private boolean dataItemToBeShown = false;

    @Schema(description = "Show process activity name", example = "true")
    @Builder.Default
    private boolean processActivityNameToBeShown = false;

    @Schema(description = "Show processor name", example = "true")
    @Builder.Default
    private boolean processorNameToBeShown = false;

    @Schema(description = "Show validity", example = "true")
    @Builder.Default
    private boolean validitytoBeShown = false;
    
    @Schema(description = "Language specific typography settings map",
            example = "{\"typographySettings\":{\"ENGLISH\":{\"fontFile\":\"base64EncodedFontFile\",\"fontSize\":12,\"fontWeight\":12,\"fontStyle\":\"italic\"},\"HINDI\":{\"fontFile\":\"base64EncodedFontFile\",\"fontSize\":12,\"fontWeight\":12,\"fontStyle\":\"bold\"}}}")
//    @NotNull(message = ErrorCodes.JCMP1060)
//    @NotEmpty(message = ErrorCodes.JCMP1060)
//    @Valid
    private Map<LANGUAGE, LanguageTypographySettings> typographySettings;

}
