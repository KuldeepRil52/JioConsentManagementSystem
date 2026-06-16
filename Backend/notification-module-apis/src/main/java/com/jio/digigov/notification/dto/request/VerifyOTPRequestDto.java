package com.jio.digigov.notification.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Data;

/**
 * Request DTO for OTP verification
 */
@Data
@Builder
@Schema(description = "OTP verification request")
public class VerifyOTPRequestDto {

    @NotBlank
    @Schema(description = "User OTP", example = "123456")
    private String userOTP;

    @NotBlank
    @Schema(description = "Transaction ID", example = "TXN_123456")
    private String txnId;
}