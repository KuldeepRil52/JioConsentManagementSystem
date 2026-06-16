package com.jio.digigov.grievance.dto.request;

import lombok.Data;

/**
 * Request DTO for updating a UserDetail.
 */

@Data
public class UserDetailUpdateRequest {
    private String name;
    private String description;
}
