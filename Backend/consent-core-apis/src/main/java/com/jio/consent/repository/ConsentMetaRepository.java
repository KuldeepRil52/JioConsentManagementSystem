package com.jio.consent.repository;

import com.jio.consent.entity.ConsentMeta;

public interface ConsentMetaRepository {

    ConsentMeta save(ConsentMeta consentMeta);

    ConsentMeta getByConsentMetaId(String consentMetaId);

}

