package com.jio.multitranslator.dto.request;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.jio.multitranslator.constant.ErrorCodes;
import com.jio.multitranslator.utils.NotEmptyRequest;
import com.jio.multitranslator.utils.ValidLanguageCode;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO representing language pair for translation (source and target languages).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@NotEmptyRequest(message = ErrorCodes.JCMPT038)
public class LanguageRequest {
    
    @NotBlank(message = ErrorCodes.JCMPT018)
    @ValidLanguageCode(message = ErrorCodes.JCMPT031)
    private String sourceLanguage;

    @NotBlank(message = ErrorCodes.JCMPT019)
    @ValidLanguageCode(message = ErrorCodes.JCMPT032)
    private String targetLanguage;
}
