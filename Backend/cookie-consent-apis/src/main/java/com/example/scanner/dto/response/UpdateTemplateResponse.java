package com.example.scanner.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Response for template update operation")
public class UpdateTemplateResponse {

    @Schema(description = "Logical template ID (same across versions)", example = "eyXXX.EXAMPLE-TOKEN-NOT-REAL.xxxXXX.....")
    private String templateId;

    @Schema(description = "New document ID for this version", example = "eyXXX.EXAMPLE-TOKEN-NOT-REAL.xxxXXX....")
    private String newVersionId;

    @Schema(description = "New version number", example = "2")
    private Integer newVersion;

    @Schema(description = "Previous version number", example = "1")
    private Integer previousVersion;

    @Schema(description = "Success message")
    private String message;

    @Schema(description = "Timestamp when the update was processed")
    private Instant updatedAt;

    public static UpdateTemplateResponse success(String templateId, String newVersionId,
                                                 Integer newVersion, Integer previousVersion) {
        return UpdateTemplateResponse.builder()
                .templateId(templateId)
                .newVersionId(newVersionId)
                .newVersion(newVersion)
                .previousVersion(previousVersion)
                .message("Template updated successfully. New version " + newVersion + " created.")
                .updatedAt(Instant.now())
                .build();
    }
}