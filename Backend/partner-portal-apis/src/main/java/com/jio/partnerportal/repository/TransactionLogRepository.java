package com.jio.partnerportal.repository;

import com.jio.partnerportal.entity.TransactionLog;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface TransactionLogRepository extends MongoRepository<TransactionLog, String> {
}
