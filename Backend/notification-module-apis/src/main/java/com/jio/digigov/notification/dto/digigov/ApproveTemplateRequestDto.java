package com.jio.digigov.notification.dto.digigov;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for DigiGov template approve API
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApproveTemplateRequestDto {
    
    @JsonProperty("status")
    private String status; // "A" for Approve
    
    @JsonProperty("templateId")
    private String templateId;
    
    @JsonProperty("type")
    private String type; // "sms" or "email"
}