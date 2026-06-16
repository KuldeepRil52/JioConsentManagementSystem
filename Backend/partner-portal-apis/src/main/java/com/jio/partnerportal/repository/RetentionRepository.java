package com.jio.partnerportal.repository;

import com.jio.partnerportal.entity.RetentionConfig;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface RetentionRepository extends MongoRepository<RetentionConfig, String> {
}
