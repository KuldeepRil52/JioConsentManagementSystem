package com.jio.partnerportal.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.jio.partnerportal.dto.IdentityType;
import com.jio.partnerportal.dto.SpocDto;
import lombok.*;
import lombok.extern.slf4j.Slf4j;

@Data
@Slf4j
public class OnboardTenantRequest {

    @JsonProperty("companyName")
    private String companyName;

    @JsonProperty("logoUrl")
    private String logoUrl;

    @JsonProperty("pan")
    private String pan;

    @JsonProperty("identityType")
    private IdentityType identityType;

    @JsonProperty("spoc")
    private SpocDto spoc;

}
