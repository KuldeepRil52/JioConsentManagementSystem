package com.jio.digigov.fides.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.Map;

@Data
public class DbIntegrationCreateRequest {

    @NotBlank
    private String systemId;

    private String datasetId;

    @NotBlank
    private String dbType;

    @NotNull
    private Map<String, Object> connectionDetails;
}
