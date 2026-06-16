package com.example.scanner.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Response after successfully creating a template")
public class TemplateResponse {

    @Schema(
            description = "Logical template ID (remains same across versions)",
            example = "eyXXX.EXAMPLE-TOKEN-NOT-REAL.xxxXXX-......"
    )
    private String templateId;
    private String message;

    public TemplateResponse(String templateId, String message) {
        this.templateId = templateId;
        this.message = message;
    }
}
