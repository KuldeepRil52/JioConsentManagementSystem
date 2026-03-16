package com.jio.partnerportal.entity;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.jio.partnerportal.client.notification.request.TriggerEventRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Document stored in tenant DB collection "data_breach_notify"
 *
 * Keeps an audit of which consents were identified for an incident and the customer identifiers for each consent.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Document(collection = "data_breach_notify")
public class DataBreachNotify {

    @Id
    @JsonSerialize(using = ToStringSerializer.class)
    private ObjectId id;

    // unique notify id (UUID)
    private String notifyId;

     // unique notify group id (UUID)
    private String notifyGroupId;

    private String tenantId;

    private String incidentId;

    /**
     * consent impacted and their customer identifiers.
     * customerIdentifiers is a list of maps: { "type": "EMAIL"|"MOBILE", "value": "..." }
     */
    private ConsentEntry consent;

    private String status; // PENDING, SENT, FAILED

    @Schema(description = "Record creation time", example = "2025-10-31T10:19:37.955+00:00")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS")
    private LocalDateTime createdAt;

    @Schema(description = "Record notification time", example = "2025-10-31T10:19:37.955+00:00")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS")
    private LocalDateTime notifiedAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ConsentEntry {
        private String consentId;
        private String businessId;
        private TriggerEventRequest.CustomerIdentifiers customerIdentifiers;
        private DataBreachReport.NotificationStatus status;
        private String failureReason;

        @Schema(description = "Record notification time", example = "2025-10-31T10:19:37.955+00:00")
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS")
        private LocalDateTime notifiedAt;
    }
}