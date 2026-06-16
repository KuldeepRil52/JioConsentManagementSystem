package com.jio.digigov.notification.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Data;

import java.util.Map;

/**
 * Request DTO for sending Email notification
 */
@Data
@Builder
@Schema(description = "Email notification send request")
public class SendEmailRequestDto {

    @NotBlank
    @Schema(description = "Template ID", example = "TEMPL0000000001", required = true)
    private String templateId;

    @NotBlank
    @Email
    @Schema(description = "Recipient email", example = "user@example.com", required = true)
    private String to;

    @Email
    @Schema(description = "CC recipient email", example = "ccuser@example.com")
    private String cc;

    @Schema(description = "Template arguments for body",
            example = "{\"<#ARG1>\": \"John\", \"<#ARG2>\": \"123456\"}")
    private Map<String, Object> argsBody;

    @Schema(description = "Template arguments for subject",
            example = "{\"<#ARG1>\": \"Your OTP\", \"<#ARG2>\": \"Code\"}")
    private Map<String, Object> argsSubject;

    @Schema(description = "Template arguments for from name",
            example = "{\"<#ARG1>\": \"John\"}")
    private Map<String, Object> argsFromName;

    @Schema(description = "Message details", example = "OTP notification")
    private String messageDetails;

    @Schema(description = "Transaction ID", example = "TXN_123456")
    private String transactionId;
}