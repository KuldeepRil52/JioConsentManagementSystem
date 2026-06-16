package com.jio.multitranslator.dto.response.token;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TokenResponse {
    private List<PipelineResponseConfig> pipelineResponseConfig;
    private PipelineInferenceEndPoint pipelineInferenceAPIEndPoint;
    private PipelineInferenceEndPoint pipelineInferenceSocketEndPoint;
}
