package com.jio.digigov.notification.service;

import com.jio.digigov.notification.dto.request.BulkCallbackRequestDto;
import com.jio.digigov.notification.dto.request.BulkNotificationRequestDto;
import com.jio.digigov.notification.dto.response.MissedNotificationResponseDto;
import com.jio.digigov.notification.dto.response.common.CountResponseDto;
import com.jio.digigov.notification.dto.response.common.PagedResponseDto;
import com.jio.digigov.notification.entity.notification.NotificationEmail;
import com.jio.digigov.notification.entity.notification.NotificationSms;
import com.jio.digigov.notification.entity.notification.NotificationCallback;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Service for retrieving missed notifications (FAILED and RETRY_SCHEDULED).
 *
 * This service provides business logic for Data Fiduciary and Data Processor systems
 * to query failed and scheduled notifications across all channels. It supports
 * multi-tenant operations with proper data isolation and filtering capabilities.
 *
 * Key Features:
 * - Retrieve missed notifications by channel (SMS, Email, Callback)
 * - Support for pagination and date range filtering
 * - Count aggregations for monitoring and alerting
 * - Multi-tenant data isolation
 * - Status filtering (FAILED and RETRY_SCHEDULED)
 *
 * @author Notification Service Team
 * @version 1.0
 * @since 2024-01-01
 */
public interface MissedNotificationService {

    /**
     * Get missed SMS notifications (FAILED and RETRY_SCHEDULED).
     *
     * @param tenantId Tenant identifier
     * @param businessId Business identifier within the tenant
     * @param page Page number (0-based)
     * @param size Page size
     * @param fromDate Start date for filtering (optional)
     * @param toDate End date for filtering (optional)
     * @return Paged response containing missed SMS notifications
     */
    PagedResponseDto<NotificationSms> getMissedSmsNotifications(
            String tenantId,
            String businessId,
            int page,
            int size,
            LocalDateTime fromDate,
            LocalDateTime toDate);

    /**
     * Get missed Email notifications (FAILED and RETRY_SCHEDULED).
     *
     * @param tenantId Tenant identifier
     * @param businessId Business identifier within the tenant
     * @param page Page number (0-based)
     * @param size Page size
     * @param fromDate Start date for filtering (optional)
     * @param toDate End date for filtering (optional)
     * @return Paged response containing missed Email notifications
     */
    PagedResponseDto<NotificationEmail> getMissedEmailNotifications(
            String tenantId,
            String businessId,
            int page,
            int size,
            LocalDateTime fromDate,
            LocalDateTime toDate);

    /**
     * Get missed Callback notifications (FAILED and RETRY_SCHEDULED).
     *
     * @param tenantId Tenant identifier
     * @param businessId Business identifier within the tenant
     * @param page Page number (0-based)
     * @param size Page size
     * @param fromDate Start date for filtering (optional)
     * @param toDate End date for filtering (optional)
     * @return Paged response containing missed Callback notifications
     */
    PagedResponseDto<NotificationCallback> getMissedCallbackNotifications(
            String tenantId,
            String businessId,
            int page,
            int size,
            LocalDateTime fromDate,
            LocalDateTime toDate);

    /**
     * Get count of missed notifications across all channels.
     *
     * @param tenantId Tenant identifier
     * @param businessId Business identifier within the tenant
     * @param fromDate Start date for filtering (optional)
     * @param toDate End date for filtering (optional)
     * @return Count response with breakdown by channel and status
     */
    CountResponseDto getMissedNotificationsCount(
            String tenantId,
            String businessId,
            LocalDateTime fromDate,
            LocalDateTime toDate);

    /**
     * Get count of missed callback notifications with filters.
     *
     * @param tenantId Tenant identifier
     * @param businessId Business identifier within the tenant
     * @param recipientType Filter by recipient type (optional)
     * @param recipientId Filter by recipient ID (optional)
     * @param status Filter by status (optional)
     * @param fromDate Start date for filtering (optional)
     * @param toDate End date for filtering (optional)
     * @return Count response with breakdown by filters
     */
    CountResponseDto getMissedCallbackNotificationsCount(
            String tenantId,
            String businessId,
            String recipientType,
            String recipientId,
            String status,
            LocalDateTime fromDate,
            LocalDateTime toDate);

    /**
     * Get list of missed callback notification IDs only with filters.
     *
     * @param tenantId Tenant identifier
     * @param businessId Business identifier within the tenant
     * @param page Page number (0-based)
     * @param size Page size
     * @param recipientType Filter by recipient type (optional)
     * @param recipientId Filter by recipient ID (optional)
     * @param status Filter by status (optional)
     * @param fromDate Start date for filtering (optional)
     * @param toDate End date for filtering (optional)
     * @return Paged response containing notification IDs only
     */
    PagedResponseDto<String> listMissedCallbackNotificationIds(
            String tenantId,
            String businessId,
            int page,
            int size,
            String recipientType,
            String recipientId,
            String status,
            LocalDateTime fromDate,
            LocalDateTime toDate);

    /**
     * Get single missed callback notification by ID.
     *
     * @param tenantId Tenant identifier
     * @param businessId Business identifier within the tenant
     * @param notificationId The notification ID to retrieve
     * @return The complete notification payload
     */
    NotificationCallback getMissedCallbackNotificationById(
            String tenantId,
            String businessId,
            String notificationId);

    /**
     * Get multiple missed callback notifications with filters (bulk fetch).
     *
     * @param tenantId Tenant identifier
     * @param businessId Business identifier within the tenant
     * @param bulkRequest Request containing filters and pagination
     * @return Paged response containing full notification payloads
     */
    PagedResponseDto<NotificationCallback> getMissedCallbackNotificationsBulk(
            String tenantId,
            String businessId,
            BulkCallbackRequestDto bulkRequest);

    /**
     * Mark a single callback notification as retrieved.
     *
     * @param tenantId Tenant identifier
     * @param notificationId The notification ID to mark as retrieved
     */
    void markCallbackNotificationAsRetrieved(String tenantId, String notificationId);

    /**
     * Mark multiple callback notifications as retrieved.
     *
     * @param tenantId Tenant identifier
     * @param notificationIds List of notification IDs to mark as retrieved
     */
    void markCallbackNotificationsAsRetrieved(String tenantId, List<String> notificationIds);

    // ========== ENHANCED CALLBACK METHODS ==========

    /**
     * Get count of missed callback notifications with enhanced filtering.
     *
     * Only includes notifications with status: FAILED, PROCESSING, RETRY_SCHEDULED
     * Excludes: SUCCESSFUL, ACKNOWLEDGED, PENDING
     *
     * @param tenantId Tenant identifier
     * @param businessId Business identifier within the tenant
     * @param recipientType Filter by recipient type (optional)
     * @param recipientId Filter by recipient ID (optional)
     * @param status Filter by status (optional, must be one of: FAILED, PROCESSING, RETRY_SCHEDULED)
     * @param eventType Filter by event type (optional)
     * @param priority Filter by priority (optional)
     * @param fromDate Start date for filtering (optional)
     * @param toDate End date for filtering (optional)
     * @return Count response with breakdown by status
     */
    CountResponseDto getMissedCallbackNotificationsCountEnhanced(
            String tenantId,
            String businessId,
            String recipientType,
            String recipientId,
            String status,
            String eventType,
            String priority,
            LocalDateTime fromDate,
            LocalDateTime toDate);

    /**
     * Get list of missed callback notification IDs with enhanced filtering.
     *
     * Only includes notifications with status: FAILED, PROCESSING, RETRY_SCHEDULED
     * Excludes: SUCCESSFUL, ACKNOWLEDGED, PENDING
     *
     * @param tenantId Tenant identifier
     * @param businessId Business identifier within the tenant
     * @param request Bulk request containing filters and pagination
     * @return Paged response containing notification IDs with minimal metadata
     */
    PagedResponseDto<MissedNotificationResponseDto> listMissedCallbackNotificationIds(
            String tenantId,
            String businessId,
            BulkNotificationRequestDto request);

    /**
     * Get single missed callback notification by ID.
     *
     * Only includes notifications with status: FAILED, PROCESSING, RETRY_SCHEDULED
     *
     * @param tenantId Tenant identifier
     * @param businessId Business identifier within the tenant
     * @param notificationId The notification ID to retrieve
     * @return The complete notification in DTO format
     */
    MissedNotificationResponseDto getMissedCallbackNotificationByIdEnhanced(
            String tenantId,
            String businessId,
            String notificationId);

    /**
     * Get multiple missed callback notifications with enhanced filtering (bulk fetch).
     *
     * Only includes notifications with status: FAILED, PROCESSING, RETRY_SCHEDULED
     * Excludes: SUCCESSFUL, ACKNOWLEDGED, PENDING
     *
     * @param tenantId Tenant identifier
     * @param businessId Business identifier within the tenant
     * @param request Bulk request containing filters and pagination
     * @return Paged response containing full notification details
     */
    PagedResponseDto<MissedNotificationResponseDto> getMissedCallbackNotificationsBulkEnhanced(
            String tenantId,
            String businessId,
            BulkNotificationRequestDto request);
}