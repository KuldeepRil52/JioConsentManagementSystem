package com.jio.partnerportal.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationDetails {

    @Schema(description = "Network type for notification service", example = "INTRANET", required = true)
    @JsonProperty("networkType")
    @NotNull(message = "Network type is required")
    private NetworkType networkType;

    @Schema(description = "Base URL for notification service", required = true)
    @JsonProperty("baseUrl")
    @NotBlank(message = "Base URL is required")
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

    @Schema(description = "Mutual SSL enabled (required for INTERNET network type)", example = "false")
    @JsonProperty("mutualSSL")
    @Builder.Default
    private Boolean mutualSSL = false;

    @Schema(description = "Mutual SSL certificate (required when mutualSSL is true)")
    @JsonProperty("mutualCertificate")
    private String mutualCertificate;

    @Schema(description = "Metadata for mutual certificate document stored in documents collection")
    @JsonProperty("mutualCertificateMeta")
    private DocumentMeta mutualCertificateMeta;

    @Schema(description = "Callback URL for notification service")
    @JsonProperty("callbackUrl")
    private String callbackUrl;

}
