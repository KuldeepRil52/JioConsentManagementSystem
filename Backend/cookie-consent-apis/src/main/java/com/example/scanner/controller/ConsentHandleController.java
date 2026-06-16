package com.example.scanner.controller;

import com.example.scanner.dto.request.CreateHandleCodeRequest;
import com.example.scanner.dto.response.ConsentHandleResponse;
import com.example.scanner.dto.request.CreateHandleRequest;
import com.example.scanner.dto.response.ErrorResponse;
import com.example.scanner.dto.response.GetConsentHandleAndSecureCodeResponse;
import com.example.scanner.dto.response.GetHandleResponse;
import com.example.scanner.exception.ScannerException;
import com.example.scanner.service.ConsentHandleService;
import com.example.scanner.service.RequestResponseSignatureService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.TreeMap;

@RestController
@RequestMapping("/consent-handle")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Consent Handle Management", description = "Secure consent handle operations for cookie consent flow")
public class ConsentHandleController {

    private final ConsentHandleService consentHandleService;

    private final RequestResponseSignatureService requestResponseSignatureService;

    @PostMapping("/create")
    @Operation(
            summary = "Create a new consent handle",
            description = """
                Generates a secure, time-limited consent handle (15 min expiry as default).
                First step in consent flow: Create Handle → Get Handle → Create Consent
                
                Error Codes: R4001 (Validation), R4091 (Handle exists), R5000 (Internal)
                """,
            requestBody = @RequestBody(
                    content = @Content(
                            schema = @Schema(implementation = CreateHandleRequest.class),
                            examples = @ExampleObject(value = """
                                {
                                  "templateId": "tpl_123",
                                  "templateVersion": 1,
                                  "url": "https://example.com",
                                  "customerIdentifiers": {"type": "DEVICE_ID", "value": "device123"}
                                }
                                """)
                    )
            ),
            parameters = {
                    @Parameter(name = "txn", description = "Transaction ID", example = "a1b2c3d4-e5f6-7890-1234-567890abcdef"),
                    @Parameter(name = "X-Tenant-ID", description = "Tenant ID", example = "a1b2c3d4-e5f6-7890-1234-567890abcdef"),
                    @Parameter(name = "business-id", description = "Business ID", example = "b1c2d3e4-f5g6-7890-1234-567890abcdef")
            },
            responses = {
                    @ApiResponse(
                            responseCode = "201",
                            description = "Consent handle created successfully",
                            content = @Content(
                                    schema = @Schema(implementation = ConsentHandleResponse.class),
                                    examples = @ExampleObject(value = """
                                        {"consentHandleId": "9bb14c63-7ec8-47f5-86b5-4a8c848012c1", "message": "Consent Handle Created successfully!", "isNewHandle": true}
                                        """)
                            )
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Invalid request or validation failed",
                            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
                    ),
                    @ApiResponse(
                            responseCode = "409",
                            description = "Consent handle already exists",
                            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
                    ),
                    @ApiResponse(
                            responseCode = "500",
                            description = "Internal server error",
                            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
                    )
            }
    )
    public ResponseEntity<ConsentHandleResponse> createConsentHandle(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @Valid @org.springframework.web.bind.annotation.RequestBody CreateHandleRequest request,
            @RequestHeader Map<String, String> headers) throws ScannerException {
        ConsentHandleResponse response = this.consentHandleService.createConsentHandle(tenantId, request, headers);
        HttpStatus status = response.isNewHandle() ? HttpStatus.CREATED : HttpStatus.OK;
        return new ResponseEntity<>(response, status);
    }

