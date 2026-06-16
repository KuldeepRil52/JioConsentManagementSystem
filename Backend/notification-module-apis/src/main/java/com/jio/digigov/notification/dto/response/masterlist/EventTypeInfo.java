package com.jio.digigov.notification.dto.response.masterlist;

import com.jio.digigov.notification.enums.EventType;
import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class EventTypeInfo {

    private EventType eventType;
    private String description;
    private String category;
    private boolean isConfigured;
    private int labelCount;
}