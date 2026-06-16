package com.jio.digigov.notification.dto.digigov;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Request DTO for DigiGov template onboard API
 * Supports both SMS and Email templates
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OnboardTemplateRequestDto {
    
    @JsonProperty("smsTemplate")
    private SmsTemplateDto smsTemplate;
    
    @JsonProperty("emailTemplate")
    private EmailTemplateDto emailTemplate;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class SmsTemplateDto {
        
        @JsonProperty("whiteListedNumber")
        private List<String> whiteListedNumber;
        
        @JsonProperty("from")
        private String from;
        
        @JsonProperty("oprCountries")
        private List<String> oprCountries;
        
        @JsonProperty("dltEntityId")
        private String dltEntityId;
        
        @JsonProperty("dltTemplateId")
        private String dltTemplateId;
        
        @JsonProperty("template")
        private String template;
        
        @JsonProperty("templateDetails")
        private String templateDetails;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class EmailTemplateDto {
        
        @JsonProperty("to")
        private List<String> to;
        
        @JsonProperty("cc")
        private List<String> cc;

        @JsonProperty("from")
        private String from;

        @JsonProperty("replyTo")
        private String replyTo;

        @JsonProperty("templateDetails")
        private String templateDetails;
        
        @JsonProperty("templateBody")
        private String templateBody;
        
        @JsonProperty("templateSubject")
        private String templateSubject;
        
        @JsonProperty("templateFromName")
        private String templateFromName;
        
        @JsonProperty("emailType")
        private String emailType;
    }
}