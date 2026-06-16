package com.jio.consent.repository;

import com.jio.consent.entity.UserSession;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserSessionRepository extends MongoRepository<UserSession, String> {

    UserSession findByAccessToken(String accessToken);
}
