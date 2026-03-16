package com.jio.schedular.repository;

import com.jio.schedular.entity.AuditMeta;

public interface AuditMetaRepository {

    AuditMeta save(AuditMeta auditMeta, String tenantId);

}