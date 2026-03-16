package com.jio.digigov.fides.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class DataProcessorSpocDto {

    @JsonProperty("name")
    @Schema(description = "Name of the SPOC", example = "John Doe")
    private String name;

    @JsonProperty("email")
    @Schema(description = "Email address of the SPOC", example = "john.doe@example.com")
    private String email;
}
