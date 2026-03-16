package com.jio.auth.config;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.jio.auth.cache.TokenCache;
import com.jio.auth.cache.InMemoryTokenCache;
import com.jio.auth.dto.IntrospectResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

@Configuration
public class TokenCacheConfig {

    @Bean
    public Cache<String, IntrospectResponse> caffeineCache(
            @Value("${token.cache.maximum-size}") int maximumSize,
            @Value("${token.cache.expire-after-minutes}") int expireAfterMinutes
    ) {
        return Caffeine.newBuilder()
                .maximumSize(maximumSize)
                .expireAfterWrite(expireAfterMinutes, TimeUnit.MINUTES)
                .build();
    }
    @Bean
    @ConditionalOnProperty(name = "token.cache.type", havingValue = "inMemory", matchIfMissing = true)
    public TokenCache inMemoryTokenCache(Cache<String, IntrospectResponse> caffeineCache) {
        return new InMemoryTokenCache(caffeineCache);
    }

}

