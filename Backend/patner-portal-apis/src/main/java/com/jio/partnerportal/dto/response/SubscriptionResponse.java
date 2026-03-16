package com.jio.partnerportal.dto.response;

import com.jio.partnerportal.dto.PremiumMeta;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubscriptionResponse {

    @Schema(description = "Success message", example = "Subscription enabled successfully")
    private String message;

    @Schema(description = "Licence ID for the subscription", example = "LIC-2024-001-ABC123")
    private String licenceId;

    @Schema(description = "Premium subscription metadata")
    private PremiumMeta premiumMeta;

    @Schema(description = "Whether the tenant has premium subscription", example = "true")
    private boolean isPremium;
}
