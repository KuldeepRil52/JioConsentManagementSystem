package com.jio.digigov.notification.dto.response.event;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.jio.digigov.notification.dto.response.notification.NotificationDetailsResponseDto;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@Builder
@Schema(description = "Notification event response")
public class NotificationEventResponseDto {

    @Schema(description = "Event ID", example = "EVT_20240120_001")
    private String eventId;

    @Schema(description = "Business ID", example = "BUS_001")
    private String businessId;

    @Schema(description = "Event type", example = "CONSENT_GRANTED")
    private String eventType;

    @Schema(description = "Resource type", example = "consent")
    private String resource;

    @Schema(description = "Source system", example = "consent-app")
    private String source;

    @Schema(description = "Resolved language", example = "en")
    private String language;

    @Schema(description = "Customer identifiers")
    private CustomerIdentifiersDto customerIdentifiers;

    @Schema(description = "Data processor IDs")
    private List<String> dataProcessorIds;

    @Schema(description = "Original event payload")
    private Map<String, Object> eventPayload;

    @Schema(description = "Detailed notification information (SMS, Email, Callback)")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private NotificationDetailsResponseDto notifications;

    @Schema(description = "Event processing status", example = "COMPLETED")
    private String status;

    @Schema(description = "Creation timestamp")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;

    @Schema(description = "Last update timestamp")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime updatedAt;

    @Data
    @Builder
    @Schema(description = "Customer identifiers")
    public static class CustomerIdentifiersDto {

        @Schema(description = "Type of identifier", example = "MOBILE")
        private String type;

        @Schema(description = "Identifier value", example = "919867123456")
        private String value;
    }
}