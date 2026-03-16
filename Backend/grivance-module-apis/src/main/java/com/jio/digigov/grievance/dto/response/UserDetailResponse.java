package com.jio.digigov.grievance.dto.response;

import com.jio.digigov.grievance.enumeration.ScopeLevel;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Response DTO for UserDetail.
 */
@Data
@Builder
public class UserDetailResponse {

    private String userDetailId;
    private String name;
    private String description;
    private ScopeLevel scope;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
