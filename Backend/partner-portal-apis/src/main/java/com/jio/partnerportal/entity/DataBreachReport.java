package com.jio.partnerportal.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = false)
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Document("data_breach_reports")
public class DataBreachReport extends AbstractEntity {

    @Id
    @Schema(description = "MongoDB object id")
    private ObjectId _id;

    @Schema(description = "Tenant id from headers", example = "a1b2c3d4-e5f6-7890-1234-567890abcdef")
    private String tenantId;

    @Schema(description = "Unique incident identifier", example = "DB-2025-001")
    private String incidentId;

    private IncidentDetails incidentDetails;
    private DataInvolved dataInvolved;
    private BreachStatus status;
    private List<StatusHistory> history;
    private NotificationDetails notificationDetails;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class IncidentDetails {
        @Schema(description = "Date and time when the breach was discovered", example = "2025-10-30T10:00:00Z")
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS")
        private LocalDateTime discoveryDateTime;

        @Schema(description = "Date and time when the breach occurred", example = "2025-10-29T22:00:00Z")
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS")
        private LocalDateTime occurrenceDateTime;

        @Schema(description = "Type of breach")
        private BreachType breachType;

        @Schema(description = "Brief description of the breach", example = "Email with user data sent to unintended recipient")
        private String briefDescription;

        @Schema(description = "List of affected systems or services", example = "[\"Consent API\", \"Consent API1\"]")
        private List<String> affectedSystemOrService;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class DataInvolved {
        @Schema(description = "Categories of personal data involved", example = "[\"Name\", \"Contact Info\", \"Consent Logs\"]")
        private List<String> personalDataCategories;

        @Schema(description = "Number of affected data principals", example = "500")
        private Integer affectedDataPrincipalsCount;

        @Schema(description = "Whether the data was encrypted or protected", example = "true")
        private Boolean dataEncryptedOrProtected;

        @Schema(description = "Description of potential impact", example = "Minor risk of unauthorized disclosure.")
        private String potentialImpactDescription;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class StatusHistory {
        @Schema(description = "Previous status")
        private BreachStatus previousStatus;

        @Schema(description = "New status")
        private BreachStatus newStatus;

        @Schema(description = "Date and time when status was updated", example = "2025-10-30T11:33:35.661Z")
        private LocalDateTime updatedAt;

        @Schema(description = "Remarks for the status change", example = "Forwarded to municipal officer")
        private String remarks;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class NotificationDetails {
        @Schema(description = "Date when DPBI was notified", example = "2025-10-31T08:00:00Z")
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS")
        private LocalDateTime dpbiNotificationDate;

        @Schema(description = "DPBI acknowledgement ID", example = "DPBI-ACK-2025-0987")
        private String dpbiAcknowledgementId;

        @Schema(description = "Date when data principals were notified", example = "2025-10-31T12:00:00Z")
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS")
        private LocalDateTime dataPrincipalNotificationDate;

        @Schema(description = "Notification channels")
        private List<NotificationChannel> channels;

        @Schema(description = "Notification Status", example = "DISPATCHED")
        private String status;

        @Schema(description = "Notify Group ID", example = "1c3f4f62-2122-4cfd-b5a1-4dcbd0bf9afc")
        private String notifyGroupId;

        @Schema(description = "Date when Updated", example = "2025-10-31T08:00:00Z")
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS")
        private LocalDateTime updatedAt;

        @Getter
        @Setter
        @NoArgsConstructor
        @AllArgsConstructor
        @Builder
        @JsonInclude(JsonInclude.Include.NON_NULL)
        public static class NotificationChannel {
            @Schema(description = "Notification channel", example = "EMAIL")
            private NotificationChannelType notificationChannel;

            @Schema(description = "Notification status", example = "SENT")
            private NotificationStatus notificationStatus;

            @Schema(description = "Count of notifications sent", example = "450")
            private Integer count;
        }
    }

    public enum BreachType {
        UNAUTHORIZED_ACCESS,
        DATA_LEAK,
        DATA_LOSS,
        MISUSE
    }

    public enum BreachStatus {
        NEW,
        INVESTIGATION,
        NOTIFIED_TO_DATA_PRINCIPALS,
        NOTIFIED_TO_DPBI,
        RESOLVED
    }

    public enum NotificationChannelType {
        EMAIL,
        SMS
    }

    public enum NotificationStatus {
        SENT,
        IN_PROGRESS,
        DISPATCHED,
        PENDING,
        FAILED,
        COMPLETED
    }
}
