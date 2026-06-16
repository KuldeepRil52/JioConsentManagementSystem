package com.wso2wrapper.credentials_generation.dto.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.wso2wrapper.credentials_generation.constants.ExternalApiConstants;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;

@Document(ExternalApiConstants.DB_WSO2_BUSINESS_APPLICATIONS)
public class WSO2_BusinessApplication {

    @Id
    private String id;
    private String tenantId;
    private String businessId;
    private String businessName;
    private String applicationId;
    private String consumerKey;
    private String consumerSecret;
    @CreatedDate
    @JsonFormat(shape = JsonFormat.Shape.STRING,
            pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
            timezone = "UTC")
    private Instant createdAt;

    private String businessUniqueId;

    public WSO2_BusinessApplication() {
        this.createdAt = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    }

    public WSO2_BusinessApplication(String tenantId, String businessId, String businessName) {
        this.tenantId = tenantId;
        this.businessId = businessId;
        this.businessName = businessName;
        this.createdAt = Instant.now().truncatedTo(ChronoUnit.MILLIS);
    }

    // Getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }

    public String getBusinessId() { return businessId; }
    public void setBusinessId(String businessId) { this.businessId = businessId; }

    public String getBusinessName() { return businessName; }
    public void setBusinessName(String businessName) { this.businessName = businessName; }

    public String getApplicationId() { return applicationId; }
    public void setApplicationId(String applicationId) { this.applicationId = applicationId; }

    public String getConsumerKey() { return consumerKey; }
    public void setConsumerKey(String consumerKey) { this.consumerKey = consumerKey; }

    public String getConsumerSecret() { return consumerSecret; }
    public void setConsumerSecret(String consumerSecret) { this.consumerSecret = consumerSecret; }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public void setSubscribedApiIds(List<String> apiIds) {
    }
    public String getBusinessUniqueId() {
        return businessUniqueId;
    }

    public void setBusinessUniqueId(String businessUniqueId) {
        this.businessUniqueId = businessUniqueId;
    }
}