    @GetMapping("/get/{consentHandleId}")
    @Operation(
            summary = "Get consent handle by ID",
            description = """
                Retrieves consent handle details with template and cookie information.
                Returns cookie report, UI config, and preferences with cookies.
                
                Error Codes: R4001 (Invalid ID), R4041 (Not found), R4101 (Expired), R5000 (Internal)
                """,
            parameters = {
                    @Parameter(name = "consentHandleId", description = "Consent Handle ID", example = "9bb14c63-7ec8-47f5-86b5-4a8c848012c1"),
                    @Parameter(name = "txn", description = "Transaction ID", example = "a1b2c3d4-e5f6-7890-1234-567890abcdef"),
                    @Parameter(name = "business-id", description = "Business ID", example = "b1c2d3e4-f5g6-7890-1234-567890abcdef"),
                    @Parameter(name = "X-Tenant-ID", description = "Tenant ID", example = "a1b2c3d4-e5f6-7890-1234-567890abcdef")
            },
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Consent handle retrieved successfully",
                            content = @Content(schema = @Schema(implementation = GetHandleResponse.class))
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Invalid consent handle ID",
                            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "Consent handle not found or expired",
                            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
                    ),
                    @ApiResponse(
                            responseCode = "500",
                            description = "Internal server error",
                            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
                    )
            }
    )
    public ResponseEntity<GetHandleResponse> getHandleById(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @PathVariable("consentHandleId") String consentHandleId) throws ScannerException {
        GetHandleResponse response = this.consentHandleService.getConsentHandleById(consentHandleId, tenantId);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @PostMapping("/handle-code")
    @Operation(
            summary = "Create consent handle and retrieve secure code",
            description = """
                Creates a consent handle internally and calls external secure code API to generate secure code.
                This is a combined operation that performs two steps:
                1. Creates a consent handle using templateId, templateVersion, and customerIdentifiers
                2. Calls external secure code API with identity information
                
                Returns both consent handle ID and secure code details in a single response.
                
                Error Codes: 
                - R4001 (Validation Error)
                - R4041 (Not Found - Template not found)
                - R5000 (Internal Server Error)
                - External API errors will be propagated with appropriate error codes
                """,
            requestBody = @RequestBody(
                    description = "Request containing template information and customer identifiers",
                    required = true,
                    content = @Content(
                            schema = @Schema(implementation = CreateHandleRequest.class),
                            examples = @ExampleObject(
                                    name = "Sample Request",
                                    value = """
                                        {
                                          "templateId": "tpl_123e4567-e89b-12d3-a456-426614174000",
                                          "templateVersion": 1,
                                          "customerIdentifiers": {
                                            "type": "DEVICE_ID",
                                            "value": "9324901354"
                                          }
                                        }
                                        """
                            )
                    )
            ),
            parameters = {
                    @Parameter(
                            name = "X-Tenant-ID",
                            description = "Tenant ID (UUID format)",
                            required = true,
                            example = "d582664d-e67c-4971-99dd-f0b4385ab35b"
                    ),
                    @Parameter(
                            name = "business-id",
                            description = "Business ID (UUID format)",
                            required = true,
                            example = "d582664d-e67c-4971-99dd-f0b4385ab35b"
                    ),
                    @Parameter(
                            name = "txn",
                            description = "Transaction ID for tracking (optional)",
                            required = false,
                            example = "a1b2c3d4-e5f6-7890-1234-567890abcdef"
                    )
            },
            responses = {
                    @ApiResponse(
                            responseCode = "201",
                            description = "Consent handle and secure code created successfully",
                            content = @Content(
                                    schema = @Schema(implementation = GetConsentHandleAndSecureCodeResponse.class),
                                    examples = @ExampleObject(
                                            name = "Success Response",
                                            value = """
                                                {
                                                  "consentHandleId": "9bb14c63-7ec8-47f5-86b5-4a8c848012c1",
                                                  "secureCode": "fa0b0f24-6419-4d80-bd2b-d9493bc48b6c",
                                                  "identity": "9324901354",
                                                  "expiry": 1763831397284,
                                                  "templateId": "tpl_123e4567-e89b-12d3-a456-426614174000",
                                                  "templateVersion": 1,
                                                  "message": "Consent handle and secure code created successfully"
                                                }
                                                """
                                    )
                            )
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Invalid request - validation failed",
                            content = @Content(
                                    schema = @Schema(implementation = ErrorResponse.class),
                                    examples = @ExampleObject(
                                            name = "Validation Error",
                                            value = """
                                                {
                                                  "errorCode": "R4001",
                                                  "message": "Validation error",
                                                  "details": "Template ID is required"
                                                }
                                                """
                                    )
                            )
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "Template not found",
                            content = @Content(
                                    schema = @Schema(implementation = ErrorResponse.class),
                                    examples = @ExampleObject(
                                            name = "Not Found Error",
                                            value = """
                                                {
                                                  "errorCode": "R4041",
                                                  "message": "Template not found",
                                                  "details": "No template found with the given ID and version"
                                                }
                                                """
                                    )
                            )
                    ),
                    @ApiResponse(
                            responseCode = "500",
                            description = "Internal server error or external API failure",
                            content = @Content(
                                    schema = @Schema(implementation = ErrorResponse.class),
                                    examples = @ExampleObject(
                                            name = "Internal Error",
                                            value = """
                                                {
                                                  "errorCode": "R5000",
                                                  "message": "Internal server error",
                                                  "details": "Failed to create secure code: Connection timeout"
                                                }
                                                """
                                    )
                            )
                    )
            }
    )
    public ResponseEntity<GetConsentHandleAndSecureCodeResponse> getConsentHandleAndSecureCode(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @RequestHeader("business-id") String businessId,
            @Valid @org.springframework.web.bind.annotation.RequestBody CreateHandleRequest request,
            @RequestHeader Map<String, String> headers) throws ScannerException {

        log.info("Received request to create consent handle and secure code for tenant: {}, businessId: {}, templateId: {}",
                tenantId, businessId, request.getTemplateId());


        ObjectMapper mapper = new ObjectMapper();
        Map<String, Object> map =
                mapper.convertValue(request, new TypeReference<TreeMap<String, Object>>() {});
        requestResponseSignatureService.verifyRequest(map ,headers);

        GetConsentHandleAndSecureCodeResponse response = consentHandleService
                .createConsentHandleAndSecureCode(tenantId, businessId, request, headers);

        log.info("Successfully created consent handle: {} and secure code: {}",
                response.getConsentHandleId(), response.getSecureCode());

        String signature = requestResponseSignatureService.signRequest(map);
        HttpHeaders responseHeaders = new HttpHeaders();
        responseHeaders.add("x-jws-signature", signature);

        return new ResponseEntity<>(response, responseHeaders, HttpStatus.OK);
    }

    @PostMapping("/create-handle-code")
    @Operation(
            summary = "Create consent handle and retrieve secure code with active template",
            description = """
                Creates consent handle using latest published template and generates secure code.
                Automatically uses the most recent published template version for the given template ID.
                
                Error Codes: R4001 (Validation), R4041 (Template not found), R5000 (Internal)
                """,
            requestBody = @RequestBody(
                    content = @Content(
                            schema = @Schema(implementation = CreateHandleCodeRequest.class),
                            examples = @ExampleObject(value = """
                                {
                                  "templateId": "tpl_123e4567",
                                  "customerIdentifiers": {"type": "DEVICE_ID", "value": "device123"},
                                  "url": "https://example.com"
                                }
                                """)
                    )
            ),
            parameters = {
                    @Parameter(name = "X-Tenant-ID", description = "Tenant ID", example = "a1b2c3d4-e5f6-7890"),
                    @Parameter(name = "business-id", description = "Business ID", example = "b1c2d3e4-f5g6-7890"),
                    @Parameter(name = "txn", description = "Transaction ID", example = "txn_123456")
            },
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Consent handle and secure code created successfully",
                            content = @Content(schema = @Schema(implementation = GetConsentHandleAndSecureCodeResponse.class))
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Invalid request or validation failed",
                            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "No published template found",
                            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
                    ),
                    @ApiResponse(
                            responseCode = "500",
                            description = "Internal server error",
                            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
                    )
            }
    )
    public ResponseEntity<GetConsentHandleAndSecureCodeResponse> createHandleCode(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @RequestHeader("business-id") String businessId,
            @Valid @org.springframework.web.bind.annotation.RequestBody CreateHandleCodeRequest request,
            @RequestHeader Map<String, String> headers) throws ScannerException {

        GetConsentHandleAndSecureCodeResponse response = this.consentHandleService
                .createConsentHandleAndSecureCodeNew(tenantId, businessId, request, headers);

        return new ResponseEntity<>(response, HttpStatus.OK);
    }

}