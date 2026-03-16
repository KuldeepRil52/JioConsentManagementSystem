package com.jio.schedular.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.jio.schedular.constant.ErrorCodes;
import com.jio.schedular.enums.PreferenceStatus;
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
public class HandlePreference {

    @Schema(hidden = true)
    private String preferenceId;

    @Schema(description = "List of purpose IDs", example = "[\"XADFDSA12\", \"ADFAD11232\"]")
    @NotEmpty(message = ErrorCodes.JCMP1001)
    private List<PurposeDetails> purposeList;

    @Schema(description = "Is this preference mandatory?", example = "false")
    private boolean isMandatory;

    @Schema(description = "Should this preference auto-renew?", example = "false")
    private boolean autoRenew;

    @Schema(description = "Validity of the preference")
    @NotNull(message = ErrorCodes.JCMP1002)
    @Valid
    private Duration preferenceValidity;

    private LocalDateTime startDate;

    private LocalDateTime endDate;

    @Schema(description = "List of purpose activity IDs")
    @NotEmpty(message = ErrorCodes.JCMP1003)
    private List<ProcessorActivityDetails> processorActivityList;

    @Schema(description = "Status of the Preference", example = "ACCEPTED", allowableValues = {"ACCEPTED", "NOTACCEPTED", "EXPIRED"})
    @NotNull(message = ErrorCodes.JCMP1004)
    private PreferenceStatus preferenceStatus;

}
