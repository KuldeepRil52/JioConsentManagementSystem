package com.jio.digigov.notification.service.impl;

import com.jio.digigov.notification.dto.request.BulkCallbackRequestDto;
import com.jio.digigov.notification.dto.request.BulkNotificationRequestDto;
import com.jio.digigov.notification.dto.response.MissedNotificationResponseDto;
import com.jio.digigov.notification.dto.response.common.CountResponseDto;
import com.jio.digigov.notification.dto.response.common.PagedResponseDto;
import com.jio.digigov.notification.entity.notification.NotificationCallback;
import com.jio.digigov.notification.entity.notification.NotificationEmail;
import com.jio.digigov.notification.entity.notification.NotificationSms;
import com.jio.digigov.notification.exception.ResourceNotFoundException;
import com.jio.digigov.notification.repository.notification.NotificationCallbackRepository;
import com.jio.digigov.notification.repository.notification.NotificationEmailRepository;
import com.jio.digigov.notification.repository.notification.NotificationSmsRepository;
import com.jio.digigov.notification.service.MissedNotificationService;
import com.jio.digigov.notification.util.MongoTemplateProvider;
import com.jio.digigov.notification.util.TenantContextHolder;
import com.jio.digigov.notification.enums.NotificationStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Implementation of MissedNotificationService.
 *
 * Provides business logic for retrieving missed notifications (FAILED and RETRY_SCHEDULED)
 * across all channels with support for multi-tenant operations, pagination, and filtering.
 *
 * Key Features:
 * - Multi-tenant data access through MongoTemplateProvider
 * - Pagination support for large datasets
 * - Date range filtering for time-based queries
 * - Channel-specific query optimization
 * - Count aggregations with detailed breakdown
 * - Proper error handling and logging
 *
 * Status Filtering:
 * - FAILED: Notifications that permanently failed after exhausting retry attempts
 * - RETRY_SCHEDULED: Notifications that failed but are scheduled for retry
 *
 * @author Notification Service Team
 * @version 1.0
 * @since 2024-01-01
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class MissedNotificationServiceImpl implements MissedNotificationService {

    private final NotificationSmsRepository smsRepository;
    private final NotificationEmailRepository emailRepository;
    private final NotificationCallbackRepository callbackRepository;
    private final MongoTemplateProvider mongoTemplateProvider;

    private static final List<String> MISSED_STATUSES = List.of(
            NotificationStatus.FAILED.name(),
            NotificationStatus.RETRY_SCHEDULED.name()
    );

    @Override
    public PagedResponseDto<NotificationSms> getMissedSmsNotifications(
            String tenantId,
            String businessId,
            int page,
            int size,
            LocalDateTime fromDate,
            LocalDateTime toDate) {
        return getMissedNotificationsGeneric(tenantId, businessId, page, size,
                fromDate, toDate, NotificationSms.class, "SMS");
    }

    @Override
    public PagedResponseDto<NotificationEmail> getMissedEmailNotifications(
            String tenantId,
            String businessId,
            int page,
            int size,
            LocalDateTime fromDate,
            LocalDateTime toDate) {
        return getMissedNotificationsGeneric(tenantId, businessId, page, size,
                fromDate, toDate, NotificationEmail.class, "Email");
    }

    @Override
    public PagedResponseDto<NotificationCallback> getMissedCallbackNotifications(
            String tenantId,
            String businessId,
            int page,
            int size,
            LocalDateTime fromDate,
            LocalDateTime toDate) {
        return getMissedNotificationsGeneric(tenantId, businessId, page, size,
                fromDate, toDate, NotificationCallback.class, "Callback");
    }

    @Override
    public CountResponseDto getMissedNotificationsCount(
            String tenantId,
            String businessId,
            LocalDateTime fromDate,
            LocalDateTime toDate) {

        log.debug("Fetching missed notifications count: tenantId={}, businessId={}, fromDate={}, toDate={}",
                tenantId, businessId, fromDate, toDate);

        // Set tenant context
        TenantContextHolder.setTenantId(tenantId);

        // Get tenant-specific MongoTemplate
        MongoTemplate mongoTemplate = mongoTemplateProvider.getTemplate(tenantId);

        // Build base query
        Query baseQuery = buildMissedNotificationsQuery(businessId, fromDate, toDate);

        // Get counts by channel
        Map<String, Integer> channelCounts = new HashMap<>();
        channelCounts.put("SMS", (int) mongoTemplate.count(baseQuery, NotificationSms.class));
        channelCounts.put("EMAIL", (int) mongoTemplate.count(baseQuery, NotificationEmail.class));
        channelCounts.put("CALLBACK", (int) mongoTemplate.count(baseQuery, NotificationCallback.class));

        // Get counts by status
        Map<String, Integer> statusCounts = new HashMap<>();
        for (String status : MISSED_STATUSES) {
            Query statusQuery = buildMissedNotificationsQuery(businessId, fromDate, toDate, status);
            long smsCount = mongoTemplate.count(statusQuery, NotificationSms.class);
            long emailCount = mongoTemplate.count(statusQuery, NotificationEmail.class);
            long callbackCount = mongoTemplate.count(statusQuery, NotificationCallback.class);
            statusCounts.put(status, (int) (smsCount + emailCount + callbackCount));
        }

        // Calculate total count
        long totalCount = channelCounts.values().stream().mapToInt(Integer::intValue).sum();

        log.info("Retrieved missed notifications count: tenantId={}, businessId={}, totalCount={}, breakdown={}",
                tenantId, businessId, totalCount, channelCounts);

        return CountResponseDto.builder()

                .data(CountResponseDto.CountData.builder()
                        .totalCount(totalCount)
                        .breakdown(CountResponseDto.CountData.CountBreakdown.builder()
                                .byChannel(channelCounts)
                                .byStatus(statusCounts)
                                .build())
                        .build())

                .build();
    }

    /**
     * Builds MongoDB query for missed notifications based on business ID and date range.
     */
    private Query buildMissedNotificationsQuery(String businessId, LocalDateTime fromDate, LocalDateTime toDate) {
        return buildMissedNotificationsQuery(businessId, fromDate, toDate, null);
    }

    /**
     * Builds MongoDB query for missed notifications with optional status filter.
     */
    private Query buildMissedNotificationsQuery(String businessId, LocalDateTime fromDate, LocalDateTime toDate, String status) {
        Query query = new Query();

        // Filter by business ID
        if (businessId != null) {
            query.addCriteria(Criteria.where("businessId").is(businessId));
        }

        // Filter by missed statuses (FAILED and RETRY_SCHEDULED)
        if (status != null) {
            query.addCriteria(Criteria.where("status").is(status));
        } else {
            query.addCriteria(Criteria.where("status").in(MISSED_STATUSES));
        }

        // Add date range filter if provided
        if (fromDate != null && toDate != null) {
            query.addCriteria(Criteria.where("createdAt").gte(fromDate).lte(toDate));
        } else if (fromDate != null) {
            query.addCriteria(Criteria.where("createdAt").gte(fromDate));
        } else if (toDate != null) {
            query.addCriteria(Criteria.where("createdAt").lte(toDate));
        }

        return query;
    }

    /**
     * Generic method for retrieving missed notifications of any type.
     * Eliminates code duplication across SMS, Email, and Callback notification methods.
     *
     * @param tenantId The tenant identifier
     * @param businessId The business identifier
     * @param page Page number (0-based)
     * @param size Page size
     * @param fromDate Start date for filtering (optional)
     * @param toDate End date for filtering (optional)
     * @param entityClass The notification entity class (NotificationSms, NotificationEmail, NotificationCallback)
     * @param notificationType Type name for logging (SMS, Email, Callback)
     * @return PagedResponseDto containing the requested notifications
     */
    private <T> PagedResponseDto<T> getMissedNotificationsGeneric(
            String tenantId,
            String businessId,
            int page,
            int size,
            LocalDateTime fromDate,
            LocalDateTime toDate,
            Class<T> entityClass,
            String notificationType) {

        log.debug("Fetching missed {} notifications: tenantId={}, businessId={}, page={}, size={}, fromDate={}, toDate={}",
                notificationType, tenantId, businessId, page, size, fromDate, toDate);

        // Set tenant context
        TenantContextHolder.setTenantId(tenantId);

        // Get tenant-specific MongoTemplate
        MongoTemplate mongoTemplate = mongoTemplateProvider.getTemplate(tenantId);

        // Build query with criteria
        Query query = buildMissedNotificationsQuery(businessId, fromDate, toDate);

        // Add pagination and sorting
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        query.with(pageable);

        // Execute query
        List<T> notifications = mongoTemplate.find(query, entityClass);

        // Get total count for pagination
        Query countQuery = buildMissedNotificationsQuery(businessId, fromDate, toDate);
        long totalCount = mongoTemplate.count(countQuery, entityClass);

        // Build pagination info
        PagedResponseDto.PaginationInfo paginationInfo = buildPaginationInfo(page, size, totalCount);

        log.info("Retrieved {} missed {} notifications out of {} total for tenantId={}, businessId={}",
                notifications.size(), notificationType, totalCount, tenantId, businessId);

        return PagedResponseDto.<T>builder()
                .data(notifications)
                .pagination(paginationInfo)
                .build();
    }

    /**
     * Builds pagination information for paged responses.
     */
    private PagedResponseDto.PaginationInfo buildPaginationInfo(int page, int size, long totalCount) {
        int totalPages = (int) Math.ceil((double) totalCount / size);
        boolean hasNext = page < totalPages - 1;
        boolean hasPrevious = page > 0;

        return PagedResponseDto.PaginationInfo.builder()
                .page(page)
                .pageSize(size)
                .totalItems(totalCount)
                .totalPages(totalPages)
                .hasNext(hasNext)
                .hasPrevious(hasPrevious)
                .build();
    }


    @Override
    public CountResponseDto getMissedCallbackNotificationsCount(
            String tenantId,
            String businessId,
            String recipientType,
            String recipientId,
            String status,
            LocalDateTime fromDate,
            LocalDateTime toDate) {

        log.debug("Fetching missed callback notifications count: tenantId={}, businessId={}, recipientType={}, recipientId={}, status={}, fromDate={}, toDate={}",
                tenantId, businessId, recipientType, recipientId, status, fromDate, toDate);

        // Set tenant context
        TenantContextHolder.setTenantId(tenantId);

        // Get tenant-specific MongoTemplate
        MongoTemplate mongoTemplate = mongoTemplateProvider.getTemplate(tenantId);

        // Build query with criteria
        Query query = buildCallbackNotificationsQuery(businessId, recipientType, recipientId, status, fromDate, toDate);

        // Get count
        long totalCount = mongoTemplate.count(query, NotificationCallback.class);

        // Get breakdown by status
        Map<String, Integer> statusCounts = new HashMap<>();
        if (status != null) {
            statusCounts.put(status, (int) totalCount);
        } else {
            long failedCount = mongoTemplate.count(
                buildCallbackNotificationsQuery(businessId, recipientType, recipientId, NotificationStatus.FAILED.name(), fromDate, toDate),
                NotificationCallback.class
            );
            long retryScheduledCount = mongoTemplate.count(
                buildCallbackNotificationsQuery(businessId, recipientType, recipientId, NotificationStatus.RETRY_SCHEDULED.name(), fromDate, toDate),
                NotificationCallback.class
            );

            statusCounts.put(NotificationStatus.FAILED.name(), (int) failedCount);
            statusCounts.put(NotificationStatus.RETRY_SCHEDULED.name(), (int) retryScheduledCount);
        }

        // Channel breakdown (only callback)
        Map<String, Integer> channelCounts = new HashMap<>();
        channelCounts.put("CALLBACK", (int) totalCount);

        log.info("Retrieved missed callback notifications count: tenantId={}, businessId={}, totalCount={}",
                tenantId, businessId, totalCount);

        return CountResponseDto.builder()

                .data(CountResponseDto.CountData.builder()
                        .totalCount(totalCount)
                        .breakdown(CountResponseDto.CountData.CountBreakdown.builder()
                                .byChannel(channelCounts)
                                .byStatus(statusCounts)
                                .build())
                        .build())

                .build();
    }

    @Override
    public PagedResponseDto<String> listMissedCallbackNotificationIds(
            String tenantId,
            String businessId,
            int page,
            int size,
            String recipientType,
            String recipientId,
            String status,
            LocalDateTime fromDate,
            LocalDateTime toDate) {

        log.debug("Listing missed callback notification IDs: tenantId={}, businessId={}, page={}, size={}, recipientType={}, recipientId={}, status={}, fromDate={}, toDate={}",
                tenantId, businessId, page, size, recipientType, recipientId, status, fromDate, toDate);

        // Set tenant context
        TenantContextHolder.setTenantId(tenantId);

        // Get tenant-specific MongoTemplate
        MongoTemplate mongoTemplate = mongoTemplateProvider.getTemplate(tenantId);

        // Build query with criteria
        Query query = buildCallbackNotificationsQuery(businessId, recipientType, recipientId, status, fromDate, toDate);

        // Only fetch the notificationId field
        query.fields().include("notificationId");

        // Add pagination and sorting
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        query.with(pageable);

        // Execute query
        List<NotificationCallback> notifications = mongoTemplate.find(query, NotificationCallback.class);

        // Extract notification IDs
        List<String> notificationIds = notifications.stream()
                .map(NotificationCallback::getNotificationId)
                .toList();

        // Get total count for pagination
        Query countQuery = buildCallbackNotificationsQuery(businessId, recipientType, recipientId, status, fromDate, toDate);
        long totalCount = mongoTemplate.count(countQuery, NotificationCallback.class);

        // Calculate pagination info
        int totalPages = (int) Math.ceil((double) totalCount / size);
        boolean hasNext = page < totalPages - 1;
        boolean hasPrevious = page > 0;

        log.info("Listed {} missed callback notification IDs out of {} total for tenantId={}, businessId={}",
                notificationIds.size(), totalCount, tenantId, businessId);

        return PagedResponseDto.<String>builder()
                .data(notificationIds)
                .pagination(PagedResponseDto.PaginationInfo.builder()
                        .page(page)
                        .pageSize(size)
                        .totalItems(totalCount)
                        .totalPages(totalPages)
                        .hasNext(hasNext)
                        .hasPrevious(hasPrevious)
                        .build())
                .build();
    }

    @Override
    public NotificationCallback getMissedCallbackNotificationById(
            String tenantId,
            String businessId,
            String notificationId) {

        log.debug("Fetching missed callback notification by ID: tenantId={}, businessId={}, notificationId={}",
                tenantId, businessId, notificationId);

        // Set tenant context
        TenantContextHolder.setTenantId(tenantId);

        // Get tenant-specific MongoTemplate
        MongoTemplate mongoTemplate = mongoTemplateProvider.getTemplate(tenantId);

        // Build query
        Query query = new Query();
        query.addCriteria(Criteria.where("notificationId").is(notificationId));
        query.addCriteria(Criteria.where("businessId").is(businessId));
        query.addCriteria(Criteria.where("status").in(MISSED_STATUSES));

        // Execute query
        NotificationCallback notification = mongoTemplate.findOne(query, NotificationCallback.class);

        if (notification == null) {
            throw new ResourceNotFoundException(
                    String.format("Missed callback notification not found: notificationId=%s, businessId=%s",
                            notificationId, businessId)
            );
        }

        log.info("Retrieved missed callback notification: tenantId={}, businessId={}, notificationId={}",
                tenantId, businessId, notificationId);

        return notification;
    }

    @Override
    public PagedResponseDto<NotificationCallback> getMissedCallbackNotificationsBulk(
            String tenantId,
            String businessId,
            BulkCallbackRequestDto bulkRequest) {

        log.debug("Fetching bulk missed callback notifications: tenantId={}, businessId={}, request={}",
                tenantId, businessId, bulkRequest);

        // Set tenant context
        TenantContextHolder.setTenantId(tenantId);

        // Get tenant-specific MongoTemplate
        MongoTemplate mongoTemplate = mongoTemplateProvider.getTemplate(tenantId);

        // Build query with all filters
        Query query = new Query();

        // Business ID filter
        query.addCriteria(Criteria.where("businessId").is(businessId));

        // Status filter
        if (StringUtils.hasText(bulkRequest.getStatus())) {
            query.addCriteria(Criteria.where("status").is(bulkRequest.getStatus()));
        } else {
            query.addCriteria(Criteria.where("status").in(MISSED_STATUSES));
        }

        // Recipient type filter
        if (StringUtils.hasText(bulkRequest.getRecipientType())) {
            query.addCriteria(Criteria.where("recipientType").is(bulkRequest.getRecipientType()));
        }

        // Recipient ID filter
        if (StringUtils.hasText(bulkRequest.getRecipientId())) {
            query.addCriteria(Criteria.where("recipientId").is(bulkRequest.getRecipientId()));
        }

        // Notification IDs filter
        if (bulkRequest.getNotificationIds() != null && !bulkRequest.getNotificationIds().isEmpty()) {
            query.addCriteria(Criteria.where("notificationId").in(bulkRequest.getNotificationIds()));
        }

        // Date range filter
        if (bulkRequest.getFromDate() != null && bulkRequest.getToDate() != null) {
            query.addCriteria(Criteria.where("createdAt").gte(bulkRequest.getFromDate()).lte(bulkRequest.getToDate()));
        } else if (bulkRequest.getFromDate() != null) {
            query.addCriteria(Criteria.where("createdAt").gte(bulkRequest.getFromDate()));
        } else if (bulkRequest.getToDate() != null) {
            query.addCriteria(Criteria.where("createdAt").lte(bulkRequest.getToDate()));
        }

        // Add sorting
        Sort.Direction direction = "ASC".equalsIgnoreCase(bulkRequest.getSortDirection())
            ? Sort.Direction.ASC
            : Sort.Direction.DESC;
        Sort sort = Sort.by(direction, bulkRequest.getSortBy());

        // Add pagination
        Pageable pageable = PageRequest.of(bulkRequest.getPage(), bulkRequest.getSize(), sort);
        query.with(pageable);

        // Execute query
        List<NotificationCallback> notifications = mongoTemplate.find(query, NotificationCallback.class);

        // Get total count for pagination
        Query countQuery = Query.of(query).limit(0).skip(0);
        long totalCount = mongoTemplate.count(countQuery, NotificationCallback.class);

        // Calculate pagination info
        int totalPages = (int) Math.ceil((double) totalCount / bulkRequest.getSize());
        boolean hasNext = bulkRequest.getPage() < totalPages - 1;
        boolean hasPrevious = bulkRequest.getPage() > 0;

        log.info("Retrieved {} bulk callback notifications out of {} total for tenantId={}, businessId={}",
                notifications.size(), totalCount, tenantId, businessId);

        return PagedResponseDto.<NotificationCallback>builder()
                .data(notifications)
                .pagination(PagedResponseDto.PaginationInfo.builder()
                        .page(bulkRequest.getPage())
                        .pageSize(bulkRequest.getSize())
                        .totalItems(totalCount)
                        .totalPages(totalPages)
                        .hasNext(hasNext)
                        .hasPrevious(hasPrevious)
                        .build())
                .build();
    }

    @Override
    public void markCallbackNotificationAsRetrieved(String tenantId, String notificationId) {
        log.debug("Marking callback notification as retrieved: tenantId={}, notificationId={}",
                tenantId, notificationId);

        // Set tenant context
        TenantContextHolder.setTenantId(tenantId);

        // Get tenant-specific MongoTemplate
        MongoTemplate mongoTemplate = mongoTemplateProvider.getTemplate(tenantId);

        // Update status to RETRIEVED
        Query query = new Query(Criteria.where("notificationId").is(notificationId));
        Update update = new Update()
                .set("status", "RETRIEVED")
                .set("retrievedAt", LocalDateTime.now());

        mongoTemplate.updateFirst(query, update, NotificationCallback.class);

        log.info("Marked callback notification as retrieved: tenantId={}, notificationId={}",
                tenantId, notificationId);
    }

    @Override
    public void markCallbackNotificationsAsRetrieved(String tenantId, List<String> notificationIds) {
        if (notificationIds == null || notificationIds.isEmpty()) {
            return;
        }

        log.debug("Marking {} callback notifications as retrieved: tenantId={}",
                notificationIds.size(), tenantId);

        // Set tenant context
        TenantContextHolder.setTenantId(tenantId);

        // Get tenant-specific MongoTemplate
        MongoTemplate mongoTemplate = mongoTemplateProvider.getTemplate(tenantId);

        // Update status to RETRIEVED for all notifications
        Query query = new Query(Criteria.where("notificationId").in(notificationIds));
        Update update = new Update()
                .set("status", "RETRIEVED")
                .set("retrievedAt", LocalDateTime.now());

        mongoTemplate.updateMulti(query, update, NotificationCallback.class);

        log.info("Marked {} callback notifications as retrieved: tenantId={}",
                notificationIds.size(), tenantId);
    }

    /**
     * Builds MongoDB query for callback notifications with optional filters.
     */
    private Query buildCallbackNotificationsQuery(String businessId, String recipientType, String recipientId,
                                                 String status, LocalDateTime fromDate, LocalDateTime toDate) {
        Query query = new Query();

        // Filter by business ID
        if (businessId != null) {
            query.addCriteria(Criteria.where("businessId").is(businessId));
        }

        // Filter by recipient type
        if (StringUtils.hasText(recipientType)) {
            query.addCriteria(Criteria.where("recipientType").is(recipientType));
        }

        // Filter by recipient ID
        if (StringUtils.hasText(recipientId)) {
            query.addCriteria(Criteria.where("recipientId").is(recipientId));
        }

        // Filter by status
        if (StringUtils.hasText(status)) {
            query.addCriteria(Criteria.where("status").is(status));
        } else {
            // Default to missed statuses (FAILED and RETRY_SCHEDULED)
            query.addCriteria(Criteria.where("status").in(MISSED_STATUSES));
        }

        // Add date range filter if provided
        if (fromDate != null && toDate != null) {
            query.addCriteria(Criteria.where("createdAt").gte(fromDate).lte(toDate));
        } else if (fromDate != null) {
            query.addCriteria(Criteria.where("createdAt").gte(fromDate));
        } else if (toDate != null) {
            query.addCriteria(Criteria.where("createdAt").lte(toDate));
        }

        return query;
    }

    // ========== ENHANCED CALLBACK METHODS IMPLEMENTATION ==========

    private static final List<String> MISSED_STATUSES_EXTENDED = List.of(
            NotificationStatus.FAILED.name(),
            NotificationStatus.PROCESSING.name(),
            NotificationStatus.RETRY_SCHEDULED.name()
    );

    @Override
    public CountResponseDto getMissedCallbackNotificationsCountEnhanced(
            String tenantId,
            String businessId,
            String recipientType,
            String recipientId,
            String status,
            String eventType,
            String priority,
            LocalDateTime fromDate,
            LocalDateTime toDate) {

        log.debug("Fetching enhanced missed callback notifications count: tenantId={}, businessId={}, recipientType={}, recipientId={}, status={}, eventType={}, priority={}, fromDate={}, toDate={}",
                tenantId, businessId, recipientType, recipientId, status, eventType, priority, fromDate, toDate);

        TenantContextHolder.setTenantId(tenantId);
        MongoTemplate mongoTemplate = mongoTemplateProvider.getTemplate(tenantId);

        Map<String, Integer> byType = new HashMap<>();
        Map<String, Integer> byStatus = new HashMap<>();
        Map<String, Integer> byEventType = new HashMap<>();

        Query query = buildEnhancedCallbackQuery(businessId, recipientType, recipientId, status, eventType, priority, fromDate, toDate);
        long totalCount = mongoTemplate.count(query, NotificationCallback.class);

        byType.put("CALLBACK", (int) totalCount);

        if (StringUtils.hasText(status)) {
            byStatus.put(status, (int) totalCount);
        } else {
            for (String statusValue : MISSED_STATUSES_EXTENDED) {
                Query statusQuery = buildEnhancedCallbackQuery(businessId, recipientType, recipientId, statusValue, eventType, priority, fromDate, toDate);
                int statusCount = (int) mongoTemplate.count(statusQuery, NotificationCallback.class);
                if (statusCount > 0) {
                    byStatus.put(statusValue, statusCount);
                }
            }
        }

        return CountResponseDto.builder()
                .data(CountResponseDto.CountData.builder()
                        .totalCount(totalCount)
                        .breakdown(CountResponseDto.CountData.CountBreakdown.builder()
                                .byType(byType)
                                .byStatus(byStatus)
                                .byEventType(byEventType)
                                .build())
                        .build())
                .build();
    }

    @Override
    public PagedResponseDto<MissedNotificationResponseDto> listMissedCallbackNotificationIds(
            String tenantId,
            String businessId,
            BulkNotificationRequestDto request) {

        log.debug("Listing missed callback notification IDs: tenantId={}, businessId={}, request={}",
                tenantId, businessId, request);

        TenantContextHolder.setTenantId(tenantId);
        MongoTemplate mongoTemplate = mongoTemplateProvider.getTemplate(tenantId);

        Query query = buildEnhancedCallbackQueryFromRequest(request);
        List<NotificationCallback> callbackNotifications = mongoTemplate.find(query, NotificationCallback.class);

        List<MissedNotificationResponseDto> allNotifications = callbackNotifications.stream()
                .map(this::convertCallbackToDTO)
                .sorted((a, b) -> {
                    if ("DESC".equalsIgnoreCase(request.getSortDirection())) {
                        return b.getCreatedAt().compareTo(a.getCreatedAt());
                    } else {
                        return a.getCreatedAt().compareTo(b.getCreatedAt());
                    }
                })
                .toList();

        int totalItems = allNotifications.size();
        int fromIndex = request.getPage() * request.getSize();
        int toIndex = Math.min(fromIndex + request.getSize(), totalItems);

        List<MissedNotificationResponseDto> pagedResults = (fromIndex < totalItems)
            ? allNotifications.subList(fromIndex, toIndex)
            : new ArrayList<>();

        pagedResults.forEach(dto -> {
            dto.setCallbackData(Map.of("summary", "ID list view - callback data excluded"));
            dto.setMetadata(Map.of("isIdListView", true));
        });

        int totalPages = (int) Math.ceil((double) totalItems / request.getSize());
        boolean hasNext = request.getPage() < totalPages - 1;
        boolean hasPrevious = request.getPage() > 0;

        return PagedResponseDto.<MissedNotificationResponseDto>builder()

                .data(pagedResults)
                .pagination(PagedResponseDto.PaginationInfo.builder()
                        .page(request.getPage())
                        .pageSize(request.getSize())
                        .totalItems((long) totalItems)
                        .totalPages(totalPages)
                        .hasNext(hasNext)
                        .hasPrevious(hasPrevious)
                        .build())

                .build();
    }

    @Override
    public MissedNotificationResponseDto getMissedCallbackNotificationByIdEnhanced(
            String tenantId,
            String businessId,
            String notificationId) {

        log.debug("Fetching missed callback notification by ID: tenantId={}, businessId={}, notificationId={}",
                tenantId, businessId, notificationId);

        TenantContextHolder.setTenantId(tenantId);
        MongoTemplate mongoTemplate = mongoTemplateProvider.getTemplate(tenantId);

        Query query = new Query(Criteria.where("notificationId").is(notificationId)
                .and("status").in(MISSED_STATUSES_EXTENDED));

        if (StringUtils.hasText(businessId)) {
            query.addCriteria(Criteria.where("businessId").is(businessId));
        }

        NotificationCallback callback = mongoTemplate.findOne(query, NotificationCallback.class);
        if (callback != null) {
            return convertCallbackToDTO(callback);
        }

        throw new ResourceNotFoundException("Missed callback notification not found with ID: " + notificationId);
    }

    @Override
    public PagedResponseDto<MissedNotificationResponseDto> getMissedCallbackNotificationsBulkEnhanced(
            String tenantId,
            String businessId,
            BulkNotificationRequestDto request) {

        log.debug("Fetching bulk missed callback notifications: tenantId={}, businessId={}, request={}",
                tenantId, businessId, request);

        TenantContextHolder.setTenantId(tenantId);
        MongoTemplate mongoTemplate = mongoTemplateProvider.getTemplate(tenantId);

        Query query = buildEnhancedCallbackQueryFromRequest(request);
        List<NotificationCallback> callbackNotifications = mongoTemplate.find(query, NotificationCallback.class);

        List<MissedNotificationResponseDto> allNotifications = callbackNotifications.stream()
                .map(this::convertCallbackToDTO)
                .sorted((a, b) -> {
                    if ("DESC".equalsIgnoreCase(request.getSortDirection())) {
                        return b.getCreatedAt().compareTo(a.getCreatedAt());
                    } else {
                        return a.getCreatedAt().compareTo(b.getCreatedAt());
                    }
                })
                .toList();

        int totalItems = allNotifications.size();
        int fromIndex = request.getPage() * request.getSize();
        int toIndex = Math.min(fromIndex + request.getSize(), totalItems);

        List<MissedNotificationResponseDto> pagedResults = (fromIndex < totalItems)
            ? allNotifications.subList(fromIndex, toIndex)
            : new ArrayList<>();

        // Always mark retrieved notifications as RETRIEVED
        if (!pagedResults.isEmpty()) {
            List<String> callbackIds = pagedResults.stream()
                    .map(MissedNotificationResponseDto::getNotificationId)
                    .toList();
            markCallbackNotificationsAsRetrieved(tenantId, callbackIds);
        }

        int totalPages = (int) Math.ceil((double) totalItems / request.getSize());
        boolean hasNext = request.getPage() < totalPages - 1;
        boolean hasPrevious = request.getPage() > 0;

        return PagedResponseDto.<MissedNotificationResponseDto>builder()

                .data(pagedResults)
                .pagination(PagedResponseDto.PaginationInfo.builder()
                        .page(request.getPage())
                        .pageSize(request.getSize())
                        .totalItems((long) totalItems)
                        .totalPages(totalPages)
                        .hasNext(hasNext)
                        .hasPrevious(hasPrevious)
                        .build())

                .build();
    }

    private Query buildEnhancedCallbackQuery(String businessId, String recipientType, String recipientId,
                                           String status, String eventType, String priority,
                                           LocalDateTime fromDate, LocalDateTime toDate) {
        Query query = new Query();

        if (StringUtils.hasText(businessId)) {
            query.addCriteria(Criteria.where("businessId").is(businessId));
        }

        if (StringUtils.hasText(recipientType)) {
            query.addCriteria(Criteria.where("recipientType").is(recipientType));
        }

        if (StringUtils.hasText(recipientId)) {
            query.addCriteria(Criteria.where("recipientId").is(recipientId));
        }

        if (StringUtils.hasText(eventType)) {
            query.addCriteria(Criteria.where("eventType").is(eventType));
        }

        if (StringUtils.hasText(priority)) {
            query.addCriteria(Criteria.where("priority").is(priority));
        }

        if (StringUtils.hasText(status)) {
            query.addCriteria(Criteria.where("status").is(status));
        } else {
            query.addCriteria(Criteria.where("status").in(MISSED_STATUSES_EXTENDED));
        }

        if (fromDate != null && toDate != null) {
            query.addCriteria(Criteria.where("createdAt").gte(fromDate).lte(toDate));
        } else if (fromDate != null) {
            query.addCriteria(Criteria.where("createdAt").gte(fromDate));
        } else if (toDate != null) {
            query.addCriteria(Criteria.where("createdAt").lte(toDate));
        }

        return query;
    }

    private Query buildEnhancedCallbackQueryFromRequest(BulkNotificationRequestDto request) {
        Query query = buildEnhancedCallbackQuery(
                request.getBusinessId(),
                request.getRecipientType(),
                request.getRecipientId(),
                request.getStatus(),
                request.getEventType(),
                request.getPriority(),
                request.getFromDate(),
                request.getToDate()
        );

        if (request.getNotificationIds() != null && !request.getNotificationIds().isEmpty()) {
            query.addCriteria(Criteria.where("notificationId").in(request.getNotificationIds()));
        }

        // Handle attempt count range criteria - combine min and max into single criteria
        if (request.getMinAttemptCount() != null || request.getMaxAttemptCount() != null) {
            Criteria attemptCriteria = Criteria.where("attemptCount");

            if (request.getMinAttemptCount() != null) {
                attemptCriteria = attemptCriteria.gte(request.getMinAttemptCount());
            }

            if (request.getMaxAttemptCount() != null) {
                attemptCriteria = attemptCriteria.lte(request.getMaxAttemptCount());
            }

            query.addCriteria(attemptCriteria);
        }

        return query;
    }

    private MissedNotificationResponseDto convertCallbackToDTO(NotificationCallback callback) {
        Map<String, Object> callbackData = new HashMap<>();
        callbackData.put("callbackUrl", callback.getCallbackUrl());
        callbackData.put("jwtToken", callback.getJwtToken());
        callbackData.put("jwtExpiresAt", callback.getJwtExpiresAt());
        callbackData.put("timeoutSeconds", callback.getTimeoutSeconds());
        callbackData.put("expectedResponseFormat", callback.getExpectedResponseFormat());
        callbackData.put("customHeaders", callback.getCustomHeaders());
        callbackData.put("eventData", callback.getEventData());

        return MissedNotificationResponseDto.builder()
                .id(callback.getId())
                .notificationId(callback.getNotificationId())
                .type("CALLBACK")
                .status(callback.getStatus())
                .eventId(callback.getEventId())
                .eventType(callback.getEventType())
                .correlationId(callback.getCorrelationId())
                .businessId(callback.getBusinessId())
                .recipientType(callback.getRecipientType())
                .recipientId(callback.getRecipientId())
                .priority(callback.getPriority())
                .attemptCount(callback.getAttemptCount())
                .maxAttempts(callback.getMaxAttempts())
                .createdAt(callback.getCreatedAt())
                .updatedAt(callback.getUpdatedAt())
                .processedAt(callback.getProcessedAt())
                .lastAttemptAt(callback.getLastAttemptAt())
                .nextRetryAt(callback.getNextRetryAt())
                .lastErrorMessage(callback.getLastErrorMessage())
                .lastResponseCode(callback.getLastHttpStatusCode())
                .callbackData(callbackData)
                .build();
    }
}