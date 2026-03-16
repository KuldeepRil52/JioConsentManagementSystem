package com.jio.partnerportal.client.vault.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OnboardKeyResponse {

    private String message;
    private String tenantId;
    private String businessId;
    private String publicKeyPem;
    private String certType;

}

