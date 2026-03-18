package com.jio.partnerportal.repository;

import com.jio.partnerportal.entity.DigiLockerCredential;

import java.util.List;
import java.util.Map;

public interface DigiLockerCredentialRepository {

    DigiLockerCredential save(DigiLockerCredential credential);

    DigiLockerCredential findByCredentialId(String credentialId);

    List<DigiLockerCredential> findCredentialByParams(Map<String, String> searchParams);

    long count();

    boolean existByScopeLevel(String scopeLevel);

    boolean existByScopeLevelAndBusinessId(String scopeLevel, String businessId);
}
