package com.jio.partnerportal.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TenantValidateOtpResponse {
    private String status;
    private int retryCount;
    private String message;
    private UserDetails userDetails;
    private String tenantId;
    private String xSessionToken;

    @Data
    public static class UserDetails {
        private String userId;
    }
}
