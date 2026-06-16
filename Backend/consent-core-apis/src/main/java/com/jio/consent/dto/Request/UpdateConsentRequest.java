package com.jio.consent.dto.Request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.jio.consent.constant.ErrorCodes;
import com.jio.consent.dto.*;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class UpdateConsentRequest {

    private LANGUAGE languagePreferences;

    @Schema(description = "preferencesStatus", example = "{\"PreferenceId1\": \"ACCEPTED\", \"PreferenceId2\": \"NOTACCEPTED\"}")
    @NotEmpty(message = ErrorCodes.JCMP1025)
    Map<String, PreferenceStatus> preferencesStatus;

    @NotNull(message = ErrorCodes.JCMP1029)
    private ConsentStatus status;

}
