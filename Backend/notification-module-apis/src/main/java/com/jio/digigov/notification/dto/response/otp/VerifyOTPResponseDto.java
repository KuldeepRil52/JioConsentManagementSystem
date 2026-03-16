package com.jio.digigov.notification.dto.response.otp;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for OTP verification.
 *
 * Provides the result of OTP verification including success/failure status,
 * remaining attempts, and detailed error messages for client handling.
 *
 * Response Scenarios:
 *
 * 1. Success - OTP verified:
 * {
 *   "verified": true,
 *   "status": "SUCCESS",
 *   "message": "OTP verified successfully",
 *   "attemptsRemaining": null
 * }
 *
 * 2. Failure - Invalid OTP:
 * {
 *   "verified": false,
 *   "status": "INVALID_OTP",
 *   "message": "Invalid OTP provided",
 *   "attemptsRemaining": 2
 * }
 *
 * 3. Failure - OTP Expired:
 * {
 *   "verified": false,
 *   "status": "OTP_EXPIRED",
 *   "message": "OTP has expired",
 *   "attemptsRemaining": 0
 * }
 *
 * 4. Failure - Max Attempts Exceeded:
 * {
 *   "verified": false,
 *   "status": "MAX_ATTEMPTS_EXCEEDED",
 *   "message": "Maximum verification attempts exceeded",
 *   "attemptsRemaining": 0
 * }
 *
 * 5. Failure - OTP Not Found:
 * {
 *   "verified": false,
 *   "status": "OTP_NOT_FOUND",
 *   "message": "No OTP found for the provided transaction ID",
 *   "attemptsRemaining": null
 * }
 *
 * @author Notification Service Team
 * @version 1.8.0
 * @since 2025-01-21
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Response for OTP verification")
public class VerifyOTPResponseDto {

    /**
     * Verification result - true if OTP is valid and verified, false otherwise.
     * Primary field for clients to check verification success.
     */
    @Schema(description = "Whether the OTP was successfully verified",
            example = "true",
            required = true)
    private Boolean verified;

    /**
     * Status code indicating the verification result.
     * Values: SUCCESS, INVALID_OTP, OTP_EXPIRED, MAX_ATTEMPTS_EXCEEDED, OTP_NOT_FOUND, ALREADY_VERIFIED
     */
    @Schema(description = "Verification status code",
            example = "SUCCESS",
            allowableValues = {"SUCCESS", "INVALID_OTP", "OTP_EXPIRED", "MAX_ATTEMPTS_EXCEEDED", "OTP_NOT_FOUND", "ALREADY_VERIFIED"},
            required = true)
    private String status;

    /**
     * Human-readable message describing the verification result.
     * Provides context for success or failure reasons.
     */
    @Schema(description = "Descriptive message about the verification result",
            example = "OTP verified successfully",
            required = true)
    private String message;

    /**
     * Number of verification attempts remaining before max limit is reached.
     * Null if OTP is verified, expired, or not found.
     * Helps clients inform users about remaining chances.
     */
    @Schema(description = "Number of verification attempts remaining",
            example = "2")
    private Integer attemptsRemaining;

    /**
     * Transaction identifier that was used for verification (for audit/logging).
     */
    @Schema(description = "Transaction identifier",
            example = "f81d4fae-7dec-11d0-a765-00a0c91e6bf6")
    private String txnId;
}
