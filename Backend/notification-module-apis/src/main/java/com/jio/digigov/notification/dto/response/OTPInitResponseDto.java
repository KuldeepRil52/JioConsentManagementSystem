package com.jio.digigov.notification.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

/**
 * Response DTO for OTP initialization
 */
@Data
@Builder
@Schema(description = "OTP initialization response")
public class OTPInitResponseDto {

    @Schema(description = "Status", example = "Success")
    private String status;

    @Schema(description = "OTP expiry time", example = "2024-01-01T10:30:00")
    private String expiry;

    @Schema(description = "Transaction ID", example = "TXN_123456")
    private String txnId;
}