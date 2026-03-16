package com.jio.consent.repository;

import com.jio.consent.entity.AuditMeta;

public interface AuditMetaRepository {

    AuditMeta save(AuditMeta auditMeta, String tenantId);

}

