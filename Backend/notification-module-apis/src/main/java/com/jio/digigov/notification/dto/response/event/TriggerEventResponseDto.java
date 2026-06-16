package com.jio.digigov.notification.dto.response.event;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
@Schema(description = "Response for event trigger request")
public class TriggerEventResponseDto {

    @Schema(description = "Generated event ID", example = "EVT_20240120_001")
    private String eventId;

    @Schema(description = "Event type", example = "CONSENT_GRANTED")
    private String eventType;

    @Schema(description = "Transaction identifier (for OTP verification)", example = "f81d4fae-7dec-11d0-a765-00a0c91e6bf6")
    private String transactionId;

    @Schema(description = "Correlation identifier for related notifications", example = "COR_20240315_67890")
    private String correlationId;

    @Schema(description = "Event processing status", example = "PROCESSING")
    private String status;

    @Schema(description = "Summary of created notifications")
    private NotificationsSummaryResponse notificationsSummary;

    @Schema(description = "Processing message")
    private String message;
    
    @Data
    @Builder
    @Schema(description = "Notifications summary response")
    public static class NotificationsSummaryResponse {
        
        @Schema(description = "SMS notification ID", example = "NOTIF_SMS_20240120_001")
        private String smsNotificationId;
        
        @Schema(description = "Email notification ID", example = "NOTIF_EMAIL_20240120_001")
        private String emailNotificationId;
        
        @Schema(description = "Callback notification IDs (for future)")
        private List<CallbackNotificationResponse> callbackNotificationIds;
    }
    
    @Data
    @Builder
    @Schema(description = "Callback notification response")
    public static class CallbackNotificationResponse {
        
        @Schema(description = "Recipient type", example = "DATA_FIDUCIARY")
        private String recipientType;
        
        @Schema(description = "Recipient ID", example = "business_001")
        private String recipientId;
        
        @Schema(description = "Notification ID", example = "NOTIF_CB_DF_20240120_001")
        private String notificationId;
    }
}