package com.jio.digigov.notification.dto.request.template;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.AssertTrue;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = "Request to update unified SMS or Email template. Only content and configuration fields can be updated. Identifying fields (eventType, language, channelType, recipientType) cannot be changed. For partial updates, provide only the fields you want to change - other fields will retain their existing values.")
public class UpdateTemplateRequestDto {

    @Schema(description = "SMS template configuration (optional - provide only if updating SMS template). Fields not provided will retain existing values from the template.")
    private SmsTemplateDto smsTemplate; // Optional - no @Valid because we merge with existing

    @Schema(description = "Email template configuration (optional - provide only if updating Email template). Fields not provided will retain existing values from the template.")
    private EmailTemplateDto emailTemplate; // Optional - no @Valid because we merge with existing

    @AssertTrue(message = "Exactly one template (SMS or Email) must be provided, not both")
    public boolean isExactlyOneTemplateProvided() {
        return (smsTemplate != null && emailTemplate == null) ||
               (smsTemplate == null && emailTemplate != null);
    }
}
