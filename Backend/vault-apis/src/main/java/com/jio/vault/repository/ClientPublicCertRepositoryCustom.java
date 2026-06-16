package com.jio.vault.repository;

import com.jio.vault.documents.ClientPublicCert;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ClientPublicCertRepositoryCustom {
    Optional<ClientPublicCert> findByBusinessIdAndTenantIdDynamic(
            String dbName, String businessId, String tenantId);
    List<ClientPublicCert> findByBusinessIdAndTenantId(String dbName, String businessId, String tenantId);
    Optional<ClientPublicCert> findByKeyId(String dbName, String keyId);
    ClientPublicCert save(String dbName, ClientPublicCert cert);
}


