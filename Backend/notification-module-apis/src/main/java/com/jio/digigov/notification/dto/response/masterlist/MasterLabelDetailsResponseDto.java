package com.jio.digigov.notification.dto.response.masterlist;

import com.jio.digigov.notification.dto.masterlist.MasterListEntry;
import com.jio.digigov.notification.enums.EventType;
import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class MasterLabelDetailsResponseDto {

    private String labelName;
    private MasterListEntry entry;
    private Set<EventType> associatedEvents;
    private String description;
}