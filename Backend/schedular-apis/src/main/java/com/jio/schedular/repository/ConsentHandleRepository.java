package com.jio.schedular.repository;

import com.jio.schedular.entity.ConsentHandle;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public interface ConsentHandleRepository {

    ConsentHandle save(ConsentHandle consentHandle, String tenantId);

    ConsentHandle getByConsentHandleId(String consentHandleId);

    List<ConsentHandle> findConsentHandleByParams(Map<String, Object> searchParams);

    List<ConsentHandle> findConsentHandleByParamsWithIds(Map<String, Object> searchParams);

    long count();

    /**
     * Finds pending consent handles that are older than the specified date
     * 
     * @param expiryDate The date before which consent handles should be considered for expiry
     * @param pageable Pagination information
     * @return List of pending consent handles older than the expiry date
     */
    List<ConsentHandle> findPendingConsentHandlesOlderThan(LocalDateTime expiryDate, Pageable pageable);
}
