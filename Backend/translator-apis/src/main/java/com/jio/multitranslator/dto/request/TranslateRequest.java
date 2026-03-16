package com.jio.multitranslator.dto.request;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.jio.multitranslator.constant.ErrorCodes;
import com.jio.multitranslator.dto.ProviderType;
import com.jio.multitranslator.dto.Source;
import com.jio.multitranslator.utils.NotEmptyRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO representing a translation request.
 */
@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@NotEmptyRequest(message = ErrorCodes.JCMPT036)
public class TranslateRequest {

    @Schema(description = "Translation provider", example = "BHASHINI or MICROSOFT")
    @NotNull(message = ErrorCodes.JCMPT016)
    private ProviderType provider;

    @Schema(description = "Source of the translation request")
    @NotNull(message = ErrorCodes.JCMPT022)
    private Source source;

    @Valid
    @NotNull(message = ErrorCodes.JCMPT035)
    private LanguageRequest language;

    @NotEmpty(message = ErrorCodes.JCMPT020)
    @NotNull(message = ErrorCodes.JCMPT020)
    @Size(min = 1, message = ErrorCodes.JCMPT021)
    @Valid
    private List<InputItem> input;
}
