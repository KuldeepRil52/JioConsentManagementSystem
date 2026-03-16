package com.jio.digigov.notification.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Request DTO for Email Template operations
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmailTemplateRequestDto {
    
    @NotEmpty(message = "To recipients are required")
    private List<String> to;
    
    private List<String> cc;
    
    @NotBlank(message = "Template details are required")
    private String templateDetails;
    
    @NotBlank(message = "Template body is required")
    private String templateBody;
    
    @NotBlank(message = "Template subject is required")
    private String templateSubject;
    
    private String templateFromName;
    
    @Builder.Default
    private String emailType = "HTML";
}