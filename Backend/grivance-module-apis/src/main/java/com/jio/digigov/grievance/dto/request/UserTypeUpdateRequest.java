package com.jio.digigov.grievance.dto.request;

import lombok.Data;

/**
 * Request DTO for updating a UserType.
 */
@Data
public class UserTypeUpdateRequest {
    private String name;
    private String description;
}
