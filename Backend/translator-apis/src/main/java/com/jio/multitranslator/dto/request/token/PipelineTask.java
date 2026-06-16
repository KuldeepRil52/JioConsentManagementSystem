package com.jio.multitranslator.dto.request.token;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PipelineTask {

    private String taskType;
    private TokenConfig config;

}
