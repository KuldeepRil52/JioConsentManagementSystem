package com.jio.consent.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.jio.consent.constant.ErrorCodes;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class Preference {

    @Schema(hidden = true)
    private String preferenceId;

    @Schema(description = "List of purpose IDs", example = "[\"UUID_1\", \"UUID_2\"]")
    @NotEmpty(message = ErrorCodes.JCMP1008)
    private List<String> purposeIds;

    @Schema(description = "Is this preference mandatory?", example = "false")
    private boolean isMandatory;

    @Schema(description = "Should this preference auto-renew?", example = "false")
    private boolean autoRenew;

    @Schema(description = "Validity of the preference")
    @NotNull(message = ErrorCodes.JCMP1009)
    @Valid
    private Duration preferenceValidity;

    @Schema(hidden = true)
    private LocalDateTime startDate;

    @Schema(hidden = true)
    private LocalDateTime endDate;

    @Schema(description = "List of purpose activity IDs", example = "[\"UUID_1\", \"UUID_2\"]")
    @NotEmpty(message = ErrorCodes.JCMP1010)
    private List<String> processorActivityIds;

    @Schema(hidden = true)
//    @Schema(description = "Status of the Preference", example = "ACCEPTED", allowableValues = {"ACCEPTED", "NOTACCEPTED", "EXPIRED"})
//    @NotNull(message = ErrorCodes.JCMP1020)
    private PreferenceStatus preferenceStatus;

}
