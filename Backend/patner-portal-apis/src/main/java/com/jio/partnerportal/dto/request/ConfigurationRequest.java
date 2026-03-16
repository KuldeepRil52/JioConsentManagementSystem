package com.jio.partnerportal.dto.request;

import lombok.*;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;


@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ConfigurationRequest<T> {

    @JsonProperty("configurationJson")
    @Schema(description = "JSON payload containing the configuration details")
    private T configurationJson;

}
