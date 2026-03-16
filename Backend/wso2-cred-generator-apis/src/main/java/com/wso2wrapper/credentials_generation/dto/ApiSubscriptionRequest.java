package com.wso2wrapper.credentials_generation.dto;

public class ApiSubscriptionRequest {

    private String applicationId;
    private String apiId;
    private String throttlingPolicy;
    private String requestedThrottlingPolicy;

    public ApiSubscriptionRequest() {
    }

    // Getters and setters
    public String getApplicationId() { return applicationId; }
    public void setApplicationId(String applicationId) { this.applicationId = applicationId; }

    public String getApiId() { return apiId; }
    public void setApiId(String apiId) { this.apiId = apiId; }

    public String getThrottlingPolicy() { return throttlingPolicy; }
    public void setThrottlingPolicy(String throttlingPolicy) { this.throttlingPolicy = throttlingPolicy; }

    public String getRequestedThrottlingPolicy() { return requestedThrottlingPolicy; }
    public void setRequestedThrottlingPolicy(String requestedThrottlingPolicy) { this.requestedThrottlingPolicy = requestedThrottlingPolicy; }
}

