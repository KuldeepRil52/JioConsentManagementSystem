package com.jio.digigov.notification.service.impl;
import com.jio.digigov.notification.repository.NotificationConfigRepository;

import com.jio.digigov.notification.config.NotificationGatewayProperties;
import com.jio.digigov.notification.config.RestTemplateConfig;
import com.jio.digigov.notification.enums.CredentialType;
import com.jio.digigov.notification.enums.NetworkType;
import com.jio.digigov.notification.entity.NotificationConfig;
import com.jio.digigov.notification.dto.token.TokenResponseDto;
import com.jio.digigov.notification.exception.TokenGenerationException;
import com.jio.digigov.notification.service.TokenService;
import com.jio.digigov.notification.util.CircuitBreakerUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.Base64;

/**
 * Service for generating DigiGov OAuth tokens
 * Supports both INTERNET and INTRANET network types
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TokenServiceImpl implements TokenService {

    private final RestTemplateConfig restTemplateConfig;
    private final NotificationConfigRepository notificationConfigRepository;
    private final NotificationGatewayProperties gatewayProperties;
    private final CircuitBreakerUtil circuitBreakerUtil;

    /**
     * Generate OAuth token for DigiGov API access
     * Uses configuration-specific credentials and network settings
     */
    @Override
    public TokenResponseDto generateToken(String tenantId, String businessId, CredentialType type) {
        log.info("Generating DigiGov token for tenantId: {}, businessId: {}", tenantId, businessId);

        NotificationConfig config = notificationConfigRepository.findByBusinessIdCustom(businessId)
        .orElseThrow(() -> new IllegalStateException("Configuration not found"));
        // Validation removed(config);

        TokenResponseDto tokenResponse = callTokenAPI(config, type);

        if (!tokenResponse.isSuccess() || tokenResponse.getAccessToken() == null) {
            log.error("Token generation failed for tenantId: {}, businessId: {}, error: {}",
                    tenantId, businessId, tokenResponse.getError());
            throw TokenGenerationException.forError(
                    tokenResponse.getError(),
                    tokenResponse.getErrorDescription()
            );
        }

        log.info("Successfully generated token for tenantId: {}, businessId: {}, tokenType: {}, expiresIn: {}s",
                tenantId, businessId, tokenResponse.getTokenType(), tokenResponse.getExpiresIn());

        return tokenResponse;
    }

    /**
     * Generate token using specific configuration
     */
    @Override
    public TokenResponseDto generateTokenWithConfig(NotificationConfig config, CredentialType type) {
        log.info("Generating token with configuration: {}", config.getConfigId());

        // Validation removed(config);
        TokenResponseDto tokenResponse = callTokenAPI(config, type);
        log.info("Token Response : {}, Success: {}, TokenType: {}, ExpiresIn: {}s",
                config.getConfigId(), tokenResponse.isSuccess(), tokenResponse.getTokenType(), tokenResponse.getExpiresIn());
        if (!tokenResponse.isSuccess() || tokenResponse.getAccessToken() == null) {
            throw TokenGenerationException.forError(
                    tokenResponse.getError(),
                    tokenResponse.getErrorDescription()
            );
        }

        return tokenResponse;
    }

    /**
     * Call DigiGov token API with circuit breaker protection
     */
    private TokenResponseDto callTokenAPI(NotificationConfig config, CredentialType type) {
        String tokenUrl = determineTokenUrl(config);
        NetworkType networkType = (config.getConfigurationJson() != null && config.getConfigurationJson().getNetworkType() != null)
                ? NetworkType.valueOf(config.getConfigurationJson().getNetworkType())
                : NetworkType.INTRANET;
        String serviceName = "digigov-token-" + networkType.name().toLowerCase();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        // based of type set clientId and secret
        if (type == CredentialType.ADMIN) {
            headers.set("Authorization", createBasicAuthHeader(gatewayProperties.getCredentials().getAdmin().getClientId(), gatewayProperties.getCredentials().getAdmin().getClientSecret()));
        } else {
            String clientId = config.getConfigurationJson() != null ? config.getConfigurationJson().getClientId() : null;
            String clientSecret = config.getConfigurationJson() != null ? config.getConfigurationJson().getClientSecret() : null;
            headers.set("Authorization", createBasicAuthHeader(clientId, clientSecret));
        }

        // Add network type header for identification
        if (networkType == NetworkType.INTERNET) {
            headers.set("X-Network-Type", "INTERNET");
        } else {
            headers.set("X-Network-Type", "INTRANET");
        }

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "client_credentials");
        body.add("scope", "read write");

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);

        return circuitBreakerUtil.executeWithCircuitBreaker(
            serviceName,
            () -> {
                try {
                    log.debug("Calling token API: {} for network type: {}", tokenUrl, networkType);

                    RestTemplate restTemplate = restTemplateConfig.restTemplate(config);
                    ResponseEntity<TokenResponseDto> response = restTemplate.postForEntity(
                            tokenUrl, request, TokenResponseDto.class);

                    if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                        TokenResponseDto tokenResponse = response.getBody();
                        tokenResponse.setSuccess(true);
                        return tokenResponse;
                    } else {
                        log.error("Token API returned non-success status: {}", response.getStatusCode());
                        throw new RuntimeException("Token API returned non-success status: " + response.getStatusCode());
                    }

                } catch (HttpClientErrorException e) {
                    log.error("Client error calling token API: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
                    throw new RuntimeException("Client error: " + e.getResponseBodyAsString());

                } catch (HttpServerErrorException e) {
                    log.error("Server error calling token API: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
                    throw new RuntimeException("Server error: " + e.getResponseBodyAsString());

                } catch (Exception e) {
                    log.error("Unexpected error calling token API");
                    throw new RuntimeException("Network error: " + e.getMessage());
                }
            },
            () -> {
                log.warn("Circuit breaker fallback triggered for token service: {}", serviceName);
                return TokenResponseDto.builder()
                        .success(false)
                        .error("circuit_breaker_open")
                        .errorDescription("DigiGov token service is currently unavailable (circuit breaker open)")
                        .build();
            }
        );
    }

    /**
     * Determine token URL based on configuration
     */
    private String determineTokenUrl(NotificationConfig config) {
        String baseUrl = config.getConfigurationJson() != null ? config.getConfigurationJson().getBaseUrl() : null;
        String endpoint = gatewayProperties.getEndpoints().getToken();

        if (baseUrl == null || baseUrl.trim().isEmpty()) {
            throw new IllegalArgumentException("Base URL is required");
        }

        // Build full URL
        if (baseUrl.endsWith("/")) {
            return baseUrl + (endpoint.startsWith("/") ? endpoint.substring(1) : endpoint);
        } else {
            return baseUrl + (endpoint.startsWith("/") ? endpoint : "/" + endpoint);
        }
    }

    /**
     * Create Basic Authentication header
     */
    private String createBasicAuthHeader(String clientId, String clientSecret) {
        String credentials = clientId + ":" + clientSecret;
        String encodedCredentials = Base64.getEncoder().encodeToString(credentials.getBytes());
        return "Basic " + encodedCredentials;
    }
}