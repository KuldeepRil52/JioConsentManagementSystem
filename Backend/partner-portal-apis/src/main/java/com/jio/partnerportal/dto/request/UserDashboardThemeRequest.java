package com.jio.partnerportal.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserDashboardThemeRequest {

    @Schema(description = "Theme configuration as JSON string")
    @JsonProperty("theme")
    private String theme;
}

