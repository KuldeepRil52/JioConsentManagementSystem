package com.example.scanner.dto.request;

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
@Schema(description = "Request object for updating an existing cookie category")
public class UpdateCookieCategoryRequest {

    @NotBlank(message = "Category is required")
    @Schema(
            description = "Name of the cookie category to update",
            example = "Analytics",
            required = true,
            minLength = 2,
            maxLength = 100
    )
    private String category;

    @NotBlank(message = "Description is required")
    @Schema(
            description = "Updated description of the cookie category",
            example = "Updated description for analytics cookies",
            required = true,
            minLength = 5,
            maxLength = 500
    )
    private String description;
}
