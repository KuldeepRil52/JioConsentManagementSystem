package com.example.scanner.enums;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Layout type for cookie consent banner")
public enum LayoutType {
    @Schema(description = "Modal overlay layout")
    MODAL,

    @Schema(description = "Tooltip layout")
    TOOLTIP,

    @Schema(description = "Banner layout")
    BANNER
}