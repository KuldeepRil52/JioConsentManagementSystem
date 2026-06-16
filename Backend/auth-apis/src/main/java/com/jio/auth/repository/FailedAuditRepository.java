package com.jio.auth.repository;

import com.jio.auth.model.FailedAudit;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FailedAuditRepository extends MongoRepository<FailedAudit, String> {
}
