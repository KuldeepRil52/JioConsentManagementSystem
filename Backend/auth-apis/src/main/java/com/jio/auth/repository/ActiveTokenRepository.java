package com.jio.auth.repository;

import com.jio.auth.model.ActiveToken;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface ActiveTokenRepository extends MongoRepository<ActiveToken, String> {

    int countByUserId(String userId);

    boolean existsByUserIdAndJti(String userId, String jti);

    List<ActiveToken> findByUserId(String userId);

    // Fetch the oldest token (for replacement)
    ActiveToken findFirstByUserIdOrderByExpiryAsc(String userId);

    void deleteByUserIdAndJti(String userId, String jti);

    void deleteByUserIdAndExpiryBefore(String userId, Instant now);

    void deleteByJti(String jti);
}
