package com.jio.digigov.grievance.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

/**
 * Request DTO for creating a GrievanceType.
 */
@Data
public class GrievanceTypeCreateRequest {
    @NotBlank(message = "Grievance category is required")
    private String grievanceType;

    private List<String> grievanceItem;

    private String description;
}
