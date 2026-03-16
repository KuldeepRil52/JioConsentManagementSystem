package com.jio.digigov.notification.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * SMTP configuration details for email notifications.
 * Contains SMTP server connection and authentication settings.
 *
 * @author Notification Service Team
 * @since 2025-01-21
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SmtpDetails {

    @Schema(description = "SMTP server address", example = "smtp.example.com")
    @JsonProperty("serverAddress")
    private String serverAddress;

    @Schema(description = "SMTP server port", example = "587")
    @JsonProperty("port")
    private Integer port;

    @Schema(description = "Email address to send from", example = "no-reply@example.com")
    @JsonProperty("fromEmail")
    private String fromEmail;

    @Schema(description = "Username for SMTP authentication", example = "user@example.com")
    @JsonProperty("username")
    private String username;

    @Schema(description = "Password for SMTP authentication", example = "your_secret_credentials")
    @JsonProperty("credentials")
    private String credentials;

    /**
     * Getter for backward compatibility - delegates to credentials field
     */
    public String getPassword() {
        return credentials;
    }

    /**
     * Setter for backward compatibility - delegates to credentials field
     */
    public void setPassword(String password) {
        this.credentials = password;
    }

    @Schema(description = "TLS/SSL encryption type", example = "TLS", allowableValues = {"TLS", "SSL", "NONE"})
    @JsonProperty("tlsSsl")
    private String tlsSsl;

    @Schema(description = "Display name for the sender", example = "My Application")
    @JsonProperty("senderDisplayName")
    private String senderDisplayName;

    @Schema(description = "Connection timeout in milliseconds", example = "5000")
    @JsonProperty("connectionTimeout")
    @Builder.Default
    private Integer connectionTimeout = 5000;

    @Schema(description = "SMTP authentication enabled", example = "true")
    @JsonProperty("smtpAuthEnabled")
    @Builder.Default
    private Boolean smtpAuthEnabled = true;

    @Schema(description = "SMTP connection timeout in milliseconds", example = "5000")
    @JsonProperty("smtpConnectionTimeout")
    @Builder.Default
    private Integer smtpConnectionTimeout = 5000;

    @Schema(description = "SMTP socket timeout in milliseconds", example = "5000")
    @JsonProperty("smtpSocketTimeout")
    @Builder.Default
    private Integer smtpSocketTimeout = 5000;

    @Schema(description = "Reply-to email address", example = "support@example.com")
    @JsonProperty("replyTo")
    private String replyTo;

    @Schema(description = "Test email address", example = "test@example.com")
    @JsonProperty("testEmail")
    private String testEmail;
}
