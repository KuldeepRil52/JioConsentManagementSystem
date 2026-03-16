package com.jio.digigov.notification.repository.notification.impl;

import com.jio.digigov.notification.dto.request.notification.CallbackPurgeStatsFilterRequestDto;
import com.jio.digigov.notification.dto.request.notification.CallbackStatsFilterRequestDto;
import com.jio.digigov.notification.dto.request.notification.ConsentDeletionFilterRequestDto;
import com.jio.digigov.notification.dto.response.notification.CallbackPurgeStatsResponseDto;
import com.jio.digigov.notification.dto.response.notification.CallbackStatsResponseDto;
import com.jio.digigov.notification.dto.response.notification.ConsentDeletionDashboardResponseDto;
import com.jio.digigov.notification.dto.response.common.PagedResponseDto;
import com.jio.digigov.notification.entity.notification.NotificationCallback;
import com.jio.digigov.notification.entity.notification.StatusHistoryEntry;
import com.jio.digigov.notification.enums.RecipientType;
import com.jio.digigov.notification.repository.notification.NotificationCallbackRepository;
import com.jio.digigov.notification.util.CallbackStatusHelper;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.*;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Implementation of NotificationCallbackRepository for callback notification data access.
 *
 * Provides optimized MongoDB operations for callback notifications with comprehensive
 * query support, webhook tracking, and retry management. All operations are
 * tenant-aware through MongoTemplate parameter injection.
 *
 * Performance Optimizations:
 * - Uses compound indexes for efficient queries
 * - Batch operations for bulk updates
 * - Projection queries for count operations
 * - Optimized sort and limit for large datasets
 * - Special webhook response tracking
 *
 * @author Notification Service Team
 * @version 1.0
 * @since 2024-01-01
 */
@Repository
@Slf4j
public class NotificationCallbackRepositoryImpl implements NotificationCallbackRepository {

    @Override
    public NotificationCallback save(NotificationCallback notification, MongoTemplate mongoTemplate) {
        try {
            // Manually set timestamps (workaround for multi-tenant auditing)
            LocalDateTime now = LocalDateTime.now();
            if (notification.getCreatedAt() == null) {
                notification.setCreatedAt(now);
            }
            notification.setUpdatedAt(now); // Always update the updatedAt timestamp

            return mongoTemplate.save(notification);
        } catch (Exception e) {
            log.error("Error saving callback notification: {}", e.getMessage());
            throw new RuntimeException("Failed to save callback notification", e);
        }
    }

    @Override
    public Optional<NotificationCallback> findByNotificationId(String notificationId, MongoTemplate mongoTemplate) {
        try {
            Query query = new Query(Criteria.where("notificationId").is(notificationId));
            NotificationCallback notification = mongoTemplate.findOne(query, NotificationCallback.class);
            return Optional.ofNullable(notification);
        } catch (Exception e) {
            log.error("Error finding callback notification by ID {}: {}", notificationId, e.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public List<NotificationCallback> findByEventId(String eventId, MongoTemplate mongoTemplate) {
        try {
            Query query = new Query(Criteria.where("eventId").is(eventId))
                    .with(Sort.by(Sort.Direction.DESC, "createdAt"));
            return mongoTemplate.find(query, NotificationCallback.class);
        } catch (Exception e) {
            log.error("Error finding callback notifications by event ID {}: {}", eventId, e.getMessage());
            return List.of();
        }
    }

    @Override
    public List<NotificationCallback> findByCorrelationId(String correlationId, MongoTemplate mongoTemplate) {
        try {
            Query query = new Query(Criteria.where("correlationId").is(correlationId))
                    .with(Sort.by(Sort.Direction.DESC, "createdAt"));
            return mongoTemplate.find(query, NotificationCallback.class);
        } catch (Exception e) {
            log.error("Error finding callback notifications by correlation ID {}: {}", correlationId, e.getMessage());
            return List.of();
        }
    }

    @Override
    public List<NotificationCallback> findByStatus(String status, MongoTemplate mongoTemplate) {
        try {
            Query query = new Query(Criteria.where("status").is(status))
                    .with(Sort.by(Sort.Direction.ASC, "createdAt"));
            return mongoTemplate.find(query, NotificationCallback.class);
        } catch (Exception e) {
            log.error("Error finding callback notifications by status {}: {}", status, e.getMessage());
            return List.of();
        }
    }

    @Override
    public List<NotificationCallback> findByStatusWithLimit(String status, int limit, MongoTemplate mongoTemplate) {
        try {
            Query query = new Query(Criteria.where("status").is(status))
                    .with(Sort.by(Sort.Direction.ASC, "createdAt"))
                    .limit(limit);
            return mongoTemplate.find(query, NotificationCallback.class);
        } catch (Exception e) {
            log.error("Error finding callback notifications by status {} with limit {}: {}",
                     status, limit, e.getMessage(), e);
            return List.of();
        }
    }

    @Override
    public List<NotificationCallback> findByRecipientId(String recipientId, MongoTemplate mongoTemplate) {
        try {
            Query query = new Query(Criteria.where("recipientId").is(recipientId))
                    .with(Sort.by(Sort.Direction.DESC, "createdAt"));
            return mongoTemplate.find(query, NotificationCallback.class);
        } catch (Exception e) {
            log.error("Error finding callback notifications by recipient ID {}: {}", recipientId, e.getMessage());
            return List.of();
        }
    }

    @Override
    public List<NotificationCallback> findRetryReady(LocalDateTime currentTime, MongoTemplate mongoTemplate) {
        try {
            Criteria criteria = new Criteria().andOperator(
                Criteria.where("status").in("FAILED", "RETRY_PENDING"),
                Criteria.where("nextRetryAt").lte(currentTime),
                Criteria.where("attemptCount").lt(5) // Higher retry count for webhooks
            );

            Query query = new Query(criteria)
                    .with(Sort.by(Sort.Direction.ASC, "nextRetryAt"))
                    .limit(50); // Smaller batch for webhook processing

            return mongoTemplate.find(query, NotificationCallback.class);
        } catch (Exception e) {
            log.error("Error finding retry-ready callback notifications: {}", e.getMessage());
            return List.of();
        }
    }

    @Override
    public long updateStatus(String notificationId, String status, MongoTemplate mongoTemplate) {
        try {
            Query query = new Query(Criteria.where("notificationId").is(notificationId));
            Update update = new Update()
                    .set("status", status)
                    .set("updatedAt", LocalDateTime.now());

            return mongoTemplate.updateFirst(query, update, NotificationCallback.class).getModifiedCount();
        } catch (Exception e) {
            log.error("Error updating callback notification status for ID {}: {}", notificationId, e.getMessage());
            return 0;
        }
    }

    @Override
    public long updateStatusAndProcessedAt(String notificationId, String status,
                                          LocalDateTime processedAt, MongoTemplate mongoTemplate) {
        try {
            Query query = new Query(Criteria.where("notificationId").is(notificationId));
            Update update = new Update()
                    .set("status", status)
                    .set("processedAt", processedAt)
                    .set("lastAttemptAt", LocalDateTime.now())
                    .set("updatedAt", LocalDateTime.now());

            return mongoTemplate.updateFirst(query, update, NotificationCallback.class).getModifiedCount();
        } catch (Exception e) {
            log.error("Error updating callback notification status and processedAt for ID {}: {}",
                     notificationId, e.getMessage(), e);
            return 0;
        }
    }

    @Override
    public long updateRetryInfo(String notificationId, int attemptCount, LocalDateTime nextRetryAt,
                               String errorMessage, MongoTemplate mongoTemplate) {
        try {
            Query query = new Query(Criteria.where("notificationId").is(notificationId));
            Update update = new Update()
                    .set("attemptCount", attemptCount)
                    .set("nextRetryAt", nextRetryAt)
                    .set("lastErrorMessage", errorMessage)
                    .set("lastAttemptAt", LocalDateTime.now())
                    .set("status", "RETRY_PENDING")
                    .set("updatedAt", LocalDateTime.now());

            return mongoTemplate.updateFirst(query, update, NotificationCallback.class).getModifiedCount();
        } catch (Exception e) {
            log.error("Error updating callback notification retry info for ID {}: {}",
                     notificationId, e.getMessage(), e);
            return 0;
        }
    }

    @Override
    public long updateWebhookResponse(String notificationId, String status, LocalDateTime acknowledgedAt,
                                     String webhookResponse, Integer httpStatusCode, MongoTemplate mongoTemplate) {
        try {
            Query query = new Query(Criteria.where("notificationId").is(notificationId));
            Update update = new Update()
                    .set("status", status)
                    .set("acknowledgedAt", acknowledgedAt)
                    .set("webhookResponse", webhookResponse)
                    .set("httpStatusCode", httpStatusCode)
                    .set("updatedAt", LocalDateTime.now());

            return mongoTemplate.updateFirst(query, update, NotificationCallback.class).getModifiedCount();
        } catch (Exception e) {
            log.error("Error updating callback notification webhook response for ID {}: {}",
                     notificationId, e.getMessage(), e);
            return 0;
        }
    }

    @Override
    public long countByStatus(String status, MongoTemplate mongoTemplate) {
        try {
            Query query = new Query(Criteria.where("status").is(status));
            return mongoTemplate.count(query, NotificationCallback.class);
        } catch (Exception e) {
            log.error("Error counting callback notifications by status {}: {}", status, e.getMessage());
            return 0;
        }
    }

    @Override
    public long countByBusinessIdAndStatus(String businessId, String status, MongoTemplate mongoTemplate) {
        try {
            Criteria criteria = new Criteria().andOperator(
                Criteria.where("businessId").is(businessId),
                Criteria.where("status").is(status)
            );
            Query query = new Query(criteria);
            return mongoTemplate.count(query, NotificationCallback.class);
        } catch (Exception e) {
            log.error("Error counting callback notifications by business {} and status {}: {}",
                     businessId, status, e.getMessage(), e);
            return 0;
        }
    }

    @Override
    public List<NotificationCallback> findPermanentlyFailed(MongoTemplate mongoTemplate) {
        try {
            Criteria criteria = new Criteria().andOperator(
                Criteria.where("status").is("FAILED"),
                Criteria.where("attemptCount").gte(5) // Higher threshold for webhooks
            );

            Query query = new Query(criteria)
                    .with(Sort.by(Sort.Direction.DESC, "lastAttemptAt"));

            return mongoTemplate.find(query, NotificationCallback.class);
        } catch (Exception e) {
            log.error("Error finding permanently failed callback notifications: {}", e.getMessage());
            return List.of();
        }
    }

    @Override
    public long deleteOldRecords(LocalDateTime cutoffDate, MongoTemplate mongoTemplate) {
        try {
            Criteria criteria = new Criteria().andOperator(
                Criteria.where("createdAt").lt(cutoffDate),
                Criteria.where("status").in("ACKNOWLEDGED", "FAILED") // Only delete completed records
            );

            Query query = new Query(criteria);
            return mongoTemplate.remove(query, NotificationCallback.class).getDeletedCount();
        } catch (Exception e) {
            log.error("Error deleting old callback notification records: {}", e.getMessage());
            return 0;
        }
    }

    @Override
    public long addStatusHistoryEntry(String notificationId, StatusHistoryEntry entry, MongoTemplate mongoTemplate) {
        try {
            Query query = new Query(Criteria.where("notificationId").is(notificationId));
            Update update = new Update().push("statusHistory", entry);
            update.set("updatedAt", LocalDateTime.now());

            var result = mongoTemplate.updateFirst(query, update, NotificationCallback.class);

            if (result.getModifiedCount() == 0) {
                log.warn("No notification found to add status history entry: notificationId={}", notificationId);
            }

            return result.getModifiedCount();
        } catch (Exception e) {
            log.error("Error adding status history entry for notificationId={}: {}",
                     notificationId, e.getMessage(), e);
            throw new RuntimeException("Failed to add status history entry", e);
        }
    }

    @Override
    public long updateStatusWithHistory(String notificationId, String status,
                                       StatusHistoryEntry entry, MongoTemplate mongoTemplate) {
        try {
            Query query = new Query(Criteria.where("notificationId").is(notificationId));
            Update update = new Update()
                .set("status", status)
                .set("updatedAt", LocalDateTime.now())
                .push("statusHistory", entry);

            // Add acknowledgedAt timestamp if status is ACKNOWLEDGED
            if ("ACKNOWLEDGED".equals(status)) {
                update.set("acknowledgedAt", LocalDateTime.now());
            }

            var result = mongoTemplate.updateFirst(query, update, NotificationCallback.class);

            if (result.getModifiedCount() == 0) {
                log.warn("No notification found to update status with history: notificationId={}, status={}",
                        notificationId, status);
            } else {
                log.debug("Updated notification status with history: notificationId={}, status={}, entry={}",
                         notificationId, status, entry);
            }

            return result.getModifiedCount();
        } catch (Exception e) {
            log.error("Error updating status with history for notificationId={}, status={}: {}",
                     notificationId, status, e.getMessage(), e);
            throw new RuntimeException("Failed to update status with history", e);
        }
    }

    @Override
    public Optional<NotificationCallback> findByIdAndRecipient(String notificationId, String recipientType,
                                                               String recipientId, MongoTemplate mongoTemplate) {
        try {
            Criteria criteria = new Criteria().andOperator(
                Criteria.where("notificationId").is(notificationId),
                Criteria.where("recipientType").is(recipientType),
                Criteria.where("recipientId").is(recipientId)
            );

            Query query = new Query(criteria);
            NotificationCallback notification = mongoTemplate.findOne(query, NotificationCallback.class);

            if (notification == null) {
                log.debug("Notification not found or not authorized: notificationId={}, recipientType={}, recipientId={}",
                         notificationId, recipientType, recipientId);
            }

            return Optional.ofNullable(notification);
        } catch (Exception e) {
            log.error("Error finding notification by ID and recipient: notificationId={}, recipientType={}, recipientId={}, error={}",
                     notificationId, recipientType, recipientId, e.getMessage(), e);
            return Optional.empty();
        }
    }

    @Override
    public CallbackStatsResponseDto getCallbackStatistics(CallbackStatsFilterRequestDto filter, MongoTemplate mongoTemplate) {
        try {
            log.info("Fetching callback statistics with filter: {}", filter);

            // Build match criteria based on filters
            Criteria matchCriteria = buildFilterCriteria(filter);

            // Create aggregation pipeline
            Aggregation aggregation = buildStatsAggregation(matchCriteria);

            // Execute aggregation
            List<Document> results = mongoTemplate.aggregate(aggregation, "notification_callback", Document.class)
                    .getMappedResults();

            // Process results and build response
            return processStatsResults(results, mongoTemplate);

        } catch (Exception e) {
            log.error("Error fetching callback statistics: {}", e.getMessage());
            throw new RuntimeException("Failed to fetch callback statistics", e);
        }
    }

    /**
     * Builds filter criteria from the request filter.
     */
    private Criteria buildFilterCriteria(CallbackStatsFilterRequestDto filter) {
        List<Criteria> criteriaList = new ArrayList<>();

        // Filter by event type
        if (filter.getEventType() != null && !filter.getEventType().trim().isEmpty()) {
            criteriaList.add(Criteria.where("eventType").is(filter.getEventType().toUpperCase()));
        }

        // Filter by recipient type (DATA_FIDUCIARY or DATA_PROCESSOR)
        if (filter.getRecipientType() != null && !filter.getRecipientType().trim().isEmpty()) {
            criteriaList.add(Criteria.where("recipientType").is(filter.getRecipientType().toUpperCase()));
        }

        // Filter by specific recipient ID
        if (filter.getRecipientId() != null && !filter.getRecipientId().trim().isEmpty()) {
            criteriaList.add(Criteria.where("recipientId").is(filter.getRecipientId()));
        }

        // Filter by date range (created_at field - MongoDB field name)
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

        // Combine all criteria with AND operator
        return criteriaList.isEmpty() ?
                new Criteria() :
                new Criteria().andOperator(criteriaList.toArray(new Criteria[0]));
    }

    /**
     * Builds the complete MongoDB aggregation pipeline for statistics with optimization options.
     */
    private Aggregation buildStatsAggregation(Criteria matchCriteria) {
        List<AggregationOperation> operations = new ArrayList<>();

        // Step 1: Match based on filter criteria (early filtering for performance)
        // This uses the stats_query_idx index: {'eventType': 1, 'recipientType': 1, 'createdAt': -1}
        if (!matchCriteria.getCriteriaObject().isEmpty()) {
            operations.add(Aggregation.match(matchCriteria));
        }

        // Step 2: Lookup Data Fiduciary names from business_applications
        operations.add(Aggregation.lookup(
                "business_applications",      // from collection
                "recipientId",                 // local field
                "businessId",                  // foreign field
                "dfInfo"                       // as field
        ));

        // Step 3: Lookup Data Processor names from data_processors
        operations.add(Aggregation.lookup(
                "data_processors",             // from collection
                "recipientId",                 // local field
                "dataProcessorId",             // foreign field
                "dpInfo"                       // as field
        ));

        // Step 4: Add computed field for recipient name with fallback
        operations.add(Aggregation.project()
                .and("eventType").as("eventType")
                .and("recipientType").as("recipientType")
                .and("recipientId").as("recipientId")
                .and("status").as("status")
                .and(ConditionalOperators.Cond.when(
                        ComparisonOperators.Eq.valueOf("recipientType").equalToValue(RecipientType.DATA_FIDUCIARY.name())
                ).then(
                        ConditionalOperators.ifNull(
                                ArrayOperators.ArrayElemAt.arrayOf("dfInfo.name").elementAt(0)
                        ).then("$recipientId")
                ).otherwise(
                        ConditionalOperators.ifNull(
                                ArrayOperators.ArrayElemAt.arrayOf("dpInfo.dataProcessorName").elementAt(0)
                        ).then("$recipientId")
                )).as("recipientName")
        );

        // Step 5: Use $facet for parallel processing of global, DF, and DP statistics
        FacetOperation facetOperation = Aggregation.facet()
                // Global statistics pipeline
                .and(
                        buildGlobalStatsPipeline()
                ).as("globalStats")
                // Data Fiduciary statistics pipeline
                .and(
                        buildRecipientStatsPipeline(RecipientType.DATA_FIDUCIARY.name())
                ).as("dfStats")
                // Data Processor statistics pipeline
                .and(
                        buildRecipientStatsPipeline(RecipientType.DATA_PROCESSOR.name())
                ).as("dpStats");

        operations.add(facetOperation);

        // Build aggregation with options for large datasets and performance
        return Aggregation.newAggregation(operations)
                .withOptions(AggregationOptions.builder()
                        .allowDiskUse(true)  // Allow using disk for large datasets (>100MB)
                        .maxTime(Duration.ofSeconds(60))  // 60 second timeout to prevent long-running queries
                        .build());
    }

    /**
     * Builds the pipeline for global statistics.
     */
    private AggregationOperation[] buildGlobalStatsPipeline() {
        List<AggregationOperation> pipeline = new ArrayList<>();

        // Group by eventType first for event-specific stats (including null values)
        GroupOperation groupByEventType = Aggregation.group("eventType")
                .count().as("totalCallbacks")
                .sum(ConditionalOperators.Cond.when(
                        ArrayOperators.In.arrayOf(CallbackStatusHelper.getSuccessStatusNames())
                                .containsValue("$status")
                ).then(1).otherwise(0)).as("successfulCallbacks")
                .sum(ConditionalOperators.Cond.when(
                        ArrayOperators.In.arrayOf(CallbackStatusHelper.getFailureStatusNames())
                                .containsValue("$status")
                ).then(1).otherwise(0)).as("failedCallbacks");

        pipeline.add(groupByEventType);

        // Project event type stats
        pipeline.add(Aggregation.project()
                .and("_id").as("eventType")
                .and("totalCallbacks").as("totalCallbacks")
                .and("successfulCallbacks").as("successfulCallbacks")
                .and("failedCallbacks").as("failedCallbacks")
        );

        // Group all to get overall stats and collect event type breakdown
        GroupOperation groupAll = Aggregation.group()
                .sum("totalCallbacks").as("totalCallbacks")
                .sum("successfulCallbacks").as("successfulCallbacks")
                .sum("failedCallbacks").as("failedCallbacks")
                .push(new Document()
                        .append("eventType", "$eventType")
                        .append("totalCallbacks", "$totalCallbacks")
                        .append("successfulCallbacks", "$successfulCallbacks")
                        .append("failedCallbacks", "$failedCallbacks")
                ).as("byEventType");

        pipeline.add(groupAll);

        return pipeline.toArray(new AggregationOperation[0]);
    }

    /**
     * Builds the pipeline for recipient-specific statistics (DF or DP).
     */
    private AggregationOperation[] buildRecipientStatsPipeline(String recipientType) {
        List<AggregationOperation> pipeline = new ArrayList<>();

        // Filter by recipient type
        pipeline.add(Aggregation.match(Criteria.where("recipientType").is(recipientType)));

        // Group by recipientId and eventType (including null eventType values)
        GroupOperation groupByRecipientAndEvent = Aggregation.group(
                Fields.fields("recipientId", "eventType")
        )
                .first("recipientName").as("recipientName")
                .count().as("totalCallbacks")
                .sum(ConditionalOperators.Cond.when(
                        ArrayOperators.In.arrayOf(CallbackStatusHelper.getSuccessStatusNames())
                                .containsValue("$status")
                ).then(1).otherwise(0)).as("successfulCallbacks")
                .sum(ConditionalOperators.Cond.when(
                        ArrayOperators.In.arrayOf(CallbackStatusHelper.getFailureStatusNames())
                                .containsValue("$status")
                ).then(1).otherwise(0)).as("failedCallbacks");

        pipeline.add(groupByRecipientAndEvent);

        // Project event type stats
        pipeline.add(Aggregation.project()
                .and("_id.recipientId").as("recipientId")
                .and("_id.eventType").as("eventType")
                .and("recipientName").as("recipientName")
                .and("totalCallbacks").as("totalCallbacks")
                .and("successfulCallbacks").as("successfulCallbacks")
                .and("failedCallbacks").as("failedCallbacks")
        );

        // Group by recipientId to aggregate event types
        GroupOperation groupByRecipient = Aggregation.group("recipientId")
                .first("recipientName").as("name")
                .sum("totalCallbacks").as("totalCallbacks")
                .sum("successfulCallbacks").as("successfulCallbacks")
                .sum("failedCallbacks").as("failedCallbacks")
                .push(new Document()
                        .append("eventType", "$eventType")
                        .append("totalCallbacks", "$totalCallbacks")
                        .append("successfulCallbacks", "$successfulCallbacks")
                        .append("failedCallbacks", "$failedCallbacks")
                ).as("byEventType");

        pipeline.add(groupByRecipient);

        return pipeline.toArray(new AggregationOperation[0]);
    }

    /**
     * Processes the aggregation results and builds the response DTO.
     */
    private CallbackStatsResponseDto processStatsResults(List<Document> results, MongoTemplate mongoTemplate) {
        CallbackStatsResponseDto.CallbackStatsResponseDtoBuilder responseBuilder = CallbackStatsResponseDto.builder();

        if (results.isEmpty()) {
            // Return empty stats if no results
            return buildEmptyStats();
        }

        Document result = results.get(0);

        // Process global stats
        List<Document> globalStatsList = (List<Document>) result.get("globalStats");
        CallbackStatsResponseDto.GlobalStats globalStats = processGlobalStats(globalStatsList);
        responseBuilder.stats(globalStats);

        // Process DF stats
        List<Document> dfStatsList = (List<Document>) result.get("dfStats");
        Map<String, CallbackStatsResponseDto.RecipientStats> dfStatsMap = processRecipientStats(dfStatsList);
        responseBuilder.dataFiduciary(dfStatsMap);

        // Process DP stats
        List<Document> dpStatsList = (List<Document>) result.get("dpStats");
        Map<String, CallbackStatsResponseDto.RecipientStats> dpStatsMap = processRecipientStats(dpStatsList);
        responseBuilder.dataProcessor(dpStatsMap);

        return responseBuilder.build();
    }

    /**
     * Processes global statistics from aggregation results.
     */
    private CallbackStatsResponseDto.GlobalStats processGlobalStats(List<Document> globalStatsList) {
        if (globalStatsList == null || globalStatsList.isEmpty()) {
            return CallbackStatsResponseDto.GlobalStats.builder()
                    .totalCallbacks(0L)
                    .successfulCallbacks(0L)
                    .failedCallbacks(0L)
                    .successPercentage(0.0)
                    .failurePercentage(0.0)
                    .byEventType(new ArrayList<>())
                    .build();
        }

        Document globalDoc = globalStatsList.get(0);

        Long totalCallbacks = getLongValue(globalDoc, "totalCallbacks");
        Long successfulCallbacks = getLongValue(globalDoc, "successfulCallbacks");
        Long failedCallbacks = getLongValue(globalDoc, "failedCallbacks");

        List<Document> eventTypeList = (List<Document>) globalDoc.get("byEventType");
        List<CallbackStatsResponseDto.EventTypeStats> eventTypeStats = processEventTypeStats(eventTypeList);

        CallbackStatsResponseDto.GlobalStats stats = CallbackStatsResponseDto.GlobalStats.builder()
                .totalCallbacks(totalCallbacks)
                .successfulCallbacks(successfulCallbacks)
                .failedCallbacks(failedCallbacks)
                .byEventType(eventTypeStats)
                .build();

        stats.calculatePercentages();
        return stats;
    }

    /**
     * Processes recipient statistics from aggregation results.
     */
    private Map<String, CallbackStatsResponseDto.RecipientStats> processRecipientStats(List<Document> recipientStatsList) {
        if (recipientStatsList == null || recipientStatsList.isEmpty()) {
            return new HashMap<>();
        }

        Map<String, CallbackStatsResponseDto.RecipientStats> statsMap = new HashMap<>();

        for (Document recipientDoc : recipientStatsList) {
            String recipientId = recipientDoc.getString("_id");
            String name = recipientDoc.getString("name");

            Long totalCallbacks = getLongValue(recipientDoc, "totalCallbacks");
            Long successfulCallbacks = getLongValue(recipientDoc, "successfulCallbacks");
            Long failedCallbacks = getLongValue(recipientDoc, "failedCallbacks");

            List<Document> eventTypeList = (List<Document>) recipientDoc.get("byEventType");
            List<CallbackStatsResponseDto.EventTypeStats> eventTypeStats = processEventTypeStats(eventTypeList);

            CallbackStatsResponseDto.RecipientStats stats = CallbackStatsResponseDto.RecipientStats.builder()
                    .name(name != null ? name : recipientId)  // Fallback to ID if name not found
                    .totalCallbacks(totalCallbacks)
                    .successfulCallbacks(successfulCallbacks)
                    .failedCallbacks(failedCallbacks)
                    .byEventType(eventTypeStats)
                    .build();

            stats.calculatePercentages();
            statsMap.put(recipientId, stats);
        }

        return statsMap;
    }

    /**
     * Processes event type statistics from aggregation results.
     */
    private List<CallbackStatsResponseDto.EventTypeStats> processEventTypeStats(List<Document> eventTypeList) {
        if (eventTypeList == null || eventTypeList.isEmpty()) {
            return new ArrayList<>();
        }

        return eventTypeList.stream()
                .map(eventDoc -> {
                    String eventType = eventDoc.getString("eventType");
                    Long totalCallbacks = getLongValue(eventDoc, "totalCallbacks");
                    Long successfulCallbacks = getLongValue(eventDoc, "successfulCallbacks");
                    Long failedCallbacks = getLongValue(eventDoc, "failedCallbacks");

                    CallbackStatsResponseDto.EventTypeStats stats = CallbackStatsResponseDto.EventTypeStats.builder()
                            .eventType(eventType)
                            .totalCallbacks(totalCallbacks)
                            .successfulCallbacks(successfulCallbacks)
                            .failedCallbacks(failedCallbacks)
                            .build();

                    stats.calculatePercentages();
                    return stats;
                })
                .collect(Collectors.toList());
    }

    /**
     * Builds an empty statistics response.
     */
    private CallbackStatsResponseDto buildEmptyStats() {
        CallbackStatsResponseDto.GlobalStats globalStats = CallbackStatsResponseDto.GlobalStats.builder()
                .totalCallbacks(0L)
                .successfulCallbacks(0L)
                .failedCallbacks(0L)
                .successPercentage(0.0)
                .failurePercentage(0.0)
                .byEventType(new ArrayList<>())
                .build();

        return CallbackStatsResponseDto.builder()
                .stats(globalStats)
                .dataFiduciary(new HashMap<>())
                .dataProcessor(new HashMap<>())
                .build();
    }

    /**
     * Safely extracts Long value from Document.
     */
    private Long getLongValue(Document doc, String key) {
        Object value = doc.get(key);
        if (value == null) {
            return 0L;
        }
        if (value instanceof Long) {
            return (Long) value;
        }
        if (value instanceof Integer) {
            return ((Integer) value).longValue();
        }
        return 0L;
    }

    @Override
    public CallbackPurgeStatsResponseDto getPurgeStatistics(CallbackPurgeStatsFilterRequestDto filter, int slaHours, MongoTemplate mongoTemplate) {
        try {
            log.info("Fetching callback purge statistics with filter: {}, SLA hours: {}", filter, slaHours);

            // Build match criteria based on filters
            Criteria matchCriteria = buildPurgeFilterCriteria(filter);

            // Calculate SLA deadline timestamp
            LocalDateTime slaDeadline = LocalDateTime.now().minusHours(slaHours);

            // Create aggregation pipeline
            Aggregation aggregation = buildPurgeStatsAggregation(matchCriteria, slaDeadline);

            // Execute aggregation
            List<Document> results = mongoTemplate.aggregate(aggregation, "notification_callback", Document.class)
                    .getMappedResults();

            // Process results and build response
            return processPurgeStatsResults(results, mongoTemplate);

        } catch (Exception e) {
            log.error("Error fetching callback purge statistics: {}", e.getMessage());
            throw new RuntimeException("Failed to fetch callback purge statistics", e);
        }
    }

    /**
     * Builds filter criteria for purge statistics.
     */
    private Criteria buildPurgeFilterCriteria(CallbackPurgeStatsFilterRequestDto filter) {
        List<Criteria> criteriaList = new ArrayList<>();

        // Filter by event types: CONSENT_EXPIRED or CONSENT_WITHDRAWN
        List<String> eventTypes = filter.getEventTypesToFilter();
        criteriaList.add(Criteria.where("eventType").in(eventTypes));

        // Filter by recipient type (DATA_FIDUCIARY or DATA_PROCESSOR)
        if (filter.getRecipientType() != null && !filter.getRecipientType().trim().isEmpty()) {
            criteriaList.add(Criteria.where("recipientType").is(filter.getRecipientType().toUpperCase()));
        }

        // Filter by specific recipient ID
        if (filter.getRecipientId() != null && !filter.getRecipientId().trim().isEmpty()) {
            criteriaList.add(Criteria.where("recipientId").is(filter.getRecipientId()));
        }

        // Filter by date range - based on ACKNOWLEDGED timestamp from statusHistory
        // For purge stats, we filter callbacks that have been acknowledged within the date range
        if (filter.getFromDate() != null || filter.getToDate() != null) {
            Criteria dateCriteria = Criteria.where("acknowledgedAt");
            if (filter.getFromDate() != null) {
                dateCriteria = dateCriteria.gte(filter.getFromDate());
            }
            if (filter.getToDate() != null) {
                dateCriteria = dateCriteria.lt(filter.getToDate());
            }
            criteriaList.add(dateCriteria);
        }

        // Combine all criteria with AND operator
        return criteriaList.isEmpty() ?
                new Criteria() :
                new Criteria().andOperator(criteriaList.toArray(new Criteria[0]));
    }

    /**
     * Builds the complete MongoDB aggregation pipeline for purge statistics.
     */
    private Aggregation buildPurgeStatsAggregation(Criteria matchCriteria, LocalDateTime slaDeadline) {
        List<AggregationOperation> operations = new ArrayList<>();

        // Step 1: Match based on filter criteria (early filtering for performance)
        if (!matchCriteria.getCriteriaObject().isEmpty()) {
            operations.add(Aggregation.match(matchCriteria));
        }

        // Step 2: Filter to only include records that have ACKNOWLEDGED status
        // This excludes records with empty statusHistory and no acknowledgedAt field
        operations.add(Aggregation.match(
                new Criteria().orOperator(
                        Criteria.where("statusHistory.status").is("ACKNOWLEDGED"),
                        Criteria.where("acknowledgedAt").exists(true)
                )
        ));

        // Step 3: Extract timestamps from statusHistory using a single $addFields stage
        // This is much simpler and more performant than multiple projection stages
        operations.add(context -> new org.bson.Document("$addFields", new org.bson.Document()
                // Extract ACKNOWLEDGED timestamp from statusHistory
                .append("acknowledgedTimestamp", new org.bson.Document("$ifNull",
                        java.util.Arrays.asList(
                                // First try to get timestamp from statusHistory ACKNOWLEDGED entry
                                new org.bson.Document("$arrayElemAt", java.util.Arrays.asList(
                                        new org.bson.Document("$map", new org.bson.Document()
                                                .append("input", new org.bson.Document("$filter", new org.bson.Document()
                                                        .append("input", "$statusHistory")
                                                        .append("as", "entry")
                                                        .append("cond", new org.bson.Document("$eq",
                                                                java.util.Arrays.asList("$$entry.status", "ACKNOWLEDGED")))))
                                                .append("as", "item")
                                                .append("in", "$$item.timestamp")),
                                        0)),
                                // Fallback to acknowledgedAt field if statusHistory doesn't have ACKNOWLEDGED
                                "$acknowledgedAt"
                        )))
                // Extract DELETED timestamp from statusHistory
                .append("deletedTimestamp", new org.bson.Document("$arrayElemAt",
                        java.util.Arrays.asList(
                                new org.bson.Document("$map", new org.bson.Document()
                                        .append("input", new org.bson.Document("$filter", new org.bson.Document()
                                                .append("input", "$statusHistory")
                                                .append("as", "entry")
                                                .append("cond", new org.bson.Document("$eq",
                                                        java.util.Arrays.asList("$$entry.status", "DELETED")))))
                                        .append("as", "item")
                                        .append("in", "$$item.timestamp")),
                                0)))
        ));

        // Step 4: Calculate purge status based on timestamps and SLA
        // Using raw BSON for cleaner and more direct logic
        // Convert SLA deadline to milliseconds for comparison
        long slaDeadlineMillis = slaDeadline.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();

        // Logic:
        // - PURGED: Both acknowledgedTimestamp AND deletedTimestamp exist, AND deletedTimestamp > acknowledgedTimestamp
        // - PENDING_OVERDUE: acknowledgedTimestamp exists, NO deletedTimestamp, AND acknowledgedTimestamp < slaDeadline
        // - PENDING_WITHIN_SLA: acknowledgedTimestamp exists, NO deletedTimestamp, AND acknowledgedTimestamp >= slaDeadline
        operations.add(context -> new org.bson.Document("$addFields", new org.bson.Document()
                .append("purgeStatus", new org.bson.Document("$cond", new org.bson.Document()
                        // Check if both timestamps exist
                        .append("if", new org.bson.Document("$and", java.util.Arrays.asList(
                                new org.bson.Document("$ne", java.util.Arrays.asList("$acknowledgedTimestamp", null)),
                                new org.bson.Document("$ne", java.util.Arrays.asList("$deletedTimestamp", null)),
                                new org.bson.Document("$gt", java.util.Arrays.asList("$deletedTimestamp", "$acknowledgedTimestamp"))
                        )))
                        .append("then", "PURGED")
                        // If not purged, check if acknowledged (for pending status)
                        .append("else", new org.bson.Document("$cond", new org.bson.Document()
                                .append("if", new org.bson.Document("$ne", java.util.Arrays.asList("$acknowledgedTimestamp", null)))
                                .append("then", new org.bson.Document("$cond", new org.bson.Document()
                                        // Check if exceeded SLA - convert timestamp to long for proper comparison
                                        .append("if", new org.bson.Document("$lt", java.util.Arrays.asList(
                                                new org.bson.Document("$toLong", "$acknowledgedTimestamp"),
                                                slaDeadlineMillis
                                        )))
                                        .append("then", "PENDING_OVERDUE")
                                        .append("else", "PENDING_WITHIN_SLA")
                                ))
                                .append("else", "UNKNOWN")
                        ))
                ))
        ));

        // Step 5: Lookup Data Fiduciary information from business_applications
        operations.add(Aggregation.lookup(
                "business_applications",
                "recipientId",
                "businessId",
                "dfInfo"
        ));

        // Step 6: Lookup Data Processor information from data_processors
        operations.add(Aggregation.lookup(
                "data_processors",
                "recipientId",
                "dataProcessorId",
                "dpInfo"
        ));

        // Step 7: Add computed field for recipient name and dataItems
        operations.add(Aggregation.project()
                .and("eventType").as("eventType")
                .and("recipientType").as("recipientType")
                .and("recipientId").as("recipientId")
                .and("purgeStatus").as("purgeStatus")
                .and(ConditionalOperators.Cond.when(
                        ComparisonOperators.Eq.valueOf("recipientType").equalToValue(RecipientType.DATA_FIDUCIARY.name())
                ).then(
                        ConditionalOperators.ifNull(
                                ArrayOperators.ArrayElemAt.arrayOf("dfInfo.name").elementAt(0)
                        ).then("$recipientId")
                ).otherwise(
                        ConditionalOperators.ifNull(
                                ArrayOperators.ArrayElemAt.arrayOf("dpInfo.dataProcessorName").elementAt(0)
                        ).then("$recipientId")
                )).as("recipientName")
                // Get complete recipient data (all fields from dfInfo or dpInfo)
                .and(ConditionalOperators.Cond.when(
                        ComparisonOperators.Eq.valueOf("recipientType").equalToValue(RecipientType.DATA_FIDUCIARY.name())
                ).then("$dfInfo").otherwise("$dpInfo")).as("dataItems")
        );

        // Step 8: Use $facet for parallel processing of global, DF, and DP statistics
        FacetOperation facetOperation = Aggregation.facet()
                .and(buildGlobalPurgeStatsPipeline()).as("globalStats")
                .and(buildRecipientPurgeStatsPipeline(RecipientType.DATA_FIDUCIARY.name())).as("dfStats")
                .and(buildRecipientPurgeStatsPipeline(RecipientType.DATA_PROCESSOR.name())).as("dpStats");

        operations.add(facetOperation);

        // Build aggregation with options for large datasets and performance
        return Aggregation.newAggregation(operations)
                .withOptions(AggregationOptions.builder()
                        .allowDiskUse(true)  // Allow using disk for large datasets (>100MB)
                        .maxTime(Duration.ofSeconds(60))  // 60 second timeout to prevent long-running queries
                        .build());
    }

    /**
     * Builds the pipeline for global purge statistics.
     */
    private AggregationOperation[] buildGlobalPurgeStatsPipeline() {
        List<AggregationOperation> pipeline = new ArrayList<>();

        // Group by eventType first for event-specific stats
        GroupOperation groupByEventType = Aggregation.group("eventType")
                .count().as("totalEvents")
                .sum(ConditionalOperators.Cond.when(
                        ComparisonOperators.Eq.valueOf("purgeStatus").equalToValue("PURGED")
                ).then(1).otherwise(0)).as("purgedEvents")
                // pendingEvents = count of PENDING_OVERDUE + PENDING_WITHIN_SLA (all acknowledged but not deleted)
                .sum(ConditionalOperators.Cond.when(
                        ArrayOperators.In.arrayOf(Arrays.asList("PENDING_OVERDUE", "PENDING_WITHIN_SLA"))
                                .containsValue("$purgeStatus")
                ).then(1).otherwise(0)).as("pendingEvents")
                // overdueEvents = count of PENDING_OVERDUE only (subset of pending)
                .sum(ConditionalOperators.Cond.when(
                        ComparisonOperators.Eq.valueOf("purgeStatus").equalToValue("PENDING_OVERDUE")
                ).then(1).otherwise(0)).as("overdueEvents");

        pipeline.add(groupByEventType);

        // Project event type stats
        pipeline.add(Aggregation.project()
                .and("_id").as("eventType")
                .and("totalEvents").as("totalEvents")
                .and("purgedEvents").as("purgedEvents")
                .and("pendingEvents").as("pendingEvents")
                .and("overdueEvents").as("overdueEvents")
        );

        // Group all to get overall stats and collect event type breakdown
        GroupOperation groupAll = Aggregation.group()
                .sum("totalEvents").as("totalEvents")
                .sum("purgedEvents").as("purgedEvents")
                .sum("pendingEvents").as("pendingEvents")
                .sum("overdueEvents").as("overdueEvents")
                .push(new Document()
                        .append("eventType", "$eventType")
                        .append("totalEvents", "$totalEvents")
                        .append("purgedEvents", "$purgedEvents")
                        .append("pendingEvents", "$pendingEvents")
                        .append("overdueEvents", "$overdueEvents")
                ).as("byEventType");

        pipeline.add(groupAll);

        return pipeline.toArray(new AggregationOperation[0]);
    }

    /**
     * Builds the pipeline for recipient-specific purge statistics.
     */
    private AggregationOperation[] buildRecipientPurgeStatsPipeline(String recipientType) {
        List<AggregationOperation> pipeline = new ArrayList<>();

        // Filter by recipient type
        pipeline.add(Aggregation.match(Criteria.where("recipientType").is(recipientType)));

        // Group by recipientId and eventType
        GroupOperation groupByRecipientAndEvent = Aggregation.group(
                Fields.fields("recipientId", "eventType")
        )
                .first("recipientName").as("recipientName")
                .first("dataItems").as("dataItems")
                .count().as("totalEvents")
                .sum(ConditionalOperators.Cond.when(
                        ComparisonOperators.Eq.valueOf("purgeStatus").equalToValue("PURGED")
                ).then(1).otherwise(0)).as("purgedEvents")
                // pendingEvents = count of PENDING_OVERDUE + PENDING_WITHIN_SLA (all acknowledged but not deleted)
                .sum(ConditionalOperators.Cond.when(
                        ArrayOperators.In.arrayOf(Arrays.asList("PENDING_OVERDUE", "PENDING_WITHIN_SLA"))
                                .containsValue("$purgeStatus")
                ).then(1).otherwise(0)).as("pendingEvents")
                // overdueEvents = count of PENDING_OVERDUE only (subset of pending)
                .sum(ConditionalOperators.Cond.when(
                        ComparisonOperators.Eq.valueOf("purgeStatus").equalToValue("PENDING_OVERDUE")
                ).then(1).otherwise(0)).as("overdueEvents");

        pipeline.add(groupByRecipientAndEvent);

        // Project event type stats
        pipeline.add(Aggregation.project()
                .and("_id.recipientId").as("recipientId")
                .and("_id.eventType").as("eventType")
                .and("recipientName").as("recipientName")
                .and("dataItems").as("dataItems")
                .and("totalEvents").as("totalEvents")
                .and("purgedEvents").as("purgedEvents")
                .and("pendingEvents").as("pendingEvents")
                .and("overdueEvents").as("overdueEvents")
        );

        // Group by recipientId to aggregate event types
        GroupOperation groupByRecipient = Aggregation.group("recipientId")
                .first("recipientName").as("name")
                .first("dataItems").as("dataItems")
                .sum("totalEvents").as("totalEvents")
                .sum("purgedEvents").as("purgedEvents")
                .sum("pendingEvents").as("pendingEvents")
                .sum("overdueEvents").as("overdueEvents")
                .push(new Document()
                        .append("eventType", "$eventType")
                        .append("totalEvents", "$totalEvents")
                        .append("purgedEvents", "$purgedEvents")
                        .append("pendingEvents", "$pendingEvents")
                        .append("overdueEvents", "$overdueEvents")
                ).as("byEventType");

        pipeline.add(groupByRecipient);

        return pipeline.toArray(new AggregationOperation[0]);
    }

    /**
     * Processes the purge aggregation results and builds the response DTO.
     */
    private CallbackPurgeStatsResponseDto processPurgeStatsResults(List<Document> results, MongoTemplate mongoTemplate) {
        CallbackPurgeStatsResponseDto.CallbackPurgeStatsResponseDtoBuilder responseBuilder =
                CallbackPurgeStatsResponseDto.builder();

        if (results.isEmpty()) {
            return buildEmptyPurgeStats();
        }

        Document result = results.get(0);

        // Process global stats
        List<Document> globalStatsList = (List<Document>) result.get("globalStats");
        CallbackPurgeStatsResponseDto.GlobalStats globalStats = processGlobalPurgeStats(globalStatsList);
        responseBuilder.stats(globalStats);

        // Process DF stats
        List<Document> dfStatsList = (List<Document>) result.get("dfStats");
        Map<String, CallbackPurgeStatsResponseDto.RecipientStats> dfStatsMap = processRecipientPurgeStats(dfStatsList);
        responseBuilder.dataFiduciary(dfStatsMap);

        // Process DP stats
        List<Document> dpStatsList = (List<Document>) result.get("dpStats");
        Map<String, CallbackPurgeStatsResponseDto.RecipientStats> dpStatsMap = processRecipientPurgeStats(dpStatsList);
        responseBuilder.dataProcessor(dpStatsMap);

        return responseBuilder.build();
    }

    /**
     * Processes global purge statistics from aggregation results.
     */
    private CallbackPurgeStatsResponseDto.GlobalStats processGlobalPurgeStats(List<Document> globalStatsList) {
        if (globalStatsList == null || globalStatsList.isEmpty()) {
            return CallbackPurgeStatsResponseDto.GlobalStats.builder()
                    .totalEvents(0L)
                    .purgedEvents(0L)
                    .pendingEvents(0L)
                    .overdueEvents(0L)
                    .purgePercentage(0.0)
                    .pendingPercentage(0.0)
                    .overduePercentage(0.0)
                    .byEventType(new ArrayList<>())
                    .build();
        }

        Document globalDoc = globalStatsList.get(0);

        Long totalEvents = getLongValue(globalDoc, "totalEvents");
        Long purgedEvents = getLongValue(globalDoc, "purgedEvents");
        Long pendingEvents = getLongValue(globalDoc, "pendingEvents");
        Long overdueEvents = getLongValue(globalDoc, "overdueEvents");

        List<Document> eventTypeList = (List<Document>) globalDoc.get("byEventType");
        List<CallbackPurgeStatsResponseDto.EventTypeStats> eventTypeStats = processPurgeEventTypeStats(eventTypeList);

        // Calculate percentages
        double purgePercentage = totalEvents > 0 ? (purgedEvents * 100.0 / totalEvents) : 0.0;
        double pendingPercentage = totalEvents > 0 ? (pendingEvents * 100.0 / totalEvents) : 0.0;
        double overduePercentage = totalEvents > 0 ? (overdueEvents * 100.0 / totalEvents) : 0.0;

        return CallbackPurgeStatsResponseDto.GlobalStats.builder()
                .totalEvents(totalEvents)
                .purgedEvents(purgedEvents)
                .pendingEvents(pendingEvents)
                .overdueEvents(overdueEvents)
                .purgePercentage(purgePercentage)
                .pendingPercentage(pendingPercentage)
                .overduePercentage(overduePercentage)
                .byEventType(eventTypeStats)
                .build();
    }

    /**
     * Processes recipient purge statistics from aggregation results.
     */
    private Map<String, CallbackPurgeStatsResponseDto.RecipientStats> processRecipientPurgeStats(List<Document> recipientStatsList) {
        if (recipientStatsList == null || recipientStatsList.isEmpty()) {
            return new HashMap<>();
        }

        Map<String, CallbackPurgeStatsResponseDto.RecipientStats> statsMap = new HashMap<>();

        for (Document recipientDoc : recipientStatsList) {
            String recipientId = recipientDoc.getString("_id");
            String name = recipientDoc.getString("name");

            // Extract dataItems (complete recipient data)
            List<Object> dataItems = (List<Object>) recipientDoc.get("dataItems");
            if (dataItems == null) {
                dataItems = new ArrayList<>();
            }

            Long totalEvents = getLongValue(recipientDoc, "totalEvents");
            Long purgedEvents = getLongValue(recipientDoc, "purgedEvents");
            Long pendingEvents = getLongValue(recipientDoc, "pendingEvents");
            Long overdueEvents = getLongValue(recipientDoc, "overdueEvents");

            List<Document> eventTypeList = (List<Document>) recipientDoc.get("byEventType");
            List<CallbackPurgeStatsResponseDto.EventTypeStats> eventTypeStats = processPurgeEventTypeStats(eventTypeList);

            // Calculate percentages
            double purgePercentage = totalEvents > 0 ? (purgedEvents * 100.0 / totalEvents) : 0.0;
            double pendingPercentage = totalEvents > 0 ? (pendingEvents * 100.0 / totalEvents) : 0.0;
            double overduePercentage = totalEvents > 0 ? (overdueEvents * 100.0 / totalEvents) : 0.0;

            CallbackPurgeStatsResponseDto.RecipientStats stats = CallbackPurgeStatsResponseDto.RecipientStats.builder()
                    .name(name != null ? name : recipientId)
                    .dataItems(dataItems)
                    .totalEvents(totalEvents)
                    .purgedEvents(purgedEvents)
                    .pendingEvents(pendingEvents)
                    .overdueEvents(overdueEvents)
                    .purgePercentage(purgePercentage)
                    .pendingPercentage(pendingPercentage)
                    .overduePercentage(overduePercentage)
                    .byEventType(eventTypeStats)
                    .build();

            statsMap.put(recipientId, stats);
        }

        return statsMap;
    }

    /**
     * Processes event type purge statistics from aggregation results.
     */
    private List<CallbackPurgeStatsResponseDto.EventTypeStats> processPurgeEventTypeStats(List<Document> eventTypeList) {
        if (eventTypeList == null || eventTypeList.isEmpty()) {
            return new ArrayList<>();
        }

        return eventTypeList.stream()
                .map(eventDoc -> {
                    String eventType = eventDoc.getString("eventType");
                    Long totalEvents = getLongValue(eventDoc, "totalEvents");
                    Long purgedEvents = getLongValue(eventDoc, "purgedEvents");
                    Long pendingEvents = getLongValue(eventDoc, "pendingEvents");
                    Long overdueEvents = getLongValue(eventDoc, "overdueEvents");

                    // Calculate percentages
                    double purgePercentage = totalEvents > 0 ? (purgedEvents * 100.0 / totalEvents) : 0.0;
                    double pendingPercentage = totalEvents > 0 ? (pendingEvents * 100.0 / totalEvents) : 0.0;
                    double overduePercentage = totalEvents > 0 ? (overdueEvents * 100.0 / totalEvents) : 0.0;

                    return CallbackPurgeStatsResponseDto.EventTypeStats.builder()
                            .eventType(eventType)
                            .totalEvents(totalEvents)
                            .purgedEvents(purgedEvents)
                            .pendingEvents(pendingEvents)
                            .overdueEvents(overdueEvents)
                            .purgePercentage(purgePercentage)
                            .pendingPercentage(pendingPercentage)
                            .overduePercentage(overduePercentage)
                            .build();
                })
                .collect(Collectors.toList());
    }

    /**
     * Builds an empty purge statistics response.
     */
    private CallbackPurgeStatsResponseDto buildEmptyPurgeStats() {
        CallbackPurgeStatsResponseDto.GlobalStats globalStats = CallbackPurgeStatsResponseDto.GlobalStats.builder()
                .totalEvents(0L)
                .purgedEvents(0L)
                .pendingEvents(0L)
                .overdueEvents(0L)
                .purgePercentage(0.0)
                .pendingPercentage(0.0)
                .overduePercentage(0.0)
                .byEventType(new ArrayList<>())
                .build();

        return CallbackPurgeStatsResponseDto.builder()
                .stats(globalStats)
                .dataFiduciary(new HashMap<>())
                .dataProcessor(new HashMap<>())
                .build();
    }

    // ==================== Consent Deletion Dashboard Implementation ====================

    @Override
    public ConsentDeletionDashboardResponseDto getConsentDeletionDashboard(
            String businessId,
            ConsentDeletionFilterRequestDto filter,
            MongoTemplate mongoTemplate) {
        try {
            log.info("Fetching consent deletion dashboard for businessId: {}, filter: {}", businessId, filter);

            // Build and execute the aggregation pipeline
            // This starts from notification_events collection and joins with notification_callback
            List<Document> results = executeConsentDeletionAggregation(businessId, filter, mongoTemplate);

            log.debug("Aggregation returned {} results", results.size());

            // Process results and build response
            return processConsentDeletionResults(results, filter);

        } catch (Exception e) {
            log.error("Error fetching consent deletion dashboard for businessId {}: {}", businessId, e.getMessage());
            log.error("Full stack trace:", e);
            throw new RuntimeException("Failed to fetch consent deletion dashboard: " + e.getMessage(), e);
        }
    }

    /**
     * Executes the MongoDB aggregation pipeline for consent deletion dashboard.
     * Starts from notification_events collection and joins with notification_callback.
     */
    private List<Document> executeConsentDeletionAggregation(
            String businessId,
            ConsentDeletionFilterRequestDto filter,
            MongoTemplate mongoTemplate) {

        List<Document> pipeline = new ArrayList<>();

        // Stage 1: Match events by businessId and event types
        Document matchStage = new Document("$match", new Document()
                .append("business_id", businessId)
                .append("event_type", new Document("$in", filter.getEventTypesToFilter())));
        pipeline.add(matchStage);

        // Stage 2: Add date range filter if provided
        if (filter.getFromDate() != null || filter.getToDate() != null) {
            Document dateMatch = new Document();
            if (filter.getFromDate() != null) {
                dateMatch.append("$gte", filter.getFromDate());
            }
            if (filter.getToDate() != null) {
                dateMatch.append("$lt", filter.getToDate());
            }
            pipeline.add(new Document("$match", new Document("created_at", dateMatch)));
        }

        // Stage 3: Extract consentId from event_payload
        pipeline.add(new Document("$addFields", new Document()
                .append("consentId", "$event_payload.consentId")));

        // Stage 4: Filter events with valid consentId
        pipeline.add(new Document("$match", new Document("consentId",
                new Document("$ne", null).append("$exists", true))));

        // Stage 5: Filter by consentId if provided
        if (filter.getConsentId() != null && !filter.getConsentId().trim().isEmpty()) {
            pipeline.add(new Document("$match", new Document("consentId", filter.getConsentId())));
        }

        // Stage 6: Filter by dataPrincipal if provided
        if (filter.getDataPrincipal() != null && !filter.getDataPrincipal().trim().isEmpty()) {
            pipeline.add(new Document("$match",
                    new Document("customer_identifiers.value", filter.getDataPrincipal())));
        }

        // Stage 7: Sort by createdAt DESC (for taking latest event per consent)
        pipeline.add(new Document("$sort", new Document("created_at", -1)));

        // Stage 8: Group by consentId, take first (latest) event
        pipeline.add(new Document("$group", new Document()
                .append("_id", "$consentId")
                .append("eventId", new Document("$first", "$event_id"))
                .append("eventType", new Document("$first", "$event_type"))
                .append("dataPrincipal", new Document("$first", "$customer_identifiers.value"))
                .append("eventTimestamp", new Document("$first", "$created_at"))
                .append("withdrawalData", new Document("$first", "$withdrawal_data"))));

        // Stage 9: Lookup callbacks from notification_callback collection
        pipeline.add(new Document("$lookup", new Document()
                .append("from", "notification_callback")
                .append("localField", "eventId")
                .append("foreignField", "eventId")
                .append("as", "callbacks")));

        // Stage 10: Add processorId filter if provided (filter consents that have a specific DP)
        if (filter.getProcessorId() != null && !filter.getProcessorId().trim().isEmpty()) {
            pipeline.add(new Document("$match", new Document("callbacks",
                    new Document("$elemMatch", new Document()
                            .append("recipientType", "DATA_PROCESSOR")
                            .append("recipientId", filter.getProcessorId())))));
        }

        // Stage 11: Extract DP callbacks for processor status calculation
        pipeline.add(new Document("$addFields", new Document()
                // Get all DP callbacks
                .append("dpCallbacks", new Document("$filter", new Document()
                        .append("input", "$callbacks")
                        .append("as", "cb")
                        .append("cond", new Document("$eq", Arrays.asList("$$cb.recipientType", "DATA_PROCESSOR")))))));

        // Stage 12: Extract DF status from withdrawal_data.dataFiduciary.overallStatus
        pipeline.add(new Document("$addFields", new Document()
                .append("dfRawStatus", new Document("$ifNull", Arrays.asList(
                        "$withdrawalData.dataFiduciary.overallStatus", "PENDING")))));

        // Stage 13: Map DF status (COMPLETED -> DELETED)
        pipeline.add(new Document("$addFields", new Document()
                .append("dfStatus", new Document("$switch", new Document()
                        .append("branches", Arrays.asList(
                                new Document().append("case", new Document("$eq", Arrays.asList("$dfRawStatus", "COMPLETED"))).append("then", "DELETED"),
                                new Document().append("case", new Document("$eq", Arrays.asList("$dfRawStatus", "DEFERRED"))).append("then", "DEFERRED"),
                                new Document().append("case", new Document("$eq", Arrays.asList("$dfRawStatus", "FAILED"))).append("then", "FAILED"),
                                new Document().append("case", new Document("$eq", Arrays.asList("$dfRawStatus", "NOT_APPLICABLE"))).append("then", "NOT_APPLICABLE"),
                                new Document().append("case", new Document("$eq", Arrays.asList("$dfRawStatus", "PENDING"))).append("then", "PENDING")))
                        .append("default", "PENDING")))));

        // Stage 14: Calculate DP counts
        pipeline.add(new Document("$addFields", new Document()
                .append("dpTotalCount", new Document("$size",
                        new Document("$ifNull", Arrays.asList("$dpCallbacks", Arrays.asList()))))
                .append("dpDeletedCount", new Document("$size", new Document("$filter", new Document()
                        .append("input", new Document("$ifNull", Arrays.asList("$dpCallbacks", Arrays.asList())))
                        .append("as", "dp")
                        .append("cond", new Document("$in", Arrays.asList(
                                "DELETED",
                                new Document("$ifNull", Arrays.asList("$$dp.statusHistory.status", Arrays.asList()))))))))
                .append("dpDeferredCount", new Document("$size", new Document("$filter", new Document()
                        .append("input", new Document("$ifNull", Arrays.asList("$dpCallbacks", Arrays.asList())))
                        .append("as", "dp")
                        .append("cond", new Document("$in", Arrays.asList(
                                "DEFERRED",
                                new Document("$ifNull", Arrays.asList("$$dp.statusHistory.status", Arrays.asList()))))))))));

        // Stage 15: Calculate overall status and overallCompletion
        // Priority: Done > Deferred > Failed > Partial > Pending
        // Done: dfStatus == DELETED AND dpDeletedCount == dpTotalCount (all deleted)
        // Deferred: dfStatus == DEFERRED OR dpDeferredCount > 0 (any deferred)
        // Failed: dfStatus == FAILED
        // Partial: At least one has DELETED but not all
        // Pending: No one has DELETED yet
        //
        // overallCompletion:
        // - Completed: DF has withdrawal_data AND all DPs have DELETED or DEFERRED status
        // - Inprogress: Any recipient has not yet responded
        pipeline.add(new Document("$addFields", new Document()
                .append("overall", new Document("$cond", new Document()
                        // Done: dfStatus == DELETED or NOT_APPLICABLE AND all DPs deleted
                        .append("if", new Document("$and", Arrays.asList(
                                new Document("$in", Arrays.asList("$dfStatus", Arrays.asList("DELETED", "NOT_APPLICABLE"))),
                                new Document("$eq", Arrays.asList("$dpDeletedCount", "$dpTotalCount")))))
                        .append("then", "Done")
                        .append("else", new Document("$cond", new Document()
                                // Deferred: dfStatus == DEFERRED OR any DP deferred
                                .append("if", new Document("$or", Arrays.asList(
                                        new Document("$eq", Arrays.asList("$dfStatus", "DEFERRED")),
                                        new Document("$gt", Arrays.asList("$dpDeferredCount", 0)))))
                                .append("then", "Deferred")
                                .append("else", new Document("$cond", new Document()
                                        // Failed: dfStatus == FAILED
                                        .append("if", new Document("$eq", Arrays.asList("$dfStatus", "FAILED")))
                                        .append("then", "Failed")
                                        .append("else", new Document("$cond", new Document()
                                                // Partial: some deleted but not all
                                                .append("if", new Document("$or", Arrays.asList(
                                                        new Document("$in", Arrays.asList("$dfStatus", Arrays.asList("DELETED", "NOT_APPLICABLE"))),
                                                        new Document("$gt", Arrays.asList("$dpDeletedCount", 0)))))
                                                .append("then", "Partial")
                                                // Pending: nothing deleted
                                                .append("else", "Pending")))))))))
                .append("overallCompletion", new Document("$cond", new Document()
                        .append("if", new Document("$and", Arrays.asList(
                                new Document("$ne", Arrays.asList("$withdrawalData", null)),
                                new Document("$eq", Arrays.asList(
                                        new Document("$add", Arrays.asList("$dpDeletedCount", "$dpDeferredCount")),
                                        "$dpTotalCount")))))
                        .append("then", "Completed")
                        .append("else", "Inprogress")))
                .append("processors", new Document("$concat", Arrays.asList(
                        new Document("$toString", "$dpDeletedCount"),
                        "/",
                        new Document("$toString", "$dpTotalCount"),
                        " Done")))));

        // Stage 16: Apply overall status filter if provided
        if (filter.getOverallStatus() != null && !filter.getOverallStatus().trim().isEmpty()) {
            // Map the filter value to expected values (Done, Partial, Pending, Deferred, Failed)
            String statusFilter = filter.getOverallStatus().toUpperCase();
            String mappedStatus = switch (statusFilter) {
                case "DONE" -> "Done";
                case "PARTIAL" -> "Partial";
                case "PENDING" -> "Pending";
                case "DEFERRED" -> "Deferred";
                case "FAILED" -> "Failed";
                default -> statusFilter;
            };
            pipeline.add(new Document("$match", new Document("overall", mappedStatus)));
        }

        // Stage 16b: Apply overallCompletion filter if provided
        if (filter.getOverallCompletion() != null && !filter.getOverallCompletion().trim().isEmpty()) {
            String completionFilter = filter.getOverallCompletion().toUpperCase();
            String mappedCompletion;
            if ("COMPLETED".equals(completionFilter)) {
                mappedCompletion = "Completed";
            } else if ("INPROGRESS".equals(completionFilter)) {
                mappedCompletion = "Inprogress";
            } else {
                mappedCompletion = completionFilter;
            }
            pipeline.add(new Document("$match", new Document("overallCompletion", mappedCompletion)));
        }

        // Stage 17: Lookup DATA_DELETION_NOTIFICATION events to check if user was notified
        // This joins with notification_events to find if a notification was sent for this consent
        pipeline.add(new Document("$lookup", new Document()
                .append("from", "notification_events")
                .append("let", new Document()
                        .append("consentId", "$_id")
                        .append("businessId", businessId))
                .append("pipeline", Arrays.asList(
                        new Document("$match", new Document("$expr", new Document("$and", Arrays.asList(
                                new Document("$eq", Arrays.asList("$event_type", "DATA_DELETION_NOTIFICATION")),
                                new Document("$eq", Arrays.asList("$event_payload.consentId", "$$consentId")),
                                new Document("$eq", Arrays.asList("$business_id", "$$businessId"))
                        )))),
                        new Document("$limit", 1)
                ))
                .append("as", "deletionNotification")));

        // Stage 18: Add field to indicate if user was notified
        pipeline.add(new Document("$addFields", new Document()
                .append("isUserNotified", new Document("$gt", Arrays.asList(
                        new Document("$size", "$deletionNotification"), 0)))));

        // Stage 19: Use $facet for overview metrics and paginated list
        Document facetStage = new Document("$facet", new Document()
                // Overview metrics pipeline
                .append("overview", Arrays.asList(
                        new Document("$group", new Document()
                                .append("_id", null)
                                .append("deletionRequests", new Document("$sum", 1))
                                .append("completed", new Document("$sum", new Document("$cond", Arrays.asList(
                                        new Document("$eq", Arrays.asList("$overall", "Done")), 1, 0))))
                                .append("deferred", new Document("$sum", new Document("$cond", Arrays.asList(
                                        new Document("$eq", Arrays.asList("$overall", "Deferred")), 1, 0))))
                                // userNotified: Count of completed (Done) consents where DATA_DELETION_NOTIFICATION was sent
                                .append("userNotified", new Document("$sum", new Document("$cond", Arrays.asList(
                                        new Document("$and", Arrays.asList(
                                                new Document("$eq", Arrays.asList("$overall", "Done")),
                                                new Document("$eq", Arrays.asList("$isUserNotified", true))
                                        )), 1, 0))))),
                        new Document("$addFields", new Document()
                                .append("inProgress", new Document("$subtract", Arrays.asList(
                                        "$deletionRequests",
                                        new Document("$add", Arrays.asList("$completed", "$deferred")))))
                                // notificationPending: completed - userNotified
                                .append("notificationPending", new Document("$subtract", Arrays.asList(
                                        "$completed", "$userNotified"))))
                ))
                // Paginated data pipeline
                .append("data", Arrays.asList(
                        new Document("$sort", new Document("eventTimestamp", -1)),
                        new Document("$skip", filter.getPage() * filter.getSize()),
                        new Document("$limit", filter.getSize()),
                        new Document("$project", new Document()
                                .append("_id", 0)
                                .append("consentId", "$_id")
                                .append("dataPrincipal", 1)
                                .append("trigger", "$eventType")
                                .append("dfStatus", 1)
                                .append("processors", 1)
                                .append("overall", 1)
                                .append("overallCompletion", 1)
                                .append("eventTimestamp", 1)
                                .append("eventId", 1))
                ))
                // Total count for pagination
                .append("totalCount", Arrays.asList(
                        new Document("$count", "count")
                )));
        pipeline.add(facetStage);

        // Execute aggregation on notification_events collection using Spring Data API
        // This ensures tenant-aware database routing through TenantAwareMongoDbFactory
        log.debug("Building aggregation with {} pipeline stages", pipeline.size());

        try {
            Aggregation aggregation = Aggregation.newAggregation(
                    pipeline.stream()
                            .map(doc -> (AggregationOperation) context -> doc)
                            .toArray(AggregationOperation[]::new)
            ).withOptions(AggregationOptions.builder()
                    .allowDiskUse(true)
                    .maxTime(Duration.ofSeconds(60))
                    .build());

            log.debug("Executing aggregation on notification_events collection");
            List<Document> results = mongoTemplate.aggregate(aggregation, "notification_events", Document.class)
                    .getMappedResults();
            log.debug("Aggregation completed with {} results", results.size());
            return results;
        } catch (Exception e) {
            log.error("Aggregation execution failed: {}", e.getMessage());
            log.error("Pipeline: {}", pipeline);
            throw e;
        }
    }

    /**
     * Processes the consent deletion aggregation results and builds the response DTO.
     */
    private ConsentDeletionDashboardResponseDto processConsentDeletionResults(
            List<Document> results,
            ConsentDeletionFilterRequestDto filter) {

        if (results.isEmpty()) {
            return buildEmptyConsentDeletionResponse(filter);
        }

        Document result = results.get(0);

        // Process overview metrics
        List<Document> overviewList = (List<Document>) result.get("overview");
        ConsentDeletionDashboardResponseDto.OverviewMetrics overview = processConsentDeletionOverview(overviewList);

        // Process data list
        List<Document> dataList = (List<Document>) result.get("data");
        List<ConsentDeletionDashboardResponseDto.ConsentDeletionItemDto> items = processConsentDeletionItems(dataList);

        // Get total count for pagination
        List<Document> totalCountList = (List<Document>) result.get("totalCount");
        long totalItems = 0;
        if (totalCountList != null && !totalCountList.isEmpty()) {
            totalItems = getLongValue(totalCountList.get(0), "count");
        }

        // Build pagination info
        int totalPages = (int) Math.ceil((double) totalItems / filter.getSize());
        PagedResponseDto.PaginationInfo paginationInfo = PagedResponseDto.PaginationInfo.builder()
                .totalItems(totalItems)
                .totalPages(totalPages)
                .page(filter.getPage())
                .pageSize(filter.getSize())
                .hasNext(filter.getPage() < totalPages - 1)
                .hasPrevious(filter.getPage() > 0)
                .build();

        // Build paged response
        PagedResponseDto<ConsentDeletionDashboardResponseDto.ConsentDeletionItemDto> pagedResponse =
                PagedResponseDto.<ConsentDeletionDashboardResponseDto.ConsentDeletionItemDto>builder()
                        .data(items)
                        .pagination(paginationInfo)
                        .build();

        return ConsentDeletionDashboardResponseDto.builder()
                .overview(overview)
                .deletionRequests(pagedResponse)
                .build();
    }

    /**
     * Processes the overview metrics from aggregation results.
     */
    private ConsentDeletionDashboardResponseDto.OverviewMetrics processConsentDeletionOverview(List<Document> overviewList) {
        if (overviewList == null || overviewList.isEmpty()) {
            return ConsentDeletionDashboardResponseDto.OverviewMetrics.builder()
                    .deletionRequests(0L)
                    .completed(0L)
                    .deferred(0L)
                    .inProgress(0L)
                    .userNotified(0L)
                    .notificationPending(0L)
                    .build();
        }

        Document overviewDoc = overviewList.get(0);
        return ConsentDeletionDashboardResponseDto.OverviewMetrics.builder()
                .deletionRequests(getLongValue(overviewDoc, "deletionRequests"))
                .completed(getLongValue(overviewDoc, "completed"))
                .deferred(getLongValue(overviewDoc, "deferred"))
                .inProgress(getLongValue(overviewDoc, "inProgress"))
                .userNotified(getLongValue(overviewDoc, "userNotified"))
                .notificationPending(getLongValue(overviewDoc, "notificationPending"))
                .build();
    }

    /**
     * Processes the consent deletion items from aggregation results.
     */
    private List<ConsentDeletionDashboardResponseDto.ConsentDeletionItemDto> processConsentDeletionItems(List<Document> dataList) {
        if (dataList == null || dataList.isEmpty()) {
            return new ArrayList<>();
        }

        return dataList.stream()
                .map(doc -> ConsentDeletionDashboardResponseDto.ConsentDeletionItemDto.builder()
                        .consentId(doc.getString("consentId"))
                        .dataPrincipal(doc.getString("dataPrincipal"))
                        .trigger(doc.getString("trigger"))
                        .dfStatus(doc.getString("dfStatus"))
                        .processors(doc.getString("processors"))
                        .overall(doc.getString("overall"))
                        .overallCompletion(doc.getString("overallCompletion"))
                        .eventTimestamp(doc.get("eventTimestamp") instanceof java.util.Date ?
                                ((java.util.Date) doc.get("eventTimestamp")).toInstant()
                                        .atZone(java.time.ZoneId.systemDefault()).toLocalDateTime() :
                                (LocalDateTime) doc.get("eventTimestamp"))
                        .eventId(doc.getString("eventId"))
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * Builds an empty consent deletion dashboard response.
     */
    private ConsentDeletionDashboardResponseDto buildEmptyConsentDeletionResponse(ConsentDeletionFilterRequestDto filter) {
        ConsentDeletionDashboardResponseDto.OverviewMetrics emptyOverview =
                ConsentDeletionDashboardResponseDto.OverviewMetrics.builder()
                        .deletionRequests(0L)
                        .completed(0L)
                        .deferred(0L)
                        .inProgress(0L)
                        .userNotified(0L)
                        .notificationPending(0L)
                        .build();

        PagedResponseDto.PaginationInfo emptyPagination = PagedResponseDto.PaginationInfo.builder()
                .totalItems(0)
                .totalPages(0)
                .page(filter.getPage())
                .pageSize(filter.getSize())
                .hasNext(false)
                .hasPrevious(false)
                .build();

        PagedResponseDto<ConsentDeletionDashboardResponseDto.ConsentDeletionItemDto> emptyPagedResponse =
                PagedResponseDto.<ConsentDeletionDashboardResponseDto.ConsentDeletionItemDto>builder()
                        .data(new ArrayList<>())
                        .pagination(emptyPagination)
                        .build();

        return ConsentDeletionDashboardResponseDto.builder()
                .overview(emptyOverview)
                .deletionRequests(emptyPagedResponse)
                .build();
    }
}