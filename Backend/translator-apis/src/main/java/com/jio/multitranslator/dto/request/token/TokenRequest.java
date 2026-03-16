package com.jio.multitranslator.dto.request.token;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TokenRequest {

    private List<PipelineTask> pipelineTasks;
    private PipelineRequestConfig pipelineRequestConfig;

}


