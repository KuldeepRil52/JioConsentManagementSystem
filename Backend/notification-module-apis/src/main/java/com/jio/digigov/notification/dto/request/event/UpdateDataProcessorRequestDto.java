package com.jio.digigov.notification.dto.request.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = "Request to update a data processor")
public class UpdateDataProcessorRequestDto {

    @Schema(description = "Data processor name", example = "AWS")
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
    private String scopeType;

    @Schema(description = "Data processor status", example = "ACTIVE", allowableValues = {"ACTIVE", "INACTIVE", "SUSPENDED"})
    private String status;
}