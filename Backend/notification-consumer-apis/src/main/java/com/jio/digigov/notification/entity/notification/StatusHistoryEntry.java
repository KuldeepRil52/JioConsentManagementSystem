package com.jio.digigov.notification.entity.notification;

import com.jio.digigov.notification.enums.StatusHistorySource;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.mapping.Field;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

/**
 * Embedded document representing a single status update entry in the notification history.
 *
 * <p>This class is used to track the complete audit trail of status changes for callback notifications.
 * Each entry records when a status change occurred, what the new status was, who initiated it,
 * and any additional context like acknowledgement IDs and remarks.</p>
 *
 * <p><b>Usage Context:</b></p>
 * <ul>
 *   <li>Stored as an array field (statusHistory) in NotificationCallback entity</li>
 *   <li>Updated when callback responses are received (source: CALLBACK_RESPONSE)</li>
 *   <li>Updated when status update APIs are called (source: API_ENDPOINT)</li>
 * </ul>
 *
 * <p><b>Example Entry:</b></p>
 * <pre>
 * {
 *   "timestamp": "2024-01-20T10:30:00",
 *   "status": "ACKNOWLEDGED",
 *   "acknowledgementId": "ACK_XYZ_123",
 *   "remark": "Processing completed successfully",
 *   "source": "CALLBACK_RESPONSE",
 *   "updatedBy": "DF_001"
 * }
 * </pre>
 *
 * @see NotificationCallback
 * @author Notification Service Team
 * @version 1.0
 * @since 2025-01-20
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StatusHistoryEntry {

    /**
     * Timestamp when this status update occurred.
     * Automatically set when the entry is created.
     */
    @NotNull(message = "Timestamp is required")
    @Field("timestamp")
    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();

    /**
     * The new status value for this update.
     * Common values: ACKNOWLEDGED, PROCESSED, RETRIEVED, etc.
     * Stored in uppercase for consistency.
     */
    @NotBlank(message = "Status is required")
    @Field("status")
    private String status;

    /**
     * Optional acknowledgement ID provided by the recipient system.
     * Typically provided when status is ACKNOWLEDGED.
     * Can be used for correlation with recipient's internal systems.
     */
    @Field("acknowledgementId")
    private String acknowledgementId;

    /**
     * Optional remark or comment about this status update.
     * Maximum length: 500 characters.
     * Useful for providing context about the status change.
     */
    @Size(max = 500, message = "Remark cannot exceed 500 characters")
    @Field("remark")
    private String remark;

    /**
     * Source of this status update.
     * Values:
     * - CALLBACK_RESPONSE: Status updated based on callback response from DF/DP
     * - API_ENDPOINT: Status updated via manual API call
     * - SYSTEM: Status updated by system/automated process
     */
    @NotNull(message = "Source is required")
    @Field("source")
    private StatusHistorySource source;

    /**
     * Identifier of the entity that initiated this update.
     * Typically the recipientId (e.g., "DF_001", "DP_XYZ_456").
     * Used for audit trail and accountability.
     */
    @NotBlank(message = "UpdatedBy is required")
    @Field("updatedBy")
    private String updatedBy;
}
