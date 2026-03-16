package com.jio.partnerportal.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.jio.partnerportal.entity.DataBreachReport;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Response body for Data Breach Report")
public class DataBreachReportResponse {

    @Schema(description = "Document id")
    @JsonProperty("id")
    private String id;

    @Schema(description = "Tenant id")
    @JsonProperty("tenantId")
    private String tenantId;

    @Schema(description = "Unique incident identifier", example = "DB-2025-001")
    @JsonProperty("incidentId")
    private String incidentId;

    @JsonProperty("incidentDetails")
    private IncidentDetails incidentDetails;

    @JsonProperty("dataInvolved")
    private DataInvolved dataInvolved;

    @JsonProperty("status")
    private DataBreachReport.BreachStatus status;

    @JsonProperty("history")
    private List<StatusHistory> history;

    @JsonProperty("notificationDetails")
    private NotificationDetails notificationDetails;

    @JsonProperty("createdAt")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS")
    private LocalDateTime createdAt;

    @JsonProperty("updatedAt")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS")
    private LocalDateTime updatedAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class IncidentDetails {
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
        private LocalDateTime discoveryDateTime;
        
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
        private LocalDateTime occurrenceDateTime;
        
        private DataBreachReport.BreachType breachType;
        private String briefDescription;
        private List<String> affectedSystemOrService;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DataInvolved {
        private List<String> personalDataCategories;
        private Integer affectedDataPrincipalsCount;
        private Boolean dataEncryptedOrProtected;
        private String potentialImpactDescription;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StatusHistory {
        private DataBreachReport.BreachStatus previousStatus;
        private DataBreachReport.BreachStatus newStatus;
        
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS")
        private LocalDateTime updatedAt;
        
        private String remarks;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class NotificationDetails {
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
        private LocalDateTime dpbiNotificationDate;
        
        private String dpbiAcknowledgementId;
        
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
        private LocalDateTime dataPrincipalNotificationDate;
        
        private List<NotificationChannel> channels;

        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class NotificationChannel {
            private DataBreachReport.NotificationChannelType notificationChannel;
            private DataBreachReport.NotificationStatus notificationStatus;
            private Integer count;
        }
    }
}
