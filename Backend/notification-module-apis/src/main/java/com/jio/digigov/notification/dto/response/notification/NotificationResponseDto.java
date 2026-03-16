package com.jio.digigov.notification.dto.response.notification;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * DTO representing a complete notification event with all associated notifications.
 *
 * This DTO aggregates information from notification_events, notification_sms,
 * notification_email, and notification_callback collections to provide a comprehensive
 * view of all notifications triggered by a specific event.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationResponseDto {

    /**
     * Event identifier linking all notifications
     */
    private String eventId;

    /**
     * Business identifier for multi-tenant routing
     */
    private String businessId;

    /**
     * Type of event that triggered the notifications
     */
    private String eventType;

    /**
     * Resource type associated with the event
     */
    private String resource;

    /**
     * Source system that triggered the event
     */
    private String source;

    /**
     * Resolved language for template processing
     */
    private String language;

    /**
     * Customer identifiers from the original event
     */
    private CustomerIdentifiers customerIdentifiers;

    /**
     * Data Processor IDs if applicable
     */
    private List<String> dataProcessorIds;

    /**
     * Original event payload data
     */
    private Map<String, Object> eventPayload;

    /**
     * Overall event status
     */
    private String status;

    /**
     * Event processing priority
     */
    private String priority;

    /**
     * Summary of all notifications created for this event
     */
    private NotificationsSummary notificationsSummary;

    /**
     * All SMS notifications for this event
     */
    private List<SmsNotificationInfo> smsNotifications;

    /**
     * All email notifications for this event
     */
    private List<EmailNotificationInfo> emailNotifications;

    /**
     * All callback notifications for this event
     */
    private List<CallbackNotificationInfo> callbackNotifications;

    /**
     * Event creation timestamp
     */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;

    /**
     * Event last update timestamp
     */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime updatedAt;

    /**
     * Event processing start timestamp
     */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime processedAt;

    /**
     * Event completion timestamp
     */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime completedAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CustomerIdentifiers {
        private String type;
        private String value;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class NotificationsSummary {
        private Integer totalCount;
        private Integer smsCount;
        private Integer emailCount;
        private Integer callbackCount;
        private Integer completedCount;
        private Integer failedCount;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SmsNotificationInfo {
        private String notificationId;
        private String correlationId;
        private String templateId;
        private String mobile;
        private String status;
        private String priority;
        private Integer attemptCount;
        private String lastErrorMessage;

        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        private LocalDateTime createdAt;

        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        private LocalDateTime processedAt;

        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        private LocalDateTime sentAt;

        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        private LocalDateTime deliveredAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EmailNotificationInfo {
        private String notificationId;
        private String correlationId;
        private String templateId;
        private List<String> to;
        private List<String> cc;
        private List<String> bcc;
        private String subject;
        private String status;
        private String priority;
        private Integer attemptCount;
        private String lastErrorMessage;

        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        private LocalDateTime createdAt;

        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        private LocalDateTime processedAt;

        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        private LocalDateTime sentAt;

        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        private LocalDateTime deliveredAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CallbackNotificationInfo {
        private String notificationId;
        private String correlationId;
        private String recipientType;
        private String recipientId;
        private String callbackUrl;
        private String eventType;
        private String status;
        private String priority;
        private Integer attemptCount;
        private String lastErrorMessage;
        private Integer lastHttpStatusCode;

        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        private LocalDateTime createdAt;

        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        private LocalDateTime processedAt;

        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        private LocalDateTime sentAt;

        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        private LocalDateTime acknowledgedAt;
    }
}