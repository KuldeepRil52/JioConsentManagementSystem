package com.jio.digigov.grievance.dto.response;

import com.jio.digigov.grievance.dto.StatusTimeline;
import com.jio.digigov.grievance.dto.SupportingDoc;
import com.jio.digigov.grievance.enumeration.GrievanceStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Full detailed view of a grievance (for GET by ID).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GrievanceDetailResponse {

    private String grievanceId;
    private String grievanceTemplateId;
    private String grievanceJwtToken;

    private String userType;
    private Map<String, Object> userDetails;

    private String grievanceType;
    private String grievanceDetail;
    private String grievanceDescription;

    private List<SupportingDoc> supportingDocs = new ArrayList<>();
    private List<StatusTimeline> resolutionRemarks = new ArrayList<>();
    private List<Map<String, Object>> history = new ArrayList<>();

    private Integer feedback;
    private GrievanceStatus status;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
