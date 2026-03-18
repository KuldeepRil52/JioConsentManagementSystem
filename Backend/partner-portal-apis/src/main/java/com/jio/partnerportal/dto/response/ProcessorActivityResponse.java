package com.jio.partnerportal.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ProcessorActivityResponse {

    private String message;
    private String processorActivityId;

}
