package com.jio.digigov.grievance.dto.response;

import com.jio.digigov.grievance.dto.StatusTimeline;
import com.jio.digigov.grievance.dto.SupportingDoc;
import com.jio.digigov.grievance.enumeration.GrievanceStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Response DTO returned to clients for Grievance details.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GrievanceResponse {
    private String grievanceId;
    private String grievanceTemplateId;
    private String grievanceJwtToken;
    private GrievanceStatus status;
    private String message;
//    private LocalDateTime createdAt;
//    private LocalDateTime updatedAt;
}