package com.wso2wrapper.credentials_generation.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.wso2wrapper.credentials_generation.constants.ErrorCodes;
import com.wso2wrapper.credentials_generation.constants.ExternalApiConstants;
import com.wso2wrapper.credentials_generation.dto.OnboardRequestDto;
import com.wso2wrapper.credentials_generation.dto.ApiSubscriptionRequest;
import com.wso2wrapper.credentials_generation.dto.ApplicationRequest;
import com.wso2wrapper.credentials_generation.dto.KeysRequest;
import com.wso2wrapper.credentials_generation.dto.entity.WSO2_BusinessApplication;
import com.wso2wrapper.credentials_generation.dto.entity.WSO2_Data_Processor;
import com.wso2wrapper.credentials_generation.dto.response.ApplicationResponse;
import com.wso2wrapper.credentials_generation.dto.response.KeyResponse;
import com.wso2wrapper.credentials_generation.dto.response.TokenResponse;
import com.wso2wrapper.credentials_generation.exception.ApiException;
import org.springframework.core.env.Environment;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.net.InetAddress;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class ExternalApiClient {

    private static final Logger log = LoggerFactory.getLogger(ExternalApiClient.class);

    private final RestTemplate restTemplate;
    private final MongoTemplate cmsMongoTemplate;
    private final Environment environment;

    public ExternalApiClient(RestTemplate restTemplate, MongoTemplate cmsMongoTemplate, Environment environment) {
        this.restTemplate = restTemplate;
        this.cmsMongoTemplate = cmsMongoTemplate;
        this.environment = environment;
    }

    private String createLog(String username, String activity, String result) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss"));
        String sourceIp = "Unknown";
        try {
            sourceIp = InetAddress.getLocalHost().getHostAddress();
        } catch (Exception ignored) {}

        return String.format(
                "Timestamp: %s | SourceIP: %s | Username: %s | Activity: %s | Result: %s",
                timestamp,
                sourceIp,
                username,
                activity,
                result
        );
    }

    /**
     * Calls the WSO2 token API and returns access token.
     */
    public String callTokenApi(String username, String password) {
        final String activity = "Token Generation";
        try {
        HttpHeaders tokenHeaders = new HttpHeaders();
        tokenHeaders.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        String auth_token= environment.getProperty(ExternalApiConstants.ENV_TOKEN_AUTH);
        tokenHeaders.set("Authorization", auth_token);

        MultiValueMap<String, String> tokenBody = new LinkedMultiValueMap<>();
        tokenBody.add("grant_type", ExternalApiConstants.TOKEN_GRANT_TYPE);
        tokenBody.add("scope", ExternalApiConstants.TOKEN_SCOPE);
        tokenBody.add("username", username);
        tokenBody.add("password", password);

        HttpEntity<MultiValueMap<String, String>> tokenRequest = new HttpEntity<>(tokenBody, tokenHeaders);
        ResponseEntity<TokenResponse> response = restTemplate.exchange(
                environment.getProperty(ExternalApiConstants.ENV_TOKEN_API),
                HttpMethod.POST,
                tokenRequest,
                TokenResponse.class
        );

        TokenResponse tokenResponse = response.getBody();
        if (tokenResponse == null || tokenResponse.getAccessToken() == null || tokenResponse.getAccessToken().isEmpty()) {
            log.error(createLog(username, activity, "Failed"));
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, ErrorCodes.ERR_WS_TOKEN_FAILED);
        }
        log.info(createLog(username, activity, "Success"));
        return tokenResponse.getAccessToken();
    } catch (Exception ex) {
        log.error(createLog(username, activity, "Failed - " + ex.getMessage()));
        throw ex;
    }
}

    /**
     * Calls WSO2 user registration API.
     */
    public void callRegisterApi(OnboardRequestDto request) {
        final String username = request.getTenantName();
        final String activity = "User Registration";
        try {
        HttpHeaders registerHeaders = new HttpHeaders();
        registerHeaders.setContentType(MediaType.APPLICATION_JSON);
        String token = environment.getProperty(ExternalApiConstants.ENV_REGISTER_TOKEN);
        registerHeaders.set("Authorization", token);
        registerHeaders.set("orgEmail", request.getTenantName().toLowerCase() + "@example.com");

        String generatedPassword = request.getTenantName() + "@123";

        Map<String, Object> userMap = new HashMap<>();
        userMap.put("username", request.getTenantName());
        userMap.put("password", generatedPassword);
        userMap.put("realm", ExternalApiConstants.REGISTER_REALM);

        List<Map<String, String>> claims = new ArrayList<>();
        Map<String, String> emailClaim = Map.of(
                "uri", ExternalApiConstants.REGISTER_EMAIL_URI,
                "value", request.getTenantName()
        );
        Map<String, String> givenNameClaim = Map.of(
                "uri", ExternalApiConstants.REGISTER_GIVENNAME_URI,
                "value", ExternalApiConstants.REGISTER_GIVEN_NAME
        );
        claims.add(emailClaim);
        claims.add(givenNameClaim);

        userMap.put("claims", claims);

        Map<String, Object> userPayload = new HashMap<>();
        userPayload.put("user", userMap);
        userPayload.put("properties", new ArrayList<>());

        HttpEntity<Map<String, Object>> registerEntity = new HttpEntity<>(userPayload, registerHeaders);
        ResponseEntity<JsonNode> registerResponse = restTemplate.exchange(
                environment.getProperty(ExternalApiConstants.ENV_REGISTER_API),
                HttpMethod.POST,
                registerEntity,
                JsonNode.class
        );

        if (!registerResponse.getStatusCode().is2xxSuccessful()) {
            log.error(createLog(username, activity, "Failed"));
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, ErrorCodes.ERR_WS_REGISTRATION_FAILED);
        }
            log.info(createLog(username, activity, "Success"));
    }catch (Exception ex) {
            log.error(createLog(username, activity, "Failed - " + ex.getMessage()));
            throw ex;
        }
    }

    /**
     * Calls WSO2 to create an application.
     */
    public ApplicationResponse createApplication(String adminUsername, String accessToken, String applicationName) {
        final String activity = "Application Creation";

        try {
        ApplicationRequest createReq = new ApplicationRequest();
        createReq.setName(applicationName);
        createReq.setThrottlingPolicy(ExternalApiConstants.APP_THROTTLING_POLICY);
        createReq.setDescription(ExternalApiConstants.APP_DESCRIPTION);
        createReq.setTokenType(ExternalApiConstants.APP_TOKEN_TYPE);
        createReq.setGroups(Collections.emptyList());
        createReq.setAttributes(new HashMap<>());
        createReq.setSubscriptionScopes(Collections.emptyList());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Username", adminUsername);
        if (accessToken != null && !accessToken.isBlank()) {
            headers.setBearerAuth(accessToken);
        }

        HttpEntity<ApplicationRequest> entity = new HttpEntity<>(createReq, headers);

        ResponseEntity<ApplicationResponse> response = restTemplate.exchange(
                environment.getProperty(ExternalApiConstants.ENV_CREATE_APPLICATION_URL),
                HttpMethod.POST,
                entity,
                ApplicationResponse.class
        );

        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            log.error(createLog(adminUsername, activity, "Failed"));
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, ErrorCodes.ERR_WS_APP_CREATION_FAILED);
        }
        log.info(createLog(adminUsername, activity, "Success"));
        return response.getBody();
    }catch (Exception ex) {
            log.error(createLog(adminUsername, activity, "Failed - " + ex.getMessage()));
            throw ex;
        }
    }

    /**
     * Calls WSO2 to generate keys for the application.
     */
    public KeyResponse generateKeys(String applicationId, String adminUsername, String accessToken) {
        final String activity = "Key Generation";
        try {
        KeysRequest keysRequest = new KeysRequest();
        keysRequest.setKeyType(ExternalApiConstants.KEY_TYPE);
        keysRequest.setKeyManager(ExternalApiConstants.KEY_MANAGER);
        keysRequest.setGrantTypesToBeSupported(ExternalApiConstants.GRANT_TYPES);
        keysRequest.setCallbackUrl(ExternalApiConstants.CALLBACK_URL);
        keysRequest.setScopes(ExternalApiConstants.SCOPES);
        keysRequest.setValidityTime(ExternalApiConstants.VALIDITY_TIME);
        keysRequest.setAdditionalProperties(Map.of());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Username", adminUsername);
        if (accessToken != null && !accessToken.isBlank()) {
            headers.setBearerAuth(accessToken);
        }

        HttpEntity<KeysRequest> entity = new HttpEntity<>(keysRequest, headers);
        String baseUrl = environment.getProperty(ExternalApiConstants.ENV_GENERATE_KEYS_URL);
        String generateKeysUrl = baseUrl + "/" + applicationId + "/generate-keys";

        ResponseEntity<KeyResponse> response = restTemplate.exchange(
                generateKeysUrl,
                HttpMethod.POST,
                entity,
                KeyResponse.class
        );

        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            log.error(createLog(adminUsername, activity, "Failed"));
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, ErrorCodes.ERR_WS_KEY_GENERATION_FAILED);
        }
        log.info(createLog(adminUsername, activity, "Success"));
        return response.getBody();
    }catch (Exception ex) {
            log.error(createLog(adminUsername, activity, "Failed - " + ex.getMessage()));
            throw ex;
        }
    }

    public void subscribeMultipleApis(WSO2_BusinessApplication wsApp, String accessToken, MongoTemplate tenantTemplate) {
        final String username = wsApp.getBusinessName() != null ? wsApp.getBusinessName() : "system";
        final String activity = "API Subscription (Business Application)";

        try {
        if (wsApp == null || wsApp.getApplicationId() == null) {
            throw new IllegalArgumentException("Application info cannot be null");
        }

        List<String> apiIds = cmsMongoTemplate.findAll(org.bson.Document.class, "wso2_available_apis")
                .stream()
                .map(doc -> doc.getString("apiId"))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        if (apiIds.isEmpty()) {
            throw new IllegalArgumentException("No API IDs found in DB for subscription");
        }

        //  Build subscription request body
        List<ApiSubscriptionRequest> subscriptions = apiIds.stream()
                .map(apiId -> {
                    ApiSubscriptionRequest sub = new ApiSubscriptionRequest();
                    sub.setApplicationId(wsApp.getApplicationId());
                    sub.setApiId(apiId);
                    sub.setThrottlingPolicy(ExternalApiConstants.SUBSCRIPTION_THROTTLING_POLICY);
                    return sub;
                })
                .toList();

        // Call WSO2 subscription API
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(accessToken);

        HttpEntity<List<ApiSubscriptionRequest>> entity = new HttpEntity<>(subscriptions, headers);

        restTemplate.exchange(
                environment.getProperty(ExternalApiConstants.ENV_MULTIPLE_SUBSCRIPTION_URL),
                HttpMethod.POST,
                entity,
                Void.class
        );

        // Save subscribed API IDs in WSO2_BusinessApplication
        wsApp.setSubscribedApiIds(apiIds);
        tenantTemplate.save(wsApp, ExternalApiConstants.DB_WSO2_BUSINESS_APPLICATIONS);
        log.info(createLog(username, activity, "Success"));
    }catch (Exception ex) {
            log.error(createLog(username, activity, "Failed - " + ex.getMessage()));
            throw ex;
        }
    }

    public void subscribeMultipleApis(WSO2_Data_Processor wsApp, String accessToken, MongoTemplate tenantTemplate) {
        final String username = wsApp.getDataProcessorName() != null ? wsApp.getDataProcessorName() : "system";
        final String activity = "API Subscription (Data Processor)";

        try {

        if (wsApp == null || wsApp.getApplicationId() == null) {
            throw new IllegalArgumentException("Application info cannot be null");
        }

        List<String> apiIds = cmsMongoTemplate.findAll(org.bson.Document.class, "wso2_available_apis")
                .stream()
                .map(doc -> doc.getString("apiId"))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        if (apiIds.isEmpty()) {
            throw new IllegalArgumentException("No API IDs found in DB for subscription");
        }

        //  Build subscription request body
        List<ApiSubscriptionRequest> subscriptions = apiIds.stream()
                .map(apiId -> {
                    ApiSubscriptionRequest sub = new ApiSubscriptionRequest();
                    sub.setApplicationId(wsApp.getApplicationId());
                    sub.setApiId(apiId);
                    sub.setThrottlingPolicy(ExternalApiConstants.SUBSCRIPTION_THROTTLING_POLICY);
                    return sub;
                })
                .toList();

        // Call WSO2 subscription API
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(accessToken);

        HttpEntity<List<ApiSubscriptionRequest>> entity = new HttpEntity<>(subscriptions, headers);

        restTemplate.exchange(
                environment.getProperty(ExternalApiConstants.ENV_MULTIPLE_SUBSCRIPTION_URL),
                HttpMethod.POST,
                entity,
                Void.class
        );

        // Save subscribed API IDs in WSO2_BusinessApplication
        wsApp.setSubscribedApiIds(apiIds);
        tenantTemplate.save(wsApp, ExternalApiConstants.DB_WSO2_DATA_PROCESSOR);
        log.info(createLog(username, activity, "Success"));
    }catch (Exception ex) {
            log.error(createLog(username, activity, "Failed - " + ex.getMessage()));
            throw ex;
        }
    }

}






