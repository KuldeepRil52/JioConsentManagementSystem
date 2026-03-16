package com.wso2wrapper.credentials_generation.dto;

public class OnboardRequestDto {

    private String tenantId;
    private String businessId;
    private String dataProcessorId;
    private String tenantName;
    private String businessName;
    private String dataProcessorName;



    // No-args constructor
    public OnboardRequestDto() {}

    // Parameterized constructor
    public OnboardRequestDto(String tenantId, String businessId, String tenantName,String dataProcessorId,String businessName,String dataProcessorName) {
        this.tenantId = tenantId;
        this.businessId = businessId;
        this.tenantName = tenantName;
        this.dataProcessorId = dataProcessorId;
        this.businessName = businessName;
        this.dataProcessorName = dataProcessorName;
    }

    // Getters and setters
    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }

    public String getBusinessId() { return businessId; }
    public void setBusinessId(String businessId) { this.businessId = businessId; }

    public String getTenantName() { return tenantName; }
    public void setTenantName(String tenantName) { this.tenantName = tenantName; }

    public String getDataProcessorId() {
        return dataProcessorId;
    }

    public void setDataProcessorId(String dataProcessorId) {
        this.dataProcessorId = dataProcessorId;
    }

    public String getBusinessName() {
        return businessName;
    }

    public void setBusinessName(String businessName) {
        this.businessName = businessName;
    }

    public String getDataProcessorName() {
        return dataProcessorName;
    }

    public void setDataProcessorName(String dataProcessorName) {
        this.dataProcessorName = dataProcessorName;
    }

}

