package com.example.scanner.dto.request;

import com.example.scanner.dto.CustomerIdentifiers;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CreateHandleCodeRequest {

    @Schema(
            description = "Logical template ID",
            example = "tpl_123e4567-CXXXXXX.....",
            required = true
    )
    @NotBlank(message = "Template ID is required")
    private String templateId;

    @Schema(
            description = "Customer identification information",
            required = true,
            implementation = CustomerIdentifiers.class
    )
    @NotNull(message = "Customer identifiers are required")
    @Valid
    private CustomerIdentifiers customerIdentifiers;

    @Schema(
            description = "Optional URL for the consent handle",
            example = "https://example.com",
            required = false
    )
    @Pattern(
            regexp = "^(https?://)[a-zA-Z0-9.-]+(:[0-9]{1,5})?(/.*)?$",
            message = "URL must be a valid HTTP or HTTPS URL"
    )
    private String url;
}