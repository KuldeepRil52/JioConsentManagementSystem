package com.jio.digigov.notification.repository.notification.impl;

import com.jio.digigov.notification.dto.request.notification.NotificationFilterRequestDto;
import com.jio.digigov.notification.dto.response.notification.NotificationCountResponseDto;
import com.jio.digigov.notification.enums.NotificationStatus;
import com.jio.digigov.notification.dto.response.notification.NotificationResponseDto;
import com.jio.digigov.notification.repository.notification.NotificationRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.*;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.springframework.data.mongodb.core.aggregation.Aggregation.*;

/**
 * Implementation of NotificationRepository using MongoDB aggregation pipelines.
 *
 * This implementation efficiently combines data from multiple collections:
 * - notification_events (main collection)
 * - notification_sms (SMS notifications)
 * - notification_email (Email notifications)
 * - notification_callback (Callback notifications)
 *
 * Uses MongoDB's aggregation framework to perform complex joins and filtering
 * across collections while maintaining performance and tenant isolation.
 */
@Repository
@Slf4j
public class NotificationRepositoryImpl implements NotificationRepository {

    @Override
    public List<NotificationResponseDto> findNotificationsWithFilter(NotificationFilterRequestDto filterRequest,
                                                             MongoTemplate mongoTemplate) {
        log.debug("Finding notifications with filter: {}", filterRequest);

        try {
            Aggregation aggregation = buildNotificationAggregation(filterRequest, false);
            AggregationResults<NotificationResponseDto> results = mongoTemplate.aggregate(
                aggregation, "notification_events", NotificationResponseDto.class);

            List<NotificationResponseDto> notifications = results.getMappedResults();
            log.debug("Found {} notifications", notifications.size());
            return notifications;
        } catch (Exception e) {
            log.error("Error finding notifications with filter");
            throw new RuntimeException("Failed to find notifications", e);
        }
    }

    @Override
    public NotificationCountResponseDto countNotificationsWithFilter(NotificationFilterRequestDto filterRequest,
                                                                 MongoTemplate mongoTemplate) {
        log.debug("Counting notifications with filter: {}", filterRequest);

        try {
            Aggregation aggregation = buildNotificationCountAggregation(filterRequest);
            AggregationResults<Map> results = mongoTemplate.aggregate(
                aggregation, "notification_events", Map.class);

            return buildCountResponse(results.getMappedResults());
        } catch (Exception e) {
            log.error("Error counting notifications with filter");
            throw new RuntimeException("Failed to count notifications", e);
        }
    }

    @Override
    public Optional<NotificationResponseDto> findNotificationByEventId(String eventId, MongoTemplate mongoTemplate) {
        log.debug("Finding notification by eventId: {}", eventId);

        try {
            NotificationFilterRequestDto filter = NotificationFilterRequestDto.builder()
                .eventId(eventId)
                .size(1)
                .includeNotificationDetails(true)
                .includeEventPayload(true)
                .build();

            List<NotificationResponseDto> results = findNotificationsWithFilter(filter, mongoTemplate);
            return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
        } catch (Exception e) {
            log.error("Error finding notification by eventId: {}", eventId);
            return Optional.empty();
        }
    }

    @Override
    public List<NotificationResponseDto> findNotificationsByBusinessId(String businessId, int page, int size,
                                                              MongoTemplate mongoTemplate) {
        NotificationFilterRequestDto filter = NotificationFilterRequestDto.builder()
            .businessId(businessId)
            .page(page)
            .size(size)
            .build();

        return findNotificationsWithFilter(filter, mongoTemplate);
    }

    @Override
    public List<NotificationResponseDto> findNotificationsByEventType(String eventType, int page, int size,
                                                             MongoTemplate mongoTemplate) {
        NotificationFilterRequestDto filter = NotificationFilterRequestDto.builder()
            .eventType(eventType)
            .page(page)
            .size(size)
            .build();

        return findNotificationsWithFilter(filter, mongoTemplate);
    }

    @Override
    public List<NotificationResponseDto> findNotificationsByStatus(String status, int page, int size,
                                                          MongoTemplate mongoTemplate) {
        NotificationFilterRequestDto filter = NotificationFilterRequestDto.builder()
            .status(status)
            .page(page)
            .size(size)
            .build();

        return findNotificationsWithFilter(filter, mongoTemplate);
    }

    @Override
    public long countNotificationsByBusinessId(String businessId, MongoTemplate mongoTemplate) {
        NotificationFilterRequestDto filter = NotificationFilterRequestDto.builder()
            .businessId(businessId)
            .build();

        NotificationCountResponseDto response = countNotificationsWithFilter(filter, mongoTemplate);
        return response.getTotalEvents();
    }

    @Override
    public long countNotificationsByEventType(String eventType, MongoTemplate mongoTemplate) {
        NotificationFilterRequestDto filter = NotificationFilterRequestDto.builder()
            .eventType(eventType)
            .build();

        NotificationCountResponseDto response = countNotificationsWithFilter(filter, mongoTemplate);
        return response.getTotalEvents();
    }

    @Override
    public long countNotificationsByStatus(String status, MongoTemplate mongoTemplate) {
        NotificationFilterRequestDto filter = NotificationFilterRequestDto.builder()
            .status(status)
            .build();

        NotificationCountResponseDto response = countNotificationsWithFilter(filter, mongoTemplate);
        return response.getTotalEvents();
    }

    @Override
    public List<NotificationResponseDto> findRecentNotifications(int limit, MongoTemplate mongoTemplate) {
        NotificationFilterRequestDto filter = NotificationFilterRequestDto.builder()
            .size(limit)
            .sortBy("createdAt")
            .sortDirection("DESC")
            .build();

        return findNotificationsWithFilter(filter, mongoTemplate);
    }

    @Override
    public List<NotificationResponseDto> findNotificationsWithFailures(int page, int size, MongoTemplate mongoTemplate) {
        NotificationFilterRequestDto filter = NotificationFilterRequestDto.builder()
            .hasFailedNotifications(true)
            .page(page)
            .size(size)
            .build();

        return findNotificationsWithFilter(filter, mongoTemplate);
    }

    @Override
    public List<NotificationResponseDto> findNotificationsWithPendingDelivery(int page, int size, MongoTemplate mongoTemplate) {
        NotificationFilterRequestDto filter = NotificationFilterRequestDto.builder()
            .hasPendingNotifications(true)
            .page(page)
            .size(size)
            .build();

        return findNotificationsWithFilter(filter, mongoTemplate);
    }

    @Override
    public NotificationCountResponseDto getNotificationStatistics(MongoTemplate mongoTemplate) {
        NotificationFilterRequestDto filter = NotificationFilterRequestDto.builder().build();
        return countNotificationsWithFilter(filter, mongoTemplate);
    }

    @Override
    public List<com.jio.digigov.notification.dto.response.notification.UnifiedNotificationDto> findFlattenedNotificationsWithFilter(
            NotificationFilterRequestDto filterRequest, MongoTemplate mongoTemplate) {
        log.debug("Finding flattened notifications with filter: {}", filterRequest);

        try {
            Aggregation aggregation = buildNotificationAggregation(filterRequest, false);
            AggregationResults<com.jio.digigov.notification.dto.response.notification.UnifiedNotificationDto> results =
                mongoTemplate.aggregate(
                    aggregation,
                    "notification_events",
                    com.jio.digigov.notification.dto.response.notification.UnifiedNotificationDto.class
                );

            List<com.jio.digigov.notification.dto.response.notification.UnifiedNotificationDto> notifications = results.getMappedResults();
            log.debug("Found {} flattened notifications", notifications.size());
            return notifications;
        } catch (Exception e) {
            log.error("Error finding flattened notifications");
            throw new RuntimeException("Failed to find flattened notifications", e);
        }
    }

    @Override
    public long countFlattenedNotifications(NotificationFilterRequestDto filterRequest, MongoTemplate mongoTemplate) {
        log.debug("Counting flattened notifications with filter: {}", filterRequest);

        try {
            Aggregation aggregation = buildNotificationAggregation(filterRequest, true);
            AggregationResults<org.bson.Document> results = mongoTemplate.aggregate(
                aggregation,
                "notification_events",
                org.bson.Document.class
            );

            List<org.bson.Document> resultList = results.getMappedResults();
            if (resultList.isEmpty()) {
                return 0L;
            }

            Object countValue = resultList.get(0).get("totalCount");
            if (countValue instanceof Number) {
                return ((Number) countValue).longValue();
            }
            return 0L;
        } catch (Exception e) {
            log.error("Error counting flattened notifications");
            return 0L;
        }
    }

