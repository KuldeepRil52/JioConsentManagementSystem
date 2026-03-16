package com.jio.schedular.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.jio.schedular.constant.ErrorCodes;
import com.jio.schedular.enums.IdentityType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
    @NotNull(message = ErrorCodes.JCMP1016)
    private IdentityType type;
    @NotBlank(message = ErrorCodes.JCMP1017)
    private String value;

    @Schema(hidden = true)
    @AssertTrue(message = ErrorCodes.JCMP1018)
    public boolean isValueValidForType() {
        if (type == null || value == null) {
            return true;
        }

        return switch (type) {
            case MOBILE -> value.matches("^\\d{10}$");
            case EMAIL -> value.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");
            case DEVICE_ID -> value != null && !value.trim().isEmpty();
        };
    }

    public CustomerIdentifiers(CustomerIdentifiers source) {
        this.type = source.type;
        this.value = source.value;
    }
}
