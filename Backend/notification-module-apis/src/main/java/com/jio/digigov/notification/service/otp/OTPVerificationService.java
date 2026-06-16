package com.jio.digigov.notification.service.otp;

import com.jio.digigov.notification.dto.request.otp.VerifyOTPRequestDto;
import com.jio.digigov.notification.dto.response.otp.VerifyOTPResponseDto;
import com.jio.digigov.notification.entity.otp.OTPRecord;
import com.jio.digigov.notification.enums.OTPStatus;
import com.jio.digigov.notification.util.MongoTemplateProvider;
import com.jio.digigov.notification.util.OTPEncryptionUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * Service for OTP verification operations.
 *
 * This service handles the verification of OTPs generated through INIT_OTP events.
 * It provides secure verification logic with expiry checks, attempt limits, and
 * proper status management for both system-wide and tenant-specific OTPs.
 *
 * Security Features:
 * - Transaction ID validation (security token)
 * - Expiry time enforcement
 * - Attempt count tracking (max 3 attempts by default)
 * - RSA-2048 encryption for OTP storage and transmission
 * - Plaintext comparison after decryption (handles non-deterministic RSA encryption)
 * - One-time use enforcement (prevents replay attacks)
 * - Tenant isolation through database-level separation
 *
 * Verification Flow:
 * 1. Lookup OTP record by txnId and eventType
 * 2. Validate record exists and is in PENDING/FAILED state
 * 3. Check expiry timestamp
 * 4. Check attempt count against maxAttempts
 * 5. Decrypt stored OTP from database
 * 6. Decrypt provided OTP (if encrypted) or use plain OTP
 * 7. Compare decrypted plaintext OTP values
 * 8. Update status and attempt count
 * 9. Return detailed response with status and remaining attempts
 *
 * Status Transitions:
 * - SUCCESS: PENDING/FAILED → VERIFIED
 * - INVALID_OTP: PENDING → FAILED (increment attemptCount)
 * - MAX_ATTEMPTS_EXCEEDED: FAILED → MAX_ATTEMPTS_EXCEEDED (when attemptCount >= maxAttempts)
 * - OTP_EXPIRED: Any state → return expired status (TTL will cleanup)
 * - ALREADY_VERIFIED: VERIFIED → return already verified status
 *
 * Multi-Tenancy Support:
 * - System OTPs: tenantId="SYSTEM" routes to tenant_db_shared
 * - Tenant OTPs: tenantId routes to tenant_db_{tenantId}
 * - Complete database-level isolation
 *
 * @author Notification Service Team
 * @version 1.8.0
 * @since 2025-01-21
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class OTPVerificationService {

    private final MongoTemplateProvider mongoTemplateProvider;
    private final OTPEncryptionUtil otpEncryptionUtil;

    /**
     * Verifies an OTP against the stored record.
     *
     * This method performs comprehensive validation including:
     * - Record existence check
     * - Expiry validation
     * - Attempt limit enforcement
     * - Decryption of stored and provided OTP values
     * - Plaintext OTP value comparison
     * - Status management
     *
     * Supports both plain and encrypted OTP submission:
     * - If otpValue is provided: uses it directly as plaintext
     * - If encryptedOtpValue is provided: decrypts it first, then compares
     *
     * @param request VerifyOTPRequestDto containing txnId and either otpValue or encryptedOtpValue
     * @param tenantId Tenant identifier (use "SYSTEM" for system OTPs)
     * @param businessId Business identifier for audit
     * @return VerifyOTPResponseDto with verification result and status
     */
    public VerifyOTPResponseDto verifyOTP(VerifyOTPRequestDto request, String tenantId, String businessId) {
        try {
            log.info("Verifying OTP for eventId: {}, txnId: {}, tenantId: {}, businessId: {}",
                     request.getEventId(), request.getTxnId(), tenantId, businessId);

            // Validate that at least one OTP value is provided
            if ((request.getOtpValue() == null || request.getOtpValue().trim().isEmpty()) &&
                (request.getEncryptedOtpValue() == null || request.getEncryptedOtpValue().trim().isEmpty())) {
                log.warn("Neither plain OTP nor encrypted OTP provided for txnId: {}", request.getTxnId());
                return buildErrorResponse(request.getTxnId(), "INVALID_REQUEST",
                                        "Either otpValue or encryptedOtpValue must be provided", null);
            }

            // Get MongoTemplate for the tenant
            MongoTemplate mongoTemplate = mongoTemplateProvider.getTemplate(tenantId);

            // Find OTP record by eventId, txnId and eventType
            OTPRecord otpRecord = findOTPRecord(request.getEventId(), request.getTxnId(), mongoTemplate);

            // Validate OTP record exists
            if (otpRecord == null) {
                log.warn("OTP not found for eventId: {}, txnId: {}, tenantId: {}", request.getEventId(), request.getTxnId(), tenantId);
                return buildErrorResponse(request.getTxnId(), "OTP_NOT_FOUND",
                                        "No OTP found for the provided transaction ID", null);
            }

            // Check if already verified
            if (OTPStatus.VERIFIED.equals(otpRecord.getStatus())) {
                log.warn("OTP already verified for txnId: {}, tenantId: {}", request.getTxnId(), tenantId);
                return buildErrorResponse(request.getTxnId(), "ALREADY_VERIFIED",
                                        "OTP has already been verified", 0);
            }

            // Check if OTP has expired
            if (isExpired(otpRecord)) {
                log.warn("OTP expired for txnId: {}, expiryAt: {}", request.getTxnId(), otpRecord.getExpiryAt());
                updateOTPStatus(otpRecord, OTPStatus.EXPIRED, "OTP has expired", mongoTemplate);
                return buildErrorResponse(request.getTxnId(), "OTP_EXPIRED",
                                        "OTP has expired", 0);
            }

            // Check if max attempts exceeded
            if (otpRecord.getAttemptCount() >= otpRecord.getMaxAttempts()) {
                log.warn("Max attempts exceeded for txnId: {}, attempts: {}/{}",
                         request.getTxnId(), otpRecord.getAttemptCount(), otpRecord.getMaxAttempts());
                updateOTPStatus(otpRecord, OTPStatus.MAX_ATTEMPTS_EXCEEDED,
                              "Maximum verification attempts exceeded", mongoTemplate);
                return buildErrorResponse(request.getTxnId(), "MAX_ATTEMPTS_EXCEEDED",
                                        "Maximum verification attempts exceeded", 0);
            }

            // Decrypt stored OTP for comparison (RSA encryption is non-deterministic, so we must compare plaintext)
            String decryptedStoredOTP;
            try {
                decryptedStoredOTP = otpEncryptionUtil.decrypt(otpRecord.getOtpValue());
            } catch (Exception e) {
                log.error("Failed to decrypt stored OTP for txnId={}", request.getTxnId());
                return buildErrorResponse(request.getTxnId(), "VERIFICATION_ERROR",
                                        "Failed to decrypt stored OTP", null);
            }

            // Determine OTP comparison strategy based on input
            boolean otpMatches;
            String providedPlainOTP;

            if (request.getEncryptedOtpValue() != null && !request.getEncryptedOtpValue().trim().isEmpty()) {
                // Client sent encrypted OTP - decrypt it first
                log.debug("Decrypting client-provided encrypted OTP for txnId={}", request.getTxnId());
                try {
                    providedPlainOTP = otpEncryptionUtil.decrypt(request.getEncryptedOtpValue());
                } catch (Exception e) {
                    log.error("Failed to decrypt client-provided OTP for txnId={}", request.getTxnId());
                    return buildErrorResponse(request.getTxnId(), "INVALID_REQUEST",
                                            "Invalid encrypted OTP value", null);
                }
            } else {
                // Client sent plain OTP
                log.debug("Using plain OTP from client for txnId={}", request.getTxnId());
                providedPlainOTP = request.getOtpValue();
            }

            // Compare decrypted/plaintext OTP values
            otpMatches = comparePlaintextOTPValues(providedPlainOTP, decryptedStoredOTP);

            if (otpMatches) {
                // OTP is valid - mark as verified
                log.info("OTP verified successfully for txnId: {}, tenantId: {}", request.getTxnId(), tenantId);
                markAsVerified(otpRecord, mongoTemplate);
                return buildSuccessResponse(request.getTxnId());
            } else {
                // OTP is invalid - increment attempt count
                int newAttemptCount = otpRecord.getAttemptCount() + 1;
                int attemptsRemaining = otpRecord.getMaxAttempts() - newAttemptCount;

                log.warn("Invalid OTP for txnId: {}, attempts: {}/{}",
                         request.getTxnId(), newAttemptCount, otpRecord.getMaxAttempts());

                if (newAttemptCount >= otpRecord.getMaxAttempts()) {
                    // Max attempts will be exceeded after this attempt
                    updateOTPStatus(otpRecord, OTPStatus.MAX_ATTEMPTS_EXCEEDED,
                                  "Invalid OTP - maximum attempts exceeded", mongoTemplate);
                    return buildErrorResponse(request.getTxnId(), "MAX_ATTEMPTS_EXCEEDED",
                                            "Maximum verification attempts exceeded", 0);
                } else {
                    // Still have attempts remaining
                    incrementAttemptCount(otpRecord, "Invalid OTP provided", mongoTemplate);
                    return buildErrorResponse(request.getTxnId(), "INVALID_OTP",
                                            "Invalid OTP provided", attemptsRemaining);
                }
            }

        } catch (Exception e) {
            log.error("Error verifying OTP for txnId: {}, tenantId: {}",
                     request.getTxnId(), tenantId, e);
            return buildErrorResponse(request.getTxnId(), "VERIFICATION_ERROR",
                                    "An error occurred during OTP verification: " + e.getMessage(), null);
        }
    }

    /**
     * Finds an OTP record by event ID, transaction ID and event type.
     *
     * @param eventId Event identifier
     * @param txnId Transaction identifier
     * @param mongoTemplate MongoTemplate for database operations
     * @return OTPRecord or null if not found
     */
    private OTPRecord findOTPRecord(String eventId, String txnId, MongoTemplate mongoTemplate) {
        Query query = new Query();
        query.addCriteria(Criteria.where("eventId").is(eventId)
                                  .and("txnId").is(txnId)
                                  .and("eventType").is("INIT_OTP"));
        return mongoTemplate.findOne(query, OTPRecord.class);
    }

    /**
     * Checks if an OTP record has expired.
     *
     * @param otpRecord OTP record to check
     * @return true if expired, false otherwise
     */
    private boolean isExpired(OTPRecord otpRecord) {
        return otpRecord.getExpiryAt() != null
               && LocalDateTime.now().isAfter(otpRecord.getExpiryAt());
    }

    /**
     * Compares two plaintext OTP values.
     *
     * Since RSA encryption with PKCS1 padding is non-deterministic (same plaintext produces
     * different ciphertext each time), we must decrypt both OTPs and compare plaintext values.
     *
     * @param providedPlainOTP Plaintext OTP value provided by user (already decrypted if sent encrypted)
     * @param storedPlainOTP Plaintext OTP value from database (already decrypted)
     * @return true if plaintext OTPs match, false otherwise
     */
    private boolean comparePlaintextOTPValues(String providedPlainOTP, String storedPlainOTP) {
        if (providedPlainOTP == null || storedPlainOTP == null) {
            return false;
        }
        // Trim and compare plaintext values (case-sensitive for numeric OTPs, could be made case-insensitive for alphanumeric)
        return providedPlainOTP.trim().equals(storedPlainOTP.trim());
    }

    /**
     * Marks an OTP record as verified.
     *
     * @param otpRecord OTP record to update
     * @param mongoTemplate MongoTemplate for database operations
     */
    private void markAsVerified(OTPRecord otpRecord, MongoTemplate mongoTemplate) {
        Query query = new Query();
        query.addCriteria(Criteria.where("txnId").is(otpRecord.getTxnId())
                                  .and("eventType").is("INIT_OTP"));

        Update update = new Update();
        update.set("status", OTPStatus.VERIFIED);
        update.set("verifiedAt", LocalDateTime.now());
        update.set("updatedAt", LocalDateTime.now());

        mongoTemplate.updateFirst(query, update, OTPRecord.class);
        log.debug("Marked OTP as VERIFIED for txnId: {}", otpRecord.getTxnId());
    }

    /**
     * Increments the attempt count for an OTP record.
     *
     * @param otpRecord OTP record to update
     * @param errorMessage Error message to store
     * @param mongoTemplate MongoTemplate for database operations
     */
    private void incrementAttemptCount(OTPRecord otpRecord, String errorMessage, MongoTemplate mongoTemplate) {
        Query query = new Query();
        query.addCriteria(Criteria.where("txnId").is(otpRecord.getTxnId())
                                  .and("eventType").is("INIT_OTP"));

        Update update = new Update();
        update.inc("attemptCount", 1);
        update.set("status", OTPStatus.FAILED);
        update.set("lastErrorMessage", errorMessage);
        update.set("updatedAt", LocalDateTime.now());

        mongoTemplate.updateFirst(query, update, OTPRecord.class);
        log.debug("Incremented attempt count for txnId: {}, new count: {}",
                  otpRecord.getTxnId(), otpRecord.getAttemptCount() + 1);
    }

    /**
     * Updates the status of an OTP record.
     *
     * @param otpRecord OTP record to update
     * @param status New status
     * @param errorMessage Error message to store
     * @param mongoTemplate MongoTemplate for database operations
     */
    private void updateOTPStatus(OTPRecord otpRecord, OTPStatus status, String errorMessage, MongoTemplate mongoTemplate) {
        Query query = new Query();
        query.addCriteria(Criteria.where("txnId").is(otpRecord.getTxnId())
                                  .and("eventType").is("INIT_OTP"));

        Update update = new Update();
        update.set("status", status);
        update.set("lastErrorMessage", errorMessage);
        update.set("updatedAt", LocalDateTime.now());

        if (status == OTPStatus.MAX_ATTEMPTS_EXCEEDED) {
            update.inc("attemptCount", 1);
        }

        mongoTemplate.updateFirst(query, update, OTPRecord.class);
        log.debug("Updated OTP status to {} for txnId: {}", status, otpRecord.getTxnId());
    }

    /**
     * Builds a success response for OTP verification.
     *
     * @param txnId Transaction identifier
     * @return VerifyOTPResponseDto with success status
     */
    private VerifyOTPResponseDto buildSuccessResponse(String txnId) {
        return VerifyOTPResponseDto.builder()
                .verified(true)
                .status("SUCCESS")
                .message("OTP verified successfully")
                .attemptsRemaining(null)
                .txnId(txnId)
                .build();
    }

    /**
     * Builds an error response for OTP verification.
     *
     * @param txnId Transaction identifier
     * @param status Status code
     * @param message Error message
     * @param attemptsRemaining Number of attempts remaining (null if not applicable)
     * @return VerifyOTPResponseDto with error status
     */
    private VerifyOTPResponseDto buildErrorResponse(String txnId, String status, String message, Integer attemptsRemaining) {
        return VerifyOTPResponseDto.builder()
                .verified(false)
                .status(status)
                .message(message)
                .attemptsRemaining(attemptsRemaining)
                .txnId(txnId)
                .build();
    }
}
