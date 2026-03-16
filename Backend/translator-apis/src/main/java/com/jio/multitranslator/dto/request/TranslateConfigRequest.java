package com.jio.multitranslator.dto.request;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.jio.multitranslator.constant.ErrorCodes;
import com.jio.multitranslator.utils.NotEmptyRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO representing a request to create or update translation configuration.
 */
@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@NotEmptyRequest(message = ErrorCodes.JCMPT036)
public class TranslateConfigRequest {

    @NotBlank(message = ErrorCodes.JCMPT004)
    private String scopeLevel;

    @Valid
    @NotNull(message = ErrorCodes.JCMPT005)
    private Config config;
}
