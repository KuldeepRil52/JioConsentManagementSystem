package com.jio.consent.dto.Response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.jio.consent.dto.BulkConsentUploadStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@AllArgsConstructor
@NoArgsConstructor
public class BulkConsentStatusResponse {

    private String transactionId;
    private BulkConsentUploadStatus status;
    private int totalCount;
    private int successCount;
    private int failedCount;
    private List<BulkConsentItemStatus> consents;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class BulkConsentItemStatus {
        private String txnId;
        private BulkConsentUploadStatus status;
        private String consentHandleId;
        private String consentId;
        private String errorMessage;
    }
}
