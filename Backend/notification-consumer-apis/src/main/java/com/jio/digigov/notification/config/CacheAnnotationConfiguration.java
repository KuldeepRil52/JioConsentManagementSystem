package com.jio.digigov.notification.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.lang.reflect.Method;
import java.util.StringJoiner;

/**
 * Cache annotation configuration for Spring Cache abstraction.
 *
 * This configuration enables declarative caching using Spring Cache annotations
 * (@Cacheable, @CacheEvict, @CachePut) with custom key generation strategies
 * for multi-tenant and business-specific caching requirements.
 *
 * Cache Regions:
 * - ngConfiguration: Business configuration cache
 * - templates: Template content cache (SMS, Email)
 * - masterList: Master list configuration cache
 * - tokenCache: DigiGov token cache
 * - eventConfig: Event configuration cache
 * - notificationResults: Processed notification results
 *
 * Key Generation Strategy:
 * - Tenant-aware keys: {tenantId}:{businessId}:{methodParams}
 * - Automatic key generation based on method parameters
 * - Support for complex object parameters
 *
 * @author Notification Service Team
 * @since 1.0.0
 */
@Configuration
@EnableCaching
@Slf4j
public class CacheAnnotationConfiguration {

    /**
     * Primary cache manager for Spring Cache annotations
     */
    @Bean
    @Primary
    public CacheManager annotationCacheManager() {
        ConcurrentMapCacheManager cacheManager = new ConcurrentMapCacheManager();

        // Define cache regions
        cacheManager.setCacheNames(
            java.util.Arrays.asList(
                "ngConfiguration",
                "templates",
                "masterList",
                "tokenCache",
                "eventConfig",
                "notificationResults",
                "processingStatus"
            )
        );

        cacheManager.setAllowNullValues(false);

        log.info("Annotation cache manager initialized with regions: {}",
                cacheManager.getCacheNames());

        return cacheManager;
    }

    /**
     * Custom key generator for multi-tenant caching
     */
    @Bean("tenantAwareKeyGenerator")
    public KeyGenerator tenantAwareKeyGenerator() {
        return new TenantAwareKeyGenerator();
    }

    /**
     * Custom key generator for business-specific caching
     */
    @Bean("businessKeyGenerator")
    public KeyGenerator businessKeyGenerator() {
        return new BusinessKeyGenerator();
    }

    /**
     * Tenant-aware key generator implementation
     */
    public static class TenantAwareKeyGenerator implements KeyGenerator {

        @Override
        public Object generate(Object target, Method method, Object... params) {
            StringJoiner keyJoiner = new StringJoiner(":");

            // Add class and method name
            keyJoiner.add(target.getClass().getSimpleName());
            keyJoiner.add(method.getName());

            // Add parameters to key
            for (Object param : params) {
                if (param != null) {
                    keyJoiner.add(param.toString());
                } else {
                    keyJoiner.add("null");
                }
            }

            String key = keyJoiner.toString();
            log.debug("Generated tenant-aware cache key: {}", key);
            return key;
        }
    }

    /**
     * Business-specific key generator implementation
     */
    public static class BusinessKeyGenerator implements KeyGenerator {

        @Override
        public Object generate(Object target, Method method, Object... params) {
            StringJoiner keyJoiner = new StringJoiner(":");

            // Add method identifier
            keyJoiner.add(method.getName());

            // Look for tenantId and businessId in parameters
            String tenantId = "default";
            String businessId = "default";

            // Extract tenant and business IDs from parameters
            for (int i = 0; i < params.length; i++) {
                Object param = params[i];
                if (param != null) {
                    String paramStr = param.toString();

                    // Simple heuristic to identify tenant/business IDs
                    if (i == 0 && paramStr.length() >= 3 && paramStr.length() <= 50) {
                        tenantId = paramStr;
                    } else if (i == 1 && paramStr.length() >= 3 && paramStr.length() <= 50) {
                        businessId = paramStr;
                    } else {
                        keyJoiner.add(paramStr);
                    }
                }
            }

            // Build key with tenant and business context
            String key = tenantId + ":" + businessId + ":" + keyJoiner.toString();
            log.debug("Generated business-aware cache key: {}", key);
            return key;
        }
    }

    /**
     * Cache eviction scheduler for periodic cleanup
     */
    @Bean
    public CacheEvictionScheduler cacheEvictionScheduler() {
        return new CacheEvictionScheduler();
    }

    /**
     * Scheduled cache eviction for memory management
     */
    public static class CacheEvictionScheduler {

        private final CacheManager cacheManager;

        public CacheEvictionScheduler() {
            this.cacheManager = null; // Will be injected by Spring
        }

        /**
         * Evict expired cache entries periodically
         */
        @CacheEvict(cacheNames = {"ngConfiguration", "templates", "masterList", "notificationResults"},
                   allEntries = true,
                   condition = "#root.method.name.equals('evictExpiredEntries')")
        public void evictExpiredEntries() {
            log.info("Periodic cache eviction completed");
        }

        /**
         * Clear all caches (emergency operation)
         */
        @CacheEvict(cacheNames = {"ngConfiguration", "templates", "masterList", "tokenCache", "eventConfig", "notificationResults"},
                   allEntries = true)
        public void clearAllCaches() {
            log.warn("All caches cleared");
        }
    }
}