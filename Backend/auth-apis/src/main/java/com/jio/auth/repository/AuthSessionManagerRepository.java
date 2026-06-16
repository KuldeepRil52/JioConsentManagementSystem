package com.jio.auth.repository;



import com.jio.auth.model.AuthSessionManager;
import com.jio.auth.config.TenantMongoTemplateFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.UUID;

@Slf4j
@Repository
@RequiredArgsConstructor
public class AuthSessionManagerRepository {

    @Autowired
    private final TenantMongoTemplateFactory tenantMongoTemplateFactory;

    private static final String COLLECTION_NAME = "auth_session_manager";
    private static final String FIELD_SESSION_ID = "sessionId";
    private static final String FIELD_REFRESH_TOKEN_HASH = "refreshTokenHash";
    private static final String FIELD_USER_ID = "userId";
    private static final String FIELD_REVOKED = "revoked";

    // ---- Create a new session ----
    public void saveSession(String tenantId, AuthSessionManager session) {
        MongoTemplate template = tenantMongoTemplateFactory.getTemplateForTenant(tenantId);
        template.save(session, COLLECTION_NAME);
    }

    // ---- Find active session by refresh token ----
    public AuthSessionManager findByRefreshToken(String tenantId, String refreshTokenHash) {
        MongoTemplate template = tenantMongoTemplateFactory.getTemplateForTenant(tenantId);
        Query query = new Query(
                Criteria.where(FIELD_REFRESH_TOKEN_HASH).is(refreshTokenHash)
                        .and(FIELD_REVOKED).ne(true)
        );
        return template.findOne(query, AuthSessionManager.class, COLLECTION_NAME);
    }

    // ---- Find session by sessionId ----
    public AuthSessionManager findBySessionId(String tenantId, UUID sessionId) {
        MongoTemplate template = tenantMongoTemplateFactory.getTemplateForTenant(tenantId);
        Query query = new Query(Criteria.where(FIELD_SESSION_ID).is(sessionId));
        return template.findOne(query, AuthSessionManager.class, COLLECTION_NAME);
    }

    // ---- Rotate refresh token ----
    public void updateRefreshToken(String tenantId,
                                   UUID sessionId,
                                   String newHash,
                                   String previousHash,
                                   Date rotatedAt,
                                   int newRotationCount) {
        MongoTemplate template = tenantMongoTemplateFactory.getTemplateForTenant(tenantId);
        Query query = new Query(Criteria.where(FIELD_SESSION_ID).is(sessionId));
        Update update = new Update()
                .set("refreshTokenHash", newHash)
                .set("previousRefreshTokenHash", previousHash)
                .set("lastRotatedAt", rotatedAt)
                .set("rotationCount", newRotationCount);
        template.updateFirst(query, update, COLLECTION_NAME);
    }

    // ---- Revoke session (logout or reuse detected) ----
    public void revokeSession(String tenantId, UUID sessionId, String reason) {
        MongoTemplate template = tenantMongoTemplateFactory.getTemplateForTenant(tenantId);
        Query query = new Query(Criteria.where(FIELD_SESSION_ID).is(sessionId));
        Update update = new Update()
                .set(FIELD_REVOKED, true)
                .set("revokedReason", reason);
        template.updateFirst(query, update, COLLECTION_NAME);
    }

    // ---- Find active sessions by user (for multi-login tracking) ----
    public java.util.List<AuthSessionManager> findActiveSessionsByUser(String tenantId, String userId) {
        MongoTemplate template = tenantMongoTemplateFactory.getTemplateForTenant(tenantId);
        Query query = new Query(
                Criteria.where(FIELD_USER_ID).is(userId)
                        .and(FIELD_REVOKED).ne(true)
        );
        return template.find(query, AuthSessionManager.class, COLLECTION_NAME);
    }

    // ---- Delete expired sessions manually (if needed) ----
    public void deleteExpiredSessions(String tenantId, Date now) {
        MongoTemplate template = tenantMongoTemplateFactory.getTemplateForTenant(tenantId);
        Query query = new Query(Criteria.where("absoluteExpiry").lt(now));
        template.remove(query, COLLECTION_NAME);
    }
    // ---- Revoke the oldest active session for a user ----
    public void revokeOldestActiveSession(String tenantId, String userId) {
        MongoTemplate template = tenantMongoTemplateFactory.getTemplateForTenant(tenantId);

        // keep the same query you trust
        Query query = new Query();
        query.addCriteria(Criteria.where("tenantId").is(tenantId));
        query.addCriteria(Criteria.where(FIELD_USER_ID).is(userId));
        query.addCriteria(Criteria.where(FIELD_REVOKED).ne(true));
        query.with(Sort.by(Sort.Direction.ASC, "createdAt"));
        query.limit(1);

        // fetch the oldest active session
        AuthSessionManager oldest = template.findOne(query, AuthSessionManager.class, COLLECTION_NAME);
        if (oldest == null) {
            log.warn("No active session found to revoke for tenant={}, user={}", tenantId, userId);
            return;
        }

        UUID sessionId = oldest.getSessionId();
        log.info("Revoking oldest active session for tenant={}, user={}, sessionId={}", tenantId, userId, sessionId);

        // now call your existing revokeSession method
        revokeSession(tenantId, sessionId, "Auto-revoked due to session limit");
    }


}
