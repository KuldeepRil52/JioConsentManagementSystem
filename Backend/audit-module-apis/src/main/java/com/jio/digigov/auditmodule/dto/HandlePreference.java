package com.jio.digigov.auditmodule.dto;


import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.jio.digigov.auditmodule.enumeration.PreferenceStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
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

    private String preferenceId;
    private List<PurposeDetails> purposeList;
    private boolean isMandatory;
    private boolean autoRenew;
    @Valid
    private Duration preferenceValidity;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS")
    private LocalDateTime startDate;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS")
    private LocalDateTime endDate;

    @Schema(description = "List of purpose activity IDs")
    private List<ProcessorActivityDetails> processorActivityList;

    @Schema(description = "Status of the Preference", example = "ACCEPTED", allowableValues = {"ACCEPTED", "NOTACCEPTED", "EXPIRED"})
    private PreferenceStatus preferenceStatus;

}
