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
public class GetSubscriptionResponse {

    @Schema(description = "Whether the tenant has premium subscription", example = "true")
    private boolean isPremium;

    @Schema(description = "Premium subscription metadata")
    private PremiumMeta premiumMeta;

    @Schema(description = "Tenant ID", example = "a1b2c3d4-e5f6-7890-1234-567890abcdef")
    private String tenantId;

    @Schema(description = "PAN number", example = "ABCDE1234F")
    private String pan;

    @Schema(description = "Client ID", example = "JCMP_ABCDE1234F")
    private String clientId;
}
