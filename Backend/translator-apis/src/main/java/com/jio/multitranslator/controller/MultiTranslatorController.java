package com.jio.multitranslator.controller;

import com.jio.multitranslator.constant.Constants;
import com.jio.multitranslator.dto.Status;
import com.jio.multitranslator.dto.request.TranslateConfigRequest;
import com.jio.multitranslator.dto.request.TranslateRequest;
import com.jio.multitranslator.dto.response.APITranslateResponse;
import com.jio.multitranslator.dto.response.CountResponse;
import com.jio.multitranslator.dto.response.TranslateConfigResponse;
import com.jio.multitranslator.entity.TranslateConfig;
import com.jio.multitranslator.exceptions.BodyValidationException;
import com.jio.multitranslator.service.TranslateConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@CrossOrigin
@Slf4j
@RestController
public class MultiTranslatorController {

    private final TranslateConfigService translateConfigService;

    @Autowired
    public MultiTranslatorController(TranslateConfigService translateConfigService) {
        this.translateConfigService = translateConfigService;
    }

    /**
     * Creates a new translation configuration for Bhashini or Azure provider.
     *
     * @param request the translation configuration request
     * @param header  request headers containing tenant, business, and transaction IDs
     * @return response containing the created configuration ID
     * @throws BodyValidationException if validation fails
     */
    @PostMapping("/translateConfig")
    @Operation(summary = "Create a configuration ID for Bhashni or Azure",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Request body for translateConfig", required = true,
                    content = @Content(schema = @Schema(implementation = TranslateConfigRequest.class))),
            parameters = {
                    @Parameter(name = "txn", description = "Transaction ID (UUID)", required = true, in = ParameterIn.HEADER, example = "a1b2c3d4-e5f6-7890-1234-567890abcdef"),
                    @Parameter(name = Constants.TENANT_ID_HEADER, description = "Tenant ID (UUID)", required = true, in = ParameterIn.HEADER, example = "a1b2c3d4-e5f6-7890-1234-567890abcdef"),
                    @Parameter(name = Constants.BUSINESS_ID_HEADER, description = "Business ID (UUID)", required = true, in = ParameterIn.HEADER, example = "a1b2c3d4-e5f6-7890-1234-567890abcdef")
            },
            responses = {
                    @ApiResponse(responseCode = "201", description = "Configuration ID created successfully",
                            content = @Content(schema = @Schema(implementation = TranslateConfigResponse.class))),
                    @ApiResponse(responseCode = "500", description = "Internal server error")
            }
    )
    public ResponseEntity<TranslateConfigResponse> createTranslateConfig(
            @Valid @RequestBody TranslateConfigRequest request,
            @RequestHeader Map<String, String> header) throws BodyValidationException {
        String tenantId = header.get(Constants.TENANT_ID_HEADER);
        String businessId = header.get(Constants.BUSINESS_ID_HEADER);
        String txn = header.get("txn");
        
        log.info("Creating translation config - TXN: {}, TenantId: {}, BusinessId: {}, Provider: {}", 
                txn, tenantId, businessId, request.getConfig().getProvider());
        
        try {
            TranslateConfig translateConfig = translateConfigService.createTranslateConfig(request, header);
            log.info("Translation config created successfully - TXN: {}, ConfigId: {}, BusinessId: {}", 
                    txn, translateConfig.getConfigId(), businessId);
            
            TranslateConfigResponse response = TranslateConfigResponse.builder()
                    .status(Status.SUCCESS)
                    .configId(translateConfig.getConfigId())
                    .message("Translator configuration saved successfully")
                    .build();
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (BodyValidationException e) {
            log.warn("Validation failed while creating translation config - TXN: {}, BusinessId: {}, Errors: {}", 
                    txn, businessId, e.getErrors().size());
            throw e;
        }
    }

    /**
     * Updates an existing translation configuration for Bhashini or Azure provider.
     *
     * @param request the translation configuration request with updated values
     * @param header  request headers containing tenant, business, and transaction IDs
     * @return response containing the updated configuration ID
     * @throws BodyValidationException if validation fails
     */
    @PutMapping("/updateTranslateConfig")
    @Operation(summary = "Update configuration for Bhashini or Azure",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Request body for translateConfig", required = true,
                    content = @Content(schema = @Schema(implementation = TranslateConfigRequest.class))),
            parameters = {
                    @Parameter(name = "txn", description = "Transaction ID (UUID)", required = true, in = ParameterIn.HEADER, example = "a1b2c3d4-e5f6-7890-1234-567890abcdef"),
                    @Parameter(name = Constants.TENANT_ID_HEADER, description = "Tenant ID (UUID)", required = true, in = ParameterIn.HEADER, example = "a1b2c3d4-e5f6-7890-1234-567890abcdef"),
                    @Parameter(name = Constants.BUSINESS_ID_HEADER, description = "Business ID (UUID)", required = true, in = ParameterIn.HEADER, example = "a1b2c3d4-e5f6-7890-1234-567890abcdef")
            },
            responses = {
                    @ApiResponse(responseCode = "200", description = "Configuration details updated successfully",
                            content = @Content(schema = @Schema(implementation = TranslateConfigResponse.class))),
                    @ApiResponse(responseCode = "500", description = "Internal server error")
            }
    )
    public ResponseEntity<TranslateConfigResponse> updateTranslateConfig(
            @Valid @RequestBody TranslateConfigRequest request,
            @RequestHeader Map<String, String> header) throws BodyValidationException {
        String tenantId = header.get(Constants.TENANT_ID_HEADER);
        String businessId = header.get(Constants.BUSINESS_ID_HEADER);
        String txn = header.get("txn");
        
        log.info("Updating translation config - TXN: {}, TenantId: {}, BusinessId: {}, Provider: {}", 
                txn, tenantId, businessId, request.getConfig().getProvider());
        
        try {
            TranslateConfig translateConfig = translateConfigService.updateTranslateConfig(request, header);
            log.info("Translation config updated successfully - TXN: {}, ConfigId: {}, BusinessId: {}", 
                    txn, translateConfig.getConfigId(), businessId);
            
            TranslateConfigResponse response = TranslateConfigResponse.builder()
                    .status(Status.SUCCESS)
                    .configId(translateConfig.getConfigId())
                    .message("Translator configuration updated successfully")
                    .build();
            return ResponseEntity.ok(response);
        } catch (BodyValidationException e) {
            log.warn("Validation failed while updating translation config - TXN: {}, BusinessId: {}, Errors: {}", 
                    txn, businessId, e.getErrors().size());
            throw e;
        }
    }

    /**
     * Retrieves translation configuration(s) based on tenant ID, optional business ID, and optional provider.
     *
     * @param tenantId   the tenant ID (required)
     * @param businessId the business ID (optional)
     * @param provider   the provider name (optional)
     * @return list of translation configurations matching the criteria
     */
    @GetMapping("/getConfig")
    @Operation(summary = "Get translation configuration by Business ID",
            parameters = {
                    @Parameter(name = "tenantId", description = "Tenant ID (UUID)", required = true, in = ParameterIn.QUERY, example = "a1b2c3d4"),
                    @Parameter(name = "businessId", description = "Business ID (UUID)", required = false, in = ParameterIn.QUERY, example = "b2c3d4e5"),
                    @Parameter(name = "provider", description = "Provider name", required = false, in = ParameterIn.QUERY, example = "bhashini")
            },
            responses = {
                    @ApiResponse(responseCode = "200", description = "Translation configuration retrieved successfully",
                            content = @Content(schema = @Schema(implementation = TranslateConfig.class))),
                    @ApiResponse(responseCode = "404", description = "Translation configuration not found"),
                    @ApiResponse(responseCode = "500", description = "Internal server error")
            }
    )
    public ResponseEntity<List<TranslateConfig>> getTranslationConfig(
            @RequestParam String tenantId,
            @RequestParam(required = false) String businessId,
            @RequestParam(required = false) String provider) {
        log.info("Fetching translation config - TenantId: {}, BusinessId: {}, Provider: {}", 
                tenantId, businessId, provider);
        
        try {
            List<TranslateConfig> configs = translateConfigService.getTranslationConfig(tenantId, businessId, provider);
            log.info("Successfully retrieved {} translation config(s) - TenantId: {}", configs.size(), tenantId);
            return ResponseEntity.ok(configs);
        } catch (Exception e) {
            log.error("Error retrieving translation config - TenantId: {}, BusinessId: {}, Provider: {}", 
                    tenantId, businessId, provider, e);
            throw e;
        }
    }

    /**
     * Gets the count of translation configurations for a given tenant ID.
     *
     * @param header request headers containing tenant ID and transaction ID
     * @return response containing the count of configurations
     * @throws BodyValidationException if validation fails
     */
    @GetMapping("/count")
    @Operation(summary = "Get count of translation configuration by tenant ID",
            parameters = {
                    @Parameter(name = "txn", description = "Transaction ID (UUID)", required = true, in = ParameterIn.HEADER, example = "a1b2c3d4-e5f6-7890-1234-567890abcdef"),
                    @Parameter(name = Constants.TENANT_ID_HEADER, description = "Tenant ID (UUID)", required = true, in = ParameterIn.HEADER, example = "a1b2c3d4-e5f6-7890-1234-567890abcdef")
            },
            responses = {
                    @ApiResponse(responseCode = "200", description = "Translation configuration count retrieved successfully",
                            content = @Content(schema = @Schema(implementation = CountResponse.class))),
                    @ApiResponse(responseCode = "400", description = "Bad request - missing mandatory headers"),
                    @ApiResponse(responseCode = "500", description = "Internal server error")
            }
    )
    public ResponseEntity<CountResponse> count(@RequestHeader Map<String, String> header) throws BodyValidationException {
        String tenantId = header.get(Constants.TENANT_ID_HEADER);
        String txn = header.get("txn");
        
        log.info("Getting translation config count - TXN: {}, TenantId: {}", txn, tenantId);
        
        try {
            long count = translateConfigService.count(tenantId, txn);
            log.info("Translation config count retrieved - TXN: {}, TenantId: {}, Count: {}", 
                    txn, tenantId, count);
            CountResponse response = CountResponse.builder()
                    .status(Status.SUCCESS)
                    .count(count)
                    .message("Translation configuration count retrieved successfully")
                    .build();
            return ResponseEntity.ok(response);
        } catch (BodyValidationException e) {
            log.warn("Validation failed while getting count - TXN: {}, TenantId: {}", txn, tenantId);
            throw e;
        }
    }

    /**
     * Translates the given input text from source language to target language.
     *
     * @param request the translation request containing input text and language details
     * @param header  request headers containing tenant, business, and transaction IDs
     * @return response containing translated text
     * @throws BodyValidationException if validation fails
     */
    @PostMapping("/translate")
    @Operation(summary = "Translate the given input text",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Request body for Translate", required = true,
                    content = @Content(schema = @Schema(implementation = TranslateRequest.class))),
            parameters = {
                    @Parameter(name = "txn", description = "Transaction ID (UUID)", required = true, in = ParameterIn.HEADER, example = "a1b2c3d4-e5f6-7890-1234-567890abcdef"),
                    @Parameter(name = Constants.TENANT_ID_HEADER, description = "Tenant ID (UUID)", required = true, in = ParameterIn.HEADER, example = "a1b2c3d4-e5f6-7890-1234-567890abcdef"),
                    @Parameter(name = Constants.BUSINESS_ID_HEADER, description = "Business ID (UUID)", required = true, in = ParameterIn.HEADER, example = "a1b2c3d4-e5f6-7890-1234-567890abcdef")
            },
            responses = {
                    @ApiResponse(responseCode = "200", description = "Translate Successfully",
                            content = @Content(schema = @Schema(implementation = APITranslateResponse.class))),
                    @ApiResponse(responseCode = "500", description = "Internal server error")
            }
    )
    public ResponseEntity<APITranslateResponse> translate(
            @Valid @RequestBody TranslateRequest request,
            @RequestHeader Map<String, String> header) throws BodyValidationException {
        String tenantId = header.get(Constants.TENANT_ID_HEADER);
        String businessId = header.get(Constants.BUSINESS_ID_HEADER);
        String txn = header.get("txn");
        String sourceLang = request.getLanguage().getSourceLanguage();
        String targetLang = request.getLanguage().getTargetLanguage();
        int inputSize = request.getInput() != null ? request.getInput().size() : 0;
        
        log.info("Translation request received - TXN: {}, TenantId: {}, BusinessId: {}, Source: {}, Target: {}, InputSize: {}", 
                txn, tenantId, businessId, sourceLang, targetLang, inputSize);
        
        try {
            APITranslateResponse response = translateConfigService.translateService(request, header);
            
            HttpStatus status = Status.SUCCESS.equals(response.getStatus()) 
                    ? HttpStatus.OK 
                    : HttpStatus.BAD_REQUEST;
            return ResponseEntity.status(status).body(response);
        } catch (BodyValidationException e) {
            log.warn("Validation failed for translation request - TXN: {}, BusinessId: {}, Errors: {}", 
                    txn, businessId, e.getErrors().size());
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error during translation - TXN: {}, BusinessId: {}, Source: {}, Target: {}", 
                    txn, businessId, sourceLang, targetLang, e);
            throw e;
        }
    }

}

