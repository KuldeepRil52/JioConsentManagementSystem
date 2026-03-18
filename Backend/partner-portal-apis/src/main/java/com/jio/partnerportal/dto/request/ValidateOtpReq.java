package com.jio.partnerportal.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ValidateOtpReq {

    private String userOTP;

    private String txnId;

}
