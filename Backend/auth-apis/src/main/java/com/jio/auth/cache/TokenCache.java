package com.jio.auth.cache;

import com.jio.auth.dto.IntrospectResponse;


public interface TokenCache {

    IntrospectResponse get(String token);

    void put(String token, IntrospectResponse response);

    boolean contains(String token);

    void remove(String token);

    boolean isExpired(String token);
}

