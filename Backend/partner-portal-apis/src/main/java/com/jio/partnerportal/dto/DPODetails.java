package com.jio.partnerportal.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DPODetails {

    @Schema(description = "Name of the DPO", example = "Jane DPO")
    @JsonProperty("name")
    private String name;
    @Schema(description = "Email address of the DPO", example = "dpo@company.com")
    @JsonProperty("email")
    private String email;
    @Schema(description = "Mobile number of the DPO", example = "+8475487845")
    @JsonProperty("mobile")
    private String mobile;
    @JsonProperty("address")
    private String address;

}
