package com.jio.digigov.notification.dto.request.masterlist;

import com.jio.digigov.notification.dto.masterlist.MasterListEntry;
import com.jio.digigov.notification.enums.EventType;
import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotEmpty;
import java.util.Map;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class UpdateMasterListRequestDto {

    @NotNull(message = "Master list configuration is required")
    @NotEmpty(message = "Master list configuration cannot be empty")
    private Map<String, MasterListEntry> masterListConfig;

    private Map<EventType, Set<String>> eventMappings;

    private String description;
}