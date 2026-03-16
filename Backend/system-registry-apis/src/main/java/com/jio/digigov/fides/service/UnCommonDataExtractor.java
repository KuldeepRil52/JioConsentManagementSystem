package com.jio.digigov.fides.service;
import com.jio.digigov.fides.dto.ConsentWithdrawalDataItems;
import com.jio.digigov.fides.entity.Consent;

import java.util.Set;

public interface UnCommonDataExtractor {

    ConsentWithdrawalDataItems extractWithdrawalDataItems(Consent consent, String tenantId, String businessId);
}