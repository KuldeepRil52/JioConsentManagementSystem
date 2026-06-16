package com.jio.partnerportal.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DataTypeResponse {

    private String message;
    private String dataTypeId;

}
