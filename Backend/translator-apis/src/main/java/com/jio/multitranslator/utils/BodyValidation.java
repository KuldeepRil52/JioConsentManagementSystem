package com.jio.multitranslator.utils;

import com.jio.multitranslator.constant.Constants;
import com.jio.multitranslator.constant.ErrorCodes;
import com.jio.multitranslator.dto.ProviderType;
import com.jio.multitranslator.dto.request.Config;
import com.jio.multitranslator.dto.request.TranslateConfigRequest;
import com.jio.multitranslator.exceptions.BodyValidationException;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class BodyValidation {

    /**
     * Validates the translation configuration request body based on provider type.
     *
     * @param request the translation configuration request
     * @throws BodyValidationException if validation fails
     */
    public void validateConfigBody(TranslateConfigRequest request) throws BodyValidationException {
        List<Map<String, String>> errors = new ArrayList<>(5);
        
        if (request == null) {
            log.warn("Translation config request is null");
            errors.add(createErrorDetails(ErrorCodes.JCMPT008, "Request cannot be null"));
            throw new BodyValidationException(errors);
        }
        
        Config config = request.getConfig();
        if (config == null) {
            log.warn("Config is null in translation config request");
            errors.add(createErrorDetails(ErrorCodes.JCMPT008, "Config cannot be null"));
            throw new BodyValidationException(errors);
        }
        
        ProviderType provider = config.getProvider();
        if (provider == null) {
            log.warn("Provider type is null in config");
            errors.add(createErrorDetails("UNKNOWN_PROVIDER", "Provider type cannot be null"));
            throw new BodyValidationException(errors);
        }

        log.debug("Validating config body for provider: {}", provider);

        if (ProviderType.BHASHINI.equals(provider)) {
            validateBhashiniConfig(config, errors);
        } else if (ProviderType.MICROSOFT.equals(provider)) {
            validateMicrosoftConfig(config, errors);
        } else {
            log.warn("Unknown provider type: {}", provider);
            String errorMessage = "Unknown provider type: " + provider;
            errors.add(createErrorDetails("UNKNOWN_PROVIDER", errorMessage));
        }

        if (!errors.isEmpty()) {
            log.warn("Config body validation failed - Provider: {}, ErrorCount: {}", provider, errors.size());
            throw new BodyValidationException(errors);
        }
        
        log.debug("Config body validation passed - Provider: {}", provider);
    }

    /**
     * Validates Bhashini-specific configuration fields.
     */
    private void validateBhashiniConfig(Config config, List<Map<String, String>> errors) {
        validateField(config.getModelPipelineEndpoint(), ErrorCodes.JCMPT008, Constants.MODELPIPELINEENDPOINT, errors);
        validateField(config.getCallbackUrl(), ErrorCodes.JCMPT009, Constants.CALLBACKURL, errors);
        validateField(config.getUserId(), ErrorCodes.JCMPT010, Constants.USERID, errors);
        validateField(config.getApiKey(), ErrorCodes.JCMPT011, Constants.APIKEY, errors);
        validateField(config.getPipelineId(), ErrorCodes.JCMPT012, Constants.PIPELINEID, errors);
    }

    /**
     * Validates Microsoft-specific configuration fields.
     */
    private void validateMicrosoftConfig(Config config, List<Map<String, String>> errors) {
        validateField(config.getSubscriptionKey(), ErrorCodes.JCMPT013, Constants.SUBSCRIPTIONKEY, errors);
        validateField(config.getRegion(), ErrorCodes.JCMPT014, Constants.REGION, errors);
    }

    /**
     * Validates a single field and adds error if blank or null.
     */
    private void validateField(String field, String errorCode, String fieldName, List<Map<String, String>> errors) {
        if (field == null || field.isBlank()) {
            errors.add(createErrorDetails(errorCode, fieldName));
        }
    }

    /**
     * Creates error details map.
     */
    private Map<String, String> createErrorDetails(String errorCode, String message) {
        Map<String, String> errorMap = HashMap.newHashMap(2);
        errorMap.put(Constants.ERROR_CODE, errorCode);
        errorMap.put(Constants.BODY, message);
        return errorMap;
    }

    /**
     * Validates that source and target languages are different.
     *
     * @param sourceLanguage the source language code
     * @param targetLanguage the target language code
     * @throws BodyValidationException if languages are the same
     */
    public void validateTranslate(String sourceLanguage, String targetLanguage) throws BodyValidationException {
        log.debug("Validating language pair - Source: {}, Target: {}", sourceLanguage, targetLanguage);
        
        if (sourceLanguage == null || targetLanguage == null) {
            log.warn("Null language detected - Source: {}, Target: {}", sourceLanguage, targetLanguage);
            List<Map<String, String>> errors = new ArrayList<>(1);
            errors.add(createErrorDetails(ErrorCodes.JCMPT033, "source language and target language cannot be null"));
            throw new BodyValidationException(errors);
        }
        
        if (sourceLanguage.equals(targetLanguage)) {
            log.warn("Source and target languages are the same - Language: {}", sourceLanguage);
            List<Map<String, String>> errors = new ArrayList<>(1);
            errors.add(createErrorDetails(ErrorCodes.JCMPT033, "source language and target language"));
            throw new BodyValidationException(errors);
        }
        
        log.debug("Language pair validation passed - Source: {}, Target: {}", sourceLanguage, targetLanguage);
    }
}
