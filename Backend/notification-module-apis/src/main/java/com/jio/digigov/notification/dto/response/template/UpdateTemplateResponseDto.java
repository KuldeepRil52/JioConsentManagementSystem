package com.jio.digigov.notification.dto.response.template;

import com.jio.digigov.notification.enums.NotificationChannel;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@Schema(description = "Response for template update operation")
public class UpdateTemplateResponseDto {

    @Schema(description = "Old Template ID that was replaced", example = "TEMPL000006296347")
    private String oldTemplateId;

    @Schema(description = "New Template ID created in DigiGov", example = "TEMPL000006296350")
    private String newTemplateId;

    @Schema(description = "Channel type", example = "SMS")
    private NotificationChannel channelType;

    @Schema(description = "Template status", example = "ACTIVE")
    private String status;

    @Schema(description = "Event type", example = "CONSENT_GRANTED")
    private String eventType;

    @Schema(description = "Language", example = "english")
    private String language;

    @Schema(description = "Update timestamp", example = "2024-01-20T10:30:45.123Z")
    private String updatedAt;
}
