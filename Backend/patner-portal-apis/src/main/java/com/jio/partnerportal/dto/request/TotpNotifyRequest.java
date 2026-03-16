package com.jio.partnerportal.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TotpNotifyRequest {

    private String accessToken;
    private String templateId;
    private String to;
    private String cc;
    private String mobileNumber;
    private String arg1;
    private String arg2;
    private String arg3;
    private String messageDetails;

    // Additional fields for metadata
    private String tenantId;
    private String clientId;
}
