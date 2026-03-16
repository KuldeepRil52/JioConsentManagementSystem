package com.wso2wrapper.credentials_generation.dto.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;

@Document("business_applications")
public class BusinessApplication {

    @Id
    private String id;

    private String tenantId;
    private String businessId;
    private String businessName;
    private String applicationId;
    private String consumerKey;
    private String consumerSecret;
    private Date createdAt;

    public BusinessApplication() {}

    public BusinessApplication(String tenantId, String businessId, String businessName,
                               String applicationId, String consumerKey, String consumerSecret) {
        this.tenantId = tenantId;
        this.businessId = businessId;
        this.businessName = businessName;
        this.applicationId = applicationId;
        this.consumerKey = consumerKey;
        this.consumerSecret = consumerSecret;
        this.createdAt = new Date();
    }

    // ---------- Getters and Setters ----------

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
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

    public String getApplicationId() {
        return applicationId;
    }

    public void setApplicationId(String applicationId) {
        this.applicationId = applicationId;
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

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }
}
