package com.wso2wrapper.credentials_generation.dto.entity;

import com.wso2wrapper.credentials_generation.dto.OnboardRequestDto;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;

//@Document("user_credentials")
public class UserCredential {

    @Id
    private String id;

    private String username;  // WSO2 username
    private String password;
    private String tenantId;

    private String businessId;
    private String businessName;
    private String businessApplicationId;

    private String consumerKey;
    private String consumerSecret;

    private boolean active;
    private Date createdAt;

    public UserCredential(OnboardRequestDto request) {
        this.active = true;
        this.createdAt = new Date();
    }

    public UserCredential(String username, String password, String tenantId,
                          String businessId, String businessName,
                          String businessApplicationId,
                          String consumerKey, String consumerSecret) {
        this.username = username;
        this.password = password;
        this.tenantId = tenantId;
        this.businessId = businessId;
        this.businessName = businessName;
        this.businessApplicationId = businessApplicationId;
        this.consumerKey = consumerKey;
        this.consumerSecret = consumerSecret;
        this.active = true;
        this.createdAt = new Date();
    }

    public UserCredential() {

    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public String getBusinessId() {
        return businessId;
    }

    public void setBusinessId(String businessId) {
        this.businessId = businessId;
    }

    public String getBusinessName() {
        return businessName;
    }

    public void setBusinessName(String businessName) {
        this.businessName = businessName;
    }

    public String getBusinessApplicationId() {
        return businessApplicationId;
    }

    public void setBusinessApplicationId(String businessApplicationId) {
        this.businessApplicationId = businessApplicationId;
    }

    public String getConsumerKey() {
        return consumerKey;
    }

    public void setConsumerKey(String consumerKey) {
        this.consumerKey = consumerKey;
    }

    public String getConsumerSecret() {
        return consumerSecret;
    }

    public void setConsumerSecret(String consumerSecret) {
        this.consumerSecret = consumerSecret;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

}
