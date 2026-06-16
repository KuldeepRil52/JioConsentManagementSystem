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

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GrievanceListResponse {

    private String grievanceId;
    private String userType;
    private String grievanceTemplateId;
    private String grievanceType;
    private String grievanceDetail;
    private Map<String, Object> userDetails; // dynamic user fields
    private String grievanceDescription;
    private List<SupportingDoc> supportingDocs;
    private Integer feedback;
    private List<StatusTimeline> resolutionRemarks = new ArrayList<>();
    private List<Map<String, Object>> history = new ArrayList<>();
    private GrievanceStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
