package com.jio.partnerportal.repository;

import com.jio.partnerportal.entity.LegalEntity;

import java.util.List;
import java.util.Map;

public interface LegalEntityRepository {

    LegalEntity findLegalEntityByLegalEntityId(String legalEntityId);

    void updateLegalEntity(LegalEntity legalEntity);

    List<LegalEntity> findLegalEntitiesByParams(Map<String, String> param);

    long count();
}
