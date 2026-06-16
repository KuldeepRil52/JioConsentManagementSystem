package com.jio.digigov.fides.dto.request;

import com.jio.digigov.fides.constant.ErrorCodes;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "Request body for creating/updating System Register")
public class SystemRegisterRequest {

    @Schema(example = "CRM Application")
    @NotBlank(message = ErrorCodes.JCMP1003)
    private String systemName;

    @Schema(example = "Customer relationship management system")
    private String description;

    @Schema(example = "ACTIVE")
    private String status;
}