package com.jio.auth.repository;

import com.jio.auth.model.JwkKey;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface JwkKeyRepository extends MongoRepository<JwkKey, String> {
    JwkKey findFirstByOrderByKidAsc();
}
