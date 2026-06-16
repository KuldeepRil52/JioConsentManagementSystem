package com.jio.digigov.fides.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.jio.digigov.fides.constant.ErrorCodes;
import com.jio.digigov.fides.enumeration.Period;
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

    @NotNull(message = ErrorCodes.JCMP1009)
    @Positive(message = ErrorCodes.JCMP1010)
    int value;
    
    @NotNull(message = ErrorCodes.JCMP1011)
    Period unit;

}
