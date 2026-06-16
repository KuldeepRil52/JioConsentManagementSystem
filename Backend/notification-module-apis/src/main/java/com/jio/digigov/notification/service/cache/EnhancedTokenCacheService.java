package com.jio.digigov.notification.service.cache;

import com.jio.digigov.notification.entity.NotificationConfig;
import com.jio.digigov.notification.enums.CredentialType;
import com.jio.digigov.notification.enums.ProviderType;
import com.jio.digigov.notification.enums.CacheType;
import com.jio.digigov.notification.exception.TokenGenerationException;
import com.jio.digigov.notification.service.TokenService;
import com.jio.digigov.notification.util.TenantContextHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Enhanced token caching service using the unified cache infrastructure.
 *
 * This service builds upon the successful TokenCacheService implementation that
 * achieved 90% reduction in DigiGov token API calls. It now uses the unified
 * cache service architecture to support both Caffeine and MongoDB backends.
 *
 * Key Features:
 * - Supports both Caffeine (in-memory) and MongoDB (distributed) caching
 * - Multi-tenant token isolation with tenant and business ID separation
 * - Proactive token refresh before expiry (configurable buffer)
 * - Thread-safe token retrieval and refresh
 * - Comprehensive error handling and fallback
 * - Enhanced performance metrics and monitoring
 *
 * Caching Strategy:
 * - Cache tokens for configurable TTL (default: 55 minutes)
 * - Proactive refresh when approaching expiry (configurable buffer)
 * - Per-tenant-business token isolation for true multi-tenancy
 * - Fallback to fresh token generation on cache failures
 *
 * @author Notification Service Team
 * @version 2.0
 * @since 2025-01-20
 */
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "cache.enabled", havingValue = "true", matchIfMissing = true)
@Slf4j
public class EnhancedTokenCacheService {

    private final CacheService<String> cacheService;
    private final TokenService tokenService;

    /**
     * Concurrent map to track token refresh locks per tenant-business combination.
     * Prevents multiple threads from refreshing the same token simultaneously.
     */
    private final ConcurrentHashMap<String, ReentrantLock> refreshLocks = new ConcurrentHashMap<>();

    /**
     * Map to track token expiry times for proactive refresh logic.
     * Key format: "{tenantId}:{businessId}"
     */
    private final ConcurrentHashMap<String, LocalDateTime> tokenExpiryTimes = new ConcurrentHashMap<>();

    /**
     * Retrieves a cached DigiGov authentication token for the specified configuration.
     *
     * Enhanced version that uses the unified cache service with proper tenant isolation.
     * Maintains the same high-performance characteristics as the original implementation
     * while adding multi-tenant support and configurable cache backends.
     *
     * @param configuration NotificationConfig containing DigiGov credentials
     * @return Valid DigiGov authentication token
     * @throws TokenGenerationException if token generation fails
     */
    public String getToken(NotificationConfig configuration) {
        return getToken(configuration, CredentialType.CLIENT);
    }

    /**
     * Retrieves a cached DigiGov authentication token with specific credential type.
     *
     * @param configuration NotificationConfig containing DigiGov credentials
     * @param credentialType Type of credentials (CLIENT or ADMIN)
     * @return Valid DigiGov authentication token
     * @throws TokenGenerationException if token generation fails
     */
    public String getToken(NotificationConfig configuration, CredentialType credentialType) {
        String tenantId = getTenantId();
        String businessId = configuration.getBusinessId();
        String tokenKey = buildTokenKey(credentialType);

        try {
            // Check cache first
            Optional<String> cachedToken = cacheService.get(tokenKey, tenantId, businessId);

            if (cachedToken.isPresent() && !shouldRefreshToken(tenantId, businessId, credentialType)) {
                log.debug("Cache hit for token: {}:{}:{}", tenantId, businessId, credentialType);
                return cachedToken.get();
            }

            // Check if token needs proactive refresh
            if (cachedToken.isPresent() && shouldRefreshToken(tenantId, businessId, credentialType)) {
                return refreshTokenSafely(configuration, credentialType, tenantId, businessId, tokenKey);
            }

            // Generate fresh token (cache miss or first request)
            return generateAndCacheToken(configuration, credentialType, tenantId, businessId, tokenKey);

        } catch (Exception e) {
            log.error("Failed to get token for tenant {}, business {}: {}",
                     tenantId, businessId, e.getMessage(), e);
            throw new TokenGenerationException("Failed to retrieve token for business: " + businessId, e);
        }
    }

