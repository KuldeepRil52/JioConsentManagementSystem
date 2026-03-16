package com.jio.partnerportal.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor

public class TenantOtpResponse {
    private String txnId;
    private String message;
    private String twoFactorEnabled;
}
