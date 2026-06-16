package com.jio.digigov.notification.entity.event;

import com.jio.digigov.notification.entity.base.BaseEntity;
import com.jio.digigov.notification.enums.DataProcessorStatus;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Data Processor entity for business-level data processor management
 * Uses multi-tenant database architecture (no tenantId field, uses tenant-specific databases)
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "data_processors")
@CompoundIndexes({
    @CompoundIndex(name = "unique_dp_business_idx",
                  def = "{'dataProcessorId': 1, 'businessId': 1}",
                  unique = true),
    @CompoundIndex(name = "business_status_idx",
                  def = "{'businessId': 1, 'status': 1}")
})
public class DataProcessor extends BaseEntity {

    // Data Processor identifier (UUID format)
    @NotBlank(message = "Data Processor ID is required")
    @Indexed
    @Field("dataProcessorId")
    private String dataProcessorId; // e.g., "a1b2c3d4-e5f6-7890-1234-567890abcdef"

    // Data Processor name
    @NotBlank(message = "Data Processor name is required")
    @Field("dataProcessorName")
    private String dataProcessorName; // e.g., "AWS", "Google Cloud"

    // Callback URL for webhook notifications
    @Field("callbackUrl")
    private String callbackUrl; // e.g., "https://randomurl.com/cb1"

    // Data Processor details/description
    @Field("details")
    private String details; // e.g., "Cloud Hosting Provider for infrastructure services."

    // Attachment reference
    @Field("attachment")
    private String attachment; // e.g., "random_attachment_string_1"

    // Attachment metadata
    @Field("attachmentMeta")
    private DocumentMeta attachmentMeta;

    // Vendor risk document reference
    @Field("vendorRiskDocument")
    private String vendorRiskDocument; // e.g., "random_risk_string_1"

    // Vendor risk document metadata
    @Field("vendorRiskDocumentMeta")
    private DocumentMeta vendorRiskDocumentMeta;

    // Business identifier (required - business-scoped)
    @NotBlank(message = "Business ID is required")
    @Field("businessId")
    private String businessId;

    // Scope type
    @Field("scopeType")
    @Builder.Default
    private String scopeType = "TENANT"; // Default to TENANT

    // Data Processor status
    @NotNull
    @Field("status")
    @Builder.Default
    private DataProcessorStatus status = DataProcessorStatus.ACTIVE;
}