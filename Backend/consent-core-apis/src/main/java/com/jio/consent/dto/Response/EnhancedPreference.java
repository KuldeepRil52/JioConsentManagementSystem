package com.jio.consent.dto.Response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.jio.consent.dto.Duration;
import com.jio.consent.dto.PreferenceStatus;
import com.jio.consent.entity.ProcessorActivity;
import com.jio.consent.entity.Purpose;
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
public class EnhancedPreference {

    private String preferenceId;
    private List<Purpose> purposeIds;
    private boolean isMandatory;
    private boolean autoRenew;
    private Duration preferenceValidity;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private List<ProcessorActivity> processorActivityIds;
    private PreferenceStatus preferenceStatus;

}
