package com.jio.digigov.notification.entity.otp;

import com.jio.digigov.notification.entity.base.BaseEntity;
import com.jio.digigov.notification.enums.OTPStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

/**
 * OTP Record entity for tracking OTP generation and verification.
 *
 * This entity represents a single OTP record for INIT_OTP events in the notification
 * system. It stores the generated OTP value along with metadata required for secure
 * verification including expiry time, retry attempts, and linking to the original event.
 *
 * Multi-Tenant Architecture:
 * - Stored in tenant-specific MongoDB databases (tenant_db_{tenantId})
 * - System OTPs stored in shared database (tenant_db_shared)
 * - No tenantId field required due to database-level isolation
 * - Indexed for optimal query performance on txnId lookups
 *
 * Security Features:
 * - TTL index on expiryAt for automatic cleanup
 * - Attempt count tracking to prevent brute force
 * - Transaction ID acts as security token for verification
 * - OTP value stored for comparison during verification
 *
 * Processing Lifecycle:
 * PENDING → VERIFIED (success)
 * PENDING → FAILED → MAX_ATTEMPTS_EXCEEDED (failure)
 * PENDING → EXPIRED (time-based via TTL)
 *
 * Integration Points:
 * - ValueGenerator creates record during OTP generation
 * - OTPVerificationService queries by txnId for verification
 * - MongoDB TTL index auto-deletes expired records
 *
 * @author Notification Service Team
 * @version 1.8.0
 * @since 2025-01-21
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "otp_records")
@CompoundIndexes({
    @CompoundIndex(name = "txn_id_event_type_idx",
                  def = "{'txnId': 1, 'eventType': 1}",
                  unique = true),
    @CompoundIndex(name = "event_id_idx",
                  def = "{'eventId': 1}"),
    @CompoundIndex(name = "correlation_id_idx",
                  def = "{'correlationId': 1}"),
    @CompoundIndex(name = "status_created_idx",
                  def = "{'status': 1, 'createdAt': -1}")
})
public class OTPRecord extends BaseEntity {

    /**
     * Transaction identifier used for OTP verification.
     * Acts as the security token - only client with this ID can verify OTP.
     * Extracted from X-Transaction-ID header or correlation ID.
     */
    @NotBlank(message = "Transaction ID is required")
    @Indexed
    @Field("txnId")
    private String txnId;

    /**
     * Correlation identifier linking this OTP to the notification event.
     * Used for audit trail and tracking multiple notifications from same event.
     */
    @NotBlank(message = "Correlation ID is required")
    @Field("correlationId")
    private String correlationId;

    /**
     * Event identifier linking this OTP to the triggering event.
     * References the NotificationEvent collection.
     */
    @NotBlank(message = "Event ID is required")
    @Field("eventId")
    private String eventId;

    /**
     * Event type that triggered OTP generation.
     * Should always be "INIT_OTP" for OTP records.
     */
    @NotBlank(message = "Event type is required")
    @Field("eventType")
    private String eventType;

    /**
     * Notification identifier linking to SMS/Email notification record.
     * References notification_sms or notification_email collection.
     */
    @Field("notificationId")
    private String notificationId;

    /**
     * Business identifier for configuration and authorization.
     * Used for tenant-specific OTP settings and audit.
     */
    @NotBlank(message = "Business ID is required")
    @Field("businessId")
    private String businessId;

    /**
     * Generated OTP value for verification.
     * Format depends on generator configuration (numeric/alphanumeric).
     * Stored for comparison during verification process.
     */
    @NotBlank(message = "OTP value is required")
    @Field("otpValue")
    private String otpValue;

    /**
     * Delivery channel for the OTP.
     * Values: SMS, EMAIL
     */
    @NotBlank(message = "Channel is required")
    @Field("channel")
    private String channel;

    /**
     * Recipient type for the OTP.
     * Values: MOBILE, EMAIL
     */
    @NotBlank(message = "Recipient type is required")
    @Field("recipientType")
    private String recipientType;

    /**
     * Recipient value (mobile number or email address).
     * Used for audit and potential resend scenarios.
     */
    @NotBlank(message = "Recipient value is required")
    @Field("recipientValue")
    private String recipientValue;

    /**
     * OTP expiry timestamp.
     * Calculated as: createdAt + expiryMinutes from configuration
     * MongoDB TTL index automatically deletes records after this time.
     */
    @NotNull(message = "Expiry timestamp is required")
    @Indexed(expireAfterSeconds = 0)
    @Field("expiryAt")
    private LocalDateTime expiryAt;

    /**
     * Timestamp when OTP was successfully verified.
     * Null if not yet verified or verification failed.
     */
    @Field("verifiedAt")
    private LocalDateTime verifiedAt;

    /**
     * Current verification attempt count.
     * Incremented on each failed verification attempt.
     * Used to enforce maxAttempts limit.
     */
    @NotNull(message = "Attempt count is required")
    @Field("attemptCount")
    @Builder.Default
    private Integer attemptCount = 0;

    /**
     * Maximum allowed verification attempts.
     * Configured via OTP generator configuration.
     * Default: 3 attempts
     */
    @NotNull(message = "Max attempts is required")
    @Field("maxAttempts")
    @Builder.Default
    private Integer maxAttempts = 3;

    /**
     * OTP length (for audit and validation).
     * Extracted from generator configuration.
     */
    @Field("otpLength")
    private Integer otpLength;

    /**
     * OTP expiry duration in minutes (for audit).
     * Extracted from generator configuration.
     */
    @Field("expiryMinutes")
    private Integer expiryMinutes;

    /**
     * Current status of the OTP record.
     * Lifecycle: PENDING → VERIFIED/FAILED/MAX_ATTEMPTS_EXCEEDED/EXPIRED
     */
    @NotNull(message = "Status is required")
    @Field("status")
    @Builder.Default
    private OTPStatus status = OTPStatus.PENDING;

    /**
     * Last error message from failed verification attempt.
     * Useful for debugging and audit trail.
     */
    @Field("lastErrorMessage")
    private String lastErrorMessage;
}