    /**
     * Builds optimized MongoDB aggregation pipeline for notification queries.
     * OPTIMIZATION: Flattens, filters, and paginates at MongoDB level instead of in Java.
     */
    private Aggregation buildNotificationAggregation(NotificationFilterRequestDto filter, boolean countOnly) {
        List<AggregationOperation> operations = new ArrayList<>();

        // Step 1: Match events early to reduce data (OPTIMIZATION: filter events first)
        Criteria eventCriteria = buildEventCriteria(filter);
        if (!eventCriteria.getCriteriaObject().isEmpty()) {
            operations.add(match(eventCriteria));
        }

        // Step 2: Lookup notifications (OPTIMIZATION: Use let/pipeline for conditional lookups)
        operations.add(lookup("notification_sms", "event_id", "eventId", "smsNotifications"));
        operations.add(lookup("notification_email", "event_id", "eventId", "emailNotifications"));
        operations.add(lookup("notification_callback", "event_id", "eventId", "callbackNotifications"));

        // Step 3: OPTIMIZATION - Filter at array level before flattening to reduce documents
        if (filter.getNotificationStatus() != null || filter.getTemplateId() != null ||
            filter.getMinAttemptCount() != null || filter.getMaxAttemptCount() != null) {
            operations.add(buildArrayFiltering(filter));
        }

        // Step 4: OPTIMIZATION - Flatten notifications at MongoDB level with type tagging
        operations.add(buildFlatteningStage(filter));

        // Step 5: Unwind to individual notifications
        operations.add(Aggregation.unwind("allNotifications", true));

        // Step 6: OPTIMIZATION - Filter unwound notifications (recipient type, notification type, etc.)
        Criteria flattenedCriteria = buildFlattenedNotificationCriteria(filter);
        if (!flattenedCriteria.getCriteriaObject().isEmpty()) {
            operations.add(match(flattenedCriteria));
        }

        // Step 7: Project to UnifiedNotificationDto structure
        operations.add(buildUnifiedProjection());

        if (countOnly) {
            operations.add(count().as("totalCount"));
        } else {
            // Step 8: OPTIMIZATION - Sort in MongoDB
            operations.add(buildFlattenedSort(filter));

            // Step 9: OPTIMIZATION - Paginate in MongoDB (not in Java!)
            int page = filter.getPage() != null ? filter.getPage() : 0;
            int size = filter.getSize() != null ? filter.getSize() : 20;

            if (page > 0) {
                operations.add(skip((long) page * size));
            }
            operations.add(limit(size));
        }

        return newAggregation(operations);
    }

    /**
     * Builds MongoDB aggregation pipeline for counting notifications.
     */
    private Aggregation buildNotificationCountAggregation(NotificationFilterRequestDto filter) {
        List<AggregationOperation> operations = new ArrayList<>();

        // Match events
        Criteria eventCriteria = buildEventCriteria(filter);
        if (!eventCriteria.getCriteriaObject().isEmpty()) {
            operations.add(match(eventCriteria));
        }

        // Lookup all notification types - notification_events has event_id (snake_case), notifications have eventId (camelCase)
        operations.add(lookup("notification_sms", "event_id", "eventId", "smsNotifications"));
        operations.add(lookup("notification_email", "event_id", "eventId", "emailNotifications"));
        operations.add(lookup("notification_callback", "event_id", "eventId", "callbackNotifications"));

        // Group and count by various dimensions - notification_events uses snake_case
        operations.add(group()
            .count().as("totalEvents")
            .sum(ArrayOperators.Size.lengthOfArray("smsNotifications")).as("totalSms")
            .sum(ArrayOperators.Size.lengthOfArray("emailNotifications")).as("totalEmail")
            .sum(ArrayOperators.Size.lengthOfArray("callbackNotifications")).as("totalCallback")
            .addToSet("status").as("eventStatuses")
            .addToSet("event_type").as("eventTypes")
            .addToSet("priority").as("priorities")
        );

        return newAggregation(operations);
    }

    /**
     * Builds criteria for filtering notification events.
     */
    private Criteria buildEventCriteria(NotificationFilterRequestDto filter) {
        List<Criteria> criteriaList = new ArrayList<>();

        // notification_events collection uses snake_case field names
        if (filter.getEventId() != null) {
            criteriaList.add(Criteria.where("event_id").is(filter.getEventId()));
        }

        if (filter.getEventIds() != null && !filter.getEventIds().isEmpty()) {
            criteriaList.add(Criteria.where("event_id").in(filter.getEventIds()));
        }

        if (filter.getBusinessId() != null) {
            criteriaList.add(Criteria.where("business_id").is(filter.getBusinessId()));
        }

        if (filter.getEventType() != null) {
            criteriaList.add(Criteria.where("event_type").is(filter.getEventType()));
        }

        if (filter.getStatus() != null) {
            criteriaList.add(Criteria.where("status").is(filter.getStatus()));
        }

        if (filter.getPriority() != null) {
            criteriaList.add(Criteria.where("priority").is(filter.getPriority()));
        }

        if (filter.getResource() != null) {
            criteriaList.add(Criteria.where("resource").is(filter.getResource()));
        }

        if (filter.getSource() != null) {
            criteriaList.add(Criteria.where("source").is(filter.getSource()));
        }

        if (filter.getLanguage() != null) {
            criteriaList.add(Criteria.where("language").is(filter.getLanguage()));
        }

        if (filter.getDataProcessorIds() != null && !filter.getDataProcessorIds().isEmpty()) {
            criteriaList.add(Criteria.where("data_processor_ids").in(filter.getDataProcessorIds()));
        }

        // Date range filtering - BaseEntity uses snake_case
        if (filter.getFromDate() != null || filter.getToDate() != null) {
            Criteria dateCriteria = Criteria.where("created_at");
            if (filter.getFromDate() != null) {
                dateCriteria = dateCriteria.gte(filter.getFromDate());
            }
            if (filter.getToDate() != null) {
                dateCriteria = dateCriteria.lt(filter.getToDate());
            }
            criteriaList.add(dateCriteria);
        }

        if (filter.getCreatedAfter() != null) {
            criteriaList.add(Criteria.where("created_at").gt(filter.getCreatedAfter()));
        }

        if (filter.getCreatedBefore() != null) {
            criteriaList.add(Criteria.where("created_at").lt(filter.getCreatedBefore()));
        }

        if (filter.getCompletedAfter() != null) {
            criteriaList.add(Criteria.where("completed_at").gt(filter.getCompletedAfter()));
        }

        if (filter.getCompletedBefore() != null) {
            criteriaList.add(Criteria.where("completed_at").lt(filter.getCompletedBefore()));
        }

        return criteriaList.isEmpty() ? new Criteria() :
               new Criteria().andOperator(criteriaList.toArray(new Criteria[0]));
    }

    /**
     * Builds criteria for filtering based on notification data.
     */
    private Criteria buildNotificationCriteria(NotificationFilterRequestDto filter) {
        List<Criteria> criteriaList = new ArrayList<>();

        // Direct field matching criteria (no OR operators)
        if (filter.getMobile() != null) {
            criteriaList.add(Criteria.where("smsNotifications.mobile").is(filter.getMobile()));
        }

        if (filter.getEmail() != null) {
            criteriaList.add(Criteria.where("emailNotifications.to").is(filter.getEmail()));
        }

        // Handle customerIdentifierType and customerIdentifierValue
        if (filter.getCustomerIdentifierType() != null && filter.getCustomerIdentifierValue() != null) {
            String identifierType = filter.getCustomerIdentifierType().toUpperCase();
            if ("MOBILE".equals(identifierType)) {
                criteriaList.add(Criteria.where("smsNotifications.mobile").is(filter.getCustomerIdentifierValue()));
            } else if ("EMAIL".equals(identifierType)) {
                criteriaList.add(Criteria.where("emailNotifications.to").is(filter.getCustomerIdentifierValue()));
            }
        }

        // Handle minAttemptCount and maxAttemptCount filters
        if (filter.getMinAttemptCount() != null || filter.getMaxAttemptCount() != null) {
            List<Criteria> attemptCriteria = new ArrayList<>();

            if (filter.getMinAttemptCount() != null && filter.getMaxAttemptCount() != null) {
                attemptCriteria.add(Criteria.where("smsNotifications.attemptCount")
                    .gte(filter.getMinAttemptCount()).lte(filter.getMaxAttemptCount()));
                attemptCriteria.add(Criteria.where("emailNotifications.attemptCount")
                    .gte(filter.getMinAttemptCount()).lte(filter.getMaxAttemptCount()));
                attemptCriteria.add(Criteria.where("callbackNotifications.attemptCount")
                    .gte(filter.getMinAttemptCount()).lte(filter.getMaxAttemptCount()));
            } else if (filter.getMinAttemptCount() != null) {
                attemptCriteria.add(Criteria.where("smsNotifications.attemptCount").gte(filter.getMinAttemptCount()));
                attemptCriteria.add(Criteria.where("emailNotifications.attemptCount").gte(filter.getMinAttemptCount()));
                attemptCriteria.add(Criteria.where("callbackNotifications.attemptCount").gte(filter.getMinAttemptCount()));
            } else {
                attemptCriteria.add(Criteria.where("smsNotifications.attemptCount").lte(filter.getMaxAttemptCount()));
                attemptCriteria.add(Criteria.where("emailNotifications.attemptCount").lte(filter.getMaxAttemptCount()));
                attemptCriteria.add(Criteria.where("callbackNotifications.attemptCount").lte(filter.getMaxAttemptCount()));
            }

            if (!attemptCriteria.isEmpty()) {
                criteriaList.add(new Criteria().orOperator(attemptCriteria.toArray(new Criteria[0])));
            }
        }

        // Handle hasNotifications filter
        if (Boolean.TRUE.equals(filter.getHasNotifications())) {
            List<Criteria> hasNotifCriteria = new ArrayList<>();
            hasNotifCriteria.add(Criteria.where("smsNotifications").exists(true).ne(null).not().size(0));
            hasNotifCriteria.add(Criteria.where("emailNotifications").exists(true).ne(null).not().size(0));
            hasNotifCriteria.add(Criteria.where("callbackNotifications").exists(true).ne(null).not().size(0));
            criteriaList.add(new Criteria().orOperator(hasNotifCriteria.toArray(new Criteria[0])));
        }

        // Handle recipientType filter - build OR conditions based on recipient type
        if (filter.getRecipientType() != null) {
            List<Criteria> recipientTypeCriteria = new ArrayList<>();
            String recipientType = filter.getRecipientType().toUpperCase();

            if ("DATA_PRINCIPAL".equals(recipientType)) {
                // DATA_PRINCIPAL means SMS or Email notifications
                recipientTypeCriteria.add(Criteria.where("smsNotifications").exists(true).ne(null).not().size(0));
                recipientTypeCriteria.add(Criteria.where("emailNotifications").exists(true).ne(null).not().size(0));
            } else if ("DATA_FIDUCIARY".equals(recipientType) || "DATA_PROCESSOR".equals(recipientType) || "DATA_PROTECTION_OFFICER".equals(recipientType)) {
                // For these types, filter callbacks by recipientType
                recipientTypeCriteria.add(Criteria.where("callbackNotifications.recipientType").is(recipientType));
            }

            if (!recipientTypeCriteria.isEmpty()) {
                criteriaList.add(new Criteria().orOperator(recipientTypeCriteria.toArray(new Criteria[0])));
            }
        }

        // Handle recipientId filter - build OR conditions across notification types
        if (filter.getRecipientId() != null) {
            List<Criteria> recipientIdCriteria = new ArrayList<>();
            recipientIdCriteria.add(Criteria.where("smsNotifications.mobile").is(filter.getRecipientId()));
            recipientIdCriteria.add(Criteria.where("emailNotifications.to").is(filter.getRecipientId()));
            recipientIdCriteria.add(Criteria.where("callbackNotifications.recipientId").is(filter.getRecipientId()));

            criteriaList.add(new Criteria().orOperator(recipientIdCriteria.toArray(new Criteria[0])));
        }

        // Handle notificationType filter
        if (filter.getNotificationType() != null) {
            String notifType = filter.getNotificationType().toUpperCase();
            if ("SMS".equals(notifType)) {
                criteriaList.add(Criteria.where("smsNotifications").exists(true).ne(null).not().size(0));
            } else if ("EMAIL".equals(notifType)) {
                criteriaList.add(Criteria.where("emailNotifications").exists(true).ne(null).not().size(0));
            } else if ("CALLBACK".equals(notifType)) {
                criteriaList.add(Criteria.where("callbackNotifications").exists(true).ne(null).not().size(0));
            }
        }

        // Collect all OR conditions and build a single comprehensive OR criteria
        List<Criteria> allOrConditions = new ArrayList<>();

        // Template ID search across different notification types
        if (filter.getTemplateId() != null) {
            allOrConditions.add(Criteria.where("smsNotifications.templateId").is(filter.getTemplateId()));
            allOrConditions.add(Criteria.where("emailNotifications.templateId").is(filter.getTemplateId()));
        }

        // Notification status search across different types
        if (filter.getNotificationStatus() != null) {
            allOrConditions.add(Criteria.where("smsNotifications.status").is(filter.getNotificationStatus()));
            allOrConditions.add(Criteria.where("emailNotifications.status").is(filter.getNotificationStatus()));
            allOrConditions.add(Criteria.where("callbackNotifications.status").is(filter.getNotificationStatus()));
        }

        // Correlation ID search across different types
        if (filter.getCorrelationId() != null) {
            allOrConditions.add(Criteria.where("smsNotifications.correlationId").is(filter.getCorrelationId()));
            allOrConditions.add(Criteria.where("emailNotifications.correlationId").is(filter.getCorrelationId()));
            allOrConditions.add(Criteria.where("callbackNotifications.correlationId").is(filter.getCorrelationId()));
        }

        // Failed notifications search
        if (Boolean.TRUE.equals(filter.getHasFailedNotifications())) {
            allOrConditions.add(Criteria.where("smsNotifications.status").is(NotificationStatus.FAILED.name()));
            allOrConditions.add(Criteria.where("emailNotifications.status").is("FAILED"));
            allOrConditions.add(Criteria.where("callbackNotifications.status").is("FAILED"));
        }

        // Pending notifications search
        if (Boolean.TRUE.equals(filter.getHasPendingNotifications())) {
            allOrConditions.add(Criteria.where("smsNotifications.status").is("PENDING"));
            allOrConditions.add(Criteria.where("emailNotifications.status").is("PENDING"));
            allOrConditions.add(Criteria.where("callbackNotifications.status").is("PENDING"));
        }

        // If we have OR conditions, create a single OR criteria
        if (!allOrConditions.isEmpty()) {
            Criteria combinedOrCriteria = new Criteria().orOperator(
                allOrConditions.toArray(new Criteria[0])
            );
            criteriaList.add(combinedOrCriteria);
        }

        return criteriaList.isEmpty() ? new Criteria() :
               new Criteria().andOperator(criteriaList.toArray(new Criteria[0]));
    }

    /**
     * Builds projection for final output.
     * notification_events uses snake_case field names in MongoDB, but we project to camelCase for DTO mapping.
     */
    private ProjectionOperation buildProjection(NotificationFilterRequestDto filter) {
        ProjectionOperation projection = project()
            .andExclude("_id")
            .and("event_id").as("eventId")
            .and("business_id").as("businessId")
            .and("event_type").as("eventType")
            .and("resource").as("resource")
            .and("source").as("source")
            .and("language").as("language")
            .and("customer_identifiers").as("customerIdentifiers")
            .and("data_processor_ids").as("dataProcessorIds")
            .and("status").as("status")
            .and("priority").as("priority")
            .and("created_at").as("createdAt")
            .and("updated_at").as("updatedAt")
            .and("processed_at").as("processedAt")
            .and("completed_at").as("completedAt")
            .and("notifications_summary").as("notificationsSummary");

        if (Boolean.TRUE.equals(filter.getIncludeEventPayload())) {
            projection = projection.and("event_payload").as("eventPayload");
        }

        if (Boolean.TRUE.equals(filter.getIncludeNotificationDetails())) {
            int limit = filter.getNotificationLimit() != null ? filter.getNotificationLimit() : 10;

            projection = projection
                .and(ArrayOperators.Slice.sliceArrayOf("smsNotifications").itemCount(limit)).as("smsNotifications")
                .and(ArrayOperators.Slice.sliceArrayOf("emailNotifications").itemCount(limit)).as("emailNotifications")
                .and(ArrayOperators.Slice.sliceArrayOf("callbackNotifications").itemCount(limit)).as("callbackNotifications");
        } else {
            projection = projection
                .and(ArrayOperators.Slice.sliceArrayOf("smsNotifications").itemCount(0)).as("smsNotifications")
                .and(ArrayOperators.Slice.sliceArrayOf("emailNotifications").itemCount(0)).as("emailNotifications")
                .and(ArrayOperators.Slice.sliceArrayOf("callbackNotifications").itemCount(0)).as("callbackNotifications");
        }

        return projection;
    }

    /**
     * Builds sort operation.
     * notification_events uses snake_case field names in MongoDB.
     */
    private SortOperation buildSort(NotificationFilterRequestDto filter) {
        String sortBy = filter.getSortBy() != null ? filter.getSortBy() : "createdAt";
        String sortDirection = filter.getSortDirection() != null ? filter.getSortDirection() : "DESC";

        // Convert camelCase to snake_case for MongoDB fields
        String mongoFieldName = convertToSnakeCase(sortBy);

        Sort.Direction direction = "ASC".equalsIgnoreCase(sortDirection) ?
            Sort.Direction.ASC : Sort.Direction.DESC;

        return sort(Sort.by(direction, mongoFieldName));
    }

    /**
     * Converts camelCase field names to snake_case for MongoDB queries.
     */
    private String convertToSnakeCase(String camelCase) {
        // Map common field names
        switch (camelCase) {
            case "eventId": return "event_id";
            case "businessId": return "business_id";
            case "eventType": return "event_type";
            case "createdAt": return "created_at";
            case "updatedAt": return "updated_at";
            case "processedAt": return "processed_at";
            case "completedAt": return "completed_at";
            default: return camelCase;
        }
    }

    /**
     * OPTIMIZATION: Builds $project stage to filter notification arrays before unwinding.
     * Reduces the number of documents processed in later stages.
     */
    private ProjectionOperation buildArrayFiltering(NotificationFilterRequestDto filter) {
        // This will filter arrays based on notification-level criteria
        // For now, return arrays as-is; array filtering can be added with $filter operator
        return project()
            .and("event_id").as("event_id")
            .and("business_id").as("business_id")
            .and("event_type").as("event_type")
            .and("status").as("status")
            .and("priority").as("priority")
            .and("created_at").as("created_at")
            .and("smsNotifications").as("smsNotifications")
            .and("emailNotifications").as("emailNotifications")
            .and("callbackNotifications").as("callbackNotifications");
    }

    /**
     * OPTIMIZATION: Builds $project stage that flattens all notifications into a single array.
     * Each notification gets tagged with its type (SMS, EMAIL, CALLBACK) and event metadata.
     */
    private ProjectionOperation buildFlatteningStage(NotificationFilterRequestDto filter) {
        // Create arrays with type tags for each notification type
        return project()
            .and("event_id").as("eventId")
            .and("business_id").as("businessId")
            .and("event_type").as("eventType")
            .and(context -> {
                // Build array of notifications with type field added
                // Using $map to add notificationType and eventType to each notification
                org.bson.Document smsMap = new org.bson.Document("$map", new org.bson.Document()
                    .append("input", "$smsNotifications")
                    .append("as", "notif")
                    .append("in", new org.bson.Document()
                        .append("notificationType", "SMS")
                        .append("notificationId", "$$notif.notificationId")
                        .append("eventId", "$event_id")
                        .append("correlationId", "$$notif.correlationId")
                        .append("businessId", "$business_id")
                        .append("templateId", "$$notif.templateId")
                        .append("eventType", "$event_type")
                        .append("status", "$$notif.status")
                        .append("priority", "$$notif.priority")
                        .append("attemptCount", "$$notif.attemptCount")
                        .append("createdAt", "$$notif.created_at")
                        .append("processedAt", "$$notif.processed_at")
                        .append("sentAt", "$$notif.sentAt")
                        .append("deliveredAt", "$$notif.deliveredAt")
                        .append("lastErrorMessage", "$$notif.lastErrorMessage")
                        .append("recipientType", "DATA_PRINCIPAL")
                        .append("recipientId", "$$notif.mobile")
                        .append("mobile", "$$notif.mobile")
                    )
                );

                org.bson.Document emailMap = new org.bson.Document("$map", new org.bson.Document()
                    .append("input", "$emailNotifications")
                    .append("as", "notif")
                    .append("in", new org.bson.Document()
                        .append("notificationType", "EMAIL")
                        .append("notificationId", "$$notif.notificationId")
                        .append("eventId", "$event_id")
                        .append("correlationId", "$$notif.correlationId")
                        .append("businessId", "$business_id")
                        .append("templateId", "$$notif.templateId")
                        .append("eventType", "$event_type")
                        .append("status", "$$notif.status")
                        .append("priority", "$$notif.priority")
                        .append("attemptCount", "$$notif.attemptCount")
                        .append("createdAt", "$$notif.created_at")
                        .append("processedAt", "$$notif.processed_at")
                        .append("sentAt", "$$notif.sentAt")
                        .append("deliveredAt", "$$notif.deliveredAt")
                        .append("lastErrorMessage", "$$notif.lastErrorMessage")
                        .append("recipientType", "DATA_PRINCIPAL")
                        .append("recipientId", new org.bson.Document("$arrayElemAt", java.util.Arrays.asList("$$notif.to", 0)))
                        .append("to", "$$notif.to")
                        .append("cc", "$$notif.cc")
                        .append("bcc", "$$notif.bcc")
                        .append("subject", "$$notif.subject")
                    )
                );

                org.bson.Document callbackMap = new org.bson.Document("$map", new org.bson.Document()
                    .append("input", "$callbackNotifications")
                    .append("as", "notif")
                    .append("in", new org.bson.Document()
                        .append("notificationType", "CALLBACK")
                        .append("notificationId", "$$notif.notificationId")
                        .append("eventId", "$event_id")
                        .append("correlationId", "$$notif.correlationId")
                        .append("businessId", "$business_id")
                        .append("templateId", null)
                        .append("eventType", "$$notif.eventType")
                        .append("status", "$$notif.status")
                        .append("priority", "$$notif.priority")
                        .append("attemptCount", "$$notif.attemptCount")
                        .append("createdAt", "$$notif.created_at")
                        .append("processedAt", "$$notif.processed_at")
                        .append("sentAt", "$$notif.sentAt")
                        .append("acknowledgedAt", "$$notif.acknowledgedAt")
                        .append("lastErrorMessage", "$$notif.lastErrorMessage")
                        .append("recipientType", "$$notif.recipientType")
                        .append("recipientId", "$$notif.recipientId")
                        .append("callbackUrl", "$$notif.callbackUrl")
                        .append("httpStatusCode", "$$notif.lastHttpStatusCode")
                        .append("statusHistory", "$$notif.statusHistory")
                    )
                );

                // Concatenate all three arrays
                return new org.bson.Document("$concatArrays", java.util.Arrays.asList(smsMap, emailMap, callbackMap));
            }).as("allNotifications");
    }

    /**
     * OPTIMIZATION: Builds criteria for filtering flattened notifications after $unwind.
     * Filters are applied on individual notification documents, not arrays.
     */
    private Criteria buildFlattenedNotificationCriteria(NotificationFilterRequestDto filter) {
        List<Criteria> criteriaList = new ArrayList<>();

        // Filter by notification type
        if (filter.getNotificationType() != null) {
            criteriaList.add(Criteria.where("allNotifications.notificationType").is(filter.getNotificationType().toUpperCase()));
        }

        // Filter by recipient type
        if (filter.getRecipientType() != null) {
            criteriaList.add(Criteria.where("allNotifications.recipientType").is(filter.getRecipientType().toUpperCase()));
        }

        // Filter by recipient ID
        if (filter.getRecipientId() != null) {
            criteriaList.add(Criteria.where("allNotifications.recipientId").is(filter.getRecipientId()));
        }

        // Filter by mobile
        if (filter.getMobile() != null) {
            criteriaList.add(Criteria.where("allNotifications.mobile").is(filter.getMobile()));
        }

        // Filter by notification status
        if (filter.getNotificationStatus() != null) {
            criteriaList.add(Criteria.where("allNotifications.status").is(filter.getNotificationStatus()));
        }

        // Filter by template ID
        if (filter.getTemplateId() != null) {
            criteriaList.add(Criteria.where("allNotifications.templateId").is(filter.getTemplateId()));
        }

        // Filter by correlation ID
        if (filter.getCorrelationId() != null) {
            criteriaList.add(Criteria.where("allNotifications.correlationId").is(filter.getCorrelationId()));
        }

        return criteriaList.isEmpty() ? new Criteria() :
               new Criteria().andOperator(criteriaList.toArray(new Criteria[0]));
    }

    /**
     * OPTIMIZATION: Projects the flattened notification to final UnifiedNotificationDto structure.
     */
    private ProjectionOperation buildUnifiedProjection() {
        return project()
            .andExclude("_id")
            .and("allNotifications.notificationType").as("notificationType")
            .and("allNotifications.notificationId").as("notificationId")
            .and("allNotifications.eventId").as("eventId")
            .and("allNotifications.correlationId").as("correlationId")
            .and("allNotifications.businessId").as("businessId")
            .and("allNotifications.templateId").as("templateId")
            .and("allNotifications.eventType").as("eventType")
            .and("allNotifications.status").as("status")
            .and("allNotifications.priority").as("priority")
            .and("allNotifications.attemptCount").as("attemptCount")
            .and("allNotifications.createdAt").as("createdAt")
            .and("allNotifications.processedAt").as("processedAt")
            .and("allNotifications.sentAt").as("sentAt")
            .and("allNotifications.deliveredAt").as("deliveredAt")
            .and("allNotifications.acknowledgedAt").as("acknowledgedAt")
            .and("allNotifications.lastErrorMessage").as("lastErrorMessage")
            .and("allNotifications.recipientType").as("recipientType")
            .and("allNotifications.recipientId").as("recipientId")
            .and("allNotifications.mobile").as("mobile")
            .and("allNotifications.to").as("to")
            .and("allNotifications.cc").as("cc")
            .and("allNotifications.bcc").as("bcc")
            .and("allNotifications.subject").as("subject")
            .and("allNotifications.callbackUrl").as("callbackUrl")
            .and("allNotifications.httpStatusCode").as("httpStatusCode")
            .and("allNotifications.statusHistory").as("statusHistory");
    }

    /**
     * OPTIMIZATION: Sorts flattened notifications (not events).
     */
    private SortOperation buildFlattenedSort(NotificationFilterRequestDto filter) {
        String sortBy = filter.getSortBy() != null ? filter.getSortBy() : "createdAt";
        String sortDirection = filter.getSortDirection() != null ? filter.getSortDirection() : "DESC";

        Sort.Direction direction = "ASC".equalsIgnoreCase(sortDirection) ?
            Sort.Direction.ASC : Sort.Direction.DESC;

        // Sort on the flattened notification fields
        return sort(Sort.by(direction, sortBy));
    }

    /**
     * Builds count response from aggregation results.
     */
    private NotificationCountResponseDto buildCountResponse(List<Map> results) {
        if (results.isEmpty()) {
            return NotificationCountResponseDto.builder()
                .totalEvents(0L)
                .totalNotifications(0L)
                .build();
        }

        Map<String, Object> result = results.get(0);

        var builder = NotificationCountResponseDto.builder()
            .totalEvents(getLong(result, "totalEvents"))
            .totalNotifications(
                getLong(result, "totalSms") +
                getLong(result, "totalEmail") +
                getLong(result, "totalCallback")
            );

        // Build type counts
        NotificationCountResponseDto.NotificationTypeCounts typeCounts =
            NotificationCountResponseDto.NotificationTypeCounts.builder()
                .sms(getLong(result, "totalSms"))
                .email(getLong(result, "totalEmail"))
                .callback(getLong(result, "totalCallback"))
                .build();
        typeCounts.calculateTotal();
        builder.notificationTypeCounts(typeCounts);

        NotificationCountResponseDto response = builder.build();
        response.calculateTotals();

        return response;
    }

    /**
     * Helper method to safely extract Long values from Map.
     */
    private Long getLong(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) return 0L;
        if (value instanceof Number) return ((Number) value).longValue();
        return 0L;
    }
}