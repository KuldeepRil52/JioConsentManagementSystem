package com.jio.digigov.notification.service.callback.impl;

import com.jio.digigov.notification.dto.response.notification.ConsentDeletionDetailsResponseDto;
import com.jio.digigov.notification.dto.response.notification.ConsentDeletionDetailsResponseDto.ConsentInformation;
import com.jio.digigov.notification.dto.response.notification.ConsentDeletionDetailsResponseDto.DataProcessorInfo;
import com.jio.digigov.notification.dto.response.notification.ConsentDeletionDetailsResponseDto.RetentionPolicy;
import com.jio.digigov.notification.dto.response.notification.ConsentDeletionDetailsResponseDto.TimelineEntry;
import com.jio.digigov.notification.entity.notification.NotificationEmail;
import com.jio.digigov.notification.entity.notification.NotificationSms;
import com.jio.digigov.notification.entity.config.RetentionConfig;
import com.jio.digigov.notification.entity.consent.Consent;
import com.jio.digigov.notification.entity.event.DataProcessor;
import com.jio.digigov.notification.entity.event.NotificationEvent;
import com.jio.digigov.notification.entity.notification.NotificationCallback;
import com.jio.digigov.notification.entity.notification.StatusHistoryEntry;
import com.jio.digigov.notification.exception.InvalidEventTypeException;
import com.jio.digigov.notification.exception.ResourceNotFoundException;
import com.jio.digigov.notification.repository.config.RetentionConfigRepository;
import com.jio.digigov.notification.repository.consent.ConsentRepository;
import com.jio.digigov.notification.repository.event.DataProcessorRepository;
import com.jio.digigov.notification.repository.notification.NotificationCallbackRepository;
import com.jio.digigov.notification.repository.notification.NotificationEmailRepository;
import com.jio.digigov.notification.repository.notification.NotificationSmsRepository;
import com.jio.digigov.notification.service.callback.ConsentDeletionDetailsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Implementation of ConsentDeletionDetailsService.
 *
 * Provides detailed consent deletion information by aggregating data from:
 * - notification_events: event details and withdrawal_data
 * - consents: template name and timestamp
 * - retention_config: data retention policy
 * - notification_callback: DP callback statuses
 * - data_processors: DP names
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ConsentDeletionDetailsServiceImpl implements ConsentDeletionDetailsService {

    private final MongoTemplate mongoTemplate;
    private final ConsentRepository consentRepository;
    private final RetentionConfigRepository retentionConfigRepository;
    private final NotificationCallbackRepository callbackRepository;
    private final DataProcessorRepository dataProcessorRepository;
    private final NotificationSmsRepository smsRepository;
    private final NotificationEmailRepository emailRepository;

    /**
     * Event types that are consent deletion events.
     */
    private static final Set<String> CONSENT_DELETION_EVENT_TYPES = Set.of(
            "CONSENT_EXPIRED",
            "CONSENT_WITHDRAWN"
    );

    @Override
    public ConsentDeletionDetailsResponseDto getConsentDeletionDetails(
            String eventId,
            String tenantId,
            String businessId) {

        log.info("Getting consent deletion details for eventId: {}, tenant: {}, businessId: {}",
                eventId, tenantId, businessId);

        // Validate inputs
        validateInputs(eventId, tenantId, businessId);

        try {
            // 1. Fetch NotificationEvent by eventId and businessId
            NotificationEvent event = fetchNotificationEvent(eventId, businessId, tenantId);

            // 2. Validate event type
            validateConsentDeletionEvent(event, businessId);

            // 3. Extract consentId from event payload
            String consentId = extractConsentId(event);

            // 4. Build ConsentInformation
            ConsentInformation consentInfo = buildConsentInformation(event, consentId, businessId);

            // 5. Build DataFiduciary info from withdrawal_data
            Map<String, Object> dataFiduciary = extractDataFiduciary(event);

            // 6. Build DataProcessors list
            List<DataProcessorInfo> dataProcessors = buildDataProcessorsList(eventId, businessId);

            // 7. Extract piiItems from withdrawal_data
            List<Object> piiItems = extractPiiItems(event);

            // 8. Build notification timeline
            List<TimelineEntry> timeline = buildNotificationTimeline(
                    event, consentId, businessId, dataFiduciary);

            // 9. Build and return response
            ConsentDeletionDetailsResponseDto response = ConsentDeletionDetailsResponseDto.builder()
                    .consentInformation(consentInfo)
                    .dataFiduciary(dataFiduciary)
                    .dataProcessors(dataProcessors)
                    .piiItems(piiItems)
                    .notificationTimeline(timeline)
                    .build();

            log.info("Successfully retrieved consent deletion details for eventId: {}, consentId: {}",
                    eventId, consentId);

            return response;

        } catch (ResourceNotFoundException | InvalidEventTypeException e) {
            // These are specific business exceptions, rethrow as-is
            throw e;
        } catch (IllegalArgumentException e) {
            log.error("Invalid parameters for eventId: {}, businessId: {}, tenantId: {}, error: {}",
                    eventId, businessId, tenantId, e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Error getting consent deletion details for eventId: {}, businessId: {}, tenantId: {}, error: {}",
                    eventId, businessId, tenantId, e.getMessage(), e);
            throw new RuntimeException("Failed to retrieve consent deletion details: " + e.getMessage(), e);
        }
    }

    /**
     * Validates input parameters.
     */
    private void validateInputs(String eventId, String tenantId, String businessId) {
        if (!StringUtils.hasText(eventId)) {
            throw new IllegalArgumentException("Event ID cannot be null or empty");
        }
        if (!StringUtils.hasText(tenantId)) {
            throw new IllegalArgumentException("Tenant ID cannot be null or empty");
        }
        if (!StringUtils.hasText(businessId)) {
            throw new IllegalArgumentException("Business ID cannot be null or empty");
        }
    }

    /**
     * Fetches NotificationEvent by eventId and businessId.
     *
     * @param eventId the event ID to search for
     * @param businessId the business ID to search within
     * @param tenantId the tenant ID for error context
     * @return the NotificationEvent
     * @throws ResourceNotFoundException if event is not found
     */
    private NotificationEvent fetchNotificationEvent(String eventId, String businessId, String tenantId) {
        Query query = Query.query(
                Criteria.where("event_id").is(eventId)
                        .and("business_id").is(businessId)
        );

        NotificationEvent event = mongoTemplate.findOne(query, NotificationEvent.class);

        if (event == null) {
            log.error("Event not found: eventId={}, businessId={}, tenantId={}", eventId, businessId, tenantId);
            throw ResourceNotFoundException.consentEventNotFound(eventId, businessId, tenantId);
        }

        return event;
    }

    /**
     * Validates that the event is a consent deletion event.
     *
     * @param event the event to validate
     * @param businessId the business ID for error context
     * @throws InvalidEventTypeException if the event is not a consent deletion event
     */
    private void validateConsentDeletionEvent(NotificationEvent event, String businessId) {
        if (!CONSENT_DELETION_EVENT_TYPES.contains(event.getEventType())) {
            log.error("Event {} is not a consent deletion event. Type: {}, businessId: {}",
                    event.getEventId(), event.getEventType(), businessId);
            throw InvalidEventTypeException.notConsentDeletionEvent(
                    event.getEventId(), event.getEventType(), businessId);
        }
    }

    /**
     * Extracts consentId from event payload.
     */
    private String extractConsentId(NotificationEvent event) {
        Map<String, Object> eventPayload = event.getEventPayload();
        if (eventPayload == null) {
            log.warn("Event payload is null for eventId: {}", event.getEventId());
            return null;
        }

        Object consentId = eventPayload.get("consentId");
        if (consentId != null) {
            return consentId.toString();
        }

        log.warn("consentId not found in event payload for eventId: {}", event.getEventId());
        return null;
    }

    /**
     * Builds ConsentInformation section.
     */
    private ConsentInformation buildConsentInformation(NotificationEvent event, String consentId, String businessId) {
        ConsentInformation.ConsentInformationBuilder builder = ConsentInformation.builder()
                .consentId(consentId)
                .trigger(mapEventTypeToTrigger(event.getEventType()));

        // Fetch consent details if consentId is available
        if (StringUtils.hasText(consentId)) {
            Optional<Consent> consentOpt = consentRepository.findByConsentIdAndBusinessId(
                    consentId, businessId, mongoTemplate);

            if (consentOpt.isPresent()) {
                Consent consent = consentOpt.get();
                builder.template(consent.getTemplateName());
                builder.timestamp(consent.getEndDate() != null ? consent.getEndDate() : consent.getUpdatedAt());
            } else {
                log.warn("Consent not found for consentId: {}, businessId: {}", consentId, businessId);
            }
        }

        // Fetch retention policy
        RetentionPolicy retentionPolicy = fetchRetentionPolicy(businessId);
        builder.retentionPolicy(retentionPolicy);

        return builder.build();
    }

    /**
     * Maps event type to trigger display value.
     */
    private String mapEventTypeToTrigger(String eventType) {
        if ("CONSENT_WITHDRAWN".equals(eventType)) {
            return "Withdrawal";
        } else if ("CONSENT_EXPIRED".equals(eventType)) {
            return "Expiry";
        }
        return eventType;
    }

    /**
     * Fetches retention policy from retention_config collection.
     */
    private RetentionPolicy fetchRetentionPolicy(String businessId) {
        Optional<RetentionConfig> configOpt = retentionConfigRepository.findByBusinessId(
                businessId, mongoTemplate);

        if (configOpt.isPresent() && configOpt.get().getRetentions() != null) {
            RetentionConfig.RetentionPolicy dataRetention = configOpt.get().getRetentions().getDataRetention();
            if (dataRetention != null) {
                return RetentionPolicy.builder()
                        .value(dataRetention.getValue())
                        .unit(dataRetention.getUnit())
                        .build();
            }
        }

        log.warn("Retention config not found for businessId: {}", businessId);
        return null;
    }

    /**
     * Extracts dataFiduciary from withdrawal_data.
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> extractDataFiduciary(NotificationEvent event) {
        Map<String, Object> withdrawalData = event.getWithdrawalData();
        if (withdrawalData == null) {
            return null;
        }

        Object dataFiduciary = withdrawalData.get("dataFiduciary");
        if (dataFiduciary instanceof Map) {
            return (Map<String, Object>) dataFiduciary;
        }

        return null;
    }

    /**
     * Extracts piiItems from withdrawal_data.
     */
    @SuppressWarnings("unchecked")
    private List<Object> extractPiiItems(NotificationEvent event) {
        Map<String, Object> withdrawalData = event.getWithdrawalData();
        if (withdrawalData == null) {
            return null;
        }

        Object piiItems = withdrawalData.get("piiItems");
        if (piiItems instanceof List) {
            return (List<Object>) piiItems;
        }

        return null;
    }

    /**
     * Builds list of DataProcessor info from notification_callback records.
     */
    private List<DataProcessorInfo> buildDataProcessorsList(String eventId, String businessId) {
        List<DataProcessorInfo> result = new ArrayList<>();

        // Fetch DP callbacks for this event
        List<NotificationCallback> dpCallbacks = callbackRepository.findByEventId(eventId, mongoTemplate);

        // Filter to only DATA_PROCESSOR recipients
        for (NotificationCallback callback : dpCallbacks) {
            if (!"DATA_PROCESSOR".equals(callback.getRecipientType())) {
                continue;
            }

            DataProcessorInfo.DataProcessorInfoBuilder dpInfoBuilder = DataProcessorInfo.builder()
                    .processorId(callback.getRecipientId())
                    .status(callback.getStatus());

            // Fetch processor name
            String processorName = fetchProcessorName(callback.getRecipientId(), businessId);
            dpInfoBuilder.processorName(processorName);

            // Get reason and date from status history
            if (callback.getStatusHistory() != null && !callback.getStatusHistory().isEmpty()) {
                // Get the latest status history entry
                StatusHistoryEntry latestEntry = callback.getStatusHistory()
                        .stream()
                        .max((e1, e2) -> {
                            LocalDateTime t1 = e1.getTimestamp() != null ? e1.getTimestamp() : LocalDateTime.MIN;
                            LocalDateTime t2 = e2.getTimestamp() != null ? e2.getTimestamp() : LocalDateTime.MIN;
                            return t1.compareTo(t2);
                        })
                        .orElse(null);

                if (latestEntry != null) {
                    dpInfoBuilder.reason(latestEntry.getRemark());
                    dpInfoBuilder.reviewDate(latestEntry.getTimestamp());
                }
            }

            result.add(dpInfoBuilder.build());
        }

        return result;
    }

    /**
     * Fetches processor name from data_processors collection.
     */
    private String fetchProcessorName(String processorId, String businessId) {
        Optional<DataProcessor> dpOpt = dataProcessorRepository.findByDataProcessorIdAndBusinessId(
                processorId, businessId, mongoTemplate);

        if (dpOpt.isPresent()) {
            return dpOpt.get().getDataProcessorName();
        }

        // Fallback to processorId if name not found
        log.warn("Data processor name not found for processorId: {}, businessId: {}", processorId, businessId);
        return processorId;
    }

    /**
     * Builds the notification timeline from various sources.
     * Timeline entries are sorted chronologically (oldest first).
     */
    private List<TimelineEntry> buildNotificationTimeline(
            NotificationEvent event,
            String consentId,
            String businessId,
            Map<String, Object> dataFiduciary) {

        List<TimelineEntry> timeline = new ArrayList<>();
        int order = 1;

        // 1. Consent Withdrawn/Expired - from DF callback's created_at
        LocalDateTime consentEventTime = getDfCallbackCreatedAt(event.getEventId(), businessId);

        if (consentEventTime != null) {
            String label = "CONSENT_WITHDRAWN".equals(event.getEventType())
                    ? "Consent Withdrawn" : "Consent Expired";
            timeline.add(TimelineEntry.builder()
                    .label(label)
                    .timestamp(consentEventTime)
                    .order(order++)
                    .build());
        } else {
            log.warn("No DF callback timestamp available for consent event: {}", event.getEventId());
        }

        // 2. DF Deletion Completed - from withdrawal_data.dataFiduciary.executedAt
        LocalDateTime dfExecutedAt = extractDfExecutedAt(dataFiduciary);
        if (dfExecutedAt != null) {
            timeline.add(TimelineEntry.builder()
                    .label("DF Deletion Completed")
                    .timestamp(dfExecutedAt)
                    .order(order++)
                    .build());
        }

        // 3. Processor Updates - last DP callback's DELETED or DEFERRED status timestamp
        LocalDateTime dpUpdatedAt = getLastDpUpdatedTimestamp(event.getEventId());
        if (dpUpdatedAt != null) {
            timeline.add(TimelineEntry.builder()
                    .label("Processor Updates")
                    .timestamp(dpUpdatedAt)
                    .order(order++)
                    .build());
        }

        // 4 & 5. SMS Sent and Email Sent - from DATA_DELETION_NOTIFICATION event
        if (StringUtils.hasText(consentId)) {
            addSmsAndEmailTimeline(timeline, consentId, businessId, event.getEventId(), order);
        }

        // Sort by timestamp (chronological order)
        timeline.sort(Comparator.comparing(
                TimelineEntry::getTimestamp,
                Comparator.nullsLast(Comparator.naturalOrder())));

        // Re-assign order after sorting
        for (int i = 0; i < timeline.size(); i++) {
            timeline.get(i).setOrder(i + 1);
        }

        return timeline;
    }

    /**
     * Extracts executedAt timestamp from dataFiduciary map.
     */
    @SuppressWarnings("unchecked")
    private LocalDateTime extractDfExecutedAt(Map<String, Object> dataFiduciary) {
        if (dataFiduciary == null) {
            return null;
        }

        Object executedAt = dataFiduciary.get("executedAt");
        return convertToLocalDateTime(executedAt);
    }

    /**
     * Gets the created_at timestamp from the DF callback for the given event.
     *
     * @param eventId The event ID
     * @param businessId The business ID
     * @return The created_at timestamp of the DF callback, or null if not found
     */
    private LocalDateTime getDfCallbackCreatedAt(String eventId, String businessId) {
        try {
            List<NotificationCallback> callbacks = callbackRepository.findByEventId(eventId, mongoTemplate);

            for (NotificationCallback callback : callbacks) {
                if ("DATA_FIDUCIARY".equals(callback.getRecipientType())
                        && businessId.equals(callback.getBusinessId())) {
                    LocalDateTime createdAt = callback.getCreatedAt();
                    log.debug("Found DF callback created_at: {} for eventId: {}", createdAt, eventId);
                    return createdAt;
                }
            }

            log.debug("No DF callback found for eventId: {}, businessId: {}", eventId, businessId);
            return null;

        } catch (Exception e) {
            log.error("Error getting DF callback created_at for eventId: {}, error: {}",
                    eventId, e.getMessage());
            return null;
        }
    }

    /**
     * Converts various date formats to LocalDateTime.
     */
    @SuppressWarnings("unchecked")
    private LocalDateTime convertToLocalDateTime(Object dateObj) {
        if (dateObj == null) {
            return null;
        }

        try {
            if (dateObj instanceof LocalDateTime) {
                return (LocalDateTime) dateObj;
            } else if (dateObj instanceof Date) {
                return ((Date) dateObj).toInstant()
                        .atZone(ZoneId.systemDefault())
                        .toLocalDateTime();
            } else if (dateObj instanceof Map) {
                // Handle MongoDB $date format: {"$date": "2026-01-12T05:20:44.322Z"}
                Map<String, Object> dateMap = (Map<String, Object>) dateObj;
                Object dateValue = dateMap.get("$date");
                if (dateValue instanceof String) {
                    return Instant.parse((String) dateValue)
                            .atZone(ZoneId.systemDefault())
                            .toLocalDateTime();
                } else if (dateValue instanceof Long) {
                    return Instant.ofEpochMilli((Long) dateValue)
                            .atZone(ZoneId.systemDefault())
                            .toLocalDateTime();
                }
            } else if (dateObj instanceof String) {
                return Instant.parse((String) dateObj)
                        .atZone(ZoneId.systemDefault())
                        .toLocalDateTime();
            }
        } catch (Exception e) {
            log.warn("Failed to parse date: {}, error: {}", dateObj, e.getMessage());
        }

        return null;
    }

    /**
     * Gets the last DP callback's DELETED or DEFERRED status timestamp.
     */
    private LocalDateTime getLastDpUpdatedTimestamp(String eventId) {
        List<NotificationCallback> dpCallbacks = callbackRepository.findByEventId(eventId, mongoTemplate);

        LocalDateTime lastUpdatedTime = null;

        for (NotificationCallback callback : dpCallbacks) {
            if (!"DATA_PROCESSOR".equals(callback.getRecipientType())) {
                continue;
            }

            if (callback.getStatusHistory() != null) {
                for (StatusHistoryEntry entry : callback.getStatusHistory()) {
                    String status = entry.getStatus();
                    // Check for both DELETED and DEFERRED statuses
                    if (("DELETED".equals(status) || "DEFERRED".equals(status))
                            && entry.getTimestamp() != null) {
                        if (lastUpdatedTime == null || entry.getTimestamp().isAfter(lastUpdatedTime)) {
                            lastUpdatedTime = entry.getTimestamp();
                        }
                    }
                }
            }
        }

        return lastUpdatedTime;
    }

    /**
     * Adds SMS and Email timeline entries by finding DATA_DELETION_NOTIFICATION events.
     * Searches by originalEventId (links to the consent event) or by consentId.
     */
    private void addSmsAndEmailTimeline(
            List<TimelineEntry> timeline,
            String consentId,
            String businessId,
            String originalEventId,
            int startOrder) {

        try {
            // Find DATA_DELETION_NOTIFICATION events linked to this consent event
            // Search by originalEventId (primary) or by consentId (fallback)
            Criteria criteria = new Criteria().andOperator(
                    Criteria.where("event_type").is("DATA_DELETION_NOTIFICATION"),
                    Criteria.where("business_id").is(businessId),
                    new Criteria().orOperator(
                            Criteria.where("event_payload.originalEventId").is(originalEventId),
                            Criteria.where("event_payload.consentId").is(consentId)
                    )
            );

            Query query = Query.query(criteria);

            List<NotificationEvent> deletionNotifEvents = mongoTemplate.find(
                    query, NotificationEvent.class);

            if (deletionNotifEvents.isEmpty()) {
                log.debug("No DATA_DELETION_NOTIFICATION events found for eventId: {} or consentId: {}",
                        originalEventId, consentId);
                return;
            }

            int order = startOrder;

            // For each DATA_DELETION_NOTIFICATION event, check SMS and Email
            for (NotificationEvent notifEvent : deletionNotifEvents) {
                String notifEventId = notifEvent.getEventId();

                // Check SMS
                LocalDateTime smsTimestamp = getSmsSuccessTimestamp(notifEventId);
                if (smsTimestamp != null) {
                    timeline.add(TimelineEntry.builder()
                            .label("SMS Sent")
                            .timestamp(smsTimestamp)
                            .order(order++)
                            .build());
                }

                // Check Email
                LocalDateTime emailTimestamp = getEmailSuccessTimestamp(notifEventId);
                if (emailTimestamp != null) {
                    timeline.add(TimelineEntry.builder()
                            .label("Email Sent")
                            .timestamp(emailTimestamp)
                            .order(order++)
                            .build());
                }
            }

        } catch (Exception e) {
            log.warn("Error fetching SMS/Email timeline for consentId: {}, error: {}",
                    consentId, e.getMessage());
        }
    }

    /**
     * Gets the SMS success timestamp for a given event ID.
     */
    private LocalDateTime getSmsSuccessTimestamp(String eventId) {
        try {
            List<NotificationSms> smsList = smsRepository.findByEventId(eventId, mongoTemplate);

            for (NotificationSms sms : smsList) {
                String status = sms.getStatus();
                if ("SENT".equals(status) || "DELIVERED".equals(status)) {
                    return sms.getUpdatedAt();
                }
            }
        } catch (Exception e) {
            log.warn("Error fetching SMS for eventId: {}, error: {}", eventId, e.getMessage());
        }

        return null;
    }

    /**
     * Gets the Email success timestamp for a given event ID.
     */
    private LocalDateTime getEmailSuccessTimestamp(String eventId) {
        try {
            List<NotificationEmail> emailList = emailRepository.findByEventId(eventId, mongoTemplate);

            for (NotificationEmail email : emailList) {
                String status = email.getStatus();
                if ("SENT".equals(status) || "DELIVERED".equals(status)) {
                    return email.getUpdatedAt();
                }
            }
        } catch (Exception e) {
            log.warn("Error fetching Email for eventId: {}, error: {}", eventId, e.getMessage());
        }

        return null;
    }
}
