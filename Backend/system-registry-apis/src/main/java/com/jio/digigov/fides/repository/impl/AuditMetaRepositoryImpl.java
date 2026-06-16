package com.jio.digigov.fides.repository.impl;

import com.jio.digigov.fides.config.MultiTenantMongoConfig;
import com.jio.digigov.fides.entity.AuditMeta;
import com.jio.digigov.fides.repository.AuditMetaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

@Repository
public class AuditMetaRepositoryImpl implements AuditMetaRepository {

    private final MultiTenantMongoConfig mongoConfig;

    @Autowired
    public AuditMetaRepositoryImpl(MultiTenantMongoConfig mongoConfig) {
        this.mongoConfig = mongoConfig;
    }

    @Override
    public AuditMeta save(AuditMeta auditMeta, String tenantId) {
        return mongoConfig.getMongoTemplateForTenant(tenantId).save(auditMeta);
    }
}