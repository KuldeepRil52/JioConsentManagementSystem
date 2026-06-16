package com.jio.partnerportal.dto.request;
import com.jio.partnerportal.dto.IdentityType;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor


public class TenantOtpRequest {
    @NotBlank(message = "pan is required")
    private String pan;

    @NotBlank(message = "idValue is required")
    private String idValue;

    @NotBlank(message = "idType is required")
    private IdentityType idType;
}