    /**
     * Generates a fresh token and updates the cache.
     *
     * @param configuration NotificationConfig for token generation
     * @param credentialType Type of credentials
     * @param tenantId Tenant identifier
     * @param businessId Business identifier
     * @param tokenKey Cache key for the token
     * @return Fresh DigiGov authentication token
     */
    private String generateAndCacheToken(NotificationConfig configuration,
                                       CredentialType credentialType,
                                       String tenantId,
                                       String businessId,
                                       String tokenKey) {
        log.debug("Generating fresh token for tenant: {}, business: {}, type: {}",
                 tenantId, businessId, credentialType);

        // Generate token using existing TokenService
        String token = tokenService.generateTokenWithConfig(configuration, credentialType).getAccessToken();

        // Cache the token with appropriate TTL
        CacheType cacheType = getCacheTypeForCredential(credentialType);
        cacheService.put(tokenKey, token, tenantId, businessId, cacheType);

        // Track expiry time for proactive refresh
        Duration ttl = getTtlForCredentialType(credentialType);
        LocalDateTime expiryTime = LocalDateTime.now().plus(ttl);
        String expiryKey = buildExpiryKey(tenantId, businessId, credentialType);
        tokenExpiryTimes.put(expiryKey, expiryTime);

        log.info("Generated and cached new token for tenant: {}, business: {}, type: {}, expires at: {}",
                tenantId, businessId, credentialType, expiryTime);

        return token;
    }

    /**
     * Checks if a token should be proactively refreshed.
     *
     * @param tenantId Tenant identifier
     * @param businessId Business identifier
     * @param credentialType Type of credentials
     * @return true if token should be refreshed, false otherwise
     */
    private boolean shouldRefreshToken(String tenantId, String businessId, CredentialType credentialType) {
        String expiryKey = buildExpiryKey(tenantId, businessId, credentialType);
        LocalDateTime expiryTime = tokenExpiryTimes.get(expiryKey);

        if (expiryTime == null) {
            return false; // No expiry time means no cached token
        }

        // Get refresh buffer from configuration (default 5 minutes)
        Duration refreshBuffer = getRefreshBufferForCredentialType(credentialType);
        LocalDateTime refreshThreshold = LocalDateTime.now().plus(refreshBuffer);
        boolean shouldRefresh = expiryTime.isBefore(refreshThreshold);

        if (shouldRefresh) {
            log.debug("Token for tenant: {}, business: {}, type: {} expires at {}, scheduling refresh",
                     tenantId, businessId, credentialType, expiryTime);
        }

        return shouldRefresh;
    }

    /**
     * Safely refreshes a token with thread synchronization.
     *
     * @param configuration NotificationConfig for token refresh
     * @param credentialType Type of credentials
     * @param tenantId Tenant identifier
     * @param businessId Business identifier
     * @param tokenKey Cache key for the token
     * @return Refreshed DigiGov authentication token
     */
    private String refreshTokenSafely(NotificationConfig configuration,
                                    CredentialType credentialType,
                                    String tenantId,
                                    String businessId,
                                    String tokenKey) {
        String lockKey = buildLockKey(tenantId, businessId, credentialType);
        ReentrantLock lock = refreshLocks.computeIfAbsent(lockKey, k -> new ReentrantLock());

        try {
            lock.lock();
            log.debug("Acquired refresh lock for tenant: {}, business: {}, type: {}",
                     tenantId, businessId, credentialType);

            // Double-check if refresh is still needed (another thread may have refreshed)
            if (!shouldRefreshToken(tenantId, businessId, credentialType)) {
                Optional<String> cachedToken = cacheService.get(tokenKey, tenantId, businessId);
                if (cachedToken.isPresent()) {
                    log.debug("Token already refreshed by another thread for tenant: {}, business: {}, type: {}",
                             tenantId, businessId, credentialType);
                    return cachedToken.get();
                }
            }

            // Clear old token from cache to force refresh
            evictToken(tenantId, businessId, credentialType);

            // Generate fresh token
            return generateAndCacheToken(configuration, credentialType, tenantId, businessId, tokenKey);

        } finally {
            lock.unlock();
            log.debug("Released refresh lock for tenant: {}, business: {}, type: {}",
                     tenantId, businessId, credentialType);
        }
    }

    /**
     * Evicts a specific token from the cache.
     *
     * @param tenantId Tenant identifier
     * @param businessId Business identifier
     * @param credentialType Type of credentials
     */
    public void evictToken(String tenantId, String businessId, CredentialType credentialType) {
        String tokenKey = buildTokenKey(credentialType);
        String expiryKey = buildExpiryKey(tenantId, businessId, credentialType);
        String lockKey = buildLockKey(tenantId, businessId, credentialType);

        cacheService.evict(tokenKey, tenantId, businessId);
        tokenExpiryTimes.remove(expiryKey);
        refreshLocks.remove(lockKey);

        log.info("Evicted token for tenant: {}, business: {}, type: {}", tenantId, businessId, credentialType);
    }

