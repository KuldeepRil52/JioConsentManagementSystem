package com.jio.digigov.notification.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.jio.digigov.notification.dto.NotificationDetails;
import com.jio.digigov.notification.dto.SmtpDetails;
import com.jio.digigov.notification.enums.ProviderType;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;

/**
 * Notification Configuration Entity.
 * Stores business-specific configuration for notification service providers (DigiGov, SMTP, etc.).
 * Replaces the legacy NGConfiguration entity with a more structured approach.
 *
 * Collection: notification_configurations
 *
 * @author Notification Service Team
 * @since 2025-01-09
 */
@EqualsAndHashCode(callSuper = false)
@Document(collection = "notification_configurations")
@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class NotificationConfig {

    @Id
    @Field("_id")
    private String id;

    @Field("configId")
    @JsonProperty("configId")
    private String configId;

    @Field("businessId")
    @Indexed(unique = true)
    @JsonProperty("businessId")
    private String businessId;

    @Field("scopeLevel")
    @JsonProperty("scopeLevel")
    private String scopeLevel;

    @Field("providerType")
    @JsonProperty("providerType")
    @Builder.Default
    private ProviderType providerType = ProviderType.DIGIGOV; // Default to DigiGov for backward compatibility

    @Field("configurationJson")
    @JsonProperty("configurationJson")
    private NotificationDetails configurationJson;

    @Field("smtpDetails")
    @JsonProperty("smtpDetails")
    private SmtpDetails smtpDetails;

    @Field("createdAt")
    @CreatedDate
    private LocalDateTime createdAt;

    @Field("updatedAt")
    @LastModifiedDate
    private LocalDateTime updatedAt;

    /**
     * Helper method to build API URLs using the base URL from configuration.
     *
     * @param endpoint The API endpoint path
     * @return Complete URL with base URL and endpoint
     */
    public String buildApiUrl(String endpoint) {
        if (configurationJson == null || configurationJson.getBaseUrl() == null
                || configurationJson.getBaseUrl().trim().isEmpty()) {
            return "";
        }

        String baseUrl = configurationJson.getBaseUrl();
        if (baseUrl.endsWith("/")) {
            return baseUrl + (endpoint.startsWith("/") ? endpoint.substring(1) : endpoint);
        } else {
            return baseUrl + (endpoint.startsWith("/") ? endpoint : "/" + endpoint);
        }
    }

    /**
     * Get base URL from nested configuration.
     *
     * @return Base URL for notification service
     */
    public String getBaseUrl() {
        return configurationJson != null ? configurationJson.getBaseUrl() : null;
    }

    /**
     * Get client ID from nested configuration.
     *
     * @return Client ID for authentication
     */
    public String getClientId() {
        return configurationJson != null ? configurationJson.getClientId() : null;
    }

    /**
     * Get client secret from nested configuration.
     *
     * @return Client secret for authentication
     */
    public String getClientSecret() {
        return configurationJson != null ? configurationJson.getClientSecret() : null;
    }

    /**
     * Get SID from nested configuration.
     *
     * @return Service ID
     */
    public String getSid() {
        return configurationJson != null ? configurationJson.getSid() : null;
    }
}
