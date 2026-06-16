package com.jio.multitranslator.dto.request.translate;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO representing a translation pipeline task.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TranslatePipelineTask {
    private String taskType;
    private TaskConfig config;
}
