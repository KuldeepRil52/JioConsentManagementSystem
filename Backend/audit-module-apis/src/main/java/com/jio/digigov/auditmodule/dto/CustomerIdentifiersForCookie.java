package com.jio.digigov.auditmodule.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class CustomerIdentifiersForCookie {

    @Schema(
            description = "Type of identifier (e.g., EMAIL, PHONE, USER_ID, CUSTOMER_ID)",
            example = "EMAIL"
    )
    private String type;

    @Schema(
            description = "Value of the identifier",
            example = "user@example.com"
    )
    private String value;
}