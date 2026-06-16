package com.jio.digigov.notification.dto.response.event;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
@Schema(description = "Data processor response")
public class DataProcessorResponseDto {
    
    @Schema(description = "Data processor ID", example = "a1b2c3d4-e5f6-7890-1234-567890abcdef")
    private String dataProcessorId;

    @Schema(description = "Data processor name", example = "AWS")
    private String dataProcessorName;

    @Schema(description = "Callback URL", example = "https://randomurl.com/cb1")
    private String callbackUrl;

    @Schema(description = "Data processor details", example = "Cloud Hosting Provider for infrastructure services.")
    private String details;

    @Schema(description = "Attachment reference")
    private String attachment;

    @Schema(description = "Attachment metadata")
    private Object attachmentMeta;

    @Schema(description = "Vendor risk document reference")
    private String vendorRiskDocument;

    @Schema(description = "Vendor risk document metadata")
    private Object vendorRiskDocumentMeta;

    @Schema(description = "Business ID", example = "fedf8ad1-dd12-4227-b49d-2cbad75b24d0")
    private String businessId;

    @Schema(description = "Scope type", example = "TENANT")
    private String scopeType;

    @Schema(description = "Status", example = "ACTIVE")
    private String status;
    
    @Schema(description = "Creation timestamp")
    private LocalDateTime createdAt;

    @Schema(description = "Last update timestamp")
    private LocalDateTime updatedAt;
}