package com.jio.digigov.notification.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

/**
 * Response DTO for OTP verification
 */
@Data
@Builder
@Schema(description = "OTP verification response")
public class OTPVerifyResponseDto {

    @JsonProperty("STATUS")
    @Schema(description = "Verification status", example = "VALID")
    private String STATUS;

    public boolean isValid() {
        return "Valid".equals(STATUS);
    }
}