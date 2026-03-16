package com.jio.partnerportal.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SendOtpReqUser {

    //	@NotEmpty(message = ErrorCodes.CM1009)
//	@ApiModelProperty(value = ApiInfoConstants.ID_TYPE)
    private String idType;

    //	@NotEmpty(message = ErrorCodes.CM1013)
//	@ApiModelProperty(value = ApiInfoConstants.ID_VALUE)
    private String idValue;

    private String templateId;

    private EmailArg1 emailArguments;


}
