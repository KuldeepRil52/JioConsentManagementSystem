package com.jio.partnerportal.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RopaUpdateResponse {

    @Schema(description = "ROPA record ID", example = "a1b2c3d4-e5f6-7890-1234-567890abcdef")
    @JsonProperty("ropaId")
    private String ropaId;

    @Schema(description = "Success message", example = "ROPA record updated successfully!")
    @JsonProperty("message")
    private String message;
}
