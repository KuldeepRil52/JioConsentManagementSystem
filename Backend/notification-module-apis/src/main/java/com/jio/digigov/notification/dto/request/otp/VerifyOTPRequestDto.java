package com.jio.digigov.notification.dto.request.otp;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for OTP verification.
 *
 * This DTO is used by clients to verify OTPs that were sent via INIT_OTP events.
 * The txnId acts as the security token linking the verification request to the
 * originally generated OTP.
 *
 * Usage Examples:
 * <pre>
 * // System OTP verification (no tenant headers required)
 * POST /api/v1/system/otp/verify
 * {
 *   "txnId": "f81d4fae-7dec-11d0-a765-00a0c91e6bf6",
 *   "otpValue": "123456"
 * }
 *
 * // Tenant OTP verification (requires X-Tenant-ID and X-Business-ID headers)
 * POST /api/v1/otp/verify
 * Headers: X-Tenant-ID: tenant123, X-Business-ID: business456
 * {
 *   "txnId": "f81d4fae-7dec-11d0-a765-00a0c91e6bf6",
 *   "otpValue": "123456"
 * }
 * </pre>
 *
 * Security Note:
 * The txnId is the transaction identifier returned in the INIT_OTP trigger response.
 * Only clients with the correct txnId can verify the OTP, providing security through
 * obscurity combined with expiry and attempt limits.
 *
 * @author Notification Service Team
 * @version 1.8.0
 * @since 2025-01-21
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request for OTP verification")
public class VerifyOTPRequestDto {

    /**
     * Event identifier linking to the OTP record.
     * This is the eventId returned in the INIT_OTP trigger API response.
     * Provides additional security by requiring both eventId and txnId.
     */
    @NotBlank(message = "Event ID is required")
    @Schema(description = "Event identifier",
            example = "EVT_20251105_90305",
            required = true)
    private String eventId;

    /**
     * Transaction identifier linking to the OTP record.
     * This is the transactionId returned in the INIT_OTP trigger API response.
     * Acts as the security token for OTP verification.
     */
    @NotBlank(message = "Transaction ID is required")
    @Schema(description = "Transaction identifier (security token)",
            example = "f81d4fae-7dec-11d0-a765-00a0c91e6bf6",
            required = true)
    private String txnId;

    /**
     * Plain OTP value to verify (as received by the user).
     * Format depends on OTP generation config (numeric or alphanumeric).
     * Either otpValue or encryptedOtpValue must be provided.
     */
    @Schema(description = "Plain OTP value to verify",
            example = "123456",
            required = false)
    private String otpValue;

    /**
     * Encrypted OTP value (Base64-encoded RSA encrypted value).
     * Clients can encrypt the OTP using the public key before sending for enhanced security.
     * Either otpValue or encryptedOtpValue must be provided.
     * If both are provided, encryptedOtpValue takes precedence.
     */
    @Schema(description = "Encrypted OTP value (Base64-encoded RSA encrypted)",
            example = "aBcDeFgHiJkLmNoPqRsTuVwXyZ0123456789...",
            required = false)
    private String encryptedOtpValue;
}
