package com.jio.digigov.notification.service.notification;

import com.jio.digigov.notification.dto.request.event.TriggerEventRequestDto;
import com.jio.digigov.notification.entity.event.NotificationEvent;
import com.jio.digigov.notification.entity.notification.NotificationCallback;
import com.jio.digigov.notification.service.kafka.AsyncNotificationProducerService;
import com.jio.digigov.notification.util.ConsentJwtDecoder;
import com.jio.digigov.notification.util.ConsentJwtDecoder.DecodedConsentInfo;
import com.jio.digigov.notification.util.MongoTemplateProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Service for triggering DATA_DELETION_NOTIFICATION events when consent-related
 * notifications are marked as DELETED.
 *
 * <p>This service is triggered when:</p>
 * <ul>
 *   <li>A notification status is updated to "DELETED"</li>
 *   <li>AND the original notification's eventType is "CONSENT_WITHDRAWN" or "CONSENT_EXPIRED"</li>
 *   <li>AND ALL recipients (Data Fiduciary + all Data Processors) have DELETED status</li>
 *   <li>AND no DATA_DELETION_NOTIFICATION has been sent for this consent yet</li>
 * </ul>
 *
 * <p>Special case: If no Data Processors exist for the event, the Data Fiduciary's
 * DELETED status alone triggers the notification.</p>
 *
 * <p>The service extracts Data Principal information from the consent JWT token
 * stored in the original event's payload and triggers a new notification to
 * inform the Data Principal that their data has been deleted.</p>
 *
 * <p><b>Fire-and-Forget Pattern:</b> All errors are logged but not propagated
 * to avoid affecting the original status update operation.</p>
 *
 * <p><b>Duplicate Prevention:</b> The service checks for existing DATA_DELETION_NOTIFICATION
 * events with the same businessId and consentId before triggering to prevent duplicates.</p>
 *
 * @author Notification Service Team
 * @since 2025-01-20
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DataDeletionNotificationService {

    private final AsyncNotificationProducerService asyncNotificationProducerService;
    private final MongoTemplateProvider mongoTemplateProvider;
    private final ConsentJwtDecoder consentJwtDecoder;

    /**
     * Event types that trigger DATA_DELETION_NOTIFICATION when marked DELETED.
     */
    private static final Set<String> CONSENT_DELETION_EVENT_TYPES = Set.of(
        "CONSENT_WITHDRAWN",
        "CONSENT_EXPIRED"
    );

    private static final String DATA_DELETION_EVENT_TYPE = "DATA_DELETION_NOTIFICATION";
    private static final String STATUS_DELETED = "DELETED";
    private static final String STATUS_DEFERRED = "DEFERRED";
    private static final String CONSENT_JWT_TOKEN_KEY = "consentJwtToken";

    /**
     * Checks if conditions are met and triggers DATA_DELETION_NOTIFICATION asynchronously.
     *
     * <p>This method is fire-and-forget - any errors are logged but not propagated
     * to avoid affecting the original status update operation.</p>
     *
     * @param notification The notification callback being updated
     * @param newStatus The new status being set
     * @param tenantId Tenant identifier
     * @param businessId Business identifier
     */
    @Async("kafkaExecutor")
    public void triggerIfApplicable(NotificationCallback notification,
                                    String newStatus,
                                    String tenantId,
                                    String businessId) {
        try {
            // Step 1: Check basic trigger conditions (eventType and status)
            if (!shouldTriggerDataDeletionNotification(notification.getEventType(), newStatus)) {
                log.debug("DATA_DELETION_NOTIFICATION not applicable: eventType={}, newStatus={}",
                    notification.getEventType(), newStatus);
                return;
            }

            log.info("Checking DATA_DELETION_NOTIFICATION eligibility for notificationId={}, eventType={}, eventId={}",
                notification.getNotificationId(), notification.getEventType(), notification.getEventId());

            // Get tenant-specific MongoTemplate
            MongoTemplate mongoTemplate = mongoTemplateProvider.getTemplate(tenantId);

            // Step 2: Check if DF has withdrawal_data and all DPs have DELETED/DEFERRED status
            if (!checkDeletionConditions(notification.getEventId(), mongoTemplate)) {
                log.debug("Deletion conditions not met for eventId={}, skipping notification",
                    notification.getEventId());
                return;
            }

            log.info("All deletion conditions met for eventId={}, proceeding with notification check",
                notification.getEventId());

            // Fetch the original NotificationEvent to get eventPayload
            Optional<NotificationEvent> eventOpt = findEventById(notification.getEventId(), mongoTemplate);

            if (eventOpt.isEmpty()) {
                log.warn("NotificationEvent not found for eventId={}, skipping DATA_DELETION_NOTIFICATION",
                    notification.getEventId());
                return;
            }

            NotificationEvent event = eventOpt.get();
            Map<String, Object> eventPayload = event.getEventPayload();

            if (eventPayload == null) {
                log.warn("EventPayload is null for eventId={}, skipping DATA_DELETION_NOTIFICATION",
                    notification.getEventId());
                return;
            }

            // Extract consentId from eventPayload for duplicate check
            Object consentIdObj = eventPayload.get("consentId");
            if (consentIdObj == null) {
                log.warn("consentId not found in eventPayload for eventId={}, skipping",
                    notification.getEventId());
                return;
            }
            String consentId = consentIdObj.toString();

            // Step 3: Check for duplicate (prevent sending notification twice for same consent)
            if (hasExistingDataDeletionNotification(businessId, consentId, mongoTemplate)) {
                log.info("DATA_DELETION_NOTIFICATION already exists for businessId={}, consentId={}, skipping",
                    businessId, consentId);
                return;
            }

            // Extract consent JWT token from eventPayload
            Object jwtTokenObj = eventPayload.get(CONSENT_JWT_TOKEN_KEY);
            if (jwtTokenObj == null) {
                log.warn("consentJwtToken not found in eventPayload for eventId={}, skipping",
                    notification.getEventId());
                return;
            }

            String consentJwtToken = jwtTokenObj.toString();

            // Decode JWT to extract Data Principal identifiers
            Optional<DecodedConsentInfo> decodedInfoOpt = consentJwtDecoder.decodeConsentJwt(consentJwtToken);
            if (decodedInfoOpt.isEmpty()) {
                log.warn("Failed to decode consent JWT for eventId={}, skipping DATA_DELETION_NOTIFICATION",
                    notification.getEventId());
                return;
            }

            DecodedConsentInfo decodedInfo = decodedInfoOpt.get();

            // Build and trigger the DATA_DELETION_NOTIFICATION event
            TriggerEventRequestDto triggerRequest = buildTriggerRequest(
                decodedInfo,
                notification,
                businessId
            );

            String transactionId = generateTransactionId();

            log.info("Sending DATA_DELETION_NOTIFICATION: consentId={}, identifierType={}, identifierValue={}",
                decodedInfo.consentId(),
                decodedInfo.identifierType(),
                maskIdentifier(decodedInfo.identifierValue()));

            // Trigger the event asynchronously (fire-and-forget)
            asyncNotificationProducerService.processEventAsync(
                triggerRequest,
                tenantId,
                businessId,
                transactionId,
                null  // No HTTP context for internal triggers
            ).whenComplete((response, error) -> {
                if (error != null) {
                    log.error("Failed to trigger DATA_DELETION_NOTIFICATION for eventId={}: {}",
                        notification.getEventId(), error.getMessage());
                } else if (response != null) {
                    log.info("DATA_DELETION_NOTIFICATION triggered successfully: eventId={}, newEventId={}, status={}",
                        notification.getEventId(), response.getEventId(), response.getStatus());
                } else {
                    log.warn("DATA_DELETION_NOTIFICATION trigger returned null response for eventId={}",
                        notification.getEventId());
                }
            });

        } catch (Exception e) {
            // Fire-and-forget: log error but don't propagate
            log.error("Error triggering DATA_DELETION_NOTIFICATION for notificationId={}: {}",
                notification.getNotificationId(), e.getMessage(), e);
        }
    }

    /**
     * Checks if the given event type and new status should trigger a data deletion notification.
     * Triggers on both DELETED and DEFERRED statuses for consent deletion events.
     *
     * @param eventType The notification's event type
     * @param newStatus The new status being set
     * @return true if DATA_DELETION_NOTIFICATION should be triggered
     */
    public boolean shouldTriggerDataDeletionNotification(String eventType, String newStatus) {
        boolean isValidStatus = STATUS_DELETED.equals(newStatus) || STATUS_DEFERRED.equals(newStatus);
        return isValidStatus &&
               eventType != null &&
               CONSENT_DELETION_EVENT_TYPES.contains(eventType);
    }

    /**
     * Finds a NotificationEvent by eventId.
     *
     * @param eventId The event ID to look up
     * @param mongoTemplate The tenant-specific MongoTemplate
     * @return Optional containing the event if found
     */
    private Optional<NotificationEvent> findEventById(String eventId, MongoTemplate mongoTemplate) {
        Query query = Query.query(Criteria.where("event_id").is(eventId));
        NotificationEvent event = mongoTemplate.findOne(query, NotificationEvent.class);
        return Optional.ofNullable(event);
    }

    /**
     * Checks if deletion conditions are met:
     * - For DF: withdrawal_data must be present in notification_events
     * - For all DPs: status_history must contain DELETED or DEFERRED
     *
     * @param eventId The event ID to check
     * @param mongoTemplate The tenant-specific MongoTemplate
     * @return true if all conditions are met, false otherwise
     */
    private boolean checkDeletionConditions(String eventId, MongoTemplate mongoTemplate) {
        try {
            // Check DF condition: withdrawal_data must be present in notification_events
            if (!checkDfWithdrawalDataPresent(eventId, mongoTemplate)) {
                log.debug("DF withdrawal_data not present for eventId={}", eventId);
                return false;
            }

            // Check DP conditions: all DPs must have DELETED or DEFERRED in status_history
            if (!checkAllDpsHaveDeletedOrDeferred(eventId, mongoTemplate)) {
                log.debug("Not all DPs have DELETED/DEFERRED status for eventId={}", eventId);
                return false;
            }

            log.info("All deletion conditions met for eventId={}", eventId);
            return true;

        } catch (Exception e) {
            log.error("Error checking deletion conditions for eventId={}: {}",
                eventId, e.getMessage(), e);
            return false;
        }
    }

    /**
     * Checks if withdrawal_data is present in notification_events for the given event.
     *
     * @param eventId The event ID to check
     * @param mongoTemplate The tenant-specific MongoTemplate
     * @return true if withdrawal_data is present
     */
    private boolean checkDfWithdrawalDataPresent(String eventId, MongoTemplate mongoTemplate) {
        try {
            Query query = Query.query(Criteria.where("event_id").is(eventId));
            NotificationEvent event = mongoTemplate.findOne(query, NotificationEvent.class);

            if (event == null) {
                log.warn("NotificationEvent not found for eventId={}", eventId);
                return false;
            }

            boolean hasWithdrawalData = event.getWithdrawalData() != null
                    && !event.getWithdrawalData().isEmpty();

            log.debug("DF withdrawal_data present: {} for eventId={}", hasWithdrawalData, eventId);
            return hasWithdrawalData;

        } catch (Exception e) {
            log.error("Error checking DF withdrawal_data for eventId={}: {}", eventId, e.getMessage());
            return false;
        }
    }

    /**
     * Checks if all DP callbacks have DELETED or DEFERRED status in their status_history.
     *
     * @param eventId The event ID to check callbacks for
     * @param mongoTemplate The tenant-specific MongoTemplate
     * @return true if all DPs have DELETED or DEFERRED status, or if no DPs exist
     */
    private boolean checkAllDpsHaveDeletedOrDeferred(String eventId, MongoTemplate mongoTemplate) {
        try {
            // Query all callbacks for this event
            Query query = Query.query(Criteria.where("eventId").is(eventId));
            List<NotificationCallback> callbacks = mongoTemplate.find(query, NotificationCallback.class);

            // Filter to only DATA_PROCESSOR recipients
            List<NotificationCallback> dpCallbacks = callbacks.stream()
                    .filter(cb -> "DATA_PROCESSOR".equals(cb.getRecipientType()))
                    .toList();

            // If no DP callbacks, condition is met
            if (dpCallbacks.isEmpty()) {
                log.debug("No DP callbacks found for eventId={}, condition met", eventId);
                return true;
            }

            // Check each DP callback has DELETED or DEFERRED in status_history
            for (NotificationCallback callback : dpCallbacks) {
                if (!hasDeletedOrDeferredStatus(callback)) {
                    log.debug("DP {} does not have DELETED/DEFERRED status for eventId={}",
                            callback.getRecipientId(), eventId);
                    return false;
                }
            }

            log.debug("All {} DPs have DELETED/DEFERRED status for eventId={}",
                    dpCallbacks.size(), eventId);
            return true;

        } catch (Exception e) {
            log.error("Error checking DP status for eventId={}: {}", eventId, e.getMessage());
            return false;
        }
    }

    /**
     * Checks if a callback has DELETED or DEFERRED status in its status_history.
     *
     * @param callback The callback to check
     * @return true if DELETED or DEFERRED status exists
     */
    private boolean hasDeletedOrDeferredStatus(NotificationCallback callback) {
        if (callback.getStatusHistory() == null || callback.getStatusHistory().isEmpty()) {
            return false;
        }
        return callback.getStatusHistory().stream()
                .anyMatch(entry -> {
                    String status = entry.getStatus();
                    return STATUS_DELETED.equals(status) || STATUS_DEFERRED.equals(status);
                });
    }

    /**
     * Checks if a DATA_DELETION_NOTIFICATION event already exists for the given businessId and consentId.
     *
     * <p>This prevents duplicate notifications from being sent for the same consent.</p>
     *
     * @param businessId The business identifier
     * @param consentId The consent identifier
     * @param mongoTemplate The tenant-specific MongoTemplate
     * @return true if a notification already exists, false otherwise
     */
    private boolean hasExistingDataDeletionNotification(String businessId, String consentId, MongoTemplate mongoTemplate) {
        try {
            Query query = Query.query(new Criteria().andOperator(
                Criteria.where("business_id").is(businessId),
                Criteria.where("event_type").is(DATA_DELETION_EVENT_TYPE),
                Criteria.where("event_payload.consentId").is(consentId)
            ));

            boolean exists = mongoTemplate.exists(query, NotificationEvent.class);
            log.debug("Checking existing DATA_DELETION_NOTIFICATION: businessId={}, consentId={}, exists={}",
                businessId, consentId, exists);

            return exists;

        } catch (Exception e) {
            log.error("Error checking existing DATA_DELETION_NOTIFICATION for businessId={}, consentId={}: {}",
                businessId, consentId, e.getMessage(), e);
            // In case of error, return true to be safe and prevent duplicate notifications
            return true;
        }
    }

    /**
     * Builds the TriggerEventRequestDto for DATA_DELETION_NOTIFICATION.
     *
     * @param decodedInfo Decoded consent information from JWT
     * @param originalNotification The original notification being updated
     * @param businessId The business identifier
     * @return Configured trigger request DTO
     */
    private TriggerEventRequestDto buildTriggerRequest(DecodedConsentInfo decodedInfo,
                                                       NotificationCallback originalNotification,
                                                       String businessId) {
        TriggerEventRequestDto request = new TriggerEventRequestDto();
        request.setEventType(DATA_DELETION_EVENT_TYPE);
        request.setResource("consent");
        request.setSource("notification-status-update");

        // Set customer identifiers from decoded JWT (Data Principal)
        TriggerEventRequestDto.CustomerIdentifiersDto customerIdentifiers =
            new TriggerEventRequestDto.CustomerIdentifiersDto();
        customerIdentifiers.setType(decodedInfo.identifierType());
        customerIdentifiers.setValue(decodedInfo.identifierValue());
        request.setCustomerIdentifiers(customerIdentifiers);

        // Build event payload with consent details and traceability info
        Map<String, Object> eventPayload = new HashMap<>();
        eventPayload.put("consentId", decodedInfo.consentId());
        eventPayload.put("originalEventType", originalNotification.getEventType());
        eventPayload.put("originalNotificationId", originalNotification.getNotificationId());
        eventPayload.put("originalEventId", originalNotification.getEventId());
        eventPayload.put("businessId", businessId);
        request.setEventPayload(eventPayload);

        return request;
    }

    /**
     * Generates a unique transaction ID for the data deletion notification.
     *
     * @return Generated transaction ID with DDN prefix
     */
    private String generateTransactionId() {
        return "DDN-" + UUID.randomUUID().toString();
    }

    /**
     * Masks identifier for logging (privacy protection).
     *
     * @param identifier The identifier to mask
     * @return Masked identifier showing only first 2 and last 2 characters
     */
    private String maskIdentifier(String identifier) {
        if (identifier == null || identifier.length() < 4) {
            return "***";
        }
        return identifier.substring(0, 2) + "***" + identifier.substring(identifier.length() - 2);
    }
}
