package com.jio.consent.client.registry.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class WithdrawConsentResponse {
    private String id;
    private String consentId;
    private String tenantId;
    private String businessId;
    private String status;
    private String failureReason;
    private Instant createdAt;
    private Instant updatedAt;
}

