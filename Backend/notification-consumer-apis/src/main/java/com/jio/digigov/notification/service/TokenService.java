package com.jio.digigov.notification.service;

import com.jio.digigov.notification.dto.token.TokenResponseDto;
import com.jio.digigov.notification.entity.NotificationConfig;
import com.jio.digigov.notification.enums.CredentialType;

/**
 * Service for generating DigiGov OAuth tokens
 * Supports both INTERNET and INTRANET network types
 */
public interface TokenService {

    /**
     * Generate OAuth token for DigiGov API access
     * Uses configuration-specific credentials and network settings
     *
     * @return TokenResponseDto containing access_token, expires_in, etc.
     */
    TokenResponseDto generateToken(String tenantId, String businessId, CredentialType type);

    /**
     * Generate token using specific configuration
     *
     * @return TokenResponseDto containing access_token, expires_in, etc.
     */
    TokenResponseDto generateTokenWithConfig(NotificationConfig config, CredentialType type);
}