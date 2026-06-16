package com.jio.consent.repositoryImpl;

import com.jio.consent.entity.AuditMeta;
import com.jio.consent.multitenancy.TenantMongoTemplateProvider;
import com.jio.consent.repository.AuditMetaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

@Repository
public class AuditMetaRepositoryImpl implements AuditMetaRepository {

    private final TenantMongoTemplateProvider tenantMongoTemplateProvider;

    @Autowired
    public AuditMetaRepositoryImpl(TenantMongoTemplateProvider tenantMongoTemplateProvider) {
        this.tenantMongoTemplateProvider = tenantMongoTemplateProvider;
    }

    @Override
    public AuditMeta save(AuditMeta auditMeta, String tenantId) {
        return tenantMongoTemplateProvider.getMongoTemplate(tenantId).save(auditMeta);
    }
}

