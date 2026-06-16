package com.jio.multitranslator.dto.request;

import com.jio.multitranslator.constant.ErrorCodes;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO representing an input item for translation.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InputItem {

    @NotBlank(message = ErrorCodes.JCMPT023)
    private String id;

    @NotBlank(message = ErrorCodes.JCMPT024)
    private String source;
}
