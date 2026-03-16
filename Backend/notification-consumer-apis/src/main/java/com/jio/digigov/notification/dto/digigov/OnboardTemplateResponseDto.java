package com.jio.digigov.notification.dto.digigov;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for DigiGov template onboard API
 * Contains templateId returned by DigiGov
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OnboardTemplateResponseDto {
    
    @JsonProperty("status")
    private String status;
    
    @JsonProperty("templateId")
    private String templateId;
    
    @JsonProperty("maxDailyLimit")
    private String maxDailyLimit;
    
    @JsonProperty("message")
    private String message;
    
    @JsonProperty("template")
    private Object template; // Contains the onboarded template details
}