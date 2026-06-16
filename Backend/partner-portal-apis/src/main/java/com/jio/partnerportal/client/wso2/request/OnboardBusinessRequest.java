package com.jio.partnerportal.client.wso2.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OnboardBusinessRequest {

    private String tenantId;
    private String businessId;
    private String businessName;

}
