package com.jio.digigov.notification.dto.response.notification;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Response DTO for consent deletion details API.
 * Provides detailed information about a specific consent deletion request.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Detailed consent deletion information for a specific event")
public class ConsentDeletionDetailsResponseDto {

    @Schema(description = "Consent information section")
    private ConsentInformation consentInformation;

    @Schema(description = "Data Fiduciary deletion status and details from withdrawal_data")
    private Map<String, Object> dataFiduciary;

    @Schema(description = "List of Data Processor deletion statuses")
    private List<DataProcessorInfo> dataProcessors;

    @Schema(description = "PII items from withdrawal_data")
    private List<Object> piiItems;

    @Schema(description = "Notification timeline showing chronological order of events")
    private List<TimelineEntry> notificationTimeline;

    /**
     * Consent information section containing basic consent details.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Schema(description = "Basic consent information")
    public static class ConsentInformation {

        @Schema(description = "The consent ID", example = "d00c2ab7-cd2d-435c-bf68-549dad0b674c")
        private String consentId;

        @Schema(description = "Trigger type: Withdrawal or Expiry", example = "Withdrawal")
        private String trigger;

        @Schema(description = "Template name from consent record", example = "Marketing v2.1")
        private String template;

        @Schema(description = "Timestamp from consent record")
        private LocalDateTime timestamp;

        @Schema(description = "Data retention policy from retention_config")
        private RetentionPolicy retentionPolicy;
    }

    /**
     * Retention policy specifying duration with value and unit.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Schema(description = "Retention policy configuration")
    public static class RetentionPolicy {

        @Schema(description = "Retention duration value", example = "1")
        private Integer value;

        @Schema(description = "Retention duration unit", example = "years")
        private String unit;
    }

    /**
     * Data Processor information with deletion status.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Schema(description = "Data Processor deletion status information")
    public static class DataProcessorInfo {

        @Schema(description = "Data Processor ID", example = "5dcd33a8-c554-4bc2-9f48-2e419f381751")
        private String processorId;

        @Schema(description = "Data Processor name", example = "AWS")
        private String processorName;

        @Schema(description = "Current status of the callback", example = "DEFERRED")
        private String status;

        @Schema(description = "Reason/remark from status history", example = "Regulatory Retention (90 days)")
        private String reason;

        @Schema(description = "Date from status history timestamp")
        private LocalDateTime reviewDate;
    }

    /**
     * Timeline entry representing a single event in the notification timeline.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Schema(description = "Timeline entry for notification events")
    public static class TimelineEntry {

        @Schema(description = "Event label displayed in timeline",
                example = "Consent Withdrawn")
        private String label;

        @Schema(description = "Timestamp when this event occurred")
        private LocalDateTime timestamp;

        @Schema(description = "Order in the timeline for sorting", example = "1")
        private Integer order;
    }
}
