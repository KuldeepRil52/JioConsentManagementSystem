package com.jio.digigov.notification.dto.registry;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;

/**
 * Request DTO for consent withdraw API call to System Registry.
 *
 * This DTO is used when calling the consent withdraw endpoint:
 * POST /registry/api/v1/consents/{consentId}/withdraw
 *
 * The API saves withdrawal_data to notification_events collection.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ConsentWithdrawRequestDto {

    /**
     * Event ID triggering the withdrawal.
     * This links the withdrawal data back to the notification event.
     */
    @NotBlank(message = "Event ID is required")
    private String eventId;
}
