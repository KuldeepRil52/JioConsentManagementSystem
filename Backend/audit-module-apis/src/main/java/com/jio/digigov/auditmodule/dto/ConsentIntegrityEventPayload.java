package com.jio.digigov.auditmodule.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConsentIntegrityEventPayload {

    private String payloadHashVerification;
    private String chainLinkVerification;
    private String consentJsonMatch;
    private String referenceId;

    private String overallIntegrityStatus;
    private String consentId;
    private String tenantId;
    private String businessId;
}
