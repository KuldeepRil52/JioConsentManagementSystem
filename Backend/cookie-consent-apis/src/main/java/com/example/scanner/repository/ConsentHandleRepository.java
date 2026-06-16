package com.example.scanner.repository;

import com.example.scanner.entity.CookieConsentHandle;

public interface ConsentHandleRepository {

    CookieConsentHandle save(CookieConsentHandle consentHandle, String tenantId);

    CookieConsentHandle getByConsentHandleId(String consentHandleId, String tenantId);

    CookieConsentHandle findActiveConsentHandle(String deviceId, String url, String templateId,
                                                int templateVersion, String tenantId);

}