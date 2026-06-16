package com.jio.digigov.notification.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jio.digigov.notification.config.NotificationGatewayProperties;
import com.jio.digigov.notification.config.RestTemplateConfig;
import com.jio.digigov.notification.entity.NotificationConfig;
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

        String approveUrl = determineApproveUrl(config, notificationType);
        String token = tokenService.generateTokenWithConfig(config, CredentialType.ADMIN).getAccessToken();

        HttpHeaders headers = createApiHeaders(token, config, transactionId);
        HttpEntity<ApproveTemplateRequestDto> requestEntity = new HttpEntity<>(request, headers);

        try {
            log.info("Calling DigiGov approve API: {} for templateId: {}", approveUrl, request.getTemplateId());
            log.debug("Approve request payload: {}", request);

            RestTemplate restTemplate = restTemplateConfig.restTemplate(config);
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

        String initUrl = config.buildApiUrl(gatewayProperties.getEndpoints().getOtp().getInit());
        String transactionId = headers.getOrDefault("txn", "TXN9A72B5C8D4E1F6G3H7J2K1M0");
        HttpHeaders httpHeaders = createApiHeaders(token, config, transactionId);

        // Add custom headers from request
        if (headers != null) {
            headers.forEach((key, value) -> {
                if (!"authorization".equalsIgnoreCase(key)) {
                    httpHeaders.set(key, value);
                }
            });
        }

        HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(request, httpHeaders);

        try {
            log.info("Calling DigiGov OTP init API: {}", initUrl);
            log.debug("Init OTP request payload: {}", request);

            RestTemplate restTemplate = restTemplateConfig.restTemplate(config);
            ResponseEntity<Map> response = restTemplate.postForEntity(
                    initUrl, requestEntity, Map.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> responseBody = response.getBody();
                log.info("OTP initialized successfully: {}", responseBody);
                return responseBody;
            } else {
                log.error("OTP init API returned non-success status: {}", response.getStatusCode());
                throw DigiGovClientException.forOtpInit(
                        response.getStatusCode().value(),
                        "OTP init API returned non-success status"
                );
            }

        } catch (HttpClientErrorException e) {
            String errorMessage = parseDigiGovError(e.getResponseBodyAsString());
            log.error("Client error calling OTP init API: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw DigiGovClientException.forOtpInit(
                    e.getStatusCode().value(),
                    errorMessage
            );

        } catch (HttpServerErrorException e) {
            String errorMessage = parseDigiGovError(e.getResponseBodyAsString());
            log.error("Server error calling OTP init API: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw DigiGovClientException.forOtpInit(
                    e.getStatusCode().value(),
                    errorMessage
            );

        } catch (Exception e) {
            log.error("Unexpected error calling OTP init API");
            throw DigiGovClientException.forOtpInit(500, "Network error: " + e.getMessage());
        }
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

        String verifyUrl = config.buildApiUrl(gatewayProperties.getEndpoints().getOtp().getVerify());
        String transactionId = headers.getOrDefault("txn", "TXN9A72B5C8D4E1F6G3H7J2K1M1");
        HttpHeaders httpHeaders = createApiHeaders(token, config, transactionId);
        // Add custom headers from request
        if (headers != null) {
            headers.forEach((key, value) -> {
                if (!"authorization".equalsIgnoreCase(key)) {
                    httpHeaders.set(key, value);
                }
            });
        }

        HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(request, httpHeaders);

        try {
            log.info("Calling DigiGov OTP verify API: {}", verifyUrl);
            log.debug("Verify OTP request payload: {}", request);

            RestTemplate restTemplate = restTemplateConfig.restTemplate(config);
            ResponseEntity<Map> response = restTemplate.postForEntity(
                    verifyUrl, requestEntity, Map.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> responseBody = response.getBody();
                log.info("OTP verification completed: {}", responseBody);
                return responseBody;
            } else {
                log.error("OTP verify API returned non-success status: {}", response.getStatusCode());
                throw DigiGovClientException.forOtpVerify(
                        response.getStatusCode().value(),
                        "OTP verify API returned non-success status"
                );
            }

        } catch (HttpClientErrorException e) {
            String errorMessage = parseDigiGovError(e.getResponseBodyAsString());
            log.error("Client error calling OTP verify API: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw DigiGovClientException.forOtpVerify(
                    e.getStatusCode().value(),
                    errorMessage
            );

        } catch (HttpServerErrorException e) {
            String errorMessage = parseDigiGovError(e.getResponseBodyAsString());
            log.error("Server error calling OTP verify API: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw DigiGovClientException.forOtpVerify(
                    e.getStatusCode().value(),
                    errorMessage
            );

        } catch (Exception e) {
            log.error("Unexpected error calling OTP verify API");
            throw DigiGovClientException.forOtpVerify(500, "Network error: " + e.getMessage());
        }
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

        String sendUrl = config.buildApiUrl(gatewayProperties.getEndpoints().getNotification().getSend());
        // fetch transaction ID from headers if available, otherwise use default value
        String transactionId = headers.getOrDefault("txn", "TXN9A72B5C8D4E1F6G3H7J2K1M0");
        log.info("transactionId in sendNotification: {}", transactionId);
        HttpHeaders httpHeaders = createApiHeaders(token, config, transactionId);

//        // Add custom headers from request
//        if (headers != null) {
//            headers.forEach((key, value) -> {
//                if (!"authorization".equalsIgnoreCase(key)) {
//                    httpHeaders.set(key, value);
//                }
//            });
//        }
        httpHeaders.setContentType(MediaType.MULTIPART_FORM_DATA);
        log.info("HTTP Headers in sendNotification: {}", httpHeaders);
        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(request, httpHeaders);

        try {
            log.info("Calling DigiGov send notification API: {}", sendUrl);
            log.debug("Send notification requestEntity: {}", requestEntity);

            RestTemplate restTemplate = restTemplateConfig.restTemplate(config);
            log.info("RestTemplate in sendNotification: {}", restTemplate);
            ResponseEntity<Map> response = restTemplate.postForEntity(
                    sendUrl, requestEntity, Map.class);
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
     * Create HTTP headers for DigiGov API calls
     */
    private HttpHeaders createApiHeaders(String token, NotificationConfig config, String clientTransactionId) {
        // Generate unique 26-character alphanumeric digigovTxn for DigiGov API
        String digigovTxn = generateAlphanumericTransactionId(26);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + token);
        headers.set("sid", config.getSid());
        headers.set("txn", digigovTxn);

        log.info("Created API headers - Client TxnId: {}, DigiGov TxnId: {}, SID: {}",
                clientTransactionId, digigovTxn, config.getSid());
        return headers;
    }

    /**
     * Determine onboard URL based on configuration
     */
    private String determineOnboardUrl(NotificationConfig config, NotificationType notificationType) {
        if (notificationType == NotificationType.OTPVALIDATOR) {
            return config.buildApiUrl(gatewayProperties.getEndpoints().getOtp().getOnboard());
        } else if (notificationType == NotificationType.NOTIFICATION) {
            return config.buildApiUrl(gatewayProperties.getEndpoints().getNotification().getOnboard());
        }
        return null;
    }

    /**
     * Determine approve URL based on configuration
     */
    private String determineApproveUrl(NotificationConfig config, NotificationType notificationType) {
        if (notificationType == NotificationType.OTPVALIDATOR) {
            return config.buildApiUrl(gatewayProperties.getEndpoints().getOtp().getApprove());
        } else if (notificationType == NotificationType.NOTIFICATION) {
            return config.buildApiUrl(gatewayProperties.getEndpoints().getNotification().getApprove());
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