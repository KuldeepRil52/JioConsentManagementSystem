package com.jio.digigov.fides.repository;

import com.jio.digigov.fides.entity.AuditMeta;

public interface AuditMetaRepository {

    AuditMeta save(AuditMeta auditMeta, String tenantId);

}