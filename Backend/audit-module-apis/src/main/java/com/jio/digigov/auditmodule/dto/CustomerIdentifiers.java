package com.jio.digigov.auditmodule.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.jio.digigov.auditmodule.enumeration.IdentityType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
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
public class CustomerIdentifiers {

    @Schema(description = "Identity type", example = "MOBILE", allowableValues = {"MOBILE", "EMAIL"})
    private IdentityType type;
    private String value;

    @Schema(hidden = true)
    public boolean isValueValidForType() {
        if (type == null || value == null) {
            return true;
        }

        return switch (type) {
            case MOBILE -> value.matches("^\\d{10}$");
            case EMAIL -> value.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");
        };
    }
}
