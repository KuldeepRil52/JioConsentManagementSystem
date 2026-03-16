package com.jio.digigov.notification.dto.response.masterlist;

import com.jio.digigov.notification.dto.masterlist.MasterListEntry;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Response DTO for tenant master list configuration.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TenantMasterListResponseDto {

    private String id;
    private Map<String, MasterListEntry> masterListConfig;
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Integer entryCount;
}