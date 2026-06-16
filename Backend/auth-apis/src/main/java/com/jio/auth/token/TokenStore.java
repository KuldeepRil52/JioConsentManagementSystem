package com.jio.auth.token;


import java.util.Date;

public interface TokenStore {

    void saveActiveToken(String userId, String jti, Date expiry);

    // Remove token from active tokens (e.g., on logout)
    void removeActiveToken(String userId, String jti);

    // Revoke a token (move to revoked list)
    void revokeToken(String jti, Date expiry);

    // Check if token is revoked
    boolean isTokenRevoked(String jti);

    // Check if token is active for a user
    boolean isTokenActive(String userId, String jti);

    // Count number of active tokens for a user
    int countActiveTokens(String userId);

    // Remove expired tokens for a user
    void removeExpiredTokens(String userId);
}


