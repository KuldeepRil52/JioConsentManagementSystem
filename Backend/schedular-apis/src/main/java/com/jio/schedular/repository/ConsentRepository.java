package com.jio.schedular.repository;

import com.jio.schedular.enums.ConsentStatus;
import com.jio.schedular.dto.CustomerIdentifiers;
import com.jio.schedular.entity.Consent;

import java.util.List;
import java.util.Map;

public interface ConsentRepository {

    Consent save(Consent consent);

    public Consent getByConsentId(String consentId);

    Consent existByTemplateIdAndTemplateVersionAndCustomerIdentifiers(String templateId, int templateVersion, CustomerIdentifiers customerIdentifiers);

    List<Consent> findConsentByParams(Map<String, Object> searchParams);

    long count();

    long countByParams(Map<String, Object> searchParams);

    Map<ConsentStatus, Long> countByStatus(Map<String, Object> searchParams);

    Consent findLatestByCreatedAt();
}
