package com.jio.partnerportal.client.auth.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TokenRequest {

    private String iss;
    private String tenantId;
    private String businessId;
    private String sub;

}

