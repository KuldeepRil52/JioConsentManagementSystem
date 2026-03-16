package com.jio.partnerportal.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SystemDetails {

    @Schema(description = "SSL Certificate details (base64 encoded)", example = "-----BEGIN CERTIFICATE-----\n...\n-----END CERTIFICATE-----")
    @JsonProperty("sslCertificate")
    private String sslCertificate;
    @Schema(description = "Metadata for SSL Certificate document")
    @JsonProperty("sslCertificateMeta")
    private DocumentMeta sslCertificateMeta;
    @Schema(description = "Logo image (base64 encoded)", example = "iVBORw0KGgoAAAANSUhEUgAAAAUA...")
    @JsonProperty("logo")
    private String logo;
    @Schema(description = "Metadata for logo document")
    @JsonProperty("logoMeta")
    private DocumentMeta logoMeta;
    @Schema(description = "Base URL for the system", example = "https://api.example.com")
    @JsonProperty("baseUrl")
    private String baseUrl;
    @Schema(description = "Default consent expiry in days", example = "365")
    @JsonProperty("defaultConsentExpiryDays")
    private int defaultConsentExpiryDays;
    @Schema(description = "JWT token Time-To-Live in minutes", example = "60")
    @JsonProperty("jwtTokenTTLMinutes")
    private int jwtTokenTTLMinutes;
    @Schema(description = "Signed artifact expiry in days", example = "30")
    @JsonProperty("signedArtifactExpiryDays")
    private int signedArtifactExpiryDays;
    @Schema(description = "Data retention")
    @JsonProperty("dataRetention")
    private Duration dataRetention;
    @Schema(description = "Client ID for authentication", example = "client123")
    @JsonProperty("clientId")
    private String clientId;
    @Schema(description = "Client Secret for authentication", example = "supersecretkey")
    @JsonProperty("clientSecret")
    private String clientSecret;
    @Schema(description = "Base64 encoded keystore data", example = "MIIKpAIBAzCCCl4GCSqGSIb3...")
    @JsonProperty("keystoreData")
    private String keystoreData;
    @Schema(description = "Password for the keystore", example = "password123")
    @JsonProperty("keystorePassword")
    private String keystorePassword;
    @Schema(description = "Alias for the certificate in the keystore", example = "form65-cert")
    @JsonProperty("alias")
    private String alias;
    @Schema(description = "Name of the keystore file", example = "abc.p12")
    @JsonProperty("keystoreName")
    private String keystoreName;

}
