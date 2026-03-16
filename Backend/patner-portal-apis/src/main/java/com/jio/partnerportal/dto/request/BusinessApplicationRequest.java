package com.jio.partnerportal.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class BusinessApplicationRequest {

    @Schema(description = "Name of the business application", example = "My CRM App")
    @JsonProperty("name")
    private String name;
    @Schema(description = "Description of the business application", example = "A comprehensive CRM application for managing customer relationships")
    @JsonProperty("description")
    private String description;

}
