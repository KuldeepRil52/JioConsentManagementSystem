package com.jio.digigov.notification.service.otp;

import com.jio.digigov.notification.entity.otp.OTPRecord;
import com.jio.digigov.notification.enums.OTPStatus;
import com.jio.digigov.notification.util.MongoTemplateProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Service for managing OTP record operations.
 *
 * Handles CRUD operations for OTP records with tenant-specific database routing.
 * Provides methods for saving, retrieving, and updating OTP records during
 * generation and verification workflows.
 *
 * Multi-Tenancy Support:
 * - Uses MongoTemplateProvider for tenant-specific database routing
 * - System OTPs (tenantId="SYSTEM") stored in shared database
 * - Tenant OTPs stored in tenant-specific databases (tenant_db_{tenantId})
 *
 * Thread Safety:
 * - Stateless service with no shared mutable state
 * - Safe for concurrent access by multiple Kafka consumer threads
 *
 * @author Notification Service Team
 * @version 1.8.0
 * @since 2025-01-21
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OTPRecordService {

    private final MongoTemplateProvider mongoTemplateProvider;

    /**
     * Saves a new OTP record to the tenant-specific database.
     *
     * @param record OTP record to save
     * @param tenantId Tenant identifier for database routing
     * @return Saved OTP record with generated ID
     */
    public OTPRecord saveOTPRecord(OTPRecord record, String tenantId) {
        log.debug("Saving OTP record: eventId={}, txnId={}, tenantId={}",
                record.getEventId(), record.getTxnId(), tenantId);

        try {
            MongoTemplate mongoTemplate = mongoTemplateProvider.getTemplate(tenantId);
            OTPRecord savedRecord = mongoTemplate.save(record);

            log.info("OTP record saved successfully: id={}, eventId={}, txnId={}, tenantId={}",
                    savedRecord.getId(), savedRecord.getEventId(), savedRecord.getTxnId(), tenantId);

            return savedRecord;
        } catch (Exception e) {
            log.error("Error saving OTP record: eventId={}, txnId={}, tenantId={}",
                    record.getEventId(), record.getTxnId(), tenantId, e);
            throw new RuntimeException("Failed to save OTP record", e);
        }
    }

    /**
     * Finds an OTP record by transaction ID and event type.
     *
     * Used during OTP verification to retrieve the stored OTP value.
     *
     * @param txnId Transaction identifier (security token)
     * @param eventType Event type (should be "INIT_OTP")
     * @param tenantId Tenant identifier for database routing
     * @return Optional containing OTP record if found
     */
    public Optional<OTPRecord> findByTxnIdAndEventType(String txnId, String eventType, String tenantId) {
        log.debug("Finding OTP record: txnId={}, eventType={}, tenantId={}", txnId, eventType, tenantId);

        try {
            MongoTemplate mongoTemplate = mongoTemplateProvider.getTemplate(tenantId);

            Query query = new Query(
                    Criteria.where("txnId").is(txnId)
                            .and("eventType").is(eventType)
            );

            OTPRecord record = mongoTemplate.findOne(query, OTPRecord.class);

            if (record != null) {
                log.debug("OTP record found: id={}, status={}, attempts={}/{}",
                        record.getId(), record.getStatus(), record.getAttemptCount(), record.getMaxAttempts());
            } else {
                log.debug("No OTP record found for txnId={}, eventType={}", txnId, eventType);
            }

            return Optional.ofNullable(record);
        } catch (Exception e) {
            log.error("Error finding OTP record: txnId={}, eventType={}, tenantId={}",
                    txnId, eventType, tenantId, e);
            throw new RuntimeException("Failed to find OTP record", e);
        }
    }

    /**
     * Increments the attempt count for an OTP record.
     *
     * Called after each failed verification attempt.
     * Automatically sets status to MAX_ATTEMPTS_EXCEEDED if limit reached.
     *
     * @param txnId Transaction identifier
     * @param eventType Event type
     * @param errorMessage Error message from failed attempt
     * @param tenantId Tenant identifier for database routing
     * @return true if increment successful, false otherwise
     */
    public boolean incrementAttemptCount(String txnId, String eventType, String errorMessage, String tenantId) {
        log.debug("Incrementing attempt count: txnId={}, tenantId={}", txnId, tenantId);

        try {
            MongoTemplate mongoTemplate = mongoTemplateProvider.getTemplate(tenantId);

            // First, get the current record to check attempts
            Optional<OTPRecord> recordOpt = findByTxnIdAndEventType(txnId, eventType, tenantId);

            if (recordOpt.isEmpty()) {
                log.warn("Cannot increment attempts - OTP record not found: txnId={}", txnId);
                return false;
            }

            OTPRecord record = recordOpt.get();
            int newAttemptCount = record.getAttemptCount() + 1;

            // Determine new status
            OTPStatus newStatus = newAttemptCount >= record.getMaxAttempts()
                    ? OTPStatus.MAX_ATTEMPTS_EXCEEDED
                    : OTPStatus.FAILED;

            Query query = new Query(
                    Criteria.where("txnId").is(txnId)
                            .and("eventType").is(eventType)
            );

            Update update = new Update()
                    .set("attemptCount", newAttemptCount)
                    .set("status", newStatus)
                    .set("lastErrorMessage", errorMessage)
                    .set("updatedAt", LocalDateTime.now());

            mongoTemplate.updateFirst(query, update, OTPRecord.class);

            log.info("OTP attempt count incremented: txnId={}, attempts={}/{}, status={}, tenantId={}",
                    txnId, newAttemptCount, record.getMaxAttempts(), newStatus, tenantId);

            return true;
        } catch (Exception e) {
            log.error("Error incrementing attempt count: txnId={}, tenantId={}", txnId, tenantId);
            throw new RuntimeException("Failed to increment attempt count", e);
        }
    }

    /**
     * Marks an OTP record as verified.
     *
     * Called after successful OTP verification.
     * Sets status to VERIFIED and records verification timestamp.
     *
     * @param txnId Transaction identifier
     * @param eventType Event type
     * @param tenantId Tenant identifier for database routing
     * @return true if update successful, false otherwise
     */
    public boolean markAsVerified(String txnId, String eventType, String tenantId) {
        log.debug("Marking OTP as verified: txnId={}, tenantId={}", txnId, tenantId);

        try {
            MongoTemplate mongoTemplate = mongoTemplateProvider.getTemplate(tenantId);

            Query query = new Query(
                    Criteria.where("txnId").is(txnId)
                            .and("eventType").is(eventType)
            );

            LocalDateTime now = LocalDateTime.now();

            Update update = new Update()
                    .set("status", OTPStatus.VERIFIED)
                    .set("verifiedAt", now)
                    .set("updatedAt", now);

            long modifiedCount = mongoTemplate.updateFirst(query, update, OTPRecord.class).getModifiedCount();

            if (modifiedCount > 0) {
                log.info("OTP marked as verified: txnId={}, tenantId={}", txnId, tenantId);
                return true;
            } else {
                log.warn("OTP record not found for verification: txnId={}, tenantId={}", txnId, tenantId);
                return false;
            }
        } catch (Exception e) {
            log.error("Error marking OTP as verified: txnId={}, tenantId={}", txnId, tenantId);
            throw new RuntimeException("Failed to mark OTP as verified", e);
        }
    }

    /**
     * Finds an OTP record by event ID.
     *
     * Useful for audit and debugging purposes.
     *
     * @param eventId Event identifier
     * @param tenantId Tenant identifier for database routing
     * @return Optional containing OTP record if found
     */
    public Optional<OTPRecord> findByEventId(String eventId, String tenantId) {
        log.debug("Finding OTP record by eventId: eventId={}, tenantId={}", eventId, tenantId);

        try {
            MongoTemplate mongoTemplate = mongoTemplateProvider.getTemplate(tenantId);

            Query query = new Query(Criteria.where("eventId").is(eventId));
            OTPRecord record = mongoTemplate.findOne(query, OTPRecord.class);

            return Optional.ofNullable(record);
        } catch (Exception e) {
            log.error("Error finding OTP record by eventId: eventId={}, tenantId={}", eventId, tenantId);
            throw new RuntimeException("Failed to find OTP record by eventId", e);
        }
    }
}
