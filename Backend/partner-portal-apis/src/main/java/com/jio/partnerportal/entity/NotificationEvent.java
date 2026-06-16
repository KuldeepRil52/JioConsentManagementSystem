package com.jio.partnerportal.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.jio.partnerportal.dto.EventType;
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
@Document("pp_notification_events")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class NotificationEvent extends AbstractEntity {

    @Id
    @JsonProperty("id")
    private ObjectId id;

    @Schema(description = "Unique notification ID", example = "a1b2c3d4-e5f6-7890-1234-567890abcdef")
    @JsonProperty("notificationId")
    private String notificationId;

    @Schema(description = "Event type", example = "ONBOARD_TENANT", allowableValues = {"ONBOARD_TENANT", "ONBOARD_DATAPROCESSOR"})
    @JsonProperty("eventType")
    private EventType eventType;

    @Schema(description = "Tenant ID", example = "566d6143-a5e2-47f6-91ae-e271e2105000")
    @JsonProperty("tenantId")
    private String tenantId;

    @Schema(description = "Client ID", example = "JCMP_PAN123456")
    @JsonProperty("clientId")
    private String clientId;

    @Schema(description = "Notification type", example = "EMAIL", allowableValues = {"EMAIL", "SMS"})
    @JsonProperty("notificationType")
    private String notificationType;

    @Schema(description = "Request transaction ID", example = "LAPN1728765432123A5B6C7D8E9F0")
    @JsonProperty("requestTxnId")
    private String requestTxnId;

    @Schema(description = "Response transaction ID from notification service", example = "TXNV5M8776JWGP5")
    @JsonProperty("responseTxnId")
    private String responseTxnId;

    @Schema(description = "Notification status", example = "SENT", allowableValues = {"PENDING", "SENT", "FAILED"})
    @JsonProperty("status")
    private String status;

    @Schema(description = "Template ID used", example = "TEMPL000005253684")
    @JsonProperty("templateId")
    private String templateId;

    @Schema(description = "Recipient - email or mobile", example = "user@example.com")
    @JsonProperty("recipient")
    private String recipient;

    @Schema(description = "Notification response message", example = "Request Accepted")
    @JsonProperty("responseMessage")
    private String responseMessage;

    @Schema(description = "Error details if failed", example = "Connection timeout")
    @JsonProperty("errorDetails")
    private String errorDetails;

    @Schema(description = "HTTP status code", example = "200")
    @JsonProperty("httpStatusCode")
    private Integer httpStatusCode;

}

