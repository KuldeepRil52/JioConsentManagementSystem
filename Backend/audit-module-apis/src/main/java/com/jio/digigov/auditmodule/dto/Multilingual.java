package com.jio.digigov.auditmodule.dto;


import com.fasterxml.jackson.annotation.JsonInclude;
import com.jio.digigov.auditmodule.enumeration.LANGUAGE;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
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
    private List<LANGUAGE> supportedLanguages;

    @Schema(description = "Language specific content map",
            example = "{\n  \"ENGLISH\": {\n    \"description\": \"Sample description in English\",\n    \"label\": \"Required\",\n    \"rightsText\": \"Your rights...\",\n    \"permissionText\": \"By clicking 'Allow all'...\"\n  },\n  \"HINDI\": {\n    \"description\": \"हिंदी में नमूना विवरण\",\n    \"label\": \"आवश्यक\",\n    \"rightsText\": \"आपके अधिकार...\",\n    \"permissionText\": \"'सभी की अनुमति दें' पर क्लिक करके...\"\n  }\n}")
    @Valid
    Map<LANGUAGE, CookieLanguageSpecificContent> languageSpecificContentMap;

}

