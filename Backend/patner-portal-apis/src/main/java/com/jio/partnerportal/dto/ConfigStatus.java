package com.jio.partnerportal.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public enum ConfigStatus {
    @Schema(description = "Indicates that the configuration is enabled")
    ENABLED,
    @Schema(description = "Indicates that the configuration is disabled")
    DISABLED;
}
