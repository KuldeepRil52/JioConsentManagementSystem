package com.jio.multitranslator.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
@EnableCaching
public class CacheConfig {

    private static final String TRANSLATE_TOKENS_CACHE = "translateTokens";
    private static final String TRANSLATE_CONFIGS_CACHE = "translateConfigs";

    @Bean
    public CacheManager cacheManager() {
        log.info("Initializing cache manager with caches: {}, {}", 
                TRANSLATE_TOKENS_CACHE, TRANSLATE_CONFIGS_CACHE);
        
        ConcurrentMapCacheManager cacheManager = new ConcurrentMapCacheManager(
                TRANSLATE_TOKENS_CACHE,
                TRANSLATE_CONFIGS_CACHE
        );
        
        // Allow dynamic cache creation (useful for development)
        cacheManager.setAllowNullValues(false);
        
        return cacheManager;
    }
}
