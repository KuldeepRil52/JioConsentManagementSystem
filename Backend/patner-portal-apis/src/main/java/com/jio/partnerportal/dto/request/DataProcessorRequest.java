package com.jio.partnerportal.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.jio.partnerportal.dto.DocumentMeta;
import com.jio.partnerportal.dto.IdentityType;
import com.jio.partnerportal.dto.DataProcessorSpocDto;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DataProcessorRequest {

    @Schema(description = "Name of the data processor", example = "Processor X")
    @JsonProperty("dataProcessorName")
    private String dataProcessorName;
    @Schema(description = "Callback URL for the data processor", example = "https://callback.example.com/dp")
    @JsonProperty("callbackUrl")
    private String callbackUrl;
    @Schema(description = "Details about the data processor", example = "This processor handles sensitive data.")
    @JsonProperty("details")
    private String details;
    @Schema(description = "Attachment related to the data processor", example = "attachment_url_here")
    @JsonProperty("attachment")
    private String attachment;
    @Schema(description = "Content type of the attachment")
    @JsonProperty("attachmentMeta")
    private DocumentMeta attachmentMeta;
    @Schema(description = "Vendor risk assessment details", example = "High risk vendor")
    @JsonProperty("vendorRiskDocument")
    private String vendorRiskDocument;
    @Schema(description = "Content type of the vendor risk document")
    @JsonProperty("vendorRiskDocumentMeta")
    private DocumentMeta vendorRiskDocumentMeta;
    @Schema(description = "Single Point of Contact details for the data processor")
    @JsonProperty("spoc")
    private DataProcessorSpocDto spoc;
    @Schema(description = "Identity type for notifications", example = "EMAIL", allowableValues = {"EMAIL", "MOBILE"})
    @JsonProperty("identityType")
    private IdentityType identityType;
    @Schema(description = "Indicates if the data processor is cross-bordered", example = "false")
    @JsonProperty("isCrossBordered")
    private Boolean isCrossBordered;
    @Schema(description = "Certificate details (base64 encoded)", example = "-----BEGIN CERTIFICATE-----\n...\n-----END CERTIFICATE-----")
    @JsonProperty("certificate")
    private String certificate;
    @Schema(description = "Metadata for Certificate document")
    @JsonProperty("certificateMeta")
    private DocumentMeta certificateMeta;
}
