package com.jio.digigov.notification.service.impl;

import com.jio.digigov.notification.constant.NotificationConstants;
import com.jio.digigov.notification.dto.request.BulkUpdateNotificationStatusRequestDto;
import com.jio.digigov.notification.dto.request.UpdateNotificationStatusRequestDto;
import com.jio.digigov.notification.dto.response.BulkNotificationStatusUpdateResponseDto;
import com.jio.digigov.notification.dto.response.NotificationStatusUpdateResponseDto;
import com.jio.digigov.notification.dto.response.common.StandardApiResponseDto;
import com.jio.digigov.notification.entity.notification.NotificationCallback;
import com.jio.digigov.notification.entity.notification.StatusHistoryEntry;
import com.jio.digigov.notification.enums.JdnmErrorCode;
import com.jio.digigov.notification.enums.StatusHistorySource;
import com.jio.digigov.notification.exception.BusinessException;
import com.jio.digigov.notification.exception.InvalidStatusTransitionException;
import com.jio.digigov.notification.exception.UnauthorizedNotificationAccessException;
import com.jio.digigov.notification.repository.notification.NotificationCallbackRepository;
import com.jio.digigov.notification.service.NotificationStatusUpdateService;
import com.jio.digigov.notification.service.notification.DataDeletionNotificationService;
import com.jio.digigov.notification.util.MongoTemplateProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Implementation of NotificationStatusUpdateService for managing notification status updates.
 *
 * <p>This service handles status updates for callback notifications with comprehensive
 * validation including authorization checks and status transition rules.</p>
 *
 * @author Notification Service Team
 * @version 1.0
 * @since 2025-01-20
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class NotificationStatusUpdateServiceImpl implements NotificationStatusUpdateService {

    private final NotificationCallbackRepository callbackRepository;
    private final MongoTemplateProvider mongoTemplateProvider;
    private final DataDeletionNotificationService dataDeletionNotificationService;

    @Override
    public StandardApiResponseDto<NotificationStatusUpdateResponseDto> updateNotificationStatus(
            String tenantId,
            String businessId,
            String notificationId,
            String recipientType,
            String recipientId,
            UpdateNotificationStatusRequestDto request) {

        log.info("Updating notification status: tenantId={}, businessId={}, notificationId={}, recipientType={}, recipientId={}, status={}",
                tenantId, businessId, notificationId, recipientType, recipientId, request.getStatus());

        try {
            // Get tenant-specific MongoTemplate
            MongoTemplate mongoTemplate = mongoTemplateProvider.getTemplate(tenantId);

            // Find notification and validate authorization
            Optional<NotificationCallback> notificationOpt = callbackRepository.findByIdAndRecipient(
                    notificationId, recipientType, recipientId, mongoTemplate);

            if (notificationOpt.isEmpty()) {
                log.warn("Notification not found or not authorized: notificationId={}, recipientType={}, recipientId={}",
                        notificationId, recipientType, recipientId);
                throw new UnauthorizedNotificationAccessException(notificationId);
            }

            NotificationCallback notification = notificationOpt.get();

            // Get status value from enum
            String newStatus = request.getStatus().getValue();

            // Validate status transition
            validateStatusTransition(notification.getStatus(), newStatus, notificationId);

            // Create status history entry
            StatusHistoryEntry historyEntry = StatusHistoryEntry.builder()
                    .timestamp(LocalDateTime.now())
                    .status(newStatus)
                    .acknowledgementId(request.getAcknowledgementId())
                    .remark(request.getRemark())
                    .source(StatusHistorySource.API_ENDPOINT)
                    .updatedBy(recipientId)
                    .build();

            // Update status with history
            long updated = callbackRepository.updateStatusWithHistory(
                    notificationId, newStatus, historyEntry, mongoTemplate);

            if (updated == 0) {
                log.error("Failed to update notification status: notificationId={}", notificationId);
                throw new BusinessException(JdnmErrorCode.JDNM5001.getCode(), "Failed to update notification status");
            }

            // Trigger DATA_DELETION_NOTIFICATION if applicable (fire-and-forget)
            dataDeletionNotificationService.triggerIfApplicable(notification, newStatus, tenantId, businessId);

            // Build response
            NotificationStatusUpdateResponseDto responseData = NotificationStatusUpdateResponseDto.builder()
                    .notificationId(notificationId)
                    .status(newStatus)
                    .acknowledgementId(request.getAcknowledgementId())
                    .remark(request.getRemark())
                    .updatedAt(LocalDateTime.now())
                    .build();

            log.info("Successfully updated notification status: notificationId={}, status={}", notificationId, newStatus);

            return StandardApiResponseDto.<NotificationStatusUpdateResponseDto>builder()
                    .success(true)
                    .code(JdnmErrorCode.JDNM0000.getCode())
                    .message("Notification status updated successfully")
                    .data(responseData)
                    .timestamp(LocalDateTime.now())
                    .build();

        } catch (UnauthorizedNotificationAccessException | InvalidStatusTransitionException e) {
            // Re-throw validation exceptions
            throw e;
        } catch (Exception e) {
            log.error("Error updating notification status: notificationId={}, error={}",
                    notificationId, e.getMessage(), e);
            throw new BusinessException(JdnmErrorCode.JDNM5001.getCode(), "Failed to update notification status: " + e.getMessage());
        }
    }

    @Override
    public StandardApiResponseDto<BulkNotificationStatusUpdateResponseDto> bulkUpdateNotificationStatus(
            String tenantId,
            String businessId,
            String recipientType,
            String recipientId,
            BulkUpdateNotificationStatusRequestDto request) {

        log.info("Bulk updating notification statuses: tenantId={}, businessId={}, recipientType={}, recipientId={}, count={}, status={}",
                tenantId, businessId, recipientType, recipientId, request.getNotificationIds().size(), request.getStatus());

        List<BulkNotificationStatusUpdateResponseDto.SuccessfulUpdate> successful = new ArrayList<>();
        List<BulkNotificationStatusUpdateResponseDto.FailedUpdate> failed = new ArrayList<>();

        // Get tenant-specific MongoTemplate
        MongoTemplate mongoTemplate = mongoTemplateProvider.getTemplate(tenantId);

        // Get status value from enum
        String newStatus = request.getStatus().getValue();

        // Process each notification ID
        for (String notificationId : request.getNotificationIds()) {
            try {
                // Find notification and validate authorization
                Optional<NotificationCallback> notificationOpt = callbackRepository.findByIdAndRecipient(
                        notificationId, recipientType, recipientId, mongoTemplate);

                if (notificationOpt.isEmpty()) {
                    failed.add(BulkNotificationStatusUpdateResponseDto.FailedUpdate.builder()
                            .notificationId(notificationId)
                            .error("Notification not found or you are not authorized to update it")
                            .errorCode("UNAUTHORIZED")
                            .build());
                    continue;
                }

                NotificationCallback notification = notificationOpt.get();

                // Validate status transition
                if (!isValidTransition(notification.getStatus(), newStatus)) {
                    failed.add(BulkNotificationStatusUpdateResponseDto.FailedUpdate.builder()
                            .notificationId(notificationId)
                            .error(String.format("Invalid status transition from %s to %s",
                                    notification.getStatus(), newStatus))
                            .errorCode("INVALID_STATUS_TRANSITION")
                            .build());
                    continue;
                }

                // Create status history entry
                StatusHistoryEntry historyEntry = StatusHistoryEntry.builder()
                        .timestamp(LocalDateTime.now())
                        .status(newStatus)
                        .acknowledgementId(request.getAcknowledgementId())
                        .remark(request.getRemark())
                        .source(StatusHistorySource.API_ENDPOINT)
                        .updatedBy(recipientId)
                        .build();

                // Update status with history
                long updated = callbackRepository.updateStatusWithHistory(
                        notificationId, newStatus, historyEntry, mongoTemplate);

                if (updated > 0) {
                    // Trigger DATA_DELETION_NOTIFICATION if applicable (fire-and-forget)
                    dataDeletionNotificationService.triggerIfApplicable(notification, newStatus, tenantId, businessId);

                    successful.add(BulkNotificationStatusUpdateResponseDto.SuccessfulUpdate.builder()
                            .notificationId(notificationId)
                            .status(newStatus)
                            .updatedAt(LocalDateTime.now())
                            .build());
                } else {
                    failed.add(BulkNotificationStatusUpdateResponseDto.FailedUpdate.builder()
                            .notificationId(notificationId)
                            .error("Failed to update notification status")
                            .errorCode("UPDATE_FAILED")
                            .build());
                }

            } catch (Exception e) {
                log.error("Error updating notification in bulk operation: notificationId={}, error={}",
                        notificationId, e.getMessage(), e);
                failed.add(BulkNotificationStatusUpdateResponseDto.FailedUpdate.builder()
                        .notificationId(notificationId)
                        .error(e.getMessage())
                        .errorCode("PROCESSING_ERROR")
                        .build());
            }
        }

        // Build response
        BulkNotificationStatusUpdateResponseDto responseData = BulkNotificationStatusUpdateResponseDto.builder()
                .totalProcessed(request.getNotificationIds().size())
                .successful(successful)
                .failed(failed)
                .build();

        String message = String.format("Processed %d notifications: %d successful, %d failed",
                responseData.getTotalProcessed(), successful.size(), failed.size());

        log.info("Bulk update completed: {}", message);

        return StandardApiResponseDto.<BulkNotificationStatusUpdateResponseDto>builder()
                .success(true)
                .code(JdnmErrorCode.JDNM0000.getCode())
                .message(message)
                .data(responseData)
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * Validates if a status transition is allowed.
     *
     * @param currentStatus The current status
     * @param newStatus The new status
     * @param notificationId The notification ID (for logging)
     * @throws InvalidStatusTransitionException if the transition is not allowed
     */
    private void validateStatusTransition(String currentStatus, String newStatus, String notificationId) {
        if (!isValidTransition(currentStatus, newStatus)) {
            log.warn("Invalid status transition attempted: notificationId={}, from={}, to={}",
                    notificationId, currentStatus, newStatus);
            throw new InvalidStatusTransitionException(currentStatus, newStatus);
        }
    }

    /**
     * Checks if a status transition is valid.
     *
     * Valid transitions:
     * - RETRIEVED → ACKNOWLEDGED ✅
     * - ACKNOWLEDGED → ACKNOWLEDGED ✅ (allows re-updates)
     * - Any status → DELETED ✅
     * - Any status → PROCESSING ✅
     * - Any status → PROCESSED ✅
     *
     * @param currentStatus The current status
     * @param newStatus The new status
     * @return true if the transition is valid, false otherwise
     */
    private boolean isValidTransition(String currentStatus, String newStatus) {
        // For ACKNOWLEDGED status, only allow from RETRIEVED or ACKNOWLEDGED
        if (NotificationConstants.STATUS_ACKNOWLEDGED.equals(newStatus)) {
            return NotificationConstants.STATUS_RETRIEVED.equals(currentStatus)
                || NotificationConstants.STATUS_ACKNOWLEDGED.equals(currentStatus);
        }

        // For other statuses (DELETED, PROCESSING, PROCESSED), allow any transition
        return true;
    }
}
