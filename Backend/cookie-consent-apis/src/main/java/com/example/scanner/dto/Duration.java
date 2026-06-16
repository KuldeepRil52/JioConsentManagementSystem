package com.example.scanner.dto;

import com.example.scanner.enums.Period;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Duration {

    @Schema(
            description = "Numeric value for the duration (must be positive)",
            example = "365",
            required = true
    )
    @NotNull(message = "Duration value is required")
    @Positive(message = "Duration value must be positive")
    int value;

    @Schema(
            description = "Unit of time: DAYS, MONTHS, or YEARS",
            example = "DAYS",
            allowableValues = {"DAYS", "MONTHS", "YEARS"},
            required = true
    )
    @NotNull(message = "Duration unit is required")
    Period unit;
}