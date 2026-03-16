package com.jio.digigov.notification.dto.request.masterlist;

import com.jio.digigov.notification.enums.EventType;
import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotEmpty;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class MasterListValidationRequestDto {

    @NotNull(message = "Event type is required")
    private EventType eventType;

    @NotNull(message = "Master labels are required")
    @NotEmpty(message = "Master labels cannot be empty")
    private Set<String> masterLabels;
}