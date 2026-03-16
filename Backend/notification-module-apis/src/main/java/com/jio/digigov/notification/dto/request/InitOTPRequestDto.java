package com.jio.digigov.notification.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

import java.util.Map;

/**
 * Request DTO for OTP initialization
 */
@Data
@Builder
@Schema(description = "OTP initialization request")
public class InitOTPRequestDto {

    @NotNull
    @Schema(description = "ID Type: 3 for phone, 4 for email", example = "3", required = true)
    private Integer idType;

    @NotBlank
    @Schema(description = "ID Value (phone number or email)", example = "9999999999", required = true)
    private String idValue;

    @NotBlank
    @Schema(description = "Transaction ID", example = "TXN_123456", required = true)
    private String txnId;

    @NotBlank
    @Schema(description = "System name", example = "JioG2CSystem", required = true)
    private String systemName;

    @NotBlank
    @Schema(description = "Template ID", example = "TEMPL0000000001", required = true)
    private String templateId;

    @NotNull
    @Schema(description = "Template arguments body", example = "{\"<#ARG1>\": \"John\", \"<#ARG2>\": \"123456\"}", required = true)
    private Map<String, Object> argsBody;

    @Schema(description = "Template arguments for from name (email only)", example = "{\"<#ARG1>\": \"John\"}")
    private Map<String, Object> argsFromName;
}