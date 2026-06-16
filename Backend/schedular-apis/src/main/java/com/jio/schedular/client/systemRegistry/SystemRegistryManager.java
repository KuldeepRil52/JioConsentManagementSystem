package com.jio.schedular.client.systemRegistry;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class SystemRegistryManager {

    private final SystemRegistryApiManager apiManager;

    public void withdrawConsent(
            String consentId,
            String tenantId,
            String businessId) {

        Map<String, String> headers = Map.of(
                "X-TENANT-ID", tenantId,
                "X-BUSINESS-ID", businessId
        );

        // 202 ACCEPTED is enough – async job starts in System Registry
        apiManager.withdrawConsent(consentId, headers, Void.class);
    }
}