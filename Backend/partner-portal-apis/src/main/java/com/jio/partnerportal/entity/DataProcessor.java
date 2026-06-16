package com.jio.partnerportal.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.jio.partnerportal.dto.DocumentMeta;
import com.jio.partnerportal.dto.IdentityType;
import com.jio.partnerportal.dto.DataProcessorSpocDto;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@EqualsAndHashCode(callSuper = true)
@Document(collection = "data_processors")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class DataProcessor extends AbstractEntity{

    @Id
    @JsonProperty("id")
    private ObjectId id;
    @Schema(description = "Unique identifier for the data processor", example = "a1b2c3d4-e5f6-7890-1234-567890abcdef")
    @JsonProperty("dataProcessorId")
    private String dataProcessorId;
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
    @Schema(description = "Content type of the attachment", example = "application/pdf")
    @JsonProperty("attachmentMeta")
    private DocumentMeta attachmentMeta;
    @Schema(description = "Vendor risk assessment details", example = "High risk vendor")
    @JsonProperty("vendorRiskDocument")
    private String vendorRiskDocument;
    @Schema(description = "Content type of the vendor risk document", example = "application/json")
    @JsonProperty("vendorRiskDocumentMeta")
    private DocumentMeta vendorRiskDocumentMeta;
    @Schema(description = "ID of the business associated with the data processor", example = "yourBusinessId")
    @JsonProperty("businessId")
    private String businessId;
    @Schema(description = "Scope type of the data processor", example = "GLOBAL", allowableValues = {"GLOBAL", "BUSINESS", "INDIVIDUAL"})
    @JsonProperty("scopeType")
    private String scopeType;
    @Schema(description = "Status of the data processor", example = "ACTIVE", allowableValues = {"ACTIVE", "INACTIVE"})
    @JsonProperty("status")
    private String status;
    @Schema(description = "Single Point of Contact details for the data processor")
    @JsonProperty("spoc")
    private DataProcessorSpocDto spoc;
    @Schema(description = "Identity type for notifications", example = "EMAIL", allowableValues = {"EMAIL", "MOBILE"})
    @JsonProperty("identityType")
    private IdentityType identityType;
    @Schema(description = "Unique identifier for the data processor in WSO2", example = "dp-uuid-12345")
    @JsonProperty("dataProcessorUniqueId")
    private String dataProcessorUniqueId;
    @Schema(description = "Consumer key for WSO2 integration", example = "IR7nsPnc5UrC8vzgCqMDDuTKhHUa")
    @JsonProperty("consumerKey")
    private String consumerKey;
    @Schema(description = "Consumer secret for WSO2 integration", example = "fenuijAwpxG_FpFMO9kn_Vj8w_Qa")
    @JsonProperty("consumerSecret")
    private String consumerSecret;
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
