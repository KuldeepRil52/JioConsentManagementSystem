package com.jio.auth.repository;

import com.jio.auth.model.RevokedToken;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RevokedTokenRepository extends MongoRepository<RevokedToken, String> {

    boolean existsByJti(String jti);

    // Optional: delete by jti if needed
    void deleteByJti(String jti);
}
