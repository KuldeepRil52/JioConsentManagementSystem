package com.jio.multitranslator.dto.request.token;


import com.jio.multitranslator.dto.request.LanguageRequest;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TokenConfig {
    private LanguageRequest language;
}
