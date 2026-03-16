package com.jio.schedular.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.jio.schedular.enums.PreferenceStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class ConsentPreference {

    private String preferenceId;
    private List<String> purposeIds;
    private boolean isMandatory;
    private boolean autoRenew;
    private Duration consentValidity;
    private LocalDate startDate;
    private LocalDate endDate;
    private List<String> purposeActivityIds;
    private PreferenceStatus preferenceStatus;

}