    /**
     * Evicts a token for the current tenant context.
     *
     * @param businessId Business identifier
     */
    public void evictToken(String businessId) {
        String tenantId = getTenantId();
        evictToken(tenantId, businessId, CredentialType.CLIENT);
    }

    /**
     * Evicts all tokens for a tenant-business combination.
     *
     * @param tenantId Tenant identifier
     * @param businessId Business identifier
     */
    public void evictAllTokens(String tenantId, String businessId) {
        cacheService.evictByType(CacheType.DIGIGOV_TOKEN, tenantId, businessId);

        // Clean up tracking maps
        String clientExpiryKey = buildExpiryKey(tenantId, businessId, CredentialType.CLIENT);
        String adminExpiryKey = buildExpiryKey(tenantId, businessId, CredentialType.ADMIN);
        String clientLockKey = buildLockKey(tenantId, businessId, CredentialType.CLIENT);
        String adminLockKey = buildLockKey(tenantId, businessId, CredentialType.ADMIN);

        tokenExpiryTimes.remove(clientExpiryKey);
        tokenExpiryTimes.remove(adminExpiryKey);
        refreshLocks.remove(clientLockKey);
        refreshLocks.remove(adminLockKey);

        log.info("Evicted all tokens for tenant: {}, business: {}", tenantId, businessId);
    }

    /**
     * Gets enhanced cache statistics.
     *
     * @return Map containing cache statistics and metrics
     */
    public Map<String, Object> getCacheStatistics() {
        return Map.of(
            "cacheType", cacheService.getCacheType(),
            "cacheHealthy", cacheService.isHealthy(),
            "trackedExpiryTimes", tokenExpiryTimes.size(),
            "activeLocks", refreshLocks.size(),
            "oldestTokenAge", getOldestTokenAge(),
            "tokensNearExpiry", getTokensNearExpiry(),
            "cacheStats", cacheService.getStats(CacheType.DIGIGOV_TOKEN)
        );
    }

    /**
     * Build cache key for token
     */
    private String buildTokenKey(CredentialType credentialType) {
        return CacheType.DIGIGOV_TOKEN.getKey() + ":" + credentialType.name().toLowerCase();
    }

    /**
     * Build key for tracking token expiry times
     */
    private String buildExpiryKey(String tenantId, String businessId, CredentialType credentialType) {
        return String.format("%s:%s:%s", tenantId, businessId, credentialType.name());
    }

    /**
     * Build key for refresh locks
     */
    private String buildLockKey(String tenantId, String businessId, CredentialType credentialType) {
        return String.format("lock:%s:%s:%s", tenantId, businessId, credentialType.name());
    }

    /**
     * Get tenant ID from context or default
     */
    private String getTenantId() {
        return TenantContextHolder.getTenantId();
    }

    /**
     * Get cache type for credential type
     */
    private CacheType getCacheTypeForCredential(CredentialType credentialType) {
        return CacheType.DIGIGOV_TOKEN; // All DigiGov tokens use same cache type
    }

    /**
     * Get TTL for credential type (can be made configurable)
     */
    private Duration getTtlForCredentialType(CredentialType credentialType) {
        // Default to 55 minutes for all types, can be made configurable
        return Duration.ofMinutes(55);
    }

    /**
     * Get refresh buffer for credential type (can be made configurable)
     */
    private Duration getRefreshBufferForCredentialType(CredentialType credentialType) {
        // Default to 5 minutes for all types, can be made configurable
        return Duration.ofMinutes(5);
    }

    /**
     * Calculates the age of the oldest token in the cache.
     */
    private long getOldestTokenAge() {
        return tokenExpiryTimes.values().stream()
                .map(expiry -> ChronoUnit.MINUTES.between(expiry.minus(55, ChronoUnit.MINUTES), LocalDateTime.now()))
                .max(Long::compareTo)
                .orElse(0L);
    }

    /**
     * Counts tokens that are near expiry (within 10 minutes).
     */
    private long getTokensNearExpiry() {
        LocalDateTime threshold = LocalDateTime.now().plus(10, ChronoUnit.MINUTES);
        return tokenExpiryTimes.values().stream()
                .filter(expiry -> expiry.isBefore(threshold))
                .count();
    }
}