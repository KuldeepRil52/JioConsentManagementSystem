package com.jio.auth.cache;

import com.jio.auth.dto.IntrospectResponse;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.stereotype.Component;


public class InMemoryTokenCache implements TokenCache {

    private final Cache<String, IntrospectResponse> cache;

    public InMemoryTokenCache(Cache<String, IntrospectResponse> cache) {
        this.cache = cache;
    }

    @Override
    public IntrospectResponse get(String token) {
        return cache.getIfPresent(token);
    }

    @Override
    public void put(String token, IntrospectResponse response) {
        cache.put(token, response);
    }

    @Override
    public boolean contains(String token) {
        return cache.getIfPresent(token) != null;
    }

    @Override
    public void remove(String token) {
        cache.invalidate(token);
    }

    @Override
    public boolean isExpired(String token) {
        return cache.getIfPresent(token) == null;
    }
}
