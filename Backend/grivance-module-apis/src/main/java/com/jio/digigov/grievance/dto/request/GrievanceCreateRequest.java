package com.jio.digigov.grievance.dto.request;

import com.jio.digigov.grievance.dto.SupportingDoc;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Request DTO for creating a new Grievance.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GrievanceCreateRequest {

    @NotBlank
    private String userType; // user-type

    @NotBlank
    private String grievanceType;

    private String grievanceDetail;

    @NotNull
    private Map<String, Object> userDetails; // dynamic user fields

    private String grievanceDescription;

    private List<SupportingDoc> supportingDocs;
}
