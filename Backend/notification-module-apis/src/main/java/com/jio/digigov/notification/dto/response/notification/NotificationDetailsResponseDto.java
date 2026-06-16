package com.jio.digigov.notification.dto.response.notification;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response DTO wrapping all notification details for an event.
 *
 * This DTO contains arrays of SMS, Email, and Callback notifications
 * associated with a specific event. It is included in the event response
 * when the includeNotifications=true query parameter is provided.
 *
 * The notifications are populated using optimized MongoDB aggregation
 * with $lookup operations to avoid N+1 query problems and ensure
 * high performance even with large notification volumes.
 *
 * @author Notification Service Team
 * @version 1.0
 * @since 2025-01-01
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class NotificationDetailsResponseDto {

    /**
     * List of SMS notifications associated with the event.
     * Populated from notification_sms collection via $lookup aggregation.
     */
    @JsonProperty("sms")
    private List<SmsNotificationDto> sms;

    /**
     * List of Email notifications associated with the event.
     * Populated from notification_email collection via $lookup aggregation.
     */
    @JsonProperty("email")
    private List<EmailNotificationDto> email;

    /**
     * List of Callback/Webhook notifications associated with the event.
     * Populated from notification_callback collection via $lookup aggregation.
     */
    @JsonProperty("callback")
    private List<CallbackNotificationDto> callback;
}
