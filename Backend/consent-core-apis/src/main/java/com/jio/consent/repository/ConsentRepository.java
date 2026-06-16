package com.jio.consent.repository;

import com.jio.consent.dto.ConsentStatus;
import com.jio.consent.dto.CustomerIdentifiers;
import com.jio.consent.entity.Consent;

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
    
    Consent findById(org.bson.types.ObjectId id);
    
    Consent getActiveByConsentId(String consentId);
}
