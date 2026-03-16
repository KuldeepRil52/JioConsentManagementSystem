package com.jio.digigov.notification.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Request DTO for SMS Template operations
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SMSTemplateRequestDto {
    
    @NotBlank(message = "DLT Entity ID is required")
    private String dltEntityId;
    
    @NotBlank(message = "DLT Template ID is required")
    private String dltTemplateId;

    private String from;
    
    @NotEmpty(message = "Operator countries are required")
    private List<String> oprCountries;

    private String template;

    private String templateDetails;
    
    @NotEmpty(message = "Whitelisted numbers are required")
    private List<String> whiteListedNumber;
}