package com.jio.auth.repository;

import com.jio.auth.model.AuthSecret;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface AuthSecretRepository extends MongoRepository<AuthSecret, Long> {
    boolean existsBySecretCodeAndIdentityValue(String secretCode, String identityValue);
}