package com.example.scanner.repository;

import com.example.scanner.dto.CustomerIdentifiers;
import com.example.scanner.entity.CookieConsent;

import java.util.List;
import java.util.Optional;

public interface ConsentRepositoryCustom {

    CookieConsent existsByTemplateIdAndTemplateVersionAndCustomerIdentifiers(
            String templateId, Integer templateVersion, CustomerIdentifiers customerIdentifiers,
            String tenantId, String consentHandleId);

    CookieConsent saveToDatabase(CookieConsent consent, String tenantId);

    CookieConsent findActiveByConsentId(String consentId, String tenantId);

    List<CookieConsent> findAllVersionsByConsentId(String consentId, String tenantId);

    Optional<CookieConsent> findByConsentIdAndVersion(String consentId, Integer version, String tenantId);

    CookieConsent findLatestByCreatedAt();

    Optional<CookieConsent> findById(String id, String tenantId);
}