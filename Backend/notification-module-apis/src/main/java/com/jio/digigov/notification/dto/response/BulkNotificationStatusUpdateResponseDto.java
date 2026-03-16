package com.jio.digigov.notification.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Response DTO for bulk notification status update operation.
 *
 * <p>This DTO provides detailed feedback about a bulk status update operation,
 * including which notifications were successfully updated and which failed with
 * specific error messages.</p>
 *
 * <p><b>Example Response:</b></p>
 * <pre>
 * {
 *   "success": true,
 *   "message": "Processed 5 notifications: 3 successful, 2 failed",
 *   "data": {
 *     "totalProcessed": 5,
 *     "successful": [
 *       {
 *         "notificationId": "NOTIF_001",
 *         "status": "ACKNOWLEDGED",
 *         "updatedAt": "2025-01-20T10:30:00"
 *       }
 *     ],
 *     "failed": [
 *       {
 *         "notificationId": "NOTIF_002",
 *         "error": "Invalid status transition from FAILED to ACKNOWLEDGED",
 *         "errorCode": "INVALID_STATUS_TRANSITION"
 *       }
 *     ]
 *   }
 * }
 * </pre>
 *
 * @see com.jio.digigov.notification.dto.request.BulkUpdateNotificationStatusRequestDto
 * @author Notification Service Team
 * @version 1.0
 * @since 2025-01-20
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BulkNotificationStatusUpdateResponseDto {

    /**
     * Total number of notifications processed in this bulk operation.
     * Equal to: successful.size() + failed.size()
     */
    private Integer totalProcessed;

    /**
     * List of successfully updated notifications with their details.
     */
    private List<SuccessfulUpdate> successful;

    /**
     * List of failed updates with error details.
     */
    private List<FailedUpdate> failed;

    /**
     * Details of a successfully updated notification.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SuccessfulUpdate {

        /**
         * The notification ID that was successfully updated.
         */
        private String notificationId;

        /**
         * The new status of the notification.
         */
        private String status;

        /**
         * Timestamp when the status was updated.
         */
        private LocalDateTime updatedAt;
    }

    /**
     * Details of a failed notification update.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FailedUpdate {

        /**
         * The notification ID that failed to update.
         */
        private String notificationId;

        /**
         * Detailed error message explaining why the update failed.
         * Examples:
         * - "Notification not found"
         * - "Invalid status transition from FAILED to ACKNOWLEDGED"
         * - "You are not authorized to update this notification"
         */
        private String error;

        /**
         * Machine-readable error code for programmatic handling.
         * Examples:
         * - "NOT_FOUND"
         * - "INVALID_STATUS_TRANSITION"
         * - "UNAUTHORIZED"
         * - "VALIDATION_ERROR"
         */
        private String errorCode;
    }
}
