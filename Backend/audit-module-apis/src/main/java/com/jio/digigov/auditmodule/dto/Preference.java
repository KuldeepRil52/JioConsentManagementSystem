package com.jio.digigov.auditmodule.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.jio.digigov.auditmodule.enumeration.PreferenceStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Preference {

    @Schema(hidden = true)
    private String preferenceId;

    @Schema(
            description = "Cookie category name. Must exist in Category table",
            example = "Analytics",
            required = true
    )
    @NotNull(message = "Purpose cannot be null")
    private String purpose;

    @Schema(
            description = "Whether this preference category is mandatory (true) or optional (false)",
            example = "false",
            required = true
    )
    private Boolean isMandatory;

    @Schema(
            description = "Validity period for this preference",
            required = true,
            implementation = Duration.class
    )
    @NotNull(message = "Preference validity is required")
    @Valid
    private Duration preferenceValidity;

    @Schema(hidden = true)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSS")
    private LocalDateTime startDate;

    @Schema(hidden = true)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSS")
    private LocalDateTime endDate;

    @Schema(hidden = true)
    private PreferenceStatus preferenceStatus;
}
