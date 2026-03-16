package com.jio.partnerportal.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor

public class OtpValidateRequest {
    private String txnId;
    private String otp;
    private String idValue;
}

