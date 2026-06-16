package com.jio.digigov.notification.service.notification.impl;

import com.jio.digigov.notification.dto.request.notification.NotificationFilterRequestDto;
import com.jio.digigov.notification.dto.response.common.PagedResponseDto;
import com.jio.digigov.notification.dto.response.notification.NotificationCountResponseDto;
import com.jio.digigov.notification.dto.response.notification.NotificationResponseDto;
import com.jio.digigov.notification.dto.response.notification.UnifiedNotificationDto;
import com.jio.digigov.notification.enums.RecipientType;
import com.jio.digigov.notification.repository.notification.NotificationRepository;
import com.jio.digigov.notification.service.notification.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Implementation of NotificationService with comprehensive business logic.
 *
 * Handles multi-tenant notification queries with validation, error handling,
 * and proper tenant context management. Provides comprehensive filtering and
 * aggregation capabilities across all notification types.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final MongoTemplate mongoTemplate;

    private static final int MAX_EXPORT_RECORDS = 10000;
    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 100;

    @Override
    public PagedResponseDto<UnifiedNotificationDto> getAllNotifications(String tenantId,
                                                             NotificationFilterRequestDto filterRequest) {
        log.info("Getting flattened notifications for tenant: {}, filter: {}", tenantId, filterRequest);

        validateTenantId(tenantId);
        validateFilterRequest(filterRequest);

        try {
            // OPTIMIZATION: MongoDB now returns flattened, filtered, and paginated notifications directly
            // No need to flatten or re-paginate in Java!
            List<UnifiedNotificationDto> notifications = notificationRepository
                .findFlattenedNotificationsWithFilter(filterRequest, mongoTemplate);

            // Get total count for pagination metadata
            long totalCount = notificationRepository.countFlattenedNotifications(filterRequest, mongoTemplate);

            int page = filterRequest.getPage() != null ? filterRequest.getPage() : 0;
            int size = filterRequest.getSize() != null ? filterRequest.getSize() : DEFAULT_PAGE_SIZE;

            return buildUnifiedPagedResponse(notifications, page, size, totalCount);

        } catch (Exception e) {
            log.error("Error getting notifications for tenant: {}", tenantId);
            throw new RuntimeException("Failed to retrieve notifications", e);
        }
    }

    @Override
    public NotificationCountResponseDto getNotificationCount(String tenantId,
                                                         NotificationFilterRequestDto filterRequest) {
        log.info("Getting notification count for tenant: {}, filter: {}", tenantId, filterRequest);

        validateTenantId(tenantId);
        validateFilterRequest(filterRequest);

        try {
            // MongoTemplate is already tenant-aware through TenantFilter and TenantContextHolder

            return notificationRepository.countNotificationsWithFilter(filterRequest, mongoTemplate);

        } catch (Exception e) {
            log.error("Error getting notification count for tenant: {}", tenantId);
            throw new RuntimeException("Failed to count notifications", e);
        }
    }

    @Override
    public Optional<NotificationResponseDto> getNotificationByEventId(String tenantId, String eventId) {
        log.info("Getting notification by eventId: {} for tenant: {}", eventId, tenantId);

        validateTenantId(tenantId);
        validateEventId(eventId);

        try {
            // MongoTemplate is already tenant-aware through TenantFilter and TenantContextHolder

            return notificationRepository.findNotificationByEventId(eventId, mongoTemplate);

        } catch (Exception e) {
            log.error("Error getting notification by eventId: {} for tenant: {}", eventId, tenantId);
            return Optional.empty();
        }
    }

    @Override
    public PagedResponseDto<NotificationResponseDto> getNotificationsByBusinessId(String tenantId, String businessId,
                                                                      int page, int size) {
        log.info("Getting notifications by businessId: {} for tenant: {}", businessId, tenantId);

        validateTenantId(tenantId);
        validateBusinessId(businessId);
        validatePagination(page, size);

        try {
            // MongoTemplate is already tenant-aware through TenantFilter and TenantContextHolder

            List<NotificationResponseDto> notifications = notificationRepository
                .findNotificationsByBusinessId(businessId, page, size, mongoTemplate);

            long totalCount = notificationRepository.countNotificationsByBusinessId(businessId, mongoTemplate);

            return buildPagedResponse(notifications, page, size, totalCount);

        } catch (Exception e) {
            log.error("Error getting notifications by businessId: {} for tenant: {}", businessId, tenantId);
            throw new RuntimeException("Failed to retrieve notifications by business ID", e);
        }
    }

    @Override
    public PagedResponseDto<NotificationResponseDto> getNotificationsByEventType(String tenantId, String eventType,
                                                                     int page, int size) {
        log.info("Getting notifications by eventType: {} for tenant: {}", eventType, tenantId);

        validateTenantId(tenantId);
        validateEventType(eventType);
        validatePagination(page, size);

        try {
            // MongoTemplate is already tenant-aware through TenantFilter and TenantContextHolder

            List<NotificationResponseDto> notifications = notificationRepository
                .findNotificationsByEventType(eventType, page, size, mongoTemplate);

            long totalCount = notificationRepository.countNotificationsByEventType(eventType, mongoTemplate);

            return buildPagedResponse(notifications, page, size, totalCount);

        } catch (Exception e) {
            log.error("Error getting notifications by eventType: {} for tenant: {}", eventType, tenantId);
            throw new RuntimeException("Failed to retrieve notifications by event type", e);
        }
    }

    @Override
    public PagedResponseDto<NotificationResponseDto> getNotificationsByStatus(String tenantId, String status,
                                                                  int page, int size) {
        log.info("Getting notifications by status: {} for tenant: {}", status, tenantId);

        validateTenantId(tenantId);
        validateStatus(status);
        validatePagination(page, size);

        try {
            // MongoTemplate is already tenant-aware through TenantFilter and TenantContextHolder

            List<NotificationResponseDto> notifications = notificationRepository
                .findNotificationsByStatus(status, page, size, mongoTemplate);

            long totalCount = notificationRepository.countNotificationsByStatus(status, mongoTemplate);

            return buildPagedResponse(notifications, page, size, totalCount);

        } catch (Exception e) {
            log.error("Error getting notifications by status: {} for tenant: {}", status, tenantId);
            throw new RuntimeException("Failed to retrieve notifications by status", e);
        }
    }

    @Override
    public PagedResponseDto<NotificationResponseDto> getRecentNotifications(String tenantId, int limit) {
        log.info("Getting recent notifications for tenant: {}, limit: {}", tenantId, limit);

        validateTenantId(tenantId);
        validateLimit(limit);

        try {
            // MongoTemplate is already tenant-aware through TenantFilter and TenantContextHolder

            List<NotificationResponseDto> notifications = notificationRepository
                .findRecentNotifications(limit, mongoTemplate);

            return buildPagedResponse(notifications, 0, limit, notifications.size());

        } catch (Exception e) {
            log.error("Error getting recent notifications for tenant: {}", tenantId);
            throw new RuntimeException("Failed to retrieve recent notifications", e);
        }
    }

    @Override
    public PagedResponseDto<NotificationResponseDto> getNotificationsWithFailures(String tenantId, int page, int size) {
        log.info("Getting notifications with failures for tenant: {}", tenantId);

        validateTenantId(tenantId);
        validatePagination(page, size);

        try {
            // MongoTemplate is already tenant-aware through TenantFilter and TenantContextHolder

            List<NotificationResponseDto> notifications = notificationRepository
                .findNotificationsWithFailures(page, size, mongoTemplate);

            NotificationFilterRequestDto countFilter = NotificationFilterRequestDto.builder()
                .hasFailedNotifications(true)
                .build();

            long totalCount = getTotalCount(tenantId, countFilter);

            return buildPagedResponse(notifications, page, size, totalCount);

        } catch (Exception e) {
            log.error("Error getting notifications with failures for tenant: {}", tenantId);
            throw new RuntimeException("Failed to retrieve notifications with failures", e);
        }
    }

    @Override
    public PagedResponseDto<NotificationResponseDto> getNotificationsWithPendingDelivery(String tenantId, int page,
                                                                                       int size) {
        log.info("Getting notifications with pending delivery for tenant: {}", tenantId);

        validateTenantId(tenantId);
        validatePagination(page, size);

        try {
            // MongoTemplate is already tenant-aware through TenantFilter and TenantContextHolder

            List<NotificationResponseDto> notifications = notificationRepository
                .findNotificationsWithPendingDelivery(page, size, mongoTemplate);

            NotificationFilterRequestDto countFilter = NotificationFilterRequestDto.builder()
                .hasPendingNotifications(true)
                .build();

            long totalCount = getTotalCount(tenantId, countFilter);

            return buildPagedResponse(notifications, page, size, totalCount);

        } catch (Exception e) {
            log.error("Error getting notifications with pending delivery for tenant: {}", tenantId);
            throw new RuntimeException("Failed to retrieve notifications with pending delivery", e);
        }
    }

    @Override
    public NotificationCountResponseDto getNotificationStatistics(String tenantId) {
        log.info("Getting notification statistics for tenant: {}", tenantId);

        validateTenantId(tenantId);

        try {
            // MongoTemplate is already tenant-aware through TenantFilter and TenantContextHolder

            return notificationRepository.getNotificationStatistics(mongoTemplate);

        } catch (Exception e) {
            log.error("Error getting notification statistics for tenant: {}", tenantId);
            throw new RuntimeException("Failed to retrieve notification statistics", e);
        }
    }

    @Override
    public PagedResponseDto<UnifiedNotificationDto> searchNotificationsByCustomer(String tenantId,
                                                                       String identifierType,
                                                                       String identifierValue,
                                                                       int page, int size) {
        log.info("Searching notifications by customer for tenant: {}, type: {}, value: {}",
                 tenantId, identifierType, identifierValue);

        validateTenantId(tenantId);
        validateCustomerIdentifier(identifierType, identifierValue);
        validatePagination(page, size);

        try {
            NotificationFilterRequestDto filterRequest = NotificationFilterRequestDto.builder()
                .customerIdentifierType(identifierType)
                .customerIdentifierValue(identifierValue)
                .page(page)
                .size(size)
                .build();

            if ("MOBILE".equalsIgnoreCase(identifierType)) {
                filterRequest.setMobile(identifierValue);
            } else if ("EMAIL".equalsIgnoreCase(identifierType)) {
                filterRequest.setEmail(identifierValue);
            }

            return getAllNotifications(tenantId, filterRequest);

        } catch (Exception e) {
            log.error("Error searching notifications by customer for tenant: {}", tenantId);
            throw new RuntimeException("Failed to search notifications by customer", e);
        }
    }

    @Override
    public PagedResponseDto<UnifiedNotificationDto> getNotificationsForDataProcessor(String tenantId,
                                                                          String dataProcessorId,
                                                                          int page, int size) {
        log.info("Getting notifications for Data Processor: {} for tenant: {}", dataProcessorId, tenantId);

        validateTenantId(tenantId);
        validateDataProcessorId(dataProcessorId);
        validatePagination(page, size);

        try {
            NotificationFilterRequestDto filterRequest = NotificationFilterRequestDto.builder()
                .recipientType("DATA_PROCESSOR")
                .recipientId(dataProcessorId)
                .page(page)
                .size(size)
                .build();

            return getAllNotifications(tenantId, filterRequest);

        } catch (Exception e) {
            log.error("Error getting notifications for Data Processor: {} for tenant: {}",
                     dataProcessorId, tenantId, e);
            throw new RuntimeException("Failed to retrieve notifications for Data Processor", e);
        }
    }

    @Override
    public void validateFilterRequest(NotificationFilterRequestDto filterRequest) {
        if (filterRequest == null) {
            return; // Null filter is acceptable for getting all records
        }

        try {
            filterRequest.validate();
        } catch (IllegalArgumentException e) {
            log.warn("Filter validation failed: {}", e.getMessage());
            throw e;
        }

        // Additional business logic validations
        if (filterRequest.getPage() != null && filterRequest.getPage() < 0) {
            throw new IllegalArgumentException("Page number cannot be negative");
        }

        if (filterRequest.getSize() != null
                && (filterRequest.getSize() < 1 || filterRequest.getSize() > MAX_PAGE_SIZE)) {
            throw new IllegalArgumentException("Page size must be between 1 and " + MAX_PAGE_SIZE);
        }
    }

    @Override
    public PagedResponseDto<UnifiedNotificationDto> getNotificationExportData(String tenantId,
                                                                   NotificationFilterRequestDto filterRequest,
                                                                   int maxRecords) {
        log.info("Getting notification export data for tenant: {}, maxRecords: {}", tenantId, maxRecords);

        validateTenantId(tenantId);
        validateFilterRequest(filterRequest);

        if (maxRecords <= 0 || maxRecords > MAX_EXPORT_RECORDS) {
            throw new IllegalArgumentException("Export records must be between 1 and " + MAX_EXPORT_RECORDS);
        }

        try {
            NotificationFilterRequestDto exportFilter = NotificationFilterRequestDto.builder()
                .eventId(filterRequest.getEventId())
                .eventType(filterRequest.getEventType())
                .businessId(filterRequest.getBusinessId())
                .status(filterRequest.getStatus())
                .fromDate(filterRequest.getFromDate())
                .toDate(filterRequest.getToDate())
                .page(0)
                .size(maxRecords)
                .sortBy(filterRequest.getSortBy())
                .sortDirection(filterRequest.getSortDirection())
                .includeNotificationDetails(true)
                .includeEventPayload(false) // Exclude payload for export to reduce size
                .build();

            return getAllNotifications(tenantId, exportFilter);

        } catch (Exception e) {
            log.error("Error getting export data for tenant: {}", tenantId);
            throw new RuntimeException("Failed to retrieve export data", e);
        }
    }

    // Private helper methods

    private void validateTenantId(String tenantId) {
        if (!StringUtils.hasText(tenantId)) {
            throw new IllegalArgumentException("Tenant ID is required");
        }
    }

    private void validateEventId(String eventId) {
        if (!StringUtils.hasText(eventId)) {
            throw new IllegalArgumentException("Event ID is required");
        }
    }

    private void validateBusinessId(String businessId) {
        if (!StringUtils.hasText(businessId)) {
            throw new IllegalArgumentException("Business ID is required");
        }
    }

    private void validateEventType(String eventType) {
        if (!StringUtils.hasText(eventType)) {
            throw new IllegalArgumentException("Event type is required");
        }
    }

    private void validateStatus(String status) {
        if (!StringUtils.hasText(status)) {
            throw new IllegalArgumentException("Status is required");
        }
    }

    private void validateDataProcessorId(String dataProcessorId) {
        if (!StringUtils.hasText(dataProcessorId)) {
            throw new IllegalArgumentException("Data Processor ID is required");
        }
    }

    private void validateCustomerIdentifier(String identifierType, String identifierValue) {
        if (!StringUtils.hasText(identifierType)) {
            throw new IllegalArgumentException("Customer identifier type is required");
        }

        if (!StringUtils.hasText(identifierValue)) {
            throw new IllegalArgumentException("Customer identifier value is required");
        }

        if (!"MOBILE".equalsIgnoreCase(identifierType) && !"EMAIL".equalsIgnoreCase(identifierType)) {
            throw new IllegalArgumentException("Customer identifier type must be MOBILE or EMAIL");
        }
    }

    private void validatePagination(int page, int size) {
        if (page < 0) {
            throw new IllegalArgumentException("Page number cannot be negative");
        }

        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException("Page size must be between 1 and " + MAX_PAGE_SIZE);
        }
    }

    private void validateLimit(int limit) {
        if (limit < 1 || limit > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException("Limit must be between 1 and " + MAX_PAGE_SIZE);
        }
    }

    private long getTotalCount(String tenantId, NotificationFilterRequestDto filterRequest) {
        try {
            NotificationCountResponseDto countResponse = getNotificationCount(tenantId, filterRequest);
            return countResponse.getTotalEvents() != null ? countResponse.getTotalEvents() : 0L;
        } catch (Exception e) {
            log.warn("Failed to get total count, returning 0", e);
            return 0L;
        }
    }

    private PagedResponseDto<NotificationResponseDto> buildPagedResponse(List<NotificationResponseDto> notifications,
                                                             NotificationFilterRequestDto filterRequest,
                                                             long totalCount) {
        return buildPagedResponse(notifications,
                                 filterRequest.getPage() != null ? filterRequest.getPage() : 0,
                                 filterRequest.getSize() != null ? filterRequest.getSize() : DEFAULT_PAGE_SIZE,
                                 totalCount);
    }

    private PagedResponseDto<NotificationResponseDto> buildPagedResponse(List<NotificationResponseDto> notifications,
                                                             int page, int size, long totalCount) {
        PagedResponseDto.PaginationInfo pagination = PagedResponseDto.PaginationInfo.builder()
            .page(page)
            .pageSize(size)
            .totalPages((int) Math.ceil((double) totalCount / size))
            .totalItems(totalCount)
            .hasNext(page < Math.ceil((double) totalCount / size) - 1)
            .hasPrevious(page > 0)
            .build();

        return PagedResponseDto.<NotificationResponseDto>builder()
            .data(notifications)
            .pagination(pagination)
            .build();
    }

    private PagedResponseDto<UnifiedNotificationDto> buildUnifiedPagedResponse(List<UnifiedNotificationDto> notifications,
                                                             int page, int size, long totalCount) {
        PagedResponseDto.PaginationInfo pagination = PagedResponseDto.PaginationInfo.builder()
            .page(page)
            .pageSize(size)
            .totalPages((int) Math.ceil((double) totalCount / size))
            .totalItems(totalCount)
            .hasNext(page < Math.ceil((double) totalCount / size) - 1)
            .hasPrevious(page > 0)
            .build();

        return PagedResponseDto.<UnifiedNotificationDto>builder()
            .data(notifications)
            .pagination(pagination)
            .build();
    }

    private UnifiedNotificationDto mapSmsToUnified(NotificationResponseDto.SmsNotificationInfo sms,
                                                   NotificationResponseDto event) {
        return UnifiedNotificationDto.builder()
            .notificationType("SMS")
            .notificationId(sms.getNotificationId())
            .eventId(event.getEventId())
            .correlationId(sms.getCorrelationId())
            .businessId(event.getBusinessId())
            .templateId(sms.getTemplateId())
            .eventType(event.getEventType())
            .status(sms.getStatus())
            .priority(sms.getPriority())
            .attemptCount(sms.getAttemptCount())
            .createdAt(sms.getCreatedAt())
            .processedAt(sms.getProcessedAt())
            .sentAt(sms.getSentAt())
            .deliveredAt(sms.getDeliveredAt())
            .lastErrorMessage(sms.getLastErrorMessage())
            .recipientType(RecipientType.DATA_PRINCIPAL.name())
            .recipientId(sms.getMobile())
            .mobile(sms.getMobile())
            .build();
    }

    private UnifiedNotificationDto mapEmailToUnified(NotificationResponseDto.EmailNotificationInfo email,
                                                     NotificationResponseDto event) {
        String recipientId = (email.getTo() != null && !email.getTo().isEmpty())
            ? email.getTo().get(0)
            : null;

        return UnifiedNotificationDto.builder()
            .notificationType("EMAIL")
            .notificationId(email.getNotificationId())
            .eventId(event.getEventId())
            .correlationId(email.getCorrelationId())
            .businessId(event.getBusinessId())
            .templateId(email.getTemplateId())
            .eventType(event.getEventType())
            .status(email.getStatus())
            .priority(email.getPriority())
            .attemptCount(email.getAttemptCount())
            .createdAt(email.getCreatedAt())
            .processedAt(email.getProcessedAt())
            .sentAt(email.getSentAt())
            .deliveredAt(email.getDeliveredAt())
            .lastErrorMessage(email.getLastErrorMessage())
            .recipientType(RecipientType.DATA_PRINCIPAL.name())
            .recipientId(recipientId)
            .to(email.getTo())
            .cc(email.getCc())
            .bcc(email.getBcc())
            .subject(email.getSubject())
            .build();
    }

    private UnifiedNotificationDto mapCallbackToUnified(NotificationResponseDto.CallbackNotificationInfo callback,
                                                        NotificationResponseDto event) {
        return UnifiedNotificationDto.builder()
            .notificationType("CALLBACK")
            .notificationId(callback.getNotificationId())
            .eventId(event.getEventId())
            .correlationId(callback.getCorrelationId())
            .businessId(event.getBusinessId())
            .templateId(null) // Callbacks don't have templateId
            .eventType(callback.getEventType())
            .status(callback.getStatus())
            .priority(callback.getPriority())
            .attemptCount(callback.getAttemptCount())
            .createdAt(callback.getCreatedAt())
            .processedAt(callback.getProcessedAt())
            .sentAt(callback.getSentAt())
            .acknowledgedAt(callback.getAcknowledgedAt())
            .lastErrorMessage(callback.getLastErrorMessage())
            .recipientType(callback.getRecipientType())
            .recipientId(callback.getRecipientId())
            .callbackUrl(callback.getCallbackUrl())
            .httpStatusCode(callback.getLastHttpStatusCode())
            .build();
    }
}