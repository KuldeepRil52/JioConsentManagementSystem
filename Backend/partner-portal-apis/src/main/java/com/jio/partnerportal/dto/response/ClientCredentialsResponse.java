package com.jio.partnerportal.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ClientCredentialsResponse {

    @Schema(description = "Unique identifier for the business application", example = "a1b2c3d4-e5f6-7890-1234-567890abcdef")
    @JsonProperty("businessId")
    private String businessId;

    @Schema(description = "Business unique identifier from WSO2", example = "wso2-business-unique-id")
    @JsonProperty("businessUniqueId")
    private String businessUniqueId;

    @Schema(description = "Consumer key for API authentication", example = "abcd1234efgh5678")
    @JsonProperty("consumerKey")
    private String consumerKey;

    @Schema(description = "Consumer secret for API authentication (encrypted/hashed in production)", example = "secret1234")
    @JsonProperty("consumerSecret")
    private String consumerSecret;

    @Schema(description = "Scope level of the credentials", example = "TENANT", allowableValues = {"TENANT", "BUSINESS"})
    @JsonProperty("scopeLevel")
    private String scopeLevel;

    @Schema(description = "Status of the credentials", example = "ACTIVE", allowableValues = {"ACTIVE", "INACTIVE", "REVOKED"})
    @JsonProperty("status")
    private String status;

    @Schema(description = "Tenant ID associated with these credentials", example = "tenant-uuid")
    @JsonProperty("tenantId")
    private String tenantId;

    @Schema(description = "Timestamp when the credentials were created", example = "2025-08-20 17:34:20")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    @Schema(description = "Timestamp when the credentials were last updated", example = "2025-08-20 17:34:20")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;
}

