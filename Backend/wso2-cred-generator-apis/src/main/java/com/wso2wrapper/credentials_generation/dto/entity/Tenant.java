package com.wso2wrapper.credentials_generation.dto.entity;


import com.fasterxml.jackson.annotation.JsonFormat;
import com.wso2wrapper.credentials_generation.constants.ExternalApiConstants;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Document(ExternalApiConstants.DB_WSO2_TENANTS)
public class Tenant {

    private String tenantUniqueId;

    private String tenantName;
    @Transient
    private String tenantId;
    private String password;

    @CreatedDate
    @JsonFormat(shape = JsonFormat.Shape.STRING,
            pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
            timezone = "UTC")
    private Instant createdAt;

    public Tenant() {
        this.createdAt = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    }

    public Tenant(String tenantName, String password) {
        this.tenantUniqueId = UUID.randomUUID().toString();
        this.tenantName = tenantName;
        this.password = password;
        this.createdAt = Instant.now().truncatedTo(ChronoUnit.MILLIS);
    }


    public String getTenantUniqueId() { return tenantUniqueId; }
    public void setTenantUniqueId(String tenantUniqueId) {
        this.tenantUniqueId = tenantUniqueId;
    }

    public String getTenantName() { return tenantName; }
    public void setTenantName(String tenantName) { this.tenantName = tenantName; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

}
