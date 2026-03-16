package com.jio.digigov.grievance.dto.response;

import com.jio.digigov.grievance.enumeration.ScopeLevel;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Response DTO for GrievanceType.
 */
@Data
@Builder
public class GrievanceTypeResponse {
    private String grievanceTypeId;
    private String grievanceType;
    private List<String> grievanceItem;
    private String description;
    private ScopeLevel scope;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
