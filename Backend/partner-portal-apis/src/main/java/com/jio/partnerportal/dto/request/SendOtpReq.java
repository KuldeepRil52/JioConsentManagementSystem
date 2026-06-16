package com.jio.partnerportal.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SendOtpReq {

    private String idType;

    private String idValue;

    private String templateId;

    private EmailArg1 emailArguments;


}
