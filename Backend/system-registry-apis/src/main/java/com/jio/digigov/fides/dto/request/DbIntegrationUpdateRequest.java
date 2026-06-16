package com.jio.digigov.fides.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Map;
import lombok.Data;

@Data
public class DbIntegrationUpdateRequest {

    @NotBlank
    private String systemId;

    private String datasetId;

    @NotBlank
    private String dbType;

    @NotNull
    private Map<String, Object> connectionDetails;

    private String status;
}
