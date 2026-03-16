package com.jio.partnerportal.repository;

import com.jio.partnerportal.entity.AuditMeta;

public interface AuditMetaRepository {

    AuditMeta save(AuditMeta auditMeta, String tenantId);

}