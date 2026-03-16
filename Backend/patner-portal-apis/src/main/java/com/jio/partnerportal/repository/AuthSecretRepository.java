package com.jio.partnerportal.repository;

import com.jio.partnerportal.entity.AuthSecret;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface AuthSecretRepository extends MongoRepository<AuthSecret, Long> {
}

