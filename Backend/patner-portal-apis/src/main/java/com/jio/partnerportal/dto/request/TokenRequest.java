package com.jio.partnerportal.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;



@Data
@NoArgsConstructor
@AllArgsConstructor
public class TokenRequest {
    @NotNull
    @NotBlank
    private String grant_type;

    @NotNull
    @NotBlank
    private String scope;

}
