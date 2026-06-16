package com.example.scanner.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.validation.constraints.NotBlank;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request object for adding a new cookie category")
public class AddCookieCategoryRequest {

    @NotBlank(message = "Category is required")
    @Schema(
            description = "Name of the cookie category",
            example = "Analytics",
            required = true,
            minLength = 2,
            maxLength = 100
    )
    private String category;

    @NotBlank(message = "Description is required")
    @Schema(
            description = "Description of the cookie category",
            example = "Cookies used to track website analytics and user behavior",
            required = true,
            minLength = 5,
            maxLength = 500
    )
    private String description;
}
