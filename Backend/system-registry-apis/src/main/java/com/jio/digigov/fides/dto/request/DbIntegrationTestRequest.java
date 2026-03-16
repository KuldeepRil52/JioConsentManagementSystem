package com.jio.digigov.fides.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.Map;

@Data
public class DbIntegrationTestRequest {

    @NotBlank
    private String dbType;

    @NotNull
    private Map<String, Object> connectionDetails;
}
