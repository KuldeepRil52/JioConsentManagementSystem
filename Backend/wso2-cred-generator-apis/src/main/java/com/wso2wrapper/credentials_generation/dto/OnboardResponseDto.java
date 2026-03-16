package com.wso2wrapper.credentials_generation.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class OnboardResponseDto {

    @JsonProperty("tenantUniqueId")
    private String tenantUniqueId;

    private String consumerKey;
    private String consumerSecret;
    private String businessUniqueId;
    private String dataProcessorUniqueId;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String message;  // Only set for special cases

    public OnboardResponseDto() {}

    // Tenant-only
    public static OnboardResponseDto tenantOnly(String tenantUniqueId, String message) {
        OnboardResponseDto dto = new OnboardResponseDto();
        dto.tenantUniqueId = tenantUniqueId;
        return dto;
    }

    // Tenant + business
    public static OnboardResponseDto tenantWithBusiness(String tenantUniqueId, String consumerKey,
                                                        String consumerSecret, String businessUniqueId) {
        OnboardResponseDto dto = new OnboardResponseDto();
        dto.tenantUniqueId = tenantUniqueId;
        dto.consumerKey = consumerKey;
        dto.consumerSecret = consumerSecret;
        dto.businessUniqueId = businessUniqueId;
        return dto;
    }

    public static OnboardResponseDto tenantWithDataProcessorOnboard(String consumerKey,
                                                               String consumerSecret, String dataProcessorUniqueId) {
        OnboardResponseDto dto = new OnboardResponseDto();
        dto.consumerKey = consumerKey;
        dto.consumerSecret = consumerSecret;
        dto.dataProcessorUniqueId = dataProcessorUniqueId;
        return dto;
    }

    public static OnboardResponseDto tenantWithBusinessOnboard(String consumerKey,
                                                        String consumerSecret, String businessUniqueId) {
        OnboardResponseDto dto = new OnboardResponseDto();
        dto.consumerKey = consumerKey;
        dto.consumerSecret = consumerSecret;
        dto.businessUniqueId = businessUniqueId;
        return dto;
    }


    public static OnboardResponseDto businessAlreadyOnboarded(String consumerKey, String consumerSecret, String businessUniqueId) {
        OnboardResponseDto dto = new OnboardResponseDto();
        dto.consumerKey = consumerKey;
        dto.consumerSecret = consumerSecret;
        dto.businessUniqueId = businessUniqueId;
        dto.message = "Business already onboarded";
        return dto;
    }

    public static OnboardResponseDto dataProcessorAlreadyOnboarded(String consumerKey, String consumerSecret, String dataProcessorUniqueId) {
        OnboardResponseDto dto = new OnboardResponseDto();
        dto.consumerKey = consumerKey;
        dto.consumerSecret = consumerSecret;
        dto.dataProcessorUniqueId = dataProcessorUniqueId;
        dto.message = "Data Processor already onboarded";
        return dto;
    }

    public String getTenantUniqueId() { return tenantUniqueId; }
    public void setTenantUniqueId(String tenantUniqueId) { this.tenantUniqueId = tenantUniqueId; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public String getConsumerKey() { return consumerKey; }
    public String getConsumerSecret() { return consumerSecret; }
    public void setConsumerKey(String consumerKey) { this.consumerKey = consumerKey; }
    public void setConsumerSecret(String consumerSecret) { this.consumerSecret = consumerSecret; }
    public String getBusinessUniqueId() { return businessUniqueId; }
    public void setBusinessUniqueId(String businessUniqueId) { this.businessUniqueId = businessUniqueId; }

    public String getDataProcessorUniqueId() {
        return dataProcessorUniqueId;
    }

    public void setDataProcessorUniqueId(String dataProcessorUniqueId) {
        this.dataProcessorUniqueId = dataProcessorUniqueId;
    }
}