package com.jio.digigov.notification.dto.request.masterlist;

import com.jio.digigov.notification.enums.EventType;
import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.validation.constraints.NotNull;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ResolutionTestRequestDto {

    @NotNull(message = "Event payload is required")
    private Map<String, Object> eventPayload;

    @NotNull(message = "Event type is required")
    private EventType eventType;
}