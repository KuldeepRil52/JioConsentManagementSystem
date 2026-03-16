package com.jio.digigov.notification.service.notification;

import com.jio.digigov.notification.enums.NotificationStatus;
import com.jio.digigov.notification.util.MongoTemplateProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Base service providing common functionality for missed notification services.
 *
 * This service contains shared logic and utilities that are used across
 * different missed notification service implementations.
 */
@RequiredArgsConstructor
public abstract class MissedNotificationBaseService {

    protected final MongoTemplateProvider mongoTemplateProvider;

    protected static final List<String> MISSED_STATUSES = List.of(
            NotificationStatus.FAILED.name(),
            NotificationStatus.RETRY_SCHEDULED.name()
    );

    protected static final List<String> MISSED_STATUSES_EXTENDED = List.of(
            NotificationStatus.FAILED.name(),
            NotificationStatus.RETRY_SCHEDULED.name(),
            NotificationStatus.ERROR.name()
    );

    /**
     * Builds a basic query for missed notifications with date filtering.
     *
     * @param businessId Business identifier for filtering
     * @param fromDate Start date for filtering
     * @param toDate End date for filtering
     * @return Query object with basic criteria
     */
    protected Query buildMissedNotificationsQuery(String businessId, LocalDateTime fromDate, LocalDateTime toDate) {
        Criteria criteria = Criteria.where("status").in(MISSED_STATUSES);

        if (businessId != null && !businessId.trim().isEmpty()) {
            criteria.and("businessId").is(businessId);
        }

        if (fromDate != null) {
            criteria.and("createdAt").gte(fromDate);
        }

        if (toDate != null) {
            criteria.and("createdAt").lte(toDate);
        }

        return new Query(criteria);
    }

    /**
     * Builds a query for missed notifications with specific status filtering.
     *
     * @param businessId Business identifier for filtering
     * @param fromDate Start date for filtering
     * @param toDate End date for filtering
     * @param status Specific status to filter by
     * @return Query object with status-specific criteria
     */
    protected Query buildMissedNotificationsQuery(String businessId, LocalDateTime fromDate, LocalDateTime toDate,
                                                   String status) {
        Criteria criteria = Criteria.where("status").is(status);

        if (businessId != null && !businessId.trim().isEmpty()) {
            criteria.and("businessId").is(businessId);
        }

        if (fromDate != null) {
            criteria.and("createdAt").gte(fromDate);
        }

        if (toDate != null) {
            criteria.and("createdAt").lte(toDate);
        }

        return new Query(criteria);
    }

    /**
     * Builds a comprehensive query for callback notifications with multiple filter options.
     *
     * @param businessId Business identifier for filtering
     * @param recipientType Type of recipient
     * @param recipientId Recipient identifier
     * @param fromDate Start date for filtering
     * @param toDate End date for filtering
     * @param useExtendedStatuses Whether to use extended status list
     * @return Query object with comprehensive criteria
     */
    protected Query buildCallbackNotificationsQuery(String businessId, String recipientType, String recipientId,
                                                     LocalDateTime fromDate, LocalDateTime toDate,
                                                     boolean useExtendedStatuses) {
        List<String> statusList = useExtendedStatuses ? MISSED_STATUSES_EXTENDED : MISSED_STATUSES;
        Criteria criteria = Criteria.where("status").in(statusList);

        if (businessId != null && !businessId.trim().isEmpty()) {
            criteria.and("businessId").is(businessId);
        }

        if (recipientType != null && !recipientType.trim().isEmpty()) {
            criteria.and("recipientType").is(recipientType);
        }

        if (recipientId != null && !recipientId.trim().isEmpty()) {
            criteria.and("recipientId").is(recipientId);
        }

        if (fromDate != null) {
            criteria.and("createdAt").gte(fromDate);
        }

        if (toDate != null) {
            criteria.and("createdAt").lte(toDate);
        }

        return new Query(criteria);
    }
}