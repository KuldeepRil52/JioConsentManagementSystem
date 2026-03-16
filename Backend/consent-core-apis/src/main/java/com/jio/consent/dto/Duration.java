package com.jio.consent.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.jio.consent.constant.ErrorCodes;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
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
public class Duration {

    @NotNull(message = ErrorCodes.JCMP1016)
    @Positive(message = ErrorCodes.JCMP1017)
    int value;
    
    @NotNull(message = ErrorCodes.JCMP1018)
    Period unit;

}
