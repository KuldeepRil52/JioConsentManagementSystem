package com.jio.digigov.notification.dto.request.template;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import java.util.Map;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = "SMS template configuration")
public class SmsTemplateDto {
    
    @NotEmpty(message = "Whitelisted numbers are required")
    @Schema(description = "List of whitelisted mobile numbers for template testing and validation. Must include at least one valid mobile number.",
            example = "[\"8369467086\", \"9876543210\"]",
            required = true)
    private List<String> whiteListedNumber;
    
    @Schema(description = "SMS template content with dynamic argument placeholders. Use <#ARG1>, <#ARG2>, etc. for dynamic values.",
            example = "Your OTP is <#ARG1>. Valid for 5 minutes. Do not share with anyone.",
            required = true)
    private String template;

    @Schema(description = "Brief description explaining the template's purpose and usage",
            example = "OTP verification for user authentication",
            required = true)
    private String templateDetails;

    @NotEmpty(message = "Operator countries are required")
    @Schema(description = "List of valid ISO country codes where this template can be used",
            example = "[\"IN\", \"US\", \"UK\"]",
            required = true)
    private List<String> oprCountries;

    @NotBlank(message = "DLT Entity ID is required")
    @Schema(description = "Distributed Ledger Technology (DLT) Entity ID for regulatory compliance",
            example = "1001140000000014192",
            required = true)
    private String dltEntityId;

    @NotBlank(message = "DLT Template ID is required")
    @Schema(description = "DLT Template ID registered with telecom authorities for spam prevention",
            example = "1207170410028998127",
            required = true)
    private String dltTemplateId;
    
    @Schema(description = "Sender ID displayed to the recipient. Must be pre-approved by telecom authorities.",
            example = "JioGCS",
            required = false,
            maxLength = 6)
    private String from;

    @Schema(description = "Mapping of template argument placeholders to master label identifiers for dynamic content substitution",
            example = "{\"<#ARG1>\": \"MASTER_LABEL_USER_NAME\", \"<#ARG2>\": \"MASTER_LABEL_OTP_CODE\"}",
            required = false)
    private Map<String, String> argumentsMap;
}