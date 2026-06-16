package com.jio.digigov.notification.service.cache;

import com.jio.digigov.notification.dto.token.TokenResponseDto;
import com.jio.digigov.notification.entity.NotificationConfig;
import com.jio.digigov.notification.enums.CredentialType;
import com.jio.digigov.notification.enums.CacheType;
import com.jio.digigov.notification.service.TokenService;
import com.jio.digigov.notification.service.impl.TokenServiceImpl;
import com.jio.digigov.notification.util.TenantContextHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;

/**
 * Cached token service that wraps the existing TokenService
 * with intelligent caching capabilities.
 *
 * This service provides transparent caching for DigiGov authentication tokens,
 * significantly reducing API calls to DigiGov while maintaining authentication
 * security. It supports both CLIENT and ADMIN credential types with appropriate
 * cache isolation.
 *
 * Key Features:
 * - Transparent caching of token generation operations
 * - Automatic cache invalidation when tokens expire (TTL-based)
 * - Multi-tenant cache isolation with tenant and business ID separation
 * - Support for both CLIENT and ADMIN credential types
 * - Configurable TTL with environment-specific settings
 * - Cache miss fallback to actual DigiGov API calls
 * - Comprehensive error handling and logging
 *
 * Caching Strategy:
 * - Cache CLIENT tokens with key format: "digigov-token"
 * - Cache ADMIN tokens with key format: "digigov-admin-token"
 * - TTL configured per environment (default: 55 minutes)
 * - Cache isolation per tenant and business ID combination
 * - Automatic eviction on token expiry
 *
 * Performance Benefits:
 * - Reduces DigiGov API load for token generation
 * - Improves response times for API calls requiring authentication
 * - Supports high-traffic scenarios with minimal external API impact
 * - Enables efficient scaling across multiple application instances
 *
 * @author Notification Service Team
 * @version 1.0
 * @since 2025-01-26
 */
@Service
@Primary
@RequiredArgsConstructor
@ConditionalOnProperty(name = "cache.enabled", havingValue = "true", matchIfMissing = true)
@Slf4j
public class CachedTokenService implements TokenService {

    private final TokenServiceImpl delegateService;
    private final CacheService<String> cacheService;

    @Override
    public TokenResponseDto generateToken(String tenantId, String businessId, CredentialType type) {
        String cacheKey = buildTokenCacheKey(type);

        try {
            // Check cache first
            Optional<String> cachedToken = cacheService.get(cacheKey, tenantId, businessId);

            if (cachedToken.isPresent()) {
                log.debug("Cache hit for token: type: {}, tenant: {}, business: {}",
                         type, tenantId, businessId);
                // Return as TokenResponseDto for consistency
                return TokenResponseDto.builder()
                        .accessToken(cachedToken.get())
                        .success(true)
                        .build();
            }

            // Cache miss - generate token using delegate service
            log.debug("Cache miss for token: type: {}, tenant: {}, business: {} - calling DigiGov API",
                     type, tenantId, businessId);

            TokenResponseDto tokenResponse = delegateService.generateToken(tenantId, businessId, type);

            // Cache the token with dynamic TTL based on expires_in
            if (tokenResponse.getExpiresIn() != null && tokenResponse.getExpiresIn() > 0) {
                Duration dynamicTtl = Duration.ofSeconds(tokenResponse.getExpiresIn());
                cacheService.put(cacheKey, tokenResponse.getAccessToken(), tenantId, businessId, dynamicTtl);
                log.info("Cached token with dynamic TTL of {}s for type: {}, tenant: {}, business: {}",
                         tokenResponse.getExpiresIn(), type, tenantId, businessId);
            } else {
                // Fallback to static TTL if expires_in not available
                CacheType cacheType = getCacheTypeForCredentialType(type);
                cacheService.put(cacheKey, tokenResponse.getAccessToken(), tenantId, businessId, cacheType);
                log.warn("No expires_in from DigiGov, using static TTL for type: {}, tenant: {}, business: {}",
                         type, tenantId, businessId);
            }

            return tokenResponse;

        } catch (Exception e) {
            log.error("Error retrieving cached token for type: {}, tenant: {}, business: {}: {}",
                     type, tenantId, businessId, e.getMessage(), e);

            // Fallback to direct API access
            log.warn("Falling back to direct DigiGov API for token: type: {}, tenant: {}, business: {}",
                    type, tenantId, businessId);

            return delegateService.generateToken(tenantId, businessId, type);
        }
    }

    @Override
    public TokenResponseDto generateTokenWithConfig(NotificationConfig config, CredentialType type) {
        String tenantId = getTenantId();
        String businessId = config.getBusinessId();
        String cacheKey = buildTokenCacheKey(type);

        try {
            // Check cache first
            Optional<String> cachedToken = cacheService.get(cacheKey, tenantId, businessId);

            if (cachedToken.isPresent()) {
                log.debug("Cache hit for token with config: type: {}, tenant: {}, business: {}, configId: {}",
                         type, tenantId, businessId, config.getConfigId());
                // Return as TokenResponseDto for consistency
                return TokenResponseDto.builder()
                        .accessToken(cachedToken.get())
                        .success(true)
                        .build();
            }

            // Cache miss - generate token using delegate service
            log.debug("Cache miss for token with config: type: {}, tenant: {}, business: {}, configId: {} - calling DigiGov API",
                     type, tenantId, businessId, config.getConfigId());

            TokenResponseDto tokenResponse = delegateService.generateTokenWithConfig(config, type);

            // Cache the token with dynamic TTL based on expires_in
            if (tokenResponse.getExpiresIn() != null && tokenResponse.getExpiresIn() > 0) {
                Duration dynamicTtl = Duration.ofSeconds(tokenResponse.getExpiresIn());
                cacheService.put(cacheKey, tokenResponse.getAccessToken(), tenantId, businessId, dynamicTtl);
                log.info("Generated and cached token with dynamic TTL of {}s for type: {}, tenant: {}, business: {}, configId: {}",
                         tokenResponse.getExpiresIn(), type, tenantId, businessId, config.getConfigId());
            } else {
                // Fallback to static TTL if expires_in not available
                CacheType cacheType = getCacheTypeForCredentialType(type);
                cacheService.put(cacheKey, tokenResponse.getAccessToken(), tenantId, businessId, cacheType);
                log.warn("No expires_in from DigiGov, using static TTL for type: {}, tenant: {}, business: {}, configId: {}",
                         type, tenantId, businessId, config.getConfigId());
            }

            return tokenResponse;

        } catch (Exception e) {
            log.error("Error retrieving cached token with config: type: {}, tenant: {}, business: {}, configId: {}: {}",
                     type, tenantId, businessId, config.getConfigId(), e.getMessage(), e);

            // Fallback to direct API access
            log.warn("Falling back to direct DigiGov API for token with config: type: {}, tenant: {}, business: {}, configId: {}",
                    type, tenantId, businessId, config.getConfigId());

            return delegateService.generateTokenWithConfig(config, type);
        }
    }

    /**
     * Evicts a specific token from the cache.
     *
     * @param tenantId Tenant identifier
     * @param businessId Business identifier
     * @param type Credential type
     */
    public void evictToken(String tenantId, String businessId, CredentialType type) {
        String cacheKey = buildTokenCacheKey(type);
        cacheService.evict(cacheKey, tenantId, businessId);

        log.info("Evicted token from cache for type: {}, tenant: {}, business: {}",
                type, tenantId, businessId);
    }

    /**
     * Evicts a token for the current tenant context.
     *
     * @param businessId Business identifier
     * @param type Credential type
     */
    public void evictToken(String businessId, CredentialType type) {
        String tenantId = getTenantId();
        evictToken(tenantId, businessId, type);
    }

    /**
     * Evicts all tokens for a tenant-business combination.
     *
     * @param tenantId Tenant identifier
     * @param businessId Business identifier
     */
    public void evictAllTokens(String tenantId, String businessId) {
        cacheService.evictByType(CacheType.DIGIGOV_TOKEN, tenantId, businessId);
        cacheService.evictByType(CacheType.DIGIGOV_ADMIN_TOKEN, tenantId, businessId);

        log.info("Evicted all tokens from cache for tenant: {}, business: {}", tenantId, businessId);
    }

    /**
     * Checks if a token can be preloaded into the cache (cache warming).
     *
     * @param tenantId Tenant identifier
     * @param businessId Business identifier
     * @param type Credential type
     * @return true if token was successfully preloaded
     */
    public boolean canPreloadToken(String tenantId, String businessId, CredentialType type) {
        try {
            // This will generate the token and cache it
            generateToken(tenantId, businessId, type);
            log.debug("Successfully preloaded token for type: {}, tenant: {}, business: {}",
                     type, tenantId, businessId);
            return true;
        } catch (Exception e) {
            log.warn("Failed to preload token for type: {}, tenant: {}, business: {}: {}",
                    type, tenantId, businessId, e.getMessage());
            return false;
        }
    }

    /**
     * Refreshes a cached token by evicting and regenerating.
     *
     * @param tenantId Tenant identifier
     * @param businessId Business identifier
     * @param type Credential type
     * @return refreshed token response
     */
    public TokenResponseDto refreshToken(String tenantId, String businessId, CredentialType type) {
        log.debug("Refreshing token cache for type: {}, tenant: {}, business: {}",
                 type, tenantId, businessId);

        // Evict from cache first
        evictToken(tenantId, businessId, type);

        // Generate fresh token (will automatically cache)
        return generateToken(tenantId, businessId, type);
    }

    /**
     * Checks if a token exists in cache.
     *
     * @param tenantId Tenant identifier
     * @param businessId Business identifier
     * @param type Credential type
     * @return true if token exists in cache
     */
    public boolean isTokenCached(String tenantId, String businessId, CredentialType type) {
        String cacheKey = buildTokenCacheKey(type);
        return cacheService.exists(cacheKey, tenantId, businessId);
    }

    /**
     * Gets cache statistics for token caching.
     *
     * @param type Credential type
     * @return cache statistics
     */
    public Object getTokenCacheStats(CredentialType type) {
        CacheType cacheType = getCacheTypeForCredentialType(type);
        return cacheService.getStats(cacheType);
    }

    /**
     * Build cache key for token based on credential type
     */
    private String buildTokenCacheKey(CredentialType type) {
        CacheType cacheType = getCacheTypeForCredentialType(type);
        return cacheType.getKey();
    }

    /**
     * Map credential type to cache type
     */
    private CacheType getCacheTypeForCredentialType(CredentialType type) {
        switch (type) {
            case CLIENT:
                return CacheType.DIGIGOV_TOKEN;
            case ADMIN:
                return CacheType.DIGIGOV_ADMIN_TOKEN;
            default:
                return CacheType.DIGIGOV_TOKEN;
        }
    }

    /**
     * Get tenant ID from context or default
     */
    private String getTenantId() {
        return TenantContextHolder.getTenantId();
    }
}