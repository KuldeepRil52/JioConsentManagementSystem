package com.jio.partnerportal.dto.request;


import com.jio.partnerportal.dto.IdentityType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor


public class TenantValidateOtpRequest {
    private String txnId;
    private String pan;
    private String idValue;
    private IdentityType idType;
    private String otp;
    private String totp;
}
