package com.jio.partnerportal.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LegalEntityUpdateResponse {

    @JsonProperty("legalEntityId")
    private String legalEntityId;

    @JsonProperty("message")
    private String message;
}
