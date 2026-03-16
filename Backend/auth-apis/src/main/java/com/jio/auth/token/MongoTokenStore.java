package com.jio.auth.token;

import com.jio.auth.model.ActiveToken;
import com.jio.auth.model.RevokedToken;
import com.jio.auth.repository.ActiveTokenRepository;
import com.jio.auth.repository.RevokedTokenRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;
import java.time.Instant;

@Slf4j
@Component
@ConditionalOnProperty(prefix = "token.store", name = "type", havingValue = "mongo", matchIfMissing = true)
public class MongoTokenStore implements TokenStore {

    private final RevokedTokenRepository revokedRepo;
    private final ActiveTokenRepository activeRepo;

    @Value("${token.max.sessions:1}")
    private int maxSessions;

    @Autowired
    public MongoTokenStore(RevokedTokenRepository revokedRepo, ActiveTokenRepository activeRepo) {
        this.revokedRepo = revokedRepo;
        this.activeRepo = activeRepo;
    }

    // --- Active Tokens ---
    @Override
    public void saveActiveToken(String userId, String jti, Date expiry) {
        removeExpiredTokens(userId);

        long activeCount = activeRepo.countByUserId(userId);
        if (activeCount >= maxSessions) {
            // Delete the oldest active token
            ActiveToken oldest = activeRepo.findFirstByUserIdOrderByExpiryAsc(userId);
            if (oldest != null) {
                revokedRepo.save(new RevokedToken(oldest.getJti(), Instant.now(), expiry.toInstant()));
                activeRepo.delete(oldest);
                log.info("Deleted oldest token {} for user {}", oldest.getJti(), userId);
            }
        }

        ActiveToken token = new ActiveToken(jti, userId, expiry.toInstant());
        activeRepo.save(token);
        log.debug("Saved active token {} for user {}", jti, userId);
    }

    @Override
    public boolean isTokenActive(String userId, String jti) {
        removeExpiredTokens(userId);
        return activeRepo.existsByUserIdAndJti(userId, jti);
    }

    @Override
    public void removeActiveToken(String userId, String jti) {
        activeRepo.deleteByUserIdAndJti(userId, jti);
        log.debug("Removed active token {} for user {}", jti, userId);
    }

    @Override
    public int countActiveTokens(String userId) {
        removeExpiredTokens(userId);
        return (int) activeRepo.countByUserId(userId);
    }

    @Override
    public void removeExpiredTokens(String userId) {
        activeRepo.deleteByUserIdAndExpiryBefore(userId, Instant.now());
        log.debug("Expired tokens removed for user {}", userId);
    }

    // --- Revoked Tokens ---
    @Override
    public void revokeToken(String jti, Date expiry) {
        if (!revokedRepo.existsById(jti)) {
            revokedRepo.save(new RevokedToken(jti, Instant.now(), expiry.toInstant()));
            log.debug("Token {} saved in revoked tokens", jti);
        }
        // Remove from active tokens
        activeRepo.deleteByJti(jti);
    }

    @Override
    public boolean isTokenRevoked(String jti) {
        return revokedRepo.existsById(jti);
    }
}
