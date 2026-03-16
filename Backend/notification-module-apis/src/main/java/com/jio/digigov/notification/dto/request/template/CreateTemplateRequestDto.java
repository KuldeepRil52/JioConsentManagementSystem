package com.jio.digigov.notification.dto.request.template;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.jio.digigov.notification.enums.ProviderType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = "Request to create unified SMS and/or Email template")
public class CreateTemplateRequestDto {
    
    @NotBlank(message = "Event type is required")
    @Schema(description = "Event type for the template", example = "CONSENT_GRANTED", required = true)
    private String eventType;

    @Schema(description = "Recipient type for this template - determines who receives this notification",
            example = "DATA_PRINCIPAL",
            allowableValues = {"DATA_PRINCIPAL", "DATA_FIDUCIARY", "DATA_PROCESSOR", "DATA_PROTECTION_OFFICER"},
            defaultValue = "DATA_PRINCIPAL",
            required = false)
    private String recipientType = "DATA_PRINCIPAL"; // Default to DATA_PRINCIPAL for backward compatibility

    @Schema(description = "Language for the template (optional, uses configuration default)",
            example = "english", required = false)
    private String language; // Optional, will use default from NGConfiguration

    @Schema(description = "Provider type for template delivery (DIGIGOV or SMTP)",
            example = "DIGIGOV",
            allowableValues = {"DIGIGOV", "SMTP"},
            defaultValue = "DIGIGOV",
            required = false)
    private ProviderType providerType = ProviderType.DIGIGOV; // Default to DigiGov for backward compatibility

    @Valid
    @Schema(description = "SMS template configuration (optional)")
    private SmsTemplateDto smsTemplate; // Optional
    
    @Valid
    @Schema(description = "Email template configuration (optional)")
    private EmailTemplateDto emailTemplate; // Optional

    @AssertTrue(message = "Exactly one template (SMS or Email) must be provided, not both")
    public boolean isExactlyOneTemplateProvided() {
        return (smsTemplate != null && emailTemplate == null) ||
               (smsTemplate == null && emailTemplate != null);
    }
}