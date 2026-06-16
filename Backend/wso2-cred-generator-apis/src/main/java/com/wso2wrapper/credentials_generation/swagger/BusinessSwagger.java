package com.wso2wrapper.credentials_generation.swagger;

import io.swagger.v3.oas.annotations.media.Schema;

public class BusinessSwagger {
    @Schema(description = "Tenant ID", example = "54ec38c4-67c5-44b9-b219-5074308db929")
    public String tenantId;

    @Schema(description = "Business ID", example = "54ec38c4-67c5-44b9-b219-5074308db929")
    public String businessId;

    @Schema(description = "Business Name", example = "BusinessName")
    public String businessName;
}