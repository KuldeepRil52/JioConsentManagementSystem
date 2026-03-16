package com.jio.digigov.notification.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for template creation
 * Matches the format specified in documentation
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TemplateCreationResponseDto {
    
    @JsonProperty("status")
    private String status;
    
    @JsonProperty("templateId")
    private String templateId;
    
    @JsonProperty("maxDailyLimit")
    private String maxDailyLimit;
    
    @JsonProperty("template")
    private TemplateDetails template;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class TemplateDetails {
        
        @JsonProperty("smsTemplate")
        private Object smsTemplate;
        
        @JsonProperty("emailTemplate")
        private Object emailTemplate;
    }
}