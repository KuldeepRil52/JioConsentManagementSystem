package com.jio.digigov.notification.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jio.digigov.notification.config.NotificationGatewayProperties;
import com.jio.digigov.notification.config.RestTemplateConfig;
import com.jio.digigov.notification.entity.NotificationConfig;
import com.jio.digigov.notification.enums.ProviderType;
import com.jio.digigov.notification.dto.digigov.ApproveTemplateRequestDto;
import com.jio.digigov.notification.dto.digigov.ApproveTemplateResponseDto;
import com.jio.digigov.notification.dto.digigov.OnboardTemplateRequestDto;
import com.jio.digigov.notification.dto.digigov.OnboardTemplateResponseDto;
import com.jio.digigov.notification.enums.CredentialType;
import com.jio.digigov.notification.enums.NotificationType;
import com.jio.digigov.notification.exception.DigiGovClientException;
import com.jio.digigov.notification.service.DigiGovClientService;
import com.jio.digigov.notification.service.TokenService;
import com.jio.digigov.notification.util.CircuitBreakerUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.security.SecureRandom;
import java.util.Map;

/**
 * Client service for DigiGov API integration
 * Handles template onboard and approve operations
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DigiGovClientServiceImpl implements DigiGovClientService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final String ALPHANUMERIC_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final RestTemplateConfig restTemplateConfig;
    private final TokenService tokenService;
    private final NotificationGatewayProperties gatewayProperties;
    private final CircuitBreakerUtil circuitBreakerUtil;

    /**
     * Onboard template to DigiGov
     */
    @Override
    public OnboardTemplateResponseDto onboardTemplate(
            OnboardTemplateRequestDto request,
            NotificationConfig config,
            NotificationType notificationType,
            String transactionId) {

        validateDigiGovConfig(config);
        String onboardUrl = determineOnboardUrl(config, notificationType);
        String token = tokenService.generateTokenWithConfig(config, CredentialType.CLIENT).getAccessToken();

        HttpHeaders headers = createApiHeaders(token, config, transactionId);
        HttpEntity<OnboardTemplateRequestDto> requestEntity = new HttpEntity<>(request, headers);

        try {
            log.info("Calling DigiGov onboard API: {}", onboardUrl);
            log.debug("Onboard request payload: {}", request);

            RestTemplate restTemplate = restTemplateConfig.restTemplate(config);
            ResponseEntity<OnboardTemplateResponseDto> response = restTemplate.postForEntity(
                    onboardUrl, requestEntity, OnboardTemplateResponseDto.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                OnboardTemplateResponseDto responseBody = response.getBody();
                log.info("Template onboarded successfully, templateId: {}, status: {}",
                        responseBody.getTemplateId(), responseBody.getStatus());
                return responseBody;
            } else {
                log.error("Onboard API returned non-success status: {}", response.getStatusCode());
                throw DigiGovClientException.forOnboard(
                        response.getStatusCode().value(),
                        "Onboard API returned non-success status"
                );
            }

        } catch (HttpClientErrorException e) {
            String errorMessage = parseDigiGovError(e.getResponseBodyAsString());
            log.error("Client error calling onboard API | HttpStatus: {} ({}) | Response: {} | ErrorMessage: {} | TxnId: {}",
                    e.getStatusCode(), e.getStatusCode().value(), e.getResponseBodyAsString(), e.getMessage(), transactionId, e);
            throw DigiGovClientException.forOnboard(
                    e.getStatusCode().value(),
                    errorMessage
            );

        } catch (HttpServerErrorException e) {
            String errorMessage = parseDigiGovError(e.getResponseBodyAsString());
            log.error("Server error calling onboard API | HttpStatus: {} ({}) | Response: {} | ErrorMessage: {} | TxnId: {}",
                    e.getStatusCode(), e.getStatusCode().value(), e.getResponseBodyAsString(), e.getMessage(), transactionId, e);
            throw DigiGovClientException.forOnboard(
                    e.getStatusCode().value(),
                    errorMessage
            );

        } catch (Exception e) {
            log.error("Unexpected error calling onboard API");
            throw DigiGovClientException.forOnboard(500, "Network error: " + e.getMessage());
        }
    }

    /**
     * Approve template in DigiGov
     */
    @Override
    public ApproveTemplateResponseDto approveTemplate(
            ApproveTemplateRequestDto request,
            NotificationConfig config,
            Map<String, String> header,
            NotificationType notificationType,
            String transactionId) {

        validateDigiGovConfig(config);
        String approveUrl = determineApproveUrl(config, notificationType);
        String token = tokenService.generateTokenWithConfig(config, CredentialType.ADMIN).getAccessToken();

        HttpHeaders headers = createApiHeaders(token, config, transactionId);
        HttpEntity<ApproveTemplateRequestDto> requestEntity = new HttpEntity<>(request, headers);

        try {
            log.info("Calling DigiGov approve API: {} for templateId: {}", approveUrl, request.getTemplateId());
            log.debug("Approve request payload: {}", request);

            RestTemplate restTemplate = restTemplateConfig.restTemplate(config);

            // First get the raw response as String to log it
            ResponseEntity<String> rawResponse = restTemplate.postForEntity(
                    approveUrl, requestEntity, String.class);
            log.info("DigiGov approve API raw response: {}", rawResponse.getBody());

            // Now parse it properly
            ResponseEntity<ApproveTemplateResponseDto> response = restTemplate.postForEntity(
                    approveUrl, requestEntity, ApproveTemplateResponseDto.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                ApproveTemplateResponseDto responseBody = response.getBody();
                log.info("Template approval response - templateId: {}, status: '{}', Full response: {}",
                        responseBody.getTemplateId(), responseBody.getCombinedStatus(), responseBody);
                return responseBody;
            } else {
                log.error("Approve API returned non-success status: {}", response.getStatusCode());
                throw DigiGovClientException.forApprove(
                        response.getStatusCode().value(),
                        "Approve API returned non-success status"
                );
            }

        } catch (HttpClientErrorException e) {
            String errorMessage = parseDigiGovError(e.getResponseBodyAsString());
            log.error("Client error calling approve API: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw DigiGovClientException.forApprove(
                    e.getStatusCode().value(),
                    errorMessage
            );

        } catch (HttpServerErrorException e) {
            String errorMessage = parseDigiGovError(e.getResponseBodyAsString());
            log.error("Server error calling approve API: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw DigiGovClientException.forApprove(
                    e.getStatusCode().value(),
                    errorMessage
            );

        } catch (Exception e) {
            log.error("Unexpected error calling approve API");
            throw DigiGovClientException.forApprove(500, "Network error: " + e.getMessage());
        }
    }

    /**
     * Initialize OTP via DigiGov
     */
    @Override
    public Map<String, Object> initOTP(
            Map<String, Object> request,
            NotificationConfig config,
            String token,
            Map<String, String> headers) {

        validateDigiGovConfig(config);
        String initUrl = buildApiUrl(config.getConfigurationJson().getBaseUrl(), gatewayProperties.getEndpoints().getOtp().getInit());
        String transactionId = headers.getOrDefault("txn", "TXN9A72B5C8D4E1F6G3H7J2K1M0");
        HttpHeaders httpHeaders = createOtpApiHeaders(token, config, transactionId, headers);
        HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(request, httpHeaders);

        return executeApiCall(
            "OTP init",
            initUrl,
            requestEntity,
            request,
            config,
            Map.class,
            DigiGovClientException::forOtpInit
        );
    }

    private HttpHeaders createOtpApiHeaders(String token, NotificationConfig config, String transactionId, Map<String, String> headers) {
        HttpHeaders httpHeaders = createApiHeaders(token, config, transactionId);

        if (headers != null) {
            headers.forEach((key, value) -> {
                if (!"authorization".equalsIgnoreCase(key)) {
                    httpHeaders.set(key, value);
                }
            });
        }

        return httpHeaders;
    }

    /**
     * Verify OTP via DigiGov
     */
    @Override
    public Map<String, Object> verifyOTP(
            Map<String, Object> request,
            NotificationConfig config,
            String token,
            Map<String, String> headers) {

        validateDigiGovConfig(config);
        String verifyUrl = buildApiUrl(config.getConfigurationJson().getBaseUrl(), gatewayProperties.getEndpoints().getOtp().getVerify());
        String transactionId = headers.getOrDefault("txn", "TXN9A72B5C8D4E1F6G3H7J2K1M1");
        HttpHeaders httpHeaders = createOtpApiHeaders(token, config, transactionId, headers);
        HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(request, httpHeaders);

        return executeApiCall(
            "OTP verify",
            verifyUrl,
            requestEntity,
            request,
            config,
            Map.class,
            DigiGovClientException::forOtpVerify
        );
    }

    /**
     * Send notification via DigiGov
     */
    @Override
    public Map<String, Object> sendNotification(
            MultiValueMap<String, Object> request,
            NotificationConfig config,
            String token,
            Map<String, String> headers) {

        validateDigiGovConfig(config);
        String sendUrl = buildApiUrl(config.getConfigurationJson().getBaseUrl(), gatewayProperties.getEndpoints().getNotification().getSend());
        String transactionId = headers.getOrDefault("txn", "TXN9A72B5C8D4E1F6G3H7J2K1M0");
        log.info("transactionId in sendNotification: {}", transactionId);

        HttpHeaders httpHeaders = createNotificationApiHeaders(token, config, transactionId);
        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(request, httpHeaders);

        return executeNotificationApiCall(sendUrl, requestEntity, request, config);
    }

    private HttpHeaders createNotificationApiHeaders(String token, NotificationConfig config, String transactionId) {
        HttpHeaders httpHeaders = createApiHeaders(token, config, transactionId);
        httpHeaders.setContentType(MediaType.MULTIPART_FORM_DATA);
        log.info("HTTP Headers in sendNotification: {}", httpHeaders);
        return httpHeaders;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> executeNotificationApiCall(String sendUrl, HttpEntity<MultiValueMap<String, Object>> requestEntity,
                                                          MultiValueMap<String, Object> request, NotificationConfig config) {
        try {
            log.info("Calling DigiGov send notification API: {}", sendUrl);
            log.debug("Send notification requestEntity: {}", requestEntity);

            RestTemplate restTemplate = restTemplateConfig.restTemplate(config);
            log.info("RestTemplate in sendNotification: {}", restTemplate);
            ResponseEntity<Map> response = restTemplate.postForEntity(sendUrl, requestEntity, Map.class);
            log.info("Response from DigiGov send notification API: {}", response);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> responseBody = response.getBody();
                log.info("Notification sent successfully: {}", responseBody);
                return responseBody;
            } else {
                log.error("Send notification API returned non-success status: {}", response.getStatusCode());
                throw DigiGovClientException.forSendNotification(
                    response.getStatusCode().value(),
                    "Send notification API returned non-success status"
                );
            }

        } catch (HttpClientErrorException e) {
            String errorMessage = parseDigiGovError(e.getResponseBodyAsString());
            log.error("Client error calling send notification API: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw DigiGovClientException.forSendNotification(
                e.getStatusCode().value(),
                errorMessage
            );

        } catch (HttpServerErrorException e) {
            String errorMessage = parseDigiGovError(e.getResponseBodyAsString());
            log.error("Server error calling send notification API: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw DigiGovClientException.forSendNotification(
                e.getStatusCode().value(),
                errorMessage
            );

        } catch (Exception e) {
            log.error("Unexpected error calling send notification API");
            throw DigiGovClientException.forSendNotification(500, "Network error: " + e.getMessage());
        }
    }

    /**
     * Common method to execute API calls with standardized error handling
     */
    @SuppressWarnings("unchecked")
    private <T> T executeApiCall(String apiName, String url, HttpEntity<?> requestEntity,
                                Object request, NotificationConfig config, Class<T> responseClass,
                                java.util.function.BiFunction<Integer, String, RuntimeException> exceptionFactory) {
        try {
            log.info("Calling DigiGov {} API: {}", apiName, url);
            log.debug("{} request payload: {}", apiName, request);

            RestTemplate restTemplate = restTemplateConfig.restTemplate(config);
            ResponseEntity<T> response = restTemplate.postForEntity(url, requestEntity, responseClass);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                T responseBody = response.getBody();
                log.info("{} completed successfully: {}", apiName, responseBody);
                return responseBody;
            } else {
                log.error("{} API returned non-success status: {}", apiName, response.getStatusCode());
                throw exceptionFactory.apply(
                    response.getStatusCode().value(),
                    apiName + " API returned non-success status"
                );
            }

        } catch (HttpClientErrorException e) {
            String errorMessage = parseDigiGovError(e.getResponseBodyAsString());
            log.error("Client error calling {} API: {} - {}", apiName, e.getStatusCode(), e.getResponseBodyAsString());
            throw exceptionFactory.apply(
                e.getStatusCode().value(),
                errorMessage
            );

        } catch (HttpServerErrorException e) {
            String errorMessage = parseDigiGovError(e.getResponseBodyAsString());
            log.error("Server error calling {} API: {} - {}", apiName, e.getStatusCode(), e.getResponseBodyAsString());
            throw exceptionFactory.apply(
                e.getStatusCode().value(),
                errorMessage
            );

        } catch (Exception e) {
            log.error("Unexpected error calling {} API", apiName);
            throw exceptionFactory.apply(500, "Network error: " + e.getMessage());
        }
    }

    /**
     * Create HTTP headers for DigiGov API calls
     */
    private HttpHeaders createApiHeaders(String token, NotificationConfig config, String clientTransactionId) {
        // Generate unique 26-character alphanumeric digigovTxn for DigiGov API
        String digigovTxn = generateAlphanumericTransactionId(26);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + token);
        headers.set("sid", config.getConfigurationJson().getSid());
        headers.set("txn", digigovTxn);

        log.info("Created API headers - Client TxnId: {}, DigiGov TxnId: {}, SID: {}",
                clientTransactionId, digigovTxn, config.getConfigurationJson().getSid());
        return headers;
    }

    /**
     * Determine onboard URL based on configuration
     */
    private String determineOnboardUrl(NotificationConfig config, NotificationType notificationType) {
        String baseUrl = config.getConfigurationJson().getBaseUrl();
        if (notificationType == NotificationType.OTPVALIDATOR) {
            return buildApiUrl(baseUrl, gatewayProperties.getEndpoints().getOtp().getOnboard());
        } else if (notificationType == NotificationType.NOTIFICATION) {
            return buildApiUrl(baseUrl, gatewayProperties.getEndpoints().getNotification().getOnboard());
        }
        return null;
    }

    /**
     * Determine approve URL based on configuration
     */
    private String determineApproveUrl(NotificationConfig config, NotificationType notificationType) {
        String baseUrl = config.getConfigurationJson().getBaseUrl();
        if (notificationType == NotificationType.OTPVALIDATOR) {
            return buildApiUrl(baseUrl, gatewayProperties.getEndpoints().getOtp().getApprove());
        } else if (notificationType == NotificationType.NOTIFICATION) {
            return buildApiUrl(baseUrl, gatewayProperties.getEndpoints().getNotification().getApprove());
        }
        return null;
    }

    /**
     * Generate alphanumeric transaction ID of specified length for DigiGov API calls
     *
     * @param length The length of the transaction ID to generate
     * @return A random alphanumeric string of the specified length
     */
    private String generateAlphanumericTransactionId(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(ALPHANUMERIC_CHARS.charAt(SECURE_RANDOM.nextInt(ALPHANUMERIC_CHARS.length())));
        }
        return sb.toString();
    }

    /**
     * Validate that configuration is DigiGov type with required fields.
     */
    private void validateDigiGovConfig(NotificationConfig config) {
        if (config == null) {
            throw new IllegalArgumentException("Configuration cannot be null");
        }

        if (config.getProviderType() != ProviderType.DIGIGOV) {
            throw new IllegalArgumentException("DigiGov client service only supports DigiGov provider, found: " + config.getProviderType());
        }

        if (config.getConfigurationJson() == null) {
            throw new IllegalArgumentException("DigiGov configuration must have configurationJson set");
        }

        var details = config.getConfigurationJson();
        if (details.getBaseUrl() == null || details.getBaseUrl().trim().isEmpty()) {
            throw new IllegalArgumentException("Base URL cannot be empty");
        }
        if (details.getSid() == null || details.getSid().trim().isEmpty()) {
            throw new IllegalArgumentException("SID cannot be empty");
        }

        log.debug("DigiGov configuration validated successfully for configId: {}", config.getConfigId());
    }

    /**
     * Build API URL from base URL and endpoint
     */
    private String buildApiUrl(String baseUrl, String endpoint) {
        if (baseUrl == null || baseUrl.trim().isEmpty()) {
            throw new IllegalArgumentException("Base URL is required");
        }

        if (baseUrl.endsWith("/")) {
            return baseUrl + (endpoint.startsWith("/") ? endpoint.substring(1) : endpoint);
        } else {
            return baseUrl + (endpoint.startsWith("/") ? endpoint : "/" + endpoint);
        }
    }

    /**
     * Parse DigiGov error response and format it for better readability.
     *
     * Expected DigiGov error structure:
     * {
     *   "errorGrp": "...",
     *   "errMap": [
     *     {
     *       "errorDesc": "...",
     *       "errorCode": "..."
     *     }
     *   ]
     * }
     *
     * @param responseBody The raw error response body from DigiGov
     * @return Formatted error message in the form "errorDesc (errorCode)" or original response if parsing fails
     */
    private String parseDigiGovError(String responseBody) {
        if (responseBody == null || responseBody.trim().isEmpty()) {
            return "Unknown error from DigiGov";
        }

        try {
            JsonNode root = OBJECT_MAPPER.readTree(responseBody);

            // Check for errMap array
            if (root.has("errMap") && root.get("errMap").isArray() && root.get("errMap").size() > 0) {
                JsonNode firstError = root.get("errMap").get(0);

                String errorDesc = firstError.has("errorDesc") ?
                    firstError.get("errorDesc").asText() : null;
                String errorCode = firstError.has("errorCode") ?
                    firstError.get("errorCode").asText() : null;

                if (errorDesc != null && errorCode != null) {
                    return String.format("%s (%s)", errorDesc, errorCode);
                } else if (errorDesc != null) {
                    return errorDesc;
                } else if (errorCode != null) {
                    return String.format("Error code: %s", errorCode);
                }
            }

            // If errMap is not available, try to extract any error message
            if (root.has("message")) {
                return root.get("message").asText();
            }
            if (root.has("error")) {
                return root.get("error").asText();
            }

        } catch (Exception e) {
            log.debug("Failed to parse DigiGov error response: {}", e.getMessage());
        }

        // Return original response if parsing fails
        return responseBody;
    }
}