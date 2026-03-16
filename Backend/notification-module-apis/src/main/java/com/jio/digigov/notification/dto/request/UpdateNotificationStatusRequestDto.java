package com.jio.digigov.notification.dto.request;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.jio.digigov.notification.enums.UpdateNotificationStatusEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for updating a single notification's status.
 *
 * <p>This DTO is used when DF/DP systems want to update the status of a notification
 * after they have retrieved it. The most common use case is marking a RETRIEVED
 * notification as ACKNOWLEDGED after processing.</p>
 *
 * <p><b>Usage Example:</b></p>
 * <pre>
 * PUT /v1/missed-notifications/{notificationId}/status
 * {
 *   "acknowledgementId": "ACK_XYZ_123",
 *   "remark": "Successfully processed and stored in our system",
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
 * <p><b>Status Validation Rules:</b></p>
 * <ul>
 *   <li>ACKNOWLEDGED: Only from RETRIEVED or ACKNOWLEDGED status</li>
 *   <li>DELETED, PROCESSING, PROCESSED: Allowed from any status</li>
 * </ul>
 *
 * <p><b>Note:</b> Status values are case-insensitive and will be normalized
 * to uppercase automatically by the system.</p>
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
public class UpdateNotificationStatusRequestDto {

    /**
     * Optional acknowledgement ID provided by the DF/DP system.
     * This can be their internal reference ID for correlation purposes.
     *
     * Example: "ACK_INTERNAL_12345", "REF_2025_001"
     *
     * Optional field.
     */
    private String acknowledgementId;

    /**
     * Optional remark or comment about this status update.
     * Can be used to provide additional context about the acknowledgement.
     * Maximum length: 500 characters.
     *
     * Example: "Successfully processed and stored in our system"
     *
     * Optional field.
     */
    @Size(max = 500, message = "Remark cannot exceed 500 characters")
    private String remark;

    /**
     * The new status to update the notification to.
     * Only specific values are allowed: ACKNOWLEDGED, DELETED, PROCESSING, PROCESSED
     *
     * Required field.
     */
    @NotNull(message = "Status is required")
    private UpdateNotificationStatusEnum status;
}
