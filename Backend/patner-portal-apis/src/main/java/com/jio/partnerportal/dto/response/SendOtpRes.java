package com.jio.partnerportal.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SendOtpRes {

    private Object expiry;
    private Object status;
    private String idType;
    private String idValue;
    private Object txnId;

}
