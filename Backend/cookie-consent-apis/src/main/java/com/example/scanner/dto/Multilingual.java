package com.example.scanner.dto;


import com.example.scanner.enums.LANGUAGE;
import com.fasterxml.jackson.annotation.JsonInclude;
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
public class Multilingual {

    @Schema(
            description = "List of supported languages using LANGUAGE enum values",
            example = "[\"ASSAMESE, BENGALI, BODO, DOGRI, GUJARATI, HINDI, KANNADA, KASHMIRI,\n" +
                    "    KONKANI, MAITHILI, MALAYALAM, MANIPURI, MARATHI, NEPALI, ODIA,\n" +
                    "    PUNJABI, SANSKRIT, SANTALI, SINDHI, TAMIL, TELUGU, URDU, ENGLISH]",
            required = true
    )
    @NotEmpty(message = "At least one supported language is required")
    private List<LANGUAGE> supportedLanguages;

    @Schema(
            description = "Map of language to its specific content. Keys must be LANGUAGE enum values",
            required = true
    )
    @NotNull(message = "Language specific content map is required")
    @Valid
    Map<LANGUAGE, LanguageSpecificContent> languageSpecificContentMap;
}