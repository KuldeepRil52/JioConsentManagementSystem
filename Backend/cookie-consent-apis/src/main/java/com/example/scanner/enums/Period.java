package com.example.scanner.enums;


import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Time period unit for duration calculations")
public enum Period {
    @Schema(description = "Duration in days")
    DAYS,

    @Schema(description = "Duration in months")
    MONTHS,

    @Schema(description = "Duration in years")
    YEARS
}
