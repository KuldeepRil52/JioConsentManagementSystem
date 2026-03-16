package com.jio.partnerportal.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public enum Period {
    @Schema(description = "Days")
    DAYS,
    @Schema(description = "Months")
    MONTHS,
    @Schema(description = "Years")
    YEARS;
}
