package com.jio.multitranslator.service;

import com.jio.multitranslator.constant.Constants;
import com.jio.multitranslator.dto.Status;
import com.jio.multitranslator.dto.request.*;
import com.jio.multitranslator.dto.request.token.PipelineRequestConfig;
import com.jio.multitranslator.dto.request.token.PipelineTask;
import com.jio.multitranslator.dto.request.token.TokenConfig;
import com.jio.multitranslator.dto.request.token.TokenRequest;
import com.jio.multitranslator.dto.request.translate.ExTranslateRequest;
import com.jio.multitranslator.dto.request.translate.InputData;
import com.jio.multitranslator.dto.request.translate.TaskConfig;
import com.jio.multitranslator.dto.request.translate.TranslatePipelineTask;
import com.jio.multitranslator.dto.response.APITranslateResponse;
import com.jio.multitranslator.dto.response.token.ConfigItem;
import com.jio.multitranslator.dto.response.token.InferenceApiKey;
import com.jio.multitranslator.dto.response.token.TokenResponse;
import com.jio.multitranslator.dto.response.translate.OutputItem;
import com.jio.multitranslator.dto.response.translate.TranslateResponse;
import com.jio.multitranslator.entity.TranslateConfig;
import com.jio.multitranslator.entity.TranslateToken;
import com.jio.multitranslator.entity.TranslateTranscation;
import com.jio.multitranslator.exceptions.BodyValidationException;
import com.jio.multitranslator.exceptions.CustomException;
import com.jio.multitranslator.repository.TranslateConfigRepository;
import com.jio.multitranslator.repository.TranslateRepository;
import com.jio.multitranslator.utils.BodyValidation;
import com.jio.multitranslator.utils.RestUtility;
import com.jio.multitranslator.utils.Validation;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.ThreadContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static org.apache.commons.lang3.StringUtils.normalizeSpace;

@Slf4j
@Service
public class TranslateConfigService {

    private final TranslateConfigRepository translateConfigRepository;
    private final TranslateRepository translateRepository;
    private final BodyValidation bodyValidation;
    private final RestUtility restUtility;
    private final NumeralConverterService numeralConverterService;
    private final Validation validation;
    private final TranslateConfigService self;

    @Autowired
    public TranslateConfigService(
            TranslateConfigRepository translateConfigRepository,
            TranslateRepository translateRepository,
            BodyValidation bodyValidation,
            RestUtility restUtility,
            NumeralConverterService numeralConverterService,
            Validation validation,
            @Lazy TranslateConfigService self) {
        this.translateConfigRepository = translateConfigRepository;
        this.translateRepository = translateRepository;
        this.bodyValidation = bodyValidation;
        this.restUtility = restUtility;
        this.numeralConverterService = numeralConverterService;
        this.validation = validation;
        this.self = self;
    }



    /**
     * Creates a new translation configuration.
     *
     * @param request the translation configuration request
     * @param header  request headers containing tenant and business IDs
     * @return the created translation configuration
     * @throws BodyValidationException if validation fails
     */
    public TranslateConfig createTranslateConfig(TranslateConfigRequest request, Map<String, String> header) 
            throws BodyValidationException {
        // Get the request TXN from header for tracking/logging
        String requestTxn = header.get(Constants.TNX_ID);
        
        // Generate a new transaction ID for the service operation
        String txn = UUID.randomUUID().toString();
        
        String tenantId = header.get(Constants.TENANT_ID_HEADER);
        String businessId = header.get(Constants.BUSINESS_ID_HEADER);
        
        log.debug("Validating translation config request - TXN: {}, TXN-ID: {}, TenantId: {}, BusinessId: {}, Provider: {}", 
                requestTxn, txn, tenantId, businessId, request.getConfig().getProvider());
        bodyValidation.validateConfigBody(request);

        String configId = UUID.randomUUID().toString();
        TranslateConfig translateConfig = TranslateConfig.builder()
                .configId(configId)
                .tenantId(tenantId)
                .businessId(businessId)
                .scopeLevel(request.getScopeLevel())
                .config(request.getConfig())
                .build();

        log.debug("Saving translation config to repository - TXN: {}, TXN-ID: {}, ConfigId: {}, BusinessId: {}", 
                requestTxn, txn, configId, businessId);
        TranslateConfig savedConfig = translateConfigRepository.save(translateConfig);
        log.info("Translation config saved successfully - TXN: {}, TXN-ID: {}, ConfigId: {}, BusinessId: {}, Provider: {}", 
                requestTxn, txn, configId, businessId, request.getConfig().getProvider());
        return savedConfig;
    }


    /**
     * Translates the given input text using the configured translation service.
     *
     * @param request the translation request
     * @param header  request headers containing tenant and business IDs
     * @return the translation response
     * @throws BodyValidationException if validation fails
     */
    public APITranslateResponse translateService(TranslateRequest request, Map<String, String> header) 
            throws BodyValidationException {
        // Get the request TXN from header for tracking/logging
        String requestTxn = header.get(Constants.TNX_ID);
        
        // Generate a new transaction ID for the translation operation
        String txn = UUID.randomUUID().toString();
        
        String sourceLanguage = request.getLanguage().getSourceLanguage();
        String targetLanguage = request.getLanguage().getTargetLanguage();
        String tenantId = header.get(Constants.TENANT_ID_HEADER);
        String businessId = header.get(Constants.BUSINESS_ID_HEADER);

        log.debug("Starting translation service - TXN: {}, TXN-ID: {}, Source: {}, Target: {}, BusinessId: {}", 
                requestTxn, txn, sourceLanguage, targetLanguage, businessId);

        validateLanguagePair(txn, sourceLanguage, targetLanguage);
        TranslateConfig transConfig = fetchTranslationConfig(txn, tenantId, businessId);
        if (transConfig == null) {
            return buildFailedResponse(txn, "Translation configuration not found for the given Business ID");
        }

        TranslateToken token = retrieveOrFetchToken(txn, transConfig, sourceLanguage, targetLanguage, businessId);
        if (token == null) {
            return buildFailedResponse(txn, "Failed to generate translation token. Please try again later.");
        }

        log.debug("Performing translation - TXN: {}, TXN-ID: {}, Source: {}, Target: {}", 
                requestTxn, txn, sourceLanguage, targetLanguage);
        return performTranslation(token, request, txn, requestTxn);
    }

    /**
     * Validates the language pair for translation.
     */
    private void validateLanguagePair(String txn, String sourceLanguage, String targetLanguage) 
            throws BodyValidationException {
        log.debug("Validating language pair - TXN: {}, Source: {}, Target: {}", txn, sourceLanguage, targetLanguage);
        bodyValidation.validateTranslate(sourceLanguage, targetLanguage);
    }

    /**
     * Fetches the translation configuration for the given tenant and business IDs.
     */
    private TranslateConfig fetchTranslationConfig(String txn, String tenantId, String businessId) {
        log.debug("Fetching translation configuration - TXN: {}, TenantId: {}, BusinessId: {}", 
                txn, tenantId, businessId);
        TranslateConfig transConfig = translateConfigRepository.getConfigurationDeatils(tenantId, businessId);
        if (transConfig == null) {
            log.error("Translation configuration not found - TXN: {}, TenantId: {}, BusinessId: {}", 
                    txn, tenantId, businessId);
            return null;
        }
        log.debug("Translation configuration found - TXN: {}, ConfigId: {}, Provider: {}", 
                txn, transConfig.getConfigId(), transConfig.getConfig().getProvider());
        return transConfig;
    }

    /**
     * Retrieves or fetches a translation token.
     */
    private TranslateToken retrieveOrFetchToken(String txn, TranslateConfig transConfig, 
                                                String sourceLanguage, String targetLanguage, String businessId) {
        log.debug("Retrieving or fetching translation token - TXN: {}, Source: {}, Target: {}, BusinessId: {}", 
                txn, sourceLanguage, targetLanguage, businessId);
        TranslateToken token = getOrFetchToken(transConfig, sourceLanguage, targetLanguage, businessId);
        if (token == null) {
            log.error("Failed to retrieve or generate token - TXN: {}, Source: {}, Target: {}, BusinessId: {}", 
                    txn, sourceLanguage, targetLanguage, businessId);
            return null;
        }
        log.debug("Token retrieved successfully - TXN: {}, ServiceId: {}", txn, token.getServiceId());
        return token;
    }

    /**
     * Gets an existing token from cache/DB or fetches a new one.
     */
    private TranslateToken getOrFetchToken(TranslateConfig transConfig, String sourceLanguage, 
                                          String targetLanguage, String businessId) {
        log.debug("Checking cache for existing token - BusinessId: {}, Source: {}, Target: {}", 
                businessId, sourceLanguage, targetLanguage);
        TranslateToken token = translateRepository.getTokenFromDB(sourceLanguage, targetLanguage, businessId);
        if (token == null) {
            log.info("Token not found in cache, fetching new token - BusinessId: {}, Source: {}, Target: {}", 
                    businessId, sourceLanguage, targetLanguage);
            token = fetchToken(transConfig, sourceLanguage, targetLanguage);
            if (token != null) {
                log.info("New token fetched and cached - BusinessId: {}, Source: {}, Target: {}, ServiceId: {}", 
                        businessId, sourceLanguage, targetLanguage, token.getServiceId());
            }
        } else {
            log.debug("Token found in cache - BusinessId: {}, Source: {}, Target: {}, ServiceId: {}", 
                    businessId, sourceLanguage, targetLanguage, token.getServiceId());
        }
        return token;
    }

    /**
     * Builds a failed translation response.
     */
    private APITranslateResponse buildFailedResponse(String txn, String message) {
        return APITranslateResponse.builder()
                .txn(txn)
                .status(Status.FAILED)
                .message(message)
                .build();
    }


    /**
     * Fetches a new translation token from the translation service API.
     *
     * @param transConfig    the translation configuration
     * @param sourceLanguage the source language code
     * @param targetLanguage the target language code
     * @return the saved translation token
     * @throws IllegalStateException if token fetching fails
     */
    private TranslateToken fetchToken(TranslateConfig transConfig, String sourceLanguage, String targetLanguage) {
        String tokenUrl = buildTokenUrl(transConfig);
        Map<String, String> tokenRequestHeader = buildTokenHeaders(transConfig);
        TokenRequest tokenRequestBody = buildTokenRequest(transConfig, sourceLanguage, targetLanguage);

        log.info("Fetching token from external API - URL: {}, BusinessId: {}, Source: {}, Target: {}", 
                tokenUrl, transConfig.getBusinessId(), sourceLanguage, targetLanguage);
        
        try {
            TokenResponse tokenResponse = restUtility.queryRemoteService(
                    tokenUrl, tokenRequestHeader, HttpMethod.POST, tokenRequestBody, TokenResponse.class, null);

            validateTokenResponse(tokenResponse);
            
            String callbackUrl = extractCallbackUrl(tokenResponse);
            String serviceId = extractServiceId(tokenResponse);
            String tokenValue = extractTokenValue(tokenResponse);

            log.debug("Token response parsed successfully - BusinessId: {}, ServiceId: {}, CallbackUrl: {}", 
                    transConfig.getBusinessId(), serviceId, callbackUrl);

            TranslateToken storeToken = TranslateToken.builder()
                    .businessId(transConfig.getBusinessId())
                    .sourceLanguage(sourceLanguage)
                    .targetLanguage(targetLanguage)
                    .serviceId(serviceId)
                    .pipelineId(transConfig.getConfig().getPipelineId())
                    .token(tokenValue)
                    .callbackUrl(callbackUrl)
                    .build();

            log.debug("Saving token to repository - BusinessId: {}, Source: {}, Target: {}, ServiceId: {}", 
                    transConfig.getBusinessId(), sourceLanguage, targetLanguage, serviceId);
            TranslateToken savedToken = translateRepository.save(storeToken);
            log.info("Token saved successfully - BusinessId: {}, Source: {}, Target: {}, ServiceId: {}", 
                    transConfig.getBusinessId(), sourceLanguage, targetLanguage, serviceId);
            return savedToken;
        } catch (IllegalStateException e) {
            log.error("Failed to fetch token from API - BusinessId: {}, Source: {}, Target: {}, Error: {}", 
                    transConfig.getBusinessId(), sourceLanguage, targetLanguage, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Builds the token request URL from configuration.
     * Validates URL format and prevents URL injection attacks.
     */
    private String buildTokenUrl(TranslateConfig transConfig) {
        if (transConfig == null || transConfig.getConfig() == null) {
            throw new IllegalStateException("Translation config or config details cannot be null");
        }
        
        String baseUrl = transConfig.getConfig().getApiBaseUrl();
        String endpoint = transConfig.getConfig().getModelPipelineEndpoint();
        
        if (baseUrl == null || baseUrl.isBlank()) {
            throw new IllegalStateException("API base URL cannot be null or empty");
        }
        if (endpoint == null || endpoint.isBlank()) {
            throw new IllegalStateException("Model pipeline endpoint cannot be null or empty");
        }
        
        // Validate URL format to prevent injection
        if (!isValidUrl(baseUrl)) {
            throw new IllegalArgumentException("Invalid base URL format: " + baseUrl);
        }
        
        // Remove trailing slash from baseUrl
        String normalizedBaseUrl = baseUrl.endsWith("/") 
                ? baseUrl.substring(0, baseUrl.length() - 1) 
                : baseUrl;
        
        // Remove leading slash from endpoint and validate
        String normalizedEndpoint = endpoint.startsWith("/") 
                ? endpoint.substring(1) 
                : endpoint;
        
        // Validate endpoint doesn't contain dangerous characters
        if (normalizedEndpoint.contains("..") || normalizedEndpoint.contains("//")) {
            throw new IllegalArgumentException("Invalid endpoint format: " + endpoint);
        }
        
        // Use proper URL construction to avoid injection
        return String.join("/", normalizedBaseUrl, normalizedEndpoint);
    }

    /**
     * Validates URL format to prevent injection attacks.
     */
    private boolean isValidUrl(String url) {
        if (url == null || url.isBlank()) {
            return false;
        }
        try {
            java.net.URI uri = new java.net.URI(url);
            String scheme = uri.getScheme();
            // Only allow http and https protocols
            return "http".equals(scheme) || "https".equals(scheme);
        } catch (java.net.URISyntaxException e) {
            return false;
        }
    }

    /**
     * Builds the token request headers.
     */
    private Map<String, String> buildTokenHeaders(TranslateConfig transConfig) {
        Map<String, String> headers = HashMap.newHashMap(2);
        headers.put(Constants.USER_ID_HEADER, transConfig.getConfig().getUserId());
        headers.put(Constants.ULCA_API_KEY_HEADER, transConfig.getConfig().getApiKey());
        return headers;
    }

    /**
     * Builds the token request body.
     */
    private TokenRequest buildTokenRequest(TranslateConfig transConfig, String sourceLanguage, String targetLanguage) {
        LanguageRequest language = LanguageRequest.builder()
                .sourceLanguage(sourceLanguage)
                .targetLanguage(targetLanguage)
                .build();

        return TokenRequest.builder()
                .pipelineTasks(List.of(
                        PipelineTask.builder()
                                .taskType(Constants.TASK_TYPE_TRANSLATION)
                                .config(TokenConfig.builder()
                                        .language(language)
                                        .build())
                                .build()
                ))
                .pipelineRequestConfig(PipelineRequestConfig.builder()
                        .pipelineId(transConfig.getConfig().getPipelineId())
                        .build())
                .build();
    }

    /**
     * Validates the token response.
     */
    private void validateTokenResponse(TokenResponse tokenResponse) {
        if (tokenResponse == null || tokenResponse.getPipelineInferenceAPIEndPoint() == null) {
            log.error("Invalid token response received - Response is null or missing inference endpoint");
            throw new IllegalStateException("Failed to fetch token from API");
        }
        log.debug("Token response validated successfully");
    }

    /**
     * Extracts the callback URL from token response.
     */
    private String extractCallbackUrl(TokenResponse tokenResponse) {
        if (tokenResponse == null || tokenResponse.getPipelineInferenceAPIEndPoint() == null) {
            throw new IllegalStateException("Token response or inference endpoint cannot be null");
        }
        return tokenResponse.getPipelineInferenceAPIEndPoint().getCallbackUrl();
    }

    /**
     * Extracts the service ID from token response.
     */
    private String extractServiceId(TokenResponse tokenResponse) {
        if (tokenResponse == null || tokenResponse.getPipelineResponseConfig() == null) {
            throw new IllegalStateException("Token response or pipeline response config cannot be null");
        }
        return tokenResponse.getPipelineResponseConfig().stream()
                .filter(prc -> prc != null && prc.getConfig() != null && !prc.getConfig().isEmpty())
                .flatMap(prc -> prc.getConfig().stream())
                .map(ConfigItem::getServiceId)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("ServiceId not found in token response"));
    }

    /**
     * Extracts the token value from token response.
     */
    private String extractTokenValue(TokenResponse tokenResponse) {
        if (tokenResponse == null || tokenResponse.getPipelineInferenceAPIEndPoint() == null) {
            throw new IllegalStateException("Token response or inference endpoint cannot be null");
        }
        return Optional.ofNullable(tokenResponse.getPipelineInferenceAPIEndPoint().getInferenceApiKey())
                .map(InferenceApiKey::getValue)
                .orElseThrow(() -> new IllegalStateException("Token value missing in response"));
    }

    /**
     * Performs the actual translation using the token and service ID.
     *
     * @param token      the translation token
     * @param serviceId  the service ID
     * @param callbackUrl the callback URL for translation API
     * @param request    the translation request
     * @param txn        the transaction ID
     * @return the translation response
     */
    private APITranslateResponse performTranslation(TranslateToken token, TranslateRequest request, String txn, String requestTxn) {
        Map<String, String> translateHeaders = Map.of(Constants.AUTHORIZATION_HEADER, token.getToken());
        ExTranslateRequest translateRequestBody = buildTranslateRequest(request, token.getServiceId());
        String sourceLang = request.getLanguage().getSourceLanguage();
        String targetLang = request.getLanguage().getTargetLanguage();
        int inputSize = request.getInput() != null ? request.getInput().size() : 0;

        log.info("Calling translation API - TXN: {}, TXN-ID: {}, CallbackUrl: {}, Source: {}, Target: {}, InputSize: {}", 
                requestTxn, txn, token.getCallbackUrl(), sourceLang, targetLang, inputSize);
        
        try {
            TranslateResponse response = callTranslationApi(token, translateHeaders, translateRequestBody, txn, sourceLang, targetLang, requestTxn);
            List<OutputItem> processedOutput = processAndSaveTranslation(response, request, txn, sourceLang, targetLang, requestTxn);
            return buildSuccessResponse(txn, processedOutput, sourceLang, targetLang, requestTxn);
        } catch (Exception e) {
            log.error("Error during translation API call - RequestTXN: {}, ServiceTXN: {}, Source: {}, Target: {}, Error: {}", 
                    requestTxn, txn, sourceLang, targetLang, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Calls the translation API and returns the response.
     */
    private TranslateResponse callTranslationApi(TranslateToken token, Map<String, String> translateHeaders,
                                                 ExTranslateRequest translateRequestBody, String txn,
                                                 String sourceLang, String targetLang, String requestTxn) {
        TranslateResponse response = restUtility.queryRemoteService(
                token.getCallbackUrl(), translateHeaders, HttpMethod.POST, 
                translateRequestBody, TranslateResponse.class, null);

        if (response == null || response.getPipelineResponse().isEmpty()) {
            log.error("Translation API returned empty or null response - TXN: {}, TXN-ID: {}, Source: {}, Target: {}", 
                    requestTxn, txn, sourceLang, targetLang);
            throw new IllegalStateException("Translation service returned an empty response. Please try again later.");
        }

        log.debug("Translation response received - RequestTXN: {}, ServiceTXN: {}, OutputSize: {}", 
                requestTxn, txn, response.getPipelineResponse().getFirst().getOutput().size());
        return response;
    }

    /**
     * Processes the translation output and saves the transaction asynchronously.
     */
    private List<OutputItem> processAndSaveTranslation(TranslateResponse response, TranslateRequest request,
                                                      String txn, String sourceLang, String targetLang, String requestTxn) {
        log.debug("Processing translation output - TXN: {}, TXN-ID: {}, Source: {}, Target: {}", 
                requestTxn, txn, sourceLang, targetLang);
        List<OutputItem> processedOutput = processTranslationOutput(response, request);
        response.getPipelineResponse().getFirst().setOutput(processedOutput);

        // Capture tenant ID from ThreadContext before async call
        String tenantId = ThreadContext.get(Constants.TENANT_ID_HEADER);
        log.debug("Saving translation transaction asynchronously - TXN: {}, TXN-ID: {}, Source: {}, Target: {}, TenantId: {}", 
                requestTxn, txn, sourceLang, targetLang, tenantId);
        self.saveTranslationTransactionAsync(request, txn, tenantId, requestTxn);
        return processedOutput;
    }

    /**
     * Builds a successful translation response.
     */
    private APITranslateResponse buildSuccessResponse(String txn, List<OutputItem> processedOutput,
                                                     String sourceLang, String targetLang, String requestTxn) {
        log.info("Translation completed successfully - RequestTXN: {}, ServiceTXN: {}, Source: {}, Target: {}, OutputSize: {}", 
                requestTxn, txn, sourceLang, targetLang, processedOutput.size());

        // Return service-generated TXN in the response
        return APITranslateResponse.builder()
                .txn(txn)
                .status(Status.SUCCESS)
                .message("Translation completed successfully")
                .output(processedOutput)
                .build();
    }

    /**
     * Builds the translation request body.
     */
    private ExTranslateRequest buildTranslateRequest(TranslateRequest request, String serviceId) {
        return ExTranslateRequest.builder()
                .pipelineTasks(List.of(
                        TranslatePipelineTask.builder()
                                .taskType(Constants.TASK_TYPE_TRANSLATION)
                                .config(TaskConfig.builder()
                                        .language(LanguageRequest.builder()
                                                .sourceLanguage(request.getLanguage().getSourceLanguage())
                                                .targetLanguage(request.getLanguage().getTargetLanguage())
                                                .build())
                                        .serviceId(serviceId)
                                        .build())
                                .build()
                ))
                .inputData(InputData.builder()
                        .input(request.getInput())
                        .build())
                .build();
    }

    /**
     * Processes the translation output by mapping IDs and converting numerals.
     */
    private List<OutputItem> processTranslationOutput(TranslateResponse response, TranslateRequest request) {
        if (request == null || request.getInput() == null || request.getInput().isEmpty()) {
            log.warn("Translation request or input is null or empty");
            return List.of();
        }
        int inputSize = request.getInput().size();
        Map<String, List<String>> sourceToIds = request.getInput().stream()
                .collect(Collectors.groupingBy(
                        item -> normalizeSpace(item.getSource()),
                        Collectors.mapping(InputItem::getId, Collectors.toList())
                ));

        Map<String, AtomicInteger> sourceUsage = HashMap.newHashMap(inputSize);
        String sourceLanguage = request.getLanguage().getSourceLanguage();
        String targetLanguage = request.getLanguage().getTargetLanguage();

        return response.getPipelineResponse().getFirst().getOutput().stream()
                .map(item -> {
                    OutputItem newItem = new OutputItem();
                    newItem.setSource(item.getSource());
                    
                    List<String> ids = sourceToIds.getOrDefault(item.getSource(), List.of());
                    if (!ids.isEmpty()) {
                        int idx = sourceUsage
                                .computeIfAbsent(item.getSource(), k -> new AtomicInteger(0))
                                .getAndIncrement();
                        newItem.setId(idx < ids.size() ? ids.get(idx) : ids.getFirst());
                    }
                    
                    newItem.setTarget(numeralConverterService.convert(
                            item.getTarget(), sourceLanguage, targetLanguage));
                    return newItem;
                })
                .toList();
    }

    /**
     * Saves the translation transaction record asynchronously.
     * This is a fire-and-forget operation to avoid blocking the response.
     * 
     * @param request the translation request
     * @param txn the service-generated transaction ID
     * @param tenantId the tenant ID to set in ThreadContext for the async thread
     * @param requestTxn the request transaction ID from header for logging
     */
    @Async("transactionExecutor")
    public void saveTranslationTransactionAsync(TranslateRequest request, String txn, String tenantId, String requestTxn) {
        try {
            // Set tenant ID in ThreadContext for the async thread
            // This is necessary because ThreadContext is thread-local and doesn't propagate to async threads
            if (tenantId != null && !tenantId.isEmpty()) {
                ThreadContext.put(Constants.TENANT_ID_HEADER, tenantId);
            } else {
                log.warn("Tenant ID is null or empty when saving translation transaction asynchronously - TXN: {}, TXN-ID: {}", requestTxn, txn);
            }
            
            TranslateTranscation saveTranslate = TranslateTranscation.builder()
                    .sourceLanguage(request.getLanguage().getSourceLanguage())
                    .targetLanguage(request.getLanguage().getTargetLanguage())
                    .status(Status.SUCCESS)
                    .source(request.getSource())
                    .txn(txn)
                    .build();
            translateRepository.saveTranslate(saveTranslate);
            log.debug("Translation transaction saved asynchronously - TXN: {}, TXN-ID: {}", requestTxn, txn);
        } catch (Exception e) {
            log.error("Failed to save translation transaction asynchronously - TXN: {}, TXN-ID: {}", requestTxn, txn, e);
            // Don't throw exception - this is fire-and-forget
        } finally {
            // Clean up ThreadContext after async operation
            ThreadContext.clearAll();
        }
    }


    /**
     * Updates an existing translation configuration.
     *
     * @param request the translation configuration request with updated values
     * @param header  request headers containing tenant and business IDs
     * @return the updated translation configuration
     * @throws BodyValidationException if validation fails
     * @throws CustomException         if configuration not found
     */
    public TranslateConfig updateTranslateConfig(@Valid TranslateConfigRequest request, Map<String, String> header) 
            throws BodyValidationException {
        // Get the request TXN from header for tracking/logging
        String requestTxn = header.get(Constants.TNX_ID);
        
        // Generate a new transaction ID for the service operation
        String txn = UUID.randomUUID().toString();
        
        String tenantId = header.get(Constants.TENANT_ID_HEADER);
        String businessId = header.get(Constants.BUSINESS_ID_HEADER);
        String provider = request.getConfig().getProvider() != null 
                ? request.getConfig().getProvider().toString() 
                : null;
        
        log.debug("Validating update request - RequestTXN: {}, ServiceTXN: {}, TenantId: {}, BusinessId: {}, Provider: {}", 
                requestTxn, txn, tenantId, businessId, provider);
        bodyValidation.validateConfigBody(request);

        log.debug("Checking if config exists - TXN: {}, TXN-ID: {}, BusinessId: {}, Provider: {}", 
                requestTxn, txn, businessId, provider);
        boolean exists = translateConfigRepository.checkAlreadyExist(businessId, provider);
        if (!exists) {
            log.error("Translation config not found for update - TXN: {}, TXN-ID: {}, TenantId: {}, BusinessId: {}, Provider: {}", 
                    requestTxn, txn, tenantId, businessId, provider);
            throw new CustomException(
                    "Translation configuration not found",
                    HttpStatus.BAD_REQUEST, 
                    "No translation configuration found for the given tenant ID, business ID, and provider");
        }

        log.info("Updating translation config - TXN: {}, TXN-ID: {}, BusinessId: {}, Provider: {}, ScopeLevel: {}", 
                requestTxn, txn, businessId, provider, request.getScopeLevel());
        TranslateConfig updatedConfig = translateConfigRepository.updateConfig(
                businessId, request.getScopeLevel(), request.getConfig());
        log.info("Translation config updated successfully - TXN: {}, TXN-ID: {}, ConfigId: {}, BusinessId: {}, Provider: {}", 
                requestTxn, txn, updatedConfig.getConfigId(), businessId, provider);
        return updatedConfig;
    }

    /**
     * Retrieves translation configuration(s) based on the provided criteria.
     *
     * @param tenantId   the tenant ID
     * @param businessId the business ID (optional)
     * @param provider   the provider name (optional)
     * @return list of translation configurations
     * @throws CustomException if no configuration found
     */
    public List<TranslateConfig> getTranslationConfig(String tenantId, String businessId, String provider) {
        // Get the request TXN from ThreadContext (set by interceptor)
        String requestTxn = ThreadContext.get("TXN_ID");
        
        // Generate a new transaction ID for the service operation
        String txn = UUID.randomUUID().toString();
        
        log.debug("Fetching translation config - RequestTXN: {}, ServiceTXN: {}, TenantId: {}, BusinessId: {}, Provider: {}", 
                requestTxn, txn, tenantId, businessId, provider);
        
        List<TranslateConfig> transConfig = translateConfigRepository.getAllConfig(tenantId, businessId, provider);
        if (transConfig == null || transConfig.isEmpty()) {
            log.error("No translation config found - TXN: {}, TXN-ID: {}, TenantId: {}, BusinessId: {}, Provider: {}", 
                    requestTxn, txn, tenantId, businessId, provider);
            throw new CustomException(
                    "Translation configuration not found",
                    HttpStatus.NOT_FOUND, 
                    "No translation configuration found for the given parameters");
        }
        log.info("Found {} translation config(s) - TXN: {}, TXN-ID: {}, TenantId: {}", 
                transConfig.size(), requestTxn, txn, tenantId);
        return transConfig;
    }

    /**
     * Gets the count of translation configurations for a given tenant.
     *
     * @param tenantId the tenant ID
     * @param requestTxn the request transaction ID from header
     * @return the count of configurations
     * @throws BodyValidationException if validation fails
     */
    public long count(String tenantId, String requestTxn) throws BodyValidationException {
        // Generate a new transaction ID for the service operation
        String txn = UUID.randomUUID().toString();
        
        log.debug("Validating headers for count request - TXN: {}, TXN-ID: {}, TenantId: {}", 
                requestTxn, txn, tenantId);
        validation.validateTenantHeader();
        validation.validateTxnHeader(requestTxn);
        
        log.debug("Fetching count from repository - TXN: {}, TXN-ID: {}, TenantId: {}", 
                requestTxn, txn, tenantId);
        long count = translateConfigRepository.getCount(tenantId);
        log.info("Translation config count retrieved - TXN: {}, TXN-ID: {}, TenantId: {}, Count: {}", 
                requestTxn, txn, tenantId, count);
        return count;
    }
}
