package com.jio.digigov.notification.dto.response.masterlist;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ResolutionTestResponseDto {

    private String labelName;
    private String resolvedValue;
    private long resolutionTime;
    private String status;
    private Map<String, Object> query;
}