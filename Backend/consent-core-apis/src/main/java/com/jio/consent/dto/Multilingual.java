package com.jio.consent.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.jio.consent.constant.ErrorCodes;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
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
public class Multilingual {

    @Schema(description = "Supported languages", example = "[\"ENGLISH\", \"HINDI\"]")
    @NotEmpty(message = ErrorCodes.JCMP1012)
    private List<LANGUAGE> supportedLanguages;

    @Schema(description = "Language specific content map",
            example = "{\n  \"ENGLISH\": {\n    \"description\": \"Sample description in English\",\n    \"label\": \"Required\",\n    \"rightsText\": \"Your rights...\",\n    \"permissionText\": \"By clicking 'Allow all'...\"\n  },\n  \"HINDI\": {\n    \"description\": \"हिंदी में नमूना विवरण\",\n    \"label\": \"आवश्यक\",\n    \"rightsText\": \"आपके अधिकार...\",\n    \"permissionText\": \"'सभी की अनुमति दें' पर क्लिक करके...\"\n  }\n}")
    @NotNull(message = ErrorCodes.JCMP1013)
    @Valid
    Map<LANGUAGE, LanguageSpecificContent> languageSpecificContentMap;

}
