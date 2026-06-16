package com.jio.digigov.fides.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.jio.digigov.fides.constant.ErrorCodes;
import com.jio.digigov.fides.enumeration.PreferenceStatus;
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
    @NotEmpty(message = ErrorCodes.JCMP1008)
    private List<PurposeDetails> purposeList;

    @Schema(description = "Is this preference mandatory?", example = "false")
    private boolean isMandatory;

    @Schema(description = "Should this preference auto-renew?", example = "false")
    private boolean autoRenew;

    @Schema(description = "Validity of the preference")
    @NotNull(message = ErrorCodes.JCMP1009)
    @Valid
    private Duration preferenceValidity;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS")
    private LocalDateTime startDate;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS")
    private LocalDateTime endDate;

    @Schema(description = "List of purpose activity IDs")
    @NotEmpty(message = ErrorCodes.JCMP1010)
    private List<ProcessorActivityDetails> processorActivityList;

    @Schema(description = "Status of the Preference", example = "ACCEPTED", allowableValues = {"ACCEPTED", "NOTACCEPTED", "EXPIRED"})
    @NotNull(message = ErrorCodes.JCMP1011)
    private PreferenceStatus preferenceStatus;

}
