package com.wso2wrapper.credentials_generation.dto.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.wso2wrapper.credentials_generation.constants.ExternalApiConstants;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;


@Document(ExternalApiConstants.DB_WSO2_DATA_PROCESSOR)
public class WSO2_Data_Processor {
    @Id
    private String id;
    private String tenantId;
    private String dataProcessorId;
    private String dataProcessorName;
    private String applicationId;
    private String consumerKey;
    private String consumerSecret;
    @CreatedDate
    @JsonFormat(shape = JsonFormat.Shape.STRING,
            pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
            timezone = "UTC")
    private Instant createdAt;

    private String dataProcessorUniqueId;

    public WSO2_Data_Processor() {
        this.createdAt = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    }

    public WSO2_Data_Processor(String tenantId, String businessId, String dataProcessorName) {
        this.tenantId = tenantId;
        this.dataProcessorId = businessId;
        this.dataProcessorName = dataProcessorName;
        this.createdAt = Instant.now().truncatedTo(ChronoUnit.MILLIS);
    }

    // Getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }

    public String getDataProcessorId() { return dataProcessorId; }
    public void setDataProcessorId(String dataProcessorId) { this.dataProcessorId = dataProcessorId; }

    public String getDataProcessorName() { return dataProcessorName; }
    public void setDataProcessorName(String dataProcessorName) { this.dataProcessorName = dataProcessorName; }

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
    public String getDataProcessorUniqueId() {
        return dataProcessorUniqueId;
    }

    public void setDataProcessorUniqueId(String dataProcessorUniqueId) {
        this.dataProcessorUniqueId = dataProcessorUniqueId;
    }
}
