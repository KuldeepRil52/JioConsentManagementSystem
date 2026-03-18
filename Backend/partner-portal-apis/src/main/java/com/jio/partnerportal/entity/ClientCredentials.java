package com.jio.partnerportal.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@EqualsAndHashCode(callSuper = true)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document("client_credentials")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class ClientCredentials extends AbstractEntity {

    @Id
    @JsonProperty("id")
    private ObjectId id;

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

    @Schema(description = "Public certificate for the business application", example = "-----BEGIN CERTIFICATE-----\\nMIIC...")
    @JsonProperty("publicCertificate")
    private String publicCertificate;

    @Schema(description = "Certificate type", example = "rsa-2048", allowableValues = {"rsa-2048", "rsa-3072", "rsa-4096"})
    @JsonProperty("certType")
    private String certType;

    @Schema(description = "Status of the credentials", example = "ACTIVE", allowableValues = {"ACTIVE", "INACTIVE", "REVOKED"})
    @JsonProperty("status")
    private String status;

    @Schema(description = "Tenant ID associated with these credentials", example = "tenant-uuid")
    @JsonProperty("tenantId")
    private String tenantId;
}

