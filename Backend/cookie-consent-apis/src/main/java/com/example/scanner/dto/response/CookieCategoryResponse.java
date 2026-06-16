package com.example.scanner.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.Date;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Response object for cookie category operations")
public class CookieCategoryResponse {

    @Schema(description = "Unique identifier of the cookie category", example = "550e8400-e29b-41d4-a716-446655440000")
    private String categoryId;

    @Schema(description = "Name of the cookie category", example = "Analytics")
    private String category;

    @Schema(description = "Description of the cookie category", example = "Cookies used for analytics")
    private String description;

    @Schema(description = "Indicates if this is a default category", example = "false")
    private boolean isDefault;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
    @Schema(description = "Timestamp when the category was created", example = "2024-01-15T10:30:00.000Z")
    private Date createdAt;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
    @Schema(description = "Timestamp when the category was last updated", example = "2024-01-15T10:30:00.000Z")
    private Date updatedAt;

    @Schema(description = "Response message", example = "Category added successfully")
    private String message;

    @Schema(description = "Indicates if the operation was successful", example = "true")
    private boolean success;
}