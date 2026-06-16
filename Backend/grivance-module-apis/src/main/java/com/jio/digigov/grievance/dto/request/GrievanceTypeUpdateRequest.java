package com.jio.digigov.grievance.dto.request;

import lombok.Data;

import java.util.List;

/**
 * Request DTO for updating a GrievanceType.
 */
@Data
public class GrievanceTypeUpdateRequest {
    private String grievanceType;
    private List<String> grievanceItem;
    private String description;
}
