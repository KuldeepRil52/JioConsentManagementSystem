package com.jio.partnerportal.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class SpocDto {

    @JsonProperty("name")
    @Schema(description = "Name of the SPOC", example = "John Doe")
    private String name;

    @JsonProperty("mobile")
    @Schema(description = "Mobile number of the SPOC", example = "9876543210")
    private String mobile;

    @JsonProperty("email")
    @Schema(description = "Email address of the SPOC", example = "john.doe@example.com")
    private String email;
}
