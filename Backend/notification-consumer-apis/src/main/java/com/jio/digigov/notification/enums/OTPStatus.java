package com.jio.digigov.notification.enums;

/**
 * Enumeration for OTP record status.
 *
 * <p>Used to track the lifecycle of OTP generation and verification.
 * This enum provides type safety for OTP status management throughout the system.</p>
 *
 * <p><b>Lifecycle Flow:</b></p>
 * <pre>
 * PENDING → VERIFIED (success path)
 *    ↓
 * FAILED (invalid OTP attempt)
 *    ↓
 * MAX_ATTEMPTS_EXCEEDED (too many failed attempts)
 *    ↓
 * EXPIRED (time-based expiry via TTL)
 * </pre>
 *
 * <p><b>Usage Examples:</b></p>
 * <pre>
 * // Creating new OTP record
 * OTPRecord record = OTPRecord.builder()
 *     .status(OTPStatus.PENDING)
 *     .build();
 *
 * // Checking status in verification logic
 * if (record.getStatus() == OTPStatus.VERIFIED) {
 *     // OTP already used
 * }
 * </pre>
 *
 * @see com.jio.digigov.notification.entity.otp.OTPRecord
 * @since 1.8.0
 */
public enum OTPStatus {
    /** OTP has been generated and is awaiting verification */
    PENDING,

    /** OTP has been successfully verified */
    VERIFIED,

    /** OTP verification attempt failed (invalid code provided) */
    FAILED,

    /** Maximum verification attempts exceeded */
    MAX_ATTEMPTS_EXCEEDED,

    /** OTP has expired (handled by MongoDB TTL index) */
    EXPIRED
}
