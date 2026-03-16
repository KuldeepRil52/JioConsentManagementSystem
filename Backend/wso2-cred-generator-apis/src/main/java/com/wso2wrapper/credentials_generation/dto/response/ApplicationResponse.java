package com.wso2wrapper.credentials_generation.dto.response;

import java.util.List;
import java.util.Map;

public class ApplicationResponse {

    private String applicationId;
    private String applicationName;
    private String name;
    private String throttlingPolicy;
    private String description;
    private String tokenType;
    private String status;
    private List<String> groups;
    private int subscriptionCount;
    private List<Object> keys;  // can refine later if needed
    private Map<String, Object> attributes;
    private List<String> subscriptionScopes;
    private String owner;
    private Boolean hashEnabled;

    // ---------- Getters and Setters ----------

    public String getApplicationId() {
        return applicationId;
    }
    public void setApplicationId(String applicationId) {
        this.applicationId = applicationId;
    }

    public String getApplicationName() {
        return applicationName;
    }
    public void setApplicationName(String applicationName) {
        this.applicationName = applicationName;
    }

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

    public String getStatus() {
        return status;
    }
    public void setStatus(String status) {
        this.status = status;
    }

    public List<String> getGroups() {
        return groups;
    }
    public void setGroups(List<String> groups) {
        this.groups = groups;
    }

    public int getSubscriptionCount() {
        return subscriptionCount;
    }
    public void setSubscriptionCount(int subscriptionCount) {
        this.subscriptionCount = subscriptionCount;
    }

    public List<Object> getKeys() {
        return keys;
    }
    public void setKeys(List<Object> keys) {
        this.keys = keys;
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

    public String getOwner() {
        return owner;
    }
    public void setOwner(String owner) {
        this.owner = owner;
    }

    public Boolean getHashEnabled() {
        return hashEnabled;
    }
    public void setHashEnabled(Boolean hashEnabled) {
        this.hashEnabled = hashEnabled;
    }
}
