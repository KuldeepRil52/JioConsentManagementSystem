package com.example.scanner.enums;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Lifecycle status of consent template")
public enum TemplateStatus {
    @Schema(description = "Template is published and available for use")
    PUBLISHED,

    @Schema(description = "Template is in draft state")
    DRAFT
}