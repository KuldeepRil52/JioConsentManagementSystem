package com.jio.digigov.notification.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Response DTO for single notification status update operation.
 *
 * <p>This DTO contains the details of a successfully updated notification,
 * including the notification ID, new status, acknowledgement details, and timestamp.</p>
 *
 * <p><b>Example Response:</b></p>
 * <pre>
 * {
 *   "success": true,
 *   "message": "Notification status updated successfully",
 *   "data": {
 *     "notificationId": "NOTIF_20250120_001",
 *     "status": "ACKNOWLEDGED",
 *     "acknowledgementId": "ACK_XYZ_123",
 *     "remark": "Successfully processed",
 *     "updatedAt": "2025-01-20T10:30:00"
 *   },
 *   "timestamp": "2025-01-20T10:30:00"
 * }
 * </pre>
 *
 * @see com.jio.digigov.notification.dto.request.UpdateNotificationStatusRequestDto
 * @author Notification Service Team
 * @version 1.0
 * @since 2025-01-20
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationStatusUpdateResponseDto {

    /**
     * The notification ID that was updated.
     */
    private String notificationId;

    /**
     * The new status of the notification.
     * Example: "ACKNOWLEDGED"
     */
    private String status;

    /**
     * The acknowledgement ID provided in the update request (if any).
     * Null if not provided.
     */
    private String acknowledgementId;

    /**
     * The remark provided in the update request (if any).
     * Null if not provided.
     */
    private String remark;

    /**
     * Timestamp when the status was updated.
     */
    private LocalDateTime updatedAt;
}
