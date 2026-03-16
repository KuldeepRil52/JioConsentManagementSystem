package com.wso2wrapper.credentials_generation.controller;

import com.wso2wrapper.credentials_generation.dto.OnboardRequestDto;
import com.wso2wrapper.credentials_generation.dto.OnboardResponseDto;
import com.wso2wrapper.credentials_generation.dto.response.ApiResponseCred;
import com.wso2wrapper.credentials_generation.service.OnboardingService;
import com.wso2wrapper.credentials_generation.service.TenantOnboardingService;

import com.wso2wrapper.credentials_generation.swagger.ApiExamples;
import com.wso2wrapper.credentials_generation.swagger.BusinessSwagger;
import com.wso2wrapper.credentials_generation.swagger.DataProcessorSwagger;
import com.wso2wrapper.credentials_generation.swagger.TenantSwagger;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.wso2wrapper.credentials_generation.dto.response.ErrorResponse;

@Slf4j
@RestController
@Tag(name = "Tenant Onboarding", description = "APIs for tenant registration and business onboarding")
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class TenantOnboardingController {

    private final OnboardingService onboardingService;
    private final TenantOnboardingService tenantOnboardingService;

    public TenantOnboardingController(OnboardingService onboardingService, TenantOnboardingService tenantOnboardingService) {
        this.onboardingService = onboardingService;
        this.tenantOnboardingService = tenantOnboardingService;
    }


    @Operation(
            summary = "Register a new tenant",
            description = "Registers a tenant and returns the onboarding response",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Tenant registration request",
                    required = true,
                    content = @Content(schema = @Schema(implementation = TenantSwagger.class))
            ),
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Tenant registered successfully",
                            content = @Content(
                                    schema = @Schema(implementation = ApiResponseCred.class),
                                    examples = @ExampleObject(name = "TenantSuccessExample", value = ApiExamples.TENANT_SUCCESS)
                            )
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Bad request",
                            content = @Content(
                                    schema = @Schema(implementation = ErrorResponse.class),
                                    examples = @ExampleObject(name = "TenantError400", value = ApiExamples.TENANT_ERROR_400)
                            )
                    ),
                    @ApiResponse(
                            responseCode = "401",
                            description = "Unauthorized",
                            content = @Content(
                                    schema = @Schema(implementation = ErrorResponse.class),
                                    examples = @ExampleObject(name = "TenantError401", value = ApiExamples.TENANT_ERROR_401)
                            )
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "Tenant DB not found",
                            content = @Content(
                                    schema = @Schema(implementation = ErrorResponse.class),
                                    examples = @ExampleObject(name = "TenantError404", value = ApiExamples.TENANT_ERROR_404)
                            )
                    ),
                    @ApiResponse(
                            responseCode = "409",
                            description = "Tenant already exists",
                            content = @Content(
                                    schema = @Schema(implementation = ErrorResponse.class),
                                    examples = @ExampleObject(name = "TenantError409", value = ApiExamples.TENANT_ERROR_409)
                            )
                    ),
                    @ApiResponse(
                            responseCode = "500",
                            description = "Internal server error",
                            content = @Content(
                                    schema = @Schema(implementation = ErrorResponse.class),
                                    examples = @ExampleObject(name = "TenantError500", value = ApiExamples.TENANT_ERROR_500)
                            )
                    )
            }
    )
    @PostMapping("/registerTenant")
    public ResponseEntity<ApiResponseCred<OnboardResponseDto>> registerTenant(@RequestBody OnboardRequestDto request) {
        // Call the service to register tenant and onboard
        OnboardResponseDto onboardResponse = tenantOnboardingService.registerTenant(request);

        // Build API response
        ApiResponseCred<OnboardResponseDto> response = new ApiResponseCred<>(
                true,
                "Tenant registered successfully",
                "NEGD_200_CREATED",
                onboardResponse
        );

        return ResponseEntity.ok(response);
    }

    // ===================== Onboard Business ====================

    @Operation(
            summary = "Onboard a business",
            description = "Handles business onboarding for a tenant",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Business onboarding request",
                    required = true,
                    content = @Content(schema = @Schema(implementation = BusinessSwagger.class))
            ),
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Successfully onboarded",
                            content = @Content(
                                    schema = @Schema(implementation = ApiResponseCred.class),
                                    examples = {
                                            @ExampleObject(name = "BusinessSuccessFirstTime", value = ApiExamples.BUSINESS_SUCCESS),
                                            @ExampleObject(name = "BusinessAlreadyOnboarded", value = ApiExamples.BUSINESS_ALREADY_ONBOARDED)
                                    }
                            )
                    ),
                    @ApiResponse(
                            responseCode = "200",
                            description = "Successfully onboarded",
                            content = @Content(
                                    schema = @Schema(implementation = ApiResponseCred.class),
                                    examples = {
                                            @ExampleObject(
                                                    name = "BusinessSuccessFirstTime",
                                                    value = ApiExamples.BUSINESS_SUCCESS
                                            ),
                                            @ExampleObject(
                                                    name = "BusinessAlreadyOnboarded",
                                                    value = ApiExamples.BUSINESS_ALREADY_ONBOARDED
                                            )
                                    }
                            )
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Bad request",
                            content = @Content(
                                    schema = @Schema(implementation = ErrorResponse.class),
                                    examples = @ExampleObject(name = "BusinessError400", value = ApiExamples.BUSINESS_ERROR_400)
                            )
                    ),
                    @ApiResponse(
                            responseCode = "401",
                            description = "Unauthorized",
                            content = @Content(
                                    schema = @Schema(implementation = ErrorResponse.class),
                                    examples = @ExampleObject(name = "BusinessError401", value = ApiExamples.BUSINESS_ERROR_401)
                            )
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "Business not found",
                            content = @Content(
                                    schema = @Schema(implementation = ErrorResponse.class),
                                    examples = @ExampleObject(name = "BusinessError404", value = ApiExamples.BUSINESS_ERROR_404)
                            )
                    ),
                    @ApiResponse(
                            responseCode = "500",
                            description = "Internal server error",
                            content = @Content(
                                    schema = @Schema(implementation = ErrorResponse.class),
                                    examples = @ExampleObject(name = "BusinessError500", value = ApiExamples.BUSINESS_ERROR_500)
                            )
                    )
            }
    )
    @PostMapping("/onboardBusiness")
    public ResponseEntity<ApiResponseCred<OnboardResponseDto>> onboard(@RequestBody OnboardRequestDto request) {
        OnboardResponseDto responseDto = onboardingService.handleOnboarding(request);
        ApiResponseCred<OnboardResponseDto> response = new ApiResponseCred<>(
                true,
                "Successfully onboarded",
                "NEGD_200_SUCCESS",
                responseDto
        );
        return ResponseEntity.ok(response);
    }


    // ===================== Onboard Data Processor =========================

    @Operation(
            summary = "Onboard a DataProcessor",
            description = "Handles DataProcessor onboarding for a tenant",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "DataProcessor onboarding request",
                    required = true,
                    content = @Content(schema = @Schema(implementation = DataProcessorSwagger.class))
            ),
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Successfully onboarded",
                            content = @Content(
                                    schema = @Schema(implementation = ApiResponseCred.class),
                                    examples = {
                                            @ExampleObject(name = "DataProcessorSuccessFirstTime", value = ApiExamples.DATA_PROCESSOR_SUCCESS),
                                            @ExampleObject(name = "DataProcessorAlreadyOnboarded", value = ApiExamples.DATA_PROCESSOR_ALREADY_ONBOARDED)
                                    }
                            )
                    ),
                    @ApiResponse(
                            responseCode = "200",
                            description = "Successfully onboarded",
                            content = @Content(
                                    schema = @Schema(implementation = ApiResponseCred.class),
                                    examples = {
                                            @ExampleObject(
                                                    name = "DataProcessorSuccessFirstTime",
                                                    value = ApiExamples.DATA_PROCESSOR_SUCCESS
                                            ),
                                            @ExampleObject(
                                                    name = "DataProcessorAlreadyOnboarded",
                                                    value = ApiExamples.DATA_PROCESSOR_ALREADY_ONBOARDED
                                            )
                                    }
                            )
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Bad request",
                            content = @Content(
                                    schema = @Schema(implementation = ErrorResponse.class),
                                    examples = @ExampleObject(name = "DataProcessorError400", value = ApiExamples.DATA_PROCESSOR_ERROR_400)
                            )
                    ),
                    @ApiResponse(
                            responseCode = "401",
                            description = "Unauthorized",
                            content = @Content(
                                    schema = @Schema(implementation = ErrorResponse.class),
                                    examples = @ExampleObject(name = "DataProcessorError401", value = ApiExamples.DATA_PROCESSOR_ERROR_401)
                            )
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "DataProcessor not found",
                            content = @Content(
                                    schema = @Schema(implementation = ErrorResponse.class),
                                    examples = @ExampleObject(name = "DataProcessorError404", value = ApiExamples.DATA_PROCESSOR_ERROR_404)
                            )
                    ),
                    @ApiResponse(
                            responseCode = "500",
                            description = "Internal server error",
                            content = @Content(
                                    schema = @Schema(implementation = ErrorResponse.class),
                                    examples = @ExampleObject(name = "DataProcessorError500", value = ApiExamples.DATA_PROCESSOR_ERROR_500)
                            )
                    )
            }
    )

    @PostMapping("/onboardDataProcessor")
    public ResponseEntity<ApiResponseCred<OnboardResponseDto>> onboardDataProcessor(@RequestBody OnboardRequestDto request) {
        OnboardResponseDto responseDto = onboardingService.handleDataProcessorOnboarding(request);
        ApiResponseCred<OnboardResponseDto> response = new ApiResponseCred<>(
                true,
                "Successfully onboarded",
                "NEGD_200_SUCCESS",
                responseDto
        );
        return ResponseEntity.ok(response);
    }
}
