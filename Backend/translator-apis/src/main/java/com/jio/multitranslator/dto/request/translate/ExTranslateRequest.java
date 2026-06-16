package com.jio.multitranslator.dto.request.translate;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO representing an external translation request to the translation service.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExTranslateRequest {
    private List<TranslatePipelineTask> pipelineTasks;
    private InputData inputData;
}
