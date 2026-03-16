package com.jio.digigov.grievance.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Request DTO for creating a UserType.
 */
@Data
public class UserTypeCreateRequest {
    @NotBlank(message = "Name is mandatory")
    private String name;

    private String description;
}
