package com.jio.digigov.notification.dto.request.template;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import java.util.Map;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = "Email template configuration")
public class EmailTemplateDto {
    
    @NotEmpty(message = "To recipients are required")
    @Schema(description = "List of primary recipient email addresses for template testing and validation",
            example = "[\"test.user@company.com\", \"admin@company.com\"]",
            required = true)
    private List<String> to;

    @Schema(description = "List of carbon copy (CC) recipient email addresses for additional notification recipients",
            example = "[\"manager@company.com\", \"audit@company.com\"]",
            required = false)
    private List<String> cc;

    @NotBlank(message = "Template details are required")
    @Schema(description = "Brief description explaining the email template's purpose and usage context",
            example = "User registration confirmation email with account details",
            required = true)
    private String templateDetails;

    @NotBlank(message = "Template body is required")
    @Schema(description = "HTML or text content of the email body with dynamic argument placeholders. Use <#ARG1>, <#ARG2>, etc. for dynamic values.",
            example = "<html><body><h2>Welcome <#ARG1>!</h2><p>Your account has been created successfully.</p><p>Login with username: <#ARG2></p></body></html>",
            required = true)
    private String templateBody;

    @NotBlank(message = "Template subject is required")
    @Schema(description = "Email subject line with optional dynamic argument placeholders",
            example = "Welcome to DigiGov Platform - <#ARG1>",
            required = true)
    private String templateSubject;
    
    @Schema(description = "Display name shown as the sender in recipient's email client",
            example = "DigiGov Support Team",
            required = false)
    private String templateFromName;

    @Schema(description = "Email content format type",
            example = "HTML",
            allowableValues = {"HTML", "TEXT"},
            defaultValue = "HTML",
            required = true)
    private String emailType = "HTML";

    @Schema(description = "Sender identifier for email delivery",
            example = "noreply@digigov.jio.com",
            required = false)
    private String from;

    @Schema(description = "Reply-to email address for recipient responses",
            example = "support@digigov.jio.com",
            required = false)
    private String replyTo;

    @Schema(description = "Mapping of subject line argument placeholders to master label identifiers",
            example = "{\"<#ARG1>\": \"MASTER_LABEL_USER_NAME\", \"<#ARG2>\": \"MASTER_LABEL_COMPANY_NAME\"}",
            required = false)
    private Map<String, String> argumentsSubjectMap;

    @Schema(description = "Mapping of email body argument placeholders to master label identifiers for dynamic content substitution",
            example = "{\"<#ARG1>\": \"MASTER_LABEL_USER_NAME\", \"<#ARG2>\": \"MASTER_LABEL_LOGIN_URL\"}",
            required = false)
    private Map<String, String> argumentsBodyMap;
}