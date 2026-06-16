package com.jio.digigov.fides.dto.response;

import com.jio.digigov.fides.entity.SystemRegister;
import java.time.LocalDateTime;

import com.jio.digigov.fides.enumeration.Status;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "System Register response")
public class SystemRegisterResponse {

    @Schema(example = "66b21d9e8f4c9b3d0a123456")
    private String systemId;

    @Schema(example = "SYS-CRM-001")
    private String systemUniqueId;

    @Schema(example = "CRM Application")
    private String systemName;

    @Schema(example = "Customer relationship management system")
    private String description;

    @Schema(example = "ACTIVE")
    private Status status;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static SystemRegisterResponse from(SystemRegister entity) {
        SystemRegisterResponse res = new SystemRegisterResponse();
        res.setSystemId(entity.getId());
        res.setSystemUniqueId(entity.getSystemUniqueId());
        res.setSystemName(entity.getSystemName());
        res.setDescription(entity.getDescription());
        res.setStatus(entity.getStatus());
        res.setCreatedAt(entity.getCreatedAt());
        res.setUpdatedAt(entity.getUpdatedAt());
        return res;
    }
}