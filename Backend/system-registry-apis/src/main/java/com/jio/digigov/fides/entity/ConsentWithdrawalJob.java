package com.jio.digigov.fides.entity;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Data
@Document(collection = "consent_withdrawal_jobs")
public class ConsentWithdrawalJob {

    @Id
    private String id;

    private String consentId;
    private String tenantId;
    private String businessId;
    private String eventId;
    private WithdrawalData withdrawalData;

    private String status; // PENDING, PROCESSING, COMPLETED, FAILED
    private String failureReason;

    private Instant createdAt;
    private Instant updatedAt;
}