package com.jio.digigov.notification.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Notification service configuration details.
 * Contains authentication credentials and service endpoints.
 *
 * @author Notification Service Team
 * @since 2025-01-09
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationDetails {

    @Schema(description = "Base URL for notification service")
    @JsonProperty("baseUrl")
    private String baseUrl;

    @Schema(description = "Client ID for notification service authentication")
    @JsonProperty("clientId")
    private String clientId;

    @Schema(description = "Client secret for notification service authentication")
    @JsonProperty("clientSecret")
    private String clientSecret;

    @Schema(description = "Service ID for notification service")
    @JsonProperty("sid")
    private String sid;

    @Schema(description = "Network type (INTRANET or INTERNET)")
    @JsonProperty("networkType")
    private String networkType;

    @Schema(description = "Enable mutual SSL for INTERNET connections")
    @JsonProperty("mutualSSL")
    private Boolean mutualSSL;

    @Schema(description = "Base64-encoded PKCS12 certificate for mutual SSL")
    @JsonProperty("mutualCertificate")
    private String mutualCertificate;

    @Schema(description = "Password for the PKCS12 certificate (optional)")
    @JsonProperty("certificatePassword")
    private String certificatePassword;

    @Schema(description = "Name of the configuration")
    @JsonProperty("name")
    private String name;

    @Schema(description = "Active status of the configuration")
    @JsonProperty("isActive")
    private Boolean isActive;

    @Schema(description = "Default language for templates")
    @JsonProperty("defaultLanguage")
    private String defaultLanguage;

    @Schema(description = "Callback URL for notifications")
    @JsonProperty("callbackUrl")
    private String callbackUrl;
}
