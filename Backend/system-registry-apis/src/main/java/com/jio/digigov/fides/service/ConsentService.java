package com.jio.digigov.fides.service;

import com.jio.digigov.fides.dto.request.WithdrawlRequest;
import com.jio.digigov.fides.entity.ConsentWithdrawalJob;

/**
 * Service for consent-related operations
 */
public interface ConsentService {

    /**
     * Withdraws or expires a consent and triggers
     * async masking of associated personal data.
     *
     * @param consentId  consent identifier
     * @param tenantId   tenant identifier (header)
     * @param businessId business identifier (header)
     */
    ConsentWithdrawalJob withdrawConsent(
            String consentId,
            String tenantId,
            String businessId,
            WithdrawlRequest request
    );
}
