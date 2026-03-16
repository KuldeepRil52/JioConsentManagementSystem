package com.example.scanner.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
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
public class LanguageSpecificContent {

    @Schema(
            description = "Main description explaining data collection and usage",
            example = "We use cookies and similar technologies to provide you with a better user experience, analyze site traffic, and serve personalized content.",
            required = true
    )
    @NotBlank(message = "Description is required and cannot be empty")
    private String description;

    @Schema(
            description = "Label for the preference category",
            example = "Essential Cookies",
            required = true
    )
    @NotBlank(message = "Label is required and cannot be empty")
    private String label;

    @Schema(
            description = "Text explaining user rights regarding data and consent",
            example = "You have the right to withdraw your consent at any time. Click 'Manage Preferences' to update your choices.",
            required = true
    )
    @NotBlank(message = "Rights text is required and cannot be empty")
    private String rightsText;

    @Schema(
            description = "Permission text displayed when user grants consent",
            example = "By clicking 'Accept All', you consent to the use of cookies as described in our Privacy Policy.",
            required = true
    )
    @NotBlank(message = "Permission text is required and cannot be empty")
    private String permissionText;

    @Schema(
            description = "Title",
            example = "This site uses cookies to make your experience better.",
            required = true
    )
    @NotBlank(message = "Rights text is required and cannot be empty")
    private String title;
}