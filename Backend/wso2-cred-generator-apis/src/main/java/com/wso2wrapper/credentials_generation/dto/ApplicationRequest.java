package com.wso2wrapper.credentials_generation.dto;

import java.util.List;
import java.util.Map;

public class ApplicationRequest {
    private String name;
    private String throttlingPolicy;
    private String description;
    private String tokenType;
    private List<String> groups;
    private Map<String, Object> attributes;
    private List<String> subscriptionScopes;

    // Getters and setters
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }

    public String getThrottlingPolicy() {
        return throttlingPolicy;
    }
    public void setThrottlingPolicy(String throttlingPolicy) {
        this.throttlingPolicy = throttlingPolicy;
    }

    public String getDescription() {
        return description;
    }
    public void setDescription(String description) {
        this.description = description;
    }

    public String getTokenType() {
        return tokenType;
    }
    public void setTokenType(String tokenType) {
        this.tokenType = tokenType;
    }

    public List<String> getGroups() {
        return groups;
    }
    public void setGroups(List<String> groups) {
        this.groups = groups;
    }

    public Map<String, Object> getAttributes() {
        return attributes;
    }
    public void setAttributes(Map<String, Object> attributes) {
        this.attributes = attributes;
    }

    public List<String> getSubscriptionScopes() {
        return subscriptionScopes;
    }
    public void setSubscriptionScopes(List<String> subscriptionScopes) {
        this.subscriptionScopes = subscriptionScopes;
    }
}