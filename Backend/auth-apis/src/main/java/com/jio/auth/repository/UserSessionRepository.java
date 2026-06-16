package com.jio.auth.repository;

import com.jio.auth.model.UserSession;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserSessionRepository extends MongoRepository<UserSession, String> {

    List<UserSession> findByUserId(String userId);

    List<UserSession> findByTenantId(String tenantId);

    List<UserSession> findByBusinessId(String businessId);

    List<UserSession> findByIdentityAndIdentityType(String identity, String identityType);

    boolean existsByUuid(String uuid);

    void deleteByUuid(String uuid);

    void deleteByUserId(String userId);

    void deleteByIdentity(String identity);

    Optional<UserSession> findByAccessTokenAndTenantIdAndBusinessIdAndIdentity(
            String accessToken, String tenantId, String businessId, String identity);

    Optional<UserSession> findByAccessToken(String accessToken);
}

