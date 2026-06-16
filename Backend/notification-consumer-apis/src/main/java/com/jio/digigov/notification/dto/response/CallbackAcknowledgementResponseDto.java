package com.jio.digigov.notification.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * DTO for parsing callback acknowledgement responses from DF/DP systems.
 *
 * <p>When the callback consumer sends a notification to a DF/DP callback URL,
 * the recipient is expected to respond with this structure to acknowledge
 * successful receipt and processing.</p>
 *
 * <p><b>Expected Response Format:</b></p>
 * <pre>
 * HTTP 200 OK
 * {
 *   "status": "ACKNOWLEDGED",
 *   "acknowledgementId": "ACK_XYZ_123",
 *   "remark": "Successfully processed and stored"
 * }
 * </pre>
 *
 * <p><b>Validation Rules:</b></p>
 * <ul>
 *   <li>Status field must equal "ACKNOWLEDGED" (case-insensitive)</li>
 *   <li>acknowledgementId is optional - can be used for correlation</li>
 *   <li>remark is optional - can provide additional context</li>
 *   <li>If status is not "ACKNOWLEDGED", the notification will be marked as FAILED</li>
 * </ul>
 *
 * <p><b>Backwards Compatibility:</b></p>
 * <ul>
 *   <li>Extra fields in the response are ignored (via @JsonIgnoreProperties)</li>
 *   <li>If status is "SUCCESS" or "ACCEPTED", a warning is logged but processed as acknowledgement</li>
 * </ul>
 *
 * @see com.jio.digigov.notification.service.kafka.impl.CallbackNotificationConsumer
 * @author Notification Service Team
 * @version 1.0
 * @since 2025-01-20
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class CallbackAcknowledgementResponseDto {

    /**
     * Status of the callback processing on the recipient's end.
     * Expected value: "ACKNOWLEDGED" (case-insensitive)
     *
     * This is the primary field used to determine if the callback was successful.
     * If this field is missing or has a value other than "ACKNOWLEDGED",
     * the notification will be marked as FAILED with an appropriate error message.
     *
     * Required field in the response.
     */
    private String status;

    /**
     * Optional acknowledgement ID from the recipient's system.
     * This can be their internal reference/transaction ID for correlation purposes.
     *
     * Examples:
     * - "ACK_INTERNAL_12345"
     * - "REF_2025_001"
     * - "TXN_DF_20250120_001"
     *
     * This value is stored in the statusHistory for audit trail.
     *
     * Optional field.
     */
    private String acknowledgementId;

    /**
     * Optional remark or comment from the recipient.
     * Can be used to provide additional context about the processing status.
     *
     * Examples:
     * - "Successfully processed and stored in our system"
     * - "Queued for async processing"
     * - "Acknowledged and forwarded to internal workflow"
     *
     * Maximum recommended length: 500 characters.
     *
     * Optional field.
     */
    private String remark;
}
