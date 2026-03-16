package com.example.scanner.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ButtonLabels {

    @Schema(description = "Label for Accept All button", example = "Accept All")
    private String acceptAll;

    @Schema(description = "Label for Reject All button", example = "Reject All")
    private String rejectAll;

    @Schema(description = "Label for Save button", example = "Save Preferences")
    private String save;

    @Schema(description = "Label for Manage Preferences button", example = "Manage Preferences")
    private String managePreferences;

    @Schema(description = "Label for Save Preferences button", example = "Save Preferences")
    private String savePreferences;
}