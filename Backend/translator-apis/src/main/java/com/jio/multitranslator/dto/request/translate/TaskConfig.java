package com.jio.multitranslator.dto.request.translate;

import com.jio.multitranslator.dto.request.LanguageRequest;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO representing task configuration for translation.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskConfig {
    private LanguageRequest language;
    private String serviceId;
}
