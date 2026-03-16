package com.jio.digigov.notification.service.event;

import com.jio.digigov.notification.dto.response.common.CountResponseDto;
import com.jio.digigov.notification.dto.response.common.PagedResponseDto;
import com.jio.digigov.notification.dto.response.event.NotificationEventResponseDto;
import com.jio.digigov.notification.dto.response.notification.CallbackNotificationDto;
import com.jio.digigov.notification.dto.response.notification.EmailNotificationDto;
import com.jio.digigov.notification.dto.response.notification.NotificationDetailsResponseDto;
import com.jio.digigov.notification.dto.response.notification.SmsNotificationDto;
import com.jio.digigov.notification.entity.event.NotificationEvent;
import com.jio.digigov.notification.entity.notification.NotificationCallback;
import com.jio.digigov.notification.entity.notification.NotificationEmail;
import com.jio.digigov.notification.entity.notification.NotificationSms;
import com.jio.digigov.notification.enums.EventStatus;
import com.jio.digigov.notification.exception.ResourceNotFoundException;
import com.jio.digigov.notification.repository.event.NotificationEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationOperation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;
import org.bson.Document;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.springframework.data.mongodb.core.aggregation.Aggregation.*;

import java.time.ZoneId;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationEventService {

    private final NotificationEventRepository eventRepository;
    private final MongoTemplate mongoTemplate;

    public PagedResponseDto<NotificationEventResponseDto> getAllEvents(
            String tenantId, String businessId, String eventType, String resource,
            String source, String status, String fromDate, String toDate,
            Boolean includeNotifications, String notificationStatus, String notificationChannel,
            String mobile, String email, String priority, String dataProcessorId, String language,
            int page, int size, String sort) {

        log.info("Getting events for tenant: {}, business: {}, includeNotifications: {}",
                tenantId, businessId, includeNotifications);

        if (Boolean.TRUE.equals(includeNotifications)) {
            // Use aggregation query to include notification details
            return getAllEventsWithNotifications(tenantId, businessId, eventType, resource, source,
                    status, fromDate, toDate, notificationStatus, notificationChannel, mobile, email,
                    priority, dataProcessorId, language, page, size, sort);
        } else {
            // Use simple query without notification details
            return getAllEventsSimple(tenantId, businessId, eventType, resource, source, status,
                    fromDate, toDate, mobile, email, priority, dataProcessorId, language,
                    page, size, sort);
        }
    }

    private PagedResponseDto<NotificationEventResponseDto> getAllEventsSimple(
            String tenantId, String businessId, String eventType, String resource,
            String source, String status, String fromDate, String toDate,
            String mobile, String email, String priority, String dataProcessorId, String language,
            int page, int size, String sort) {

        log.info("Executing simple query for events (no notification details)");

        // Build query criteria with all filters
        Criteria criteria = buildCriteria(businessId, eventType, resource, source, status,
                fromDate, toDate, mobile, email, priority, dataProcessorId, language);

        // Build sort
        Sort sortObj = buildSort(sort);
        Pageable pageable = PageRequest.of(page, size, sortObj);

        // Execute query
        Query query = new Query(criteria);
        long totalElements = mongoTemplate.count(query, NotificationEvent.class);

        query.with(pageable);
        List<NotificationEvent> events = mongoTemplate.find(query, NotificationEvent.class);

        // Convert to response DTOs (without notifications)
        List<NotificationEventResponseDto> responses = events.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());

        int totalPages = (int) Math.ceil((double) totalElements / size);

        PagedResponseDto.PaginationInfo paginationInfo = PagedResponseDto.PaginationInfo.builder()
                .page(page)
                .pageSize(size)
                .totalItems(totalElements)
                .totalPages(totalPages)
                .hasNext((page + 1) < totalPages)
                .hasPrevious(page > 0)
                .build();

        return PagedResponseDto.<NotificationEventResponseDto>builder()
                .data(responses)
                .pagination(paginationInfo)
                .build();
    }

    private PagedResponseDto<NotificationEventResponseDto> getAllEventsWithNotifications(
            String tenantId, String businessId, String eventType, String resource,
            String source, String status, String fromDate, String toDate,
            String notificationStatus, String notificationChannel,
            String mobile, String email, String priority, String dataProcessorId, String language,
            int page, int size, String sort) {

        log.info("Executing aggregation query with notification details");

        // Build aggregation pipeline
        List<AggregationOperation> operations = new ArrayList<>();

        // Step 1: Match events based on criteria
        Criteria eventCriteria = buildCriteria(businessId, eventType, resource, source, status,
                fromDate, toDate, mobile, email, priority, dataProcessorId, language);
        operations.add(match(eventCriteria));

        // Step 2: Lookup SMS notifications
        // localField is event_id (snake_case in notification_events collection)
        // foreignField is eventId (camelCase in notification_sms collection - actual DB field)
        operations.add(lookup("notification_sms", "event_id", "eventId", "smsNotifications"));

        // Step 3: Lookup Email notifications
        // localField is event_id (snake_case in notification_events collection)
        // foreignField is eventId (camelCase in notification_email collection)
        operations.add(lookup("notification_email", "event_id", "eventId", "emailNotifications"));

        // Step 4: Lookup Callback notifications
        // localField is event_id (snake_case in notification_events collection)
        // foreignField is eventId (camelCase in notification_callback collection)
        operations.add(lookup("notification_callback", "event_id", "eventId", "callbackNotifications"));

        // Note: Notification status/channel filtering is done in-memory after aggregation
        // to avoid complex MongoDB aggregation expression syntax issues

        // Step 5: Sort
        Sort sortObj = buildSort(sort);
        operations.add(sort(sortObj));

        // Step 6: Get total count before pagination
        Aggregation countAggregation = newAggregation(operations.toArray(new AggregationOperation[0]));
        countAggregation = Aggregation.newAggregation(
                operations.get(0), // match
                count().as("total")
        );
        AggregationResults<Document> countResults = mongoTemplate.aggregate(countAggregation, "notification_events", Document.class);
        long totalElements = countResults.getUniqueMappedResult() != null ?
                ((Number) countResults.getUniqueMappedResult().get("total")).longValue() : 0;

        // Step 7: Add pagination
        operations.add(skip((long) page * size));
        operations.add(limit(size));

        // Execute aggregation
        Aggregation aggregation = newAggregation(operations);
        AggregationResults<Document> results = mongoTemplate.aggregate(aggregation, "notification_events", Document.class);

        // Debug: Log the raw aggregation results to verify lookup joins
        if (!results.getMappedResults().isEmpty()) {
            Document firstDoc = results.getMappedResults().get(0);
            log.debug("Aggregation result sample - eventId: {}", firstDoc.get("event_id"));
            log.debug("SMS notifications count: {}",
                firstDoc.get("smsNotifications") != null ?
                    ((List<?>) firstDoc.get("smsNotifications")).size() : 0);
            log.debug("Email notifications count: {}",
                firstDoc.get("emailNotifications") != null ?
                    ((List<?>) firstDoc.get("emailNotifications")).size() : 0);
            log.debug("Callback notifications count: {}",
                firstDoc.get("callbackNotifications") != null ?
                    ((List<?>) firstDoc.get("callbackNotifications")).size() : 0);
        }

        // Convert results to DTOs
        List<NotificationEventResponseDto> responses = results.getMappedResults().stream()
                .map(this::documentToResponse)
                .collect(Collectors.toList());

        int totalPages = (int) Math.ceil((double) totalElements / size);

        PagedResponseDto.PaginationInfo paginationInfo = PagedResponseDto.PaginationInfo.builder()
                .page(page)
                .pageSize(size)
                .totalItems(totalElements)
                .totalPages(totalPages)
                .hasNext((page + 1) < totalPages)
                .hasPrevious(page > 0)
                .build();

        return PagedResponseDto.<NotificationEventResponseDto>builder()
                .data(responses)
                .pagination(paginationInfo)
                .build();
    }
    
    public NotificationEventResponseDto getEventById(String eventId, String tenantId, String businessId) {
        log.info("Getting event: {} for tenant: {}, business: {}", eventId, tenantId, businessId);

        // Use mongoTemplate for multi-tenant support (matches getAllEvents pattern)
        Query query = new Query(Criteria.where("event_id").is(eventId).and("business_id").is(businessId));
        NotificationEvent event = mongoTemplate.findOne(query, NotificationEvent.class);

        if (event == null) {
            throw new ResourceNotFoundException("Event not found with ID: " + eventId);
        }

        return toResponse(event);
    }
    
    public CountResponseDto getEventCount(String tenantId, String businessId, String eventType, 
            String resource, String source, String status, String fromDate, String toDate) {
        
        log.info("Getting event count for tenant: {}, business: {}", tenantId, businessId);

        // Build query criteria
        Criteria criteria = buildCriteria(businessId, eventType, resource, source, status, fromDate, toDate,
                null, null, null, null, null);

        // Get total count
        Query query = new Query(criteria);
        long totalCount = mongoTemplate.count(query, NotificationEvent.class);

        // Get status breakdown - build base criteria without status filter
        Map<String, Long> statusBreakdown = new HashMap<>();
        for (String statusValue : List.of(EventStatus.PENDING.name(), EventStatus.PROCESSING.name(),
                                          EventStatus.COMPLETED.name(), EventStatus.FAILED.name())) {
            // Build criteria without the original status filter for breakdown
            Criteria statusCriteria = buildCriteria(businessId, eventType, resource, source, null, fromDate, toDate,
                    null, null, null, null, null)
                    .and("status").is(statusValue);
            Query statusQuery = new Query(statusCriteria);
            long count = mongoTemplate.count(statusQuery, NotificationEvent.class);
            if (count > 0) {
                statusBreakdown.put(statusValue.toLowerCase(), count);
            }
        }
        
        return CountResponseDto.builder()
                .data(CountResponseDto.CountData.builder()
                        .totalCount(totalCount)
                        .breakdown(CountResponseDto.CountData.CountBreakdown.builder()
                                .byStatus(statusBreakdown.entrySet().stream()
                                        .collect(Collectors.toMap(
                                                Map.Entry::getKey,
                                                entry -> entry.getValue().intValue())))
                                .build())
                        .build())
                .build();
    }
    
    private Criteria buildCriteria(String businessId, String eventType, String resource,
            String source, String status, String fromDate, String toDate,
            String mobile, String email, String priority, String dataProcessorId, String language) {

        // Start with base criteria - businessId is optional now
        Criteria criteria = new Criteria();
        List<Criteria> criteriaList = new ArrayList<>();

        // Business ID filter (optional)
        if (businessId != null && !businessId.trim().isEmpty()) {
            criteriaList.add(Criteria.where("business_id").is(businessId));
        }

        // Event type filter
        if (eventType != null && !eventType.trim().isEmpty()) {
            criteriaList.add(Criteria.where("event_type").is(eventType));
        }

        // Resource filter
        if (resource != null && !resource.trim().isEmpty()) {
            criteriaList.add(Criteria.where("resource").is(resource));
        }

        // Source filter
        if (source != null && !source.trim().isEmpty()) {
            criteriaList.add(Criteria.where("source").is(source));
        }

        // Status filter
        if (status != null && !status.trim().isEmpty()) {
            criteriaList.add(Criteria.where("status").is(status.toUpperCase()));
        }

        // Priority filter
        if (priority != null && !priority.trim().isEmpty()) {
            criteriaList.add(Criteria.where("priority").is(priority.toUpperCase()));
        }

        // Language filter
        if (language != null && !language.trim().isEmpty()) {
            criteriaList.add(Criteria.where("language").is(language));
        }

        // Data processor ID filter
        if (dataProcessorId != null && !dataProcessorId.trim().isEmpty()) {
            criteriaList.add(Criteria.where("data_processor_ids").in(dataProcessorId));
        }

        // Customer mobile filter (nested field)
        if (mobile != null && !mobile.trim().isEmpty()) {
            criteriaList.add(Criteria.where("customer_identifiers.value").is(mobile));
        }

        // Customer email filter (nested field)
        if (email != null && !email.trim().isEmpty()) {
            criteriaList.add(Criteria.where("customer_identifiers.value").is(email));
        }

        // Date filtering
        if (fromDate != null && !fromDate.trim().isEmpty()) {
            try {
                LocalDateTime from = fromDate.contains("T") ?
                        LocalDateTime.parse(fromDate) :
                        LocalDateTime.parse(fromDate + "T00:00:00");
                criteriaList.add(Criteria.where("created_at").gte(from));
            } catch (Exception e) {
                log.warn("Invalid fromDate format: {}", fromDate);
            }
        }

        if (toDate != null && !toDate.trim().isEmpty()) {
            try {
                LocalDateTime to = toDate.contains("T") ?
                        LocalDateTime.parse(toDate) :
                        LocalDateTime.parse(toDate + "T23:59:59");
                criteriaList.add(Criteria.where("created_at").lte(to));
            } catch (Exception e) {
                log.warn("Invalid toDate format: {}", toDate);
            }
        }

        // Combine all criteria
        if (!criteriaList.isEmpty()) {
            criteria.andOperator(criteriaList.toArray(new Criteria[0]));
        }

        return criteria;
    }
    
    private Sort buildSort(String sort) {
        if (sort == null || sort.trim().isEmpty()) {
            return Sort.by(Sort.Direction.DESC, "created_at");
        }
        
        String[] parts = sort.split(",");
        List<Sort.Order> orders = new ArrayList<>();
        
        for (String part : parts) {
            String[] fieldDir = part.trim().split(":");
            String field = fieldDir[0];
            Sort.Direction direction = Sort.Direction.DESC;
            
            if (fieldDir.length > 1 && "asc".equalsIgnoreCase(fieldDir[1])) {
                direction = Sort.Direction.ASC;
            }
            
            orders.add(new Sort.Order(direction, field));
        }
        
        return Sort.by(orders);
    }
    
    private NotificationEventResponseDto toResponse(NotificationEvent event) {
        return NotificationEventResponseDto.builder()
                .eventId(event.getEventId())
                .businessId(event.getBusinessId())
                .eventType(event.getEventType())
                .resource(event.getResource())
                .source(event.getSource())
                .language(event.getLanguage())
                .customerIdentifiers(toCustomerIdentifiersResponse(event.getCustomerIdentifiers()))
                .dataProcessorIds(event.getDataProcessorIds())
                .eventPayload(event.getEventPayload())
                .notifications(null) // No notifications in simple query
                .status(event.getStatus() != null ? event.getStatus().name() : null)
                .createdAt(event.getCreatedAt())
                .updatedAt(event.getUpdatedAt())
                .build();
    }

    private NotificationEventResponseDto documentToResponse(Document doc) {
        // Extract event fields
        String eventId = doc.getString("event_id");
        String businessId = doc.getString("business_id");
        String eventType = doc.getString("event_type");
        String resource = doc.getString("resource");
        String source = doc.getString("source");
        String language = doc.getString("language");
        String status = doc.getString("status");

        // Extract customer identifiers
        Document customerIdDoc = doc.get("customer_identifiers", Document.class);
        NotificationEventResponseDto.CustomerIdentifiersDto customerIdentifiers = null;
        if (customerIdDoc != null) {
            customerIdentifiers = NotificationEventResponseDto.CustomerIdentifiersDto.builder()
                    .type(customerIdDoc.getString("type"))
                    .value(customerIdDoc.getString("value"))
                    .build();
        }

        // Extract data processor IDs
        @SuppressWarnings("unchecked")
        List<String> dataProcessorIds = (List<String>) doc.get("data_processor_ids");

        // Extract event payload
        @SuppressWarnings("unchecked")
        Map<String, Object> eventPayload = (Map<String, Object>) doc.get("event_payload");

        // Extract timestamps
        LocalDateTime createdAt = convertToLocalDateTime(doc.getDate("created_at"));
        LocalDateTime updatedAt = convertToLocalDateTime(doc.getDate("updated_at"));

        // Extract and map notifications
        @SuppressWarnings("unchecked")
        List<Document> smsDocsRaw = (List<Document>) doc.get("smsNotifications");
        log.debug("SMS documents raw: {}", smsDocsRaw != null ? smsDocsRaw.size() : "null");
        List<SmsNotificationDto> smsNotifications = smsDocsRaw != null ?
                smsDocsRaw.stream().map(this::mapSmsNotification).collect(Collectors.toList()) : new ArrayList<>();
        log.debug("SMS notifications mapped: {}", smsNotifications.size());

        @SuppressWarnings("unchecked")
        List<Document> emailDocsRaw = (List<Document>) doc.get("emailNotifications");
        List<EmailNotificationDto> emailNotifications = emailDocsRaw != null ?
                emailDocsRaw.stream().map(this::mapEmailNotification).collect(Collectors.toList()) : new ArrayList<>();

        @SuppressWarnings("unchecked")
        List<Document> callbackDocsRaw = (List<Document>) doc.get("callbackNotifications");
        List<CallbackNotificationDto> callbackNotifications = callbackDocsRaw != null ?
                callbackDocsRaw.stream().map(this::mapCallbackNotification).collect(Collectors.toList()) : new ArrayList<>();

        // Build notification details
        NotificationDetailsResponseDto notifications = NotificationDetailsResponseDto.builder()
                .sms(smsNotifications.isEmpty() ? null : smsNotifications)
                .email(emailNotifications.isEmpty() ? null : emailNotifications)
                .callback(callbackNotifications.isEmpty() ? null : callbackNotifications)
                .build();

        return NotificationEventResponseDto.builder()
                .eventId(eventId)
                .businessId(businessId)
                .eventType(eventType)
                .resource(resource)
                .source(source)
                .language(language)
                .customerIdentifiers(customerIdentifiers)
                .dataProcessorIds(dataProcessorIds)
                .eventPayload(eventPayload)
                .notifications(notifications)
                .status(status)
                .createdAt(createdAt)
                .updatedAt(updatedAt)
                .build();
    }

    private SmsNotificationDto mapSmsNotification(Document doc) {
        if (doc == null) return null;

        @SuppressWarnings("unchecked")
        List<String> oprCountries = (List<String>) doc.get("oprCountries");
        @SuppressWarnings("unchecked")
        Map<String, Object> digiGovResponse = (Map<String, Object>) doc.get("digiGovResponse");

        return SmsNotificationDto.builder()
                .notificationId(doc.getString("notificationId"))
                .eventId(doc.getString("eventId"))
                .correlationId(doc.getString("correlationId"))
                .businessId(doc.getString("businessId"))
                .templateId(doc.getString("templateId"))
                .mobile(doc.getString("mobile"))
                .messageContent(doc.getString("messageContent"))
                // .templateArgs(templateArgs) - Removed as per requirement
                .dltEntityId(doc.getString("dltEntityId"))
                .dltTemplateId(doc.getString("dltTemplateId"))
                .from(doc.getString("from"))
                .oprCountries(oprCountries)
                .status(doc.getString("status"))
                .priority(doc.getString("priority"))
                .attemptCount(doc.getInteger("attemptCount"))
                .maxAttempts(doc.getInteger("maxAttempts"))
                .processedAt(convertToLocalDateTime(doc.getDate("processedAt")))
                .sentAt(convertToLocalDateTime(doc.getDate("sentAt")))
                .deliveredAt(convertToLocalDateTime(doc.getDate("deliveredAt")))
                .lastAttemptAt(convertToLocalDateTime(doc.getDate("lastAttemptAt")))
                .nextRetryAt(convertToLocalDateTime(doc.getDate("nextRetryAt")))
                .lastErrorMessage(doc.getString("lastErrorMessage"))
                .digiGovResponse(digiGovResponse)
                .createdAt(convertToLocalDateTime(doc.getDate("createdAt")))
                .updatedAt(convertToLocalDateTime(doc.getDate("updatedAt")))
                .build();
    }

    private EmailNotificationDto mapEmailNotification(Document doc) {
        if (doc == null) return null;

        @SuppressWarnings("unchecked")
        List<String> to = (List<String>) doc.get("to");
        @SuppressWarnings("unchecked")
        List<String> cc = (List<String>) doc.get("cc");
        @SuppressWarnings("unchecked")
        List<String> bcc = (List<String>) doc.get("bcc");
        @SuppressWarnings("unchecked")
        List<Object> attachments = (List<Object>) doc.get("attachments");
        @SuppressWarnings("unchecked")
        Map<String, Object> digiGovResponse = (Map<String, Object>) doc.get("digiGovResponse");

        return EmailNotificationDto.builder()
                .notificationId(doc.getString("notificationId"))
                .eventId(doc.getString("eventId"))
                .correlationId(doc.getString("correlationId"))
                .businessId(doc.getString("businessId"))
                .templateId(doc.getString("templateId"))
                .to(to)
                .cc(cc)
                .bcc(bcc)
                .subject(doc.getString("subject"))
                .content(doc.getString("content"))
                .contentType(doc.getString("contentType"))
                // .templateArgs(templateArgs) - Removed as per requirement
                .emailType(doc.getString("emailType"))
                .replyTo(doc.getString("replyTo"))
                .from(doc.getString("from"))
                .fromName(doc.getString("fromName"))
                .attachments(attachments)
                .status(doc.getString("status"))
                .priority(doc.getString("priority"))
                .attemptCount(doc.getInteger("attemptCount"))
                .maxAttempts(doc.getInteger("maxAttempts"))
                .processedAt(convertToLocalDateTime(doc.getDate("processedAt")))
                .sentAt(convertToLocalDateTime(doc.getDate("sentAt")))
                .deliveredAt(convertToLocalDateTime(doc.getDate("deliveredAt")))
                .openedAt(convertToLocalDateTime(doc.getDate("openedAt")))
                .lastAttemptAt(convertToLocalDateTime(doc.getDate("lastAttemptAt")))
                .nextRetryAt(convertToLocalDateTime(doc.getDate("nextRetryAt")))
                .lastErrorMessage(doc.getString("lastErrorMessage"))
                .digiGovResponse(digiGovResponse)
                .createdAt(convertToLocalDateTime(doc.getDate("createdAt")))
                .updatedAt(convertToLocalDateTime(doc.getDate("updatedAt")))
                .build();
    }

    private CallbackNotificationDto mapCallbackNotification(Document doc) {
        if (doc == null) return null;

        @SuppressWarnings("unchecked")
        Map<String, String> customHeaders = (Map<String, String>) doc.get("customHeaders");
        @SuppressWarnings("unchecked")
        Map<String, Object> webhookResponse = (Map<String, Object>) doc.get("webhookResponse");
        @SuppressWarnings("unchecked")
        List<Object> statusHistory = (List<Object>) doc.get("statusHistory");

        return CallbackNotificationDto.builder()
                .notificationId(doc.getString("notificationId"))
                .eventId(doc.getString("eventId"))
                .correlationId(doc.getString("correlationId"))
                .businessId(doc.getString("businessId"))
                .recipientType(doc.getString("recipientType"))
                .recipientId(doc.getString("recipientId"))
                .callbackUrl(doc.getString("callbackUrl"))
                .eventType(doc.getString("eventType"))
                // .eventData(eventData) - Removed as per requirement: don't need event payload in notifications
                .jwtExpiresAt(convertToLocalDateTime(doc.getDate("jwtExpiresAt")))
                .customHeaders(customHeaders)
                .timeoutSeconds(doc.getInteger("timeoutSeconds"))
                .expectedResponseFormat(doc.getString("expectedResponseFormat"))
                .status(doc.getString("status"))
                .priority(doc.getString("priority"))
                .attemptCount(doc.getInteger("attemptCount"))
                .maxAttempts(doc.getInteger("maxAttempts"))
                .processedAt(convertToLocalDateTime(doc.getDate("processedAt")))
                .deliveredAt(convertToLocalDateTime(doc.getDate("deliveredAt")))
                .acknowledgedAt(convertToLocalDateTime(doc.getDate("acknowledgedAt")))
                .lastAttemptAt(convertToLocalDateTime(doc.getDate("lastAttemptAt")))
                .nextRetryAt(convertToLocalDateTime(doc.getDate("nextRetryAt")))
                .lastErrorMessage(doc.getString("lastErrorMessage"))
                .webhookResponse(webhookResponse)
                .httpStatusCode(doc.getInteger("httpStatusCode"))
                .requestDurationMs(doc.getLong("requestDurationMs"))
                .statusHistory(statusHistory)
                .createdAt(convertToLocalDateTime(doc.getDate("createdAt")))
                .updatedAt(convertToLocalDateTime(doc.getDate("updatedAt")))
                .build();
    }

    private NotificationEventResponseDto.CustomerIdentifiersDto toCustomerIdentifiersResponse(
            NotificationEvent.CustomerIdentifiers customerIdentifiers) {
        if (customerIdentifiers == null) {
            return null;
        }

        return NotificationEventResponseDto.CustomerIdentifiersDto.builder()
                .type(customerIdentifiers.getType())
                .value(customerIdentifiers.getValue())
                .build();
    }

    /**
     * Helper method to convert java.util.Date to LocalDateTime.
     * Handles null values gracefully.
     */
    private LocalDateTime convertToLocalDateTime(java.util.Date date) {
        if (date == null) {
            return null;
        }
        return LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault());
    }
}