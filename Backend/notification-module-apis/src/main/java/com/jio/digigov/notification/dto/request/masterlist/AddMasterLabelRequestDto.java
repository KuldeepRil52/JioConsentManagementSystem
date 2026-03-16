package com.jio.digigov.notification.dto.request.masterlist;

import com.jio.digigov.notification.dto.masterlist.MasterListEntry;
import com.jio.digigov.notification.enums.EventType;
import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.Valid;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class AddMasterLabelRequestDto {

    @NotBlank(message = "Label name is required")
    private String labelName;

    @NotNull(message = "Master list entry is required")
    @Valid
    private MasterListEntry entry;

    private Set<EventType> events;
}