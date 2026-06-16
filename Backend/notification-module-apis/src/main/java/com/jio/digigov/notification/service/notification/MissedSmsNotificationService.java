package com.jio.digigov.notification.service.notification;

import com.jio.digigov.notification.dto.response.common.PagedResponseDto;
import com.jio.digigov.notification.dto.response.common.ResponseMetadata;
import com.jio.digigov.notification.entity.notification.NotificationSms;
import com.jio.digigov.notification.repository.notification.NotificationSmsRepository;
import com.jio.digigov.notification.util.MongoTemplateProvider;
import com.jio.digigov.notification.util.TenantContextHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Service for handling missed SMS notifications.
 *
 * This service provides functionality to retrieve and process missed SMS notifications
 * with support for pagination, filtering, and multi-tenant operations.
 */
@Service
@Slf4j
public class MissedSmsNotificationService extends MissedNotificationBaseService {

    private final NotificationSmsRepository smsRepository;

    public MissedSmsNotificationService(NotificationSmsRepository smsRepository,
                                       MongoTemplateProvider mongoTemplateProvider) {
        super(mongoTemplateProvider);
        this.smsRepository = smsRepository;
    }

    /**
     * Retrieves missed SMS notifications with pagination and filtering.
     *
     * @param tenantId Tenant identifier
     * @param businessId Business identifier for filtering
     * @param fromDate Start date for filtering
     * @param toDate End date for filtering
     * @param page Page number (0-based)
     * @param size Page size
     * @return PagedResponseDto containing missed SMS notifications
     */
    public PagedResponseDto<NotificationSms> getMissedSmsNotifications(
            String tenantId, String businessId, LocalDateTime fromDate, LocalDateTime toDate,
            int page, int size) {

        log.debug("Retrieving missed SMS notifications for tenant: {}, businessId: {}, page: {}, size: {}",
                tenantId, businessId, page, size);

        return executeNotificationQuery(
                tenantId,
                () -> buildMissedNotificationsQuery(businessId, fromDate, toDate),
                page,
                size,
                "Missed SMS notifications retrieved successfully",
                "Failed to retrieve missed SMS notifications"
        );
    }

    /**
     * Retrieves missed SMS notifications by specific status.
     *
     * @param tenantId Tenant identifier
     * @param businessId Business identifier for filtering
     * @param status Specific status to filter by
     * @param fromDate Start date for filtering
     * @param toDate End date for filtering
     * @param page Page number (0-based)
     * @param size Page size
     * @return PagedResponseDto containing missed SMS notifications
     */
    public PagedResponseDto<NotificationSms> getMissedSmsNotificationsByStatus(
            String tenantId, String businessId, String status, LocalDateTime fromDate, LocalDateTime toDate,
            int page, int size) {

        log.debug("Retrieving missed SMS notifications by status: {} for tenant: {}", status, tenantId);

        return executeNotificationQuery(
                tenantId,
                () -> buildMissedNotificationsQuery(businessId, fromDate, toDate, status),
                page,
                size,
                "Missed SMS notifications by status retrieved successfully",
                "Failed to retrieve missed SMS notifications by status"
        );
    }

    /**
     * Executes a notification query with common pagination and error handling logic.
     *
     * @param tenantId Tenant identifier
     * @param querySupplier Supplier that provides the query to execute
     * @param page Page number (0-based)
     * @param size Page size
     * @param successMessage Message to include in successful response
     * @param errorMessage Message to use in error cases
     * @return PagedResponseDto containing notification results
     */
    private PagedResponseDto<NotificationSms> executeNotificationQuery(
            String tenantId, java.util.function.Supplier<Query> querySupplier,
            int page, int size, String successMessage, String errorMessage) {

        try {
            setupTenantContext(tenantId);

            Query query = prepareQuery(querySupplier.get());
            MongoTemplate mongoTemplate = mongoTemplateProvider.getTemplate(tenantId);

            long totalElements = mongoTemplate.count(query, NotificationSms.class);
            List<NotificationSms> notifications = executeQuery(query, mongoTemplate, page, size);

            log.debug("Found {} missed SMS notifications for tenant: {}", notifications.size(), tenantId);

            return buildPagedResponse(notifications, page, size, totalElements, successMessage);

        } catch (Exception e) {
            log.error("Error retrieving missed SMS notifications for tenant {}: {}", tenantId, e.getMessage());
            throw new RuntimeException(errorMessage, e);
        } finally {
            clearTenantContext();
        }
    }

    /**
     * Sets up tenant context for multi-tenant operations.
     *
     * @param tenantId Tenant identifier
     */
    private void setupTenantContext(String tenantId) {
        TenantContextHolder.setTenantId(tenantId);
    }

    /**
     * Clears tenant context after operations.
     */
    private void clearTenantContext() {
        TenantContextHolder.clear();
    }

    /**
     * Prepares query by adding sorting criteria.
     *
     * @param query Base query to prepare
     * @return Query with sorting applied
     */
    private Query prepareQuery(Query query) {
        return query.with(Sort.by(Sort.Direction.DESC, "createdAt"));
    }

    /**
     * Executes the query with pagination.
     *
     * @param query Query to execute
     * @param mongoTemplate MongoDB template for execution
     * @param page Page number
     * @param size Page size
     * @return List of notification results
     */
    private List<NotificationSms> executeQuery(Query query, MongoTemplate mongoTemplate, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        query.with(pageable);
        return mongoTemplate.find(query, NotificationSms.class);
    }

    /**
     * Builds paginated response with calculated pagination info.
     *
     * @param notifications List of notifications
     * @param page Current page number
     * @param size Page size
     * @param totalElements Total number of elements
     * @param message Success message
     * @return PagedResponseDto with complete pagination info
     */
    private PagedResponseDto<NotificationSms> buildPagedResponse(
            List<NotificationSms> notifications, int page, int size, long totalElements, String message) {

        PagedResponseDto.PaginationInfo paginationInfo = calculatePaginationInfo(page, size, totalElements);

        return PagedResponseDto.<NotificationSms>builder()
                .data(notifications)
                .pagination(paginationInfo)
                .build();
    }

    /**
     * Calculates pagination information.
     *
     * @param page Current page number
     * @param size Page size
     * @param totalElements Total number of elements
     * @return PaginationInfo with calculated values
     */
    private PagedResponseDto.PaginationInfo calculatePaginationInfo(int page, int size, long totalElements) {
        int totalPages = (int) Math.ceil((double) totalElements / size);
        boolean hasNext = page < totalPages - 1;
        boolean hasPrevious = page > 0;

        return PagedResponseDto.PaginationInfo.builder()
                .page(page)
                .pageSize(size)
                .totalItems(totalElements)
                .totalPages(totalPages)
                .hasNext(hasNext)
                .hasPrevious(hasPrevious)
                .build();
    }
}