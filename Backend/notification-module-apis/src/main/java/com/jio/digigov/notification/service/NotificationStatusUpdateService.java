package com.jio.digigov.notification.service;

import com.jio.digigov.notification.dto.request.BulkUpdateNotificationStatusRequestDto;
import com.jio.digigov.notification.dto.request.UpdateNotificationStatusRequestDto;
import com.jio.digigov.notification.dto.response.BulkNotificationStatusUpdateResponseDto;
import com.jio.digigov.notification.dto.response.NotificationStatusUpdateResponseDto;
import com.jio.digigov.notification.dto.response.common.StandardApiResponseDto;

/**
 * Service interface for notification status update operations.
 *
 * <p>This service provides methods for DF/DP systems to update the status of
 * callback notifications after they have retrieved them. It includes validation
 * for authorization and status transition rules.</p>
 *
 * <p><b>Key Features:</b></p>
 * <ul>
 *   <li>Single and bulk status update operations</li>
 *   <li>Status transition validation (e.g., only RETRIEVED → ACKNOWLEDGED)</li>
 *   <li>Authorization validation (recipientId + recipientType)</li>
 *   <li>Status history tracking for complete audit trail</li>
 *   <li>Multi-tenant support via tenant-specific databases</li>
 * </ul>
 *
 * <p><b>Valid Status Transitions:</b></p>
 * <ul>
 *   <li>RETRIEVED → ACKNOWLEDGED ✅</li>
 *   <li>ACKNOWLEDGED → ACKNOWLEDGED ✅ (allows re-updates)</li>
 *   <li>FAILED → ACKNOWLEDGED ❌</li>
 *   <li>RETRY_SCHEDULED → ACKNOWLEDGED ❌</li>
 * </ul>
 *
 * @see com.jio.digigov.notification.controller.v1.MissedNotificationController
 * @see com.jio.digigov.notification.repository.notification.NotificationCallbackRepository
 * @author Notification Service Team
 * @version 1.0
 * @since 2025-01-20
 */
public interface NotificationStatusUpdateService {

    /**
     * Updates the status of a single notification.
     *
     * <p>This method validates:</p>
     * <ul>
     *   <li>Notification exists</li>
     *   <li>Requester is authorized (recipientId + recipientType match)</li>
     *   <li>Status transition is valid</li>
     * </ul>
     *
     * <p>If all validations pass, the notification's status is updated and a
     * status history entry is added with the acknowledgementId and remark.</p>
     *
     * @param tenantId The tenant ID
     * @param businessId The business ID
     * @param notificationId The notification ID to update
     * @param recipientType The recipient type (DATA_FIDUCIARY or DATA_PROCESSOR)
     * @param recipientId The recipient ID
     * @param request The status update request containing status, acknowledgementId, and remark
     * @return StandardApiResponseDto containing the update result
     * @throws com.jio.digigov.notification.exception.UnauthorizedNotificationAccessException
     *         if the notification doesn't belong to the requester
     * @throws com.jio.digigov.notification.exception.InvalidStatusTransitionException
     *         if the status transition is not allowed
     * @throws com.jio.digigov.notification.exception.BusinessException
     *         if the notification is not found
     */
    StandardApiResponseDto<NotificationStatusUpdateResponseDto> updateNotificationStatus(
            String tenantId,
            String businessId,
            String notificationId,
            String recipientType,
            String recipientId,
            UpdateNotificationStatusRequestDto request
    );

    /**
     * Updates the status of multiple notifications in a single operation.
     *
     * <p>This method processes each notification ID individually and returns
     * detailed results showing which updates succeeded and which failed with
     * specific error messages.</p>
     *
     * <p>The operation is partially atomic - valid notifications will be updated
     * even if some fail validation. This allows bulk operations to complete
     * successfully for valid items while reporting failures.</p>
     *
     * @param tenantId The tenant ID
     * @param businessId The business ID
     * @param recipientType The recipient type (DATA_FIDUCIARY or DATA_PROCESSOR)
     * @param recipientId The recipient ID
     * @param request The bulk update request containing list of notification IDs and status details
     * @return StandardApiResponseDto containing successful and failed updates
     */
    StandardApiResponseDto<BulkNotificationStatusUpdateResponseDto> bulkUpdateNotificationStatus(
            String tenantId,
            String businessId,
            String recipientType,
            String recipientId,
            BulkUpdateNotificationStatusRequestDto request
    );
}
