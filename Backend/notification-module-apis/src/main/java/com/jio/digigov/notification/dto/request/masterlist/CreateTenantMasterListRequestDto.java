package com.jio.digigov.notification.dto.request.masterlist;

import com.jio.digigov.notification.dto.masterlist.MasterListEntry;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Request DTO for creating or updating tenant-specific master list configuration.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateTenantMasterListRequestDto {

    @NotNull(message = "Master list configuration is required")
    @NotEmpty(message = "Master list configuration cannot be empty")
    @Valid
    private Map<String, MasterListEntry> masterListConfig;

    private String description;
}