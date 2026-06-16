package com.jio.digigov.notification.dto.response.masterlist;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ValidationResponseDto {

    private boolean isValid;
    private Set<String> validLabels;
    private Set<String> invalidLabels;
    private Map<String, List<String>> suggestions;
}