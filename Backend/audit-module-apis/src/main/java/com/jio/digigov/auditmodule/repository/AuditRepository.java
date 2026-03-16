package com.jio.digigov.auditmodule.repository;

import com.jio.digigov.auditmodule.entity.AuditDocument;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AuditRepository extends MongoRepository<AuditDocument, String> {
}
