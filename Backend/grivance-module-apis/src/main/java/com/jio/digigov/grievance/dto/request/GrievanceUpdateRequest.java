package com.jio.digigov.grievance.dto.request;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.jio.digigov.grievance.enumeration.GrievanceStatus;
import com.jio.digigov.grievance.enumeration.GrievanceUpdateStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for updating the status or remarks of an existing Grievance.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonPropertyOrder(alphabetic=true)
public class GrievanceUpdateRequest {
    private GrievanceUpdateStatus status;            // NEW, IN_PROGRESS, RESOLVED, ESCALATED, REJECTED
    private String resolutionRemark;  // Remarks for the status update (by officer)
}
