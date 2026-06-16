package com.jio.schedular.repository;

import com.jio.schedular.entity.CookieConsent;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface CookieConsentRepository {

    List<CookieConsent> findAndMarkExpiredConsents(LocalDateTime now);

    List<CookieConsent> findExpiredConsents(LocalDateTime now);

    Optional<CookieConsent> findById(String id, String tenantId);

    void saveToDatabase(CookieConsent consent, String tenantId);
}