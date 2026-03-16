package com.wso2wrapper.credentials_generation.swagger;
import io.swagger.v3.oas.annotations.media.Schema;

public class DataProcessorSwagger {

    @Schema(description = "Tenant ID", example = "54ec38c4-67c5-44b9-b219-5074308db929")
    public String tenantId;

    @Schema(description = "Data Processor ID", example = "f3c5a4e8-12de-4c45-8a01-37f90d6e9a2b")
    public String dataProcessorId;

    @Schema(description = "Data Processor Name", example = "DataProcessorName")
    public String dataProcessorName;
}