package com.jio.digigov.notification.dto.digigov;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.jio.digigov.notification.enums.NotificationChannel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for DigiGov template approve API
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApproveTemplateResponseDto {

    @JsonProperty("emailStatus")
    private String emailStatus;

    @JsonProperty("smsStatus")
    private String smsStatus;

    @JsonProperty("templateId")
    private String templateId;

    /**
     * Checks if both email and SMS statuses are "Active"
     * @return true if both emailStatus and smsStatus are "Active", false otherwise
     */
    public boolean isBothActive() {
        return "Active".equalsIgnoreCase(emailStatus) && "Active".equalsIgnoreCase(smsStatus);
    }

    /**
     * Gets a combined status message for logging
     * @return formatted status string showing both email and SMS statuses
     */
    public String getCombinedStatus() {
        return String.format("emailStatus='%s', smsStatus='%s'", emailStatus, smsStatus);
    }

    /**
     * Checks if SMS status is "Active"
     * @return true if smsStatus is "Active", false otherwise
     */
    public boolean isSmsActive() {
        return "Active".equalsIgnoreCase(smsStatus);
    }

    /**
     * Checks if Email status is "Active"
     * @return true if emailStatus is "Active", false otherwise
     */
    public boolean isEmailActive() {
        return "Active".equalsIgnoreCase(emailStatus);
    }

    /**
     * Checks if the specified channel status is "Active"
     * @param channel the notification channel to check (SMS or EMAIL)
     * @return true if the channel status is "Active", false otherwise
     */
    public boolean isChannelActive(NotificationChannel channel) {
        if (channel == NotificationChannel.SMS) {
            return isSmsActive();
        } else if (channel == NotificationChannel.EMAIL) {
            return isEmailActive();
        }
        return false;
    }
}