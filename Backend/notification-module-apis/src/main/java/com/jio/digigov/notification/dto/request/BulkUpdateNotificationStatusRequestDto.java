package com.jio.digigov.notification.dto.request;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.jio.digigov.notification.enums.UpdateNotificationStatusEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * Request DTO for bulk updating multiple notifications' status in a single request.
 *
 * <p>This DTO allows DF/DP systems to update the status of multiple notifications
 * at once with a single API call. All notifications will be updated with the same
 * status, acknowledgementId, and remark.</p>
 *
 * <p><b>Usage Example:</b></p>
 * <pre>
 * POST /v1/missed-notifications/status
 * {
 *   "acknowledgementId": "ACK_BULK_001",
 *   "notificationIds": ["NOTIF_001", "NOTIF_002", "NOTIF_003"],
 *   "remark": "Bulk processing completed successfully",
 *   "status": "ACKNOWLEDGED"
 * }
 * </pre>
 *
 * <p><b>Allowed Status Values:</b></p>
 * <ul>
 *   <li>ACKNOWLEDGED - Notification acknowledged by recipient</li>
 *   <li>DELETED - Notification deleted by recipient</li>
 *   <li>PROCESSING - Notification is being processed</li>
 *   <li>PROCESSED - Notification has been processed</li>
 * </ul>
 *
 * <p><b>Response Includes:</b></p>
 * <ul>
 *   <li>List of successfully updated notification IDs</li>
 *   <li>List of failed updates with error details</li>
 *   <li>Total count of processed notifications</li>
 * </ul>
 *
 * <p><b>Note:</b> This operation is partially atomic - if some notifications fail
 * validation or status transition rules, they will be reported in the failed list,
 * while valid notifications will still be updated successfully. Status values are
 * case-insensitive and will be normalized to uppercase automatically by the system.</p>
 *
 * @see com.jio.digigov.notification.service.NotificationStatusUpdateService
 * @see com.jio.digigov.notification.enums.UpdateNotificationStatusEnum
 * @author Notification Service Team
 * @version 1.0
 * @since 2025-01-20
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonPropertyOrder(alphabetic = true)
public class BulkUpdateNotificationStatusRequestDto {

    /**
     * Optional single acknowledgement ID to apply to all updated notifications.
     * This can be a bulk processing reference ID.
     *
     * Example: "ACK_BULK_2025_001"
     *
     * Optional field.
     */
    private String acknowledgementId;

    /**
     * List of notification IDs to update.
     * All IDs must belong to the requester (validated via recipientId and recipientType headers).
     *
     * Required field. Must contain at least one notification ID.
     */
    @NotEmpty(message = "Notification IDs are required")
    private List<String> notificationIds;

    /**
     * Optional remark to apply to all updated notifications.
     * Maximum length: 500 characters.
     *
     * Example: "Bulk processing completed successfully at 2025-01-20 10:30:00"
     *
     * Optional field.
     */
    @Size(max = 500, message = "Remark cannot exceed 500 characters")
    private String remark;

    /**
     * The new status to update all notifications to.
     * Only specific values are allowed: ACKNOWLEDGED, DELETED, PROCESSING, PROCESSED
     *
     * Required field.
     */
    @NotNull(message = "Status is required")
    private UpdateNotificationStatusEnum status;
}
