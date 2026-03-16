package com.jio.digigov.notification.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

import java.util.Map;

/**
 * Request DTO for sending SMS notification
 */
@Data
@Builder
@Schema(description = "SMS notification send request")
public class SendSMSRequestDto {

    @NotBlank
    @Schema(description = "Template ID", example = "TEMPL0000000001", required = true)
    private String templateId;

    @NotBlank
    @Schema(description = "Mobile number", example = "9999999999", required = true)
    private String mobileNumber;

    @NotNull
    @Schema(description = "Template arguments", example = "{\"<#ARG1>\": \"John\", \"<#ARG2>\": \"123456\"}", required = true)
    private Map<String, Object> args;

    @Schema(description = "Message details", example = "OTP notification")
    private String messageDetails;

    @Schema(description = "Transaction ID", example = "TXN_123456")
    private String transactionId;
}