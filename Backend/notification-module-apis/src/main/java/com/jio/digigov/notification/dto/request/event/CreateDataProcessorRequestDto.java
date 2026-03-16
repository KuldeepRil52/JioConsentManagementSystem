package com.jio.digigov.notification.dto.request.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.jio.digigov.notification.enums.DataProcessorStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.NotBlank;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = "Request to create a data processor")
public class CreateDataProcessorRequestDto {
    
    @Schema(description = "Unique identifier for the data processor (UUID format). If not provided, system will auto-generate one", example = "a1b2c3d4-e5f6-7890-1234-567890abcdef", required = false)
    private String dataProcessorId;

    @NotBlank(message = "Data Processor name is required")
    @Schema(description = "Name of the data processor", example = "AWS", required = true)
    private String dataProcessorName;

    @Schema(description = "Callback URL for webhook notifications", example = "https://randomurl.com/cb1")
    private String callbackUrl;

    @Schema(description = "Details/description of the data processor", example = "Cloud Hosting Provider for infrastructure services.")
    private String details;

    @Schema(description = "Attachment reference string", example = "random_attachment_string_1")
    private String attachment;

    @Schema(description = "Attachment metadata")
    private Object attachmentMeta;

    @Schema(description = "Vendor risk document reference string", example = "random_risk_string_1")
    private String vendorRiskDocument;

    @Schema(description = "Vendor risk document metadata")
    private Object vendorRiskDocumentMeta;

    @Schema(description = "Scope type", example = "TENANT", allowableValues = {"TENANT"})
    private String scopeType = "TENANT";

    @Schema(description = "Data processor status", example = "ACTIVE", allowableValues = {"ACTIVE", "INACTIVE", "SUSPENDED"})
    private String status = DataProcessorStatus.ACTIVE.name();
}