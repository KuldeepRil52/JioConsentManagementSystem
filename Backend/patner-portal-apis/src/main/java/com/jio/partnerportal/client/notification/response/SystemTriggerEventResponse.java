package com.jio.partnerportal.client.notification.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = "Response from notification service system event trigger")
public class SystemTriggerEventResponse {

    @Schema(description = "Success status", example = "true")
    @JsonProperty("success")
    private Boolean success;

    @Schema(description = "Response code", example = "JDNM0000")
    @JsonProperty("code")
    private String code;

    @Schema(description = "Response message", example = "Event accepted for asynchronous processing")
    @JsonProperty("message")
    private String message;

    @Schema(description = "Event data")
    @JsonProperty("data")
    private EventData data;

    @Schema(description = "Timestamp", example = "2025-11-17T12:42:09.874Z")
    @JsonProperty("timestamp")
    private String timestamp;

    @Schema(description = "API path", example = "/notification/v1/events/trigger")
    @JsonProperty("path")
    private String path;

    @Schema(description = "Error flag", example = "false")
    @JsonProperty("error")
    private Boolean error;

    @Schema(description = "Transaction ID", example = "ABCD1234567890EF1234567890")
    @JsonProperty("transactionId")
    private String transactionId;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
    @Schema(description = "Event data")
    public static class EventData {

        @Schema(description = "Event ID", example = "EVT_20251117_11939")
        @JsonProperty("eventId")
        private String eventId;

        @Schema(description = "Transaction ID", example = "ABCD1234567890EF1234567890")
        @JsonProperty("transactionId")
        private String transactionId;

        @Schema(description = "Status", example = "ACCEPTED")
        @JsonProperty("status")
        private String status;

        @Schema(description = "Message", example = "Event accepted for asynchronous processing")
        @JsonProperty("message")
        private String message;

        @Schema(description = "Queued at timestamp", example = "2025-11-17T12:42:09.8740692")
        @JsonProperty("queuedAt")
        private String queuedAt;

        @Schema(description = "Event metadata")
        @JsonProperty("eventMetadata")
        private EventMetadata eventMetadata;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
    @Schema(description = "Event metadata")
    public static class EventMetadata {

        @Schema(description = "Event type", example = "INIT_OTP")
        @JsonProperty("eventType")
        private String eventType;

        @Schema(description = "Data processor count", example = "1")
        @JsonProperty("dataProcessorCount")
        private Integer dataProcessorCount;

        @Schema(description = "Customer identifier type", example = "MOBILE")
        @JsonProperty("customerIdentifierType")
        private String customerIdentifierType;
    }

    /**
     * Helper method to get event ID from nested data structure
     */
    public String getEventId() {
        return data != null ? data.getEventId() : null;
    }

    /**
     * Helper method to get status from nested data structure
     */
    public String getStatus() {
        return data != null ? data.getStatus() : null;
    }

    /**
     * Helper method to get transaction ID from nested data structure (prefers data.transactionId)
     */
    public String getTransactionId() {
        if (data != null && data.getTransactionId() != null) {
            return data.getTransactionId();
        }
        return transactionId;
    }

    /**
     * Helper method to get queued at timestamp from nested data structure
     */
    public String getQueuedAt() {
        return data != null ? data.getQueuedAt() : null;
    }

    /**
     * Helper method to get event metadata from nested data structure
     */
    public EventMetadata getEventMetadata() {
        return data != null ? data.getEventMetadata() : null;
    }

    /**
     * Helper method to get event type from nested event metadata
     */
    public String getEventType() {
        return data != null && data.getEventMetadata() != null 
                ? data.getEventMetadata().getEventType() : null;
    }

    /**
     * Helper method to get customer identifier type from nested event metadata
     */
    public String getCustomerIdentifierType() {
        return data != null && data.getEventMetadata() != null 
                ? data.getEventMetadata().getCustomerIdentifierType() : null;
    }

    /**
     * Helper method to get data processor count from nested event metadata
     */
    public Integer getDataProcessorCount() {
        return data != null && data.getEventMetadata() != null 
                ? data.getEventMetadata().getDataProcessorCount() : null;
    }

    /**
     * Helper method to check if the response indicates success
     */
    public boolean isSuccessful() {
        return Boolean.TRUE.equals(success) && Boolean.FALSE.equals(error);
    }
}

