package com.jio.digigov.notification.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Response DTO for notification sending
 */
@Data
@Builder
@Schema(description = "Notification send response")
public class SendSmsNotificationResponseDto {

    @Schema(description = "emailStatus", example = "Request Accepted")
    private String emailStatus;

    @Schema(description = "smsStatus", example = "Request Accepted")
    private String smsStatus;

    @Schema(description = "Transaction ID", example = "TXN_123456")
    private String txn;

    @Schema(description = "Template ID", example = "TEMPL0000000001")
    private String templateId;

    @Schema(description = "Recipient", example = "9999999999")
    private String recipient;

    @Schema(description = "Notification type", example = "SMS")
    private String notificationType;

    @Schema(description = "Message ID from DigiGov", example = "MSG_123456")
    private String messageId;

    @Schema(description = "Timestamp", example = "2024-01-01T10:30:00")
    private LocalDateTime timestamp;

    @Schema(description = "Response message", example = "Notification sent successfully")
    private String message;

    @Schema(description = "Error details (if any)", example = "Rate limit exceeded")
    private String errorDetails;
}