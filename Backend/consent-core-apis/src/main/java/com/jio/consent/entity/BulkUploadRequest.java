package com.jio.consent.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.jio.consent.dto.BulkConsentUploadStatus;
import lombok.*;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;

import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
@Builder
@org.springframework.data.mongodb.core.mapping.Document("bulk_upload_requests")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@AllArgsConstructor
@NoArgsConstructor
public class BulkUploadRequest extends AbstractEntity {

    @Id
    private ObjectId id;

    @Indexed(unique = true, name = "transactionId")
    private String transactionId;

    private String tenantId;

    private int totalCount;

    private int successCount;

    private int failedCount;

    @Indexed(name = "status")
    @Builder.Default
    private BulkConsentUploadStatus status = BulkConsentUploadStatus.ACCEPTED;

    private List<BulkConsentUpload> consents;
}
