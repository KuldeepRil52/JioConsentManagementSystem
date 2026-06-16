package com.jio.partnerportal.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.jio.partnerportal.dto.UserDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Data
@Slf4j
@AllArgsConstructor
@NoArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OnboardTenantResponse {

    @JsonProperty("tenantId")
    private String tenantId;

    @JsonProperty("clientId")
    private String clientId;

    @JsonProperty("message")
    private String message;

    @JsonProperty("userDetails")
    private UserDto userDetails;

    @JsonProperty("xSessionToken")
    private String xSessionToken;
}
