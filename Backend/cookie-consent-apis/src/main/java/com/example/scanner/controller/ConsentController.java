package com.example.scanner.controller;

import com.example.scanner.dto.request.CreateConsentRequest;
import com.example.scanner.dto.request.UpdateConsentRequest;
import com.example.scanner.dto.response.*;
import com.example.scanner.entity.CookieConsent;
import com.example.scanner.service.ConsentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/consent")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Consent Management System", description = "Complete consent lifecycle operations")
public class ConsentController {

    private final ConsentService consentService;

    @PostMapping("/create")
    @Operation(
            summary = "Create a consent by consent handle ID",
            description = """
                Creates a consent record based on user's preference choices.
                Third step in consent flow: ConsentHandle → User Preferences → Create Consent
                
                Error Codes: R4001 (Validation), R4041 (Handle not found), R4091 (Handle used), R4101 (Handle expired), R5000 (Internal)
                """,
            requestBody = @RequestBody(
                    content = @Content(
                            schema = @Schema(implementation = CreateConsentRequest.class)
                    )
            ),
            parameters = {
                    @Parameter(name = "txn", description = "Transaction ID", example = "a1b2c3d4-e5f6-7890-1234XXXXXbcdef"),
                    @Parameter(name = "X-Tenant-ID", description = "Tenant ID", example = "pl_123e4567-CXXXXXX....")
            },
            responses = {
                    @ApiResponse(
                            responseCode = "201",
                            description = "Consent created successfully",
                            content = @Content(
                                    schema = @Schema(implementation = ConsentCreateResponse.class),
                                    examples = @ExampleObject(value = """
                                        {"consentId": "cst_123", "status": "ACTIVE", "message": "Consent created successfully"}
                                        """)
                            ),
                            headers = @io.swagger.v3.oas.annotations.headers.Header(
                                    name = "x-jws-signature",
                                    description = "JWS token for verification"
                            )
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Invalid request",
                            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
                    ),
                    @ApiResponse(
                            responseCode = "409",
                            description = "Consent handle already used or expired",
                            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
                    ),
                    @ApiResponse(
                            responseCode = "500",
                            description = "Internal server error",
                            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
                    )
            }
    )
    public ResponseEntity<ConsentCreateResponse> createConsentByConsentHandleId(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @org.springframework.web.bind.annotation.RequestBody @Valid CreateConsentRequest request) throws Exception {

        ConsentCreateResponse response = consentService.createConsentByConsentHandleId(request, tenantId);

        HttpHeaders responseHeaders = new HttpHeaders();
        if (response.getJwsToken() != null) {
            responseHeaders.set("x-jws-signature", response.getJwsToken());
        }

        return new ResponseEntity<>(response, responseHeaders, HttpStatus.CREATED);
    }

    @PutMapping("/{consentId}/update")
    @Operation(
            summary = "Update a consent (creates new version)",
            description = """
                Creates new version of consent. ConsentId remains same, version number increments.
                
                Error Codes: R4001 (Validation), R4041 (Not found), R4091 (Handle used), R4221 (Cannot update), R5000 (Internal)
                """,
            requestBody = @RequestBody(
                    content = @Content(
                            schema = @Schema(implementation = UpdateConsentRequest.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "consentHandleId": "pl_123e4567-CXXXXXX...",
                                      "languagePreference": "HINDI",
                                      "preferencesStatus": {
                                        "Necessary": "ACCEPTED",
                                        "Analytics": "NOTACCEPTED"
                                      }
                                    }
                                """)
                    )
            ),
            parameters = {
                    @Parameter(name = "X-Tenant-ID", description = "Tenant ID", example = "pl_123e4567-CXXXXXX...."),
                    @Parameter(name = "consentId", description = "Consent ID", example = "pl_123e4567-CXXXXXX....")
            },
            responses = {
                    @ApiResponse(responseCode = "200", description = "Consent updated successfully",
                            content = @Content(schema = @Schema(implementation = UpdateConsentResponse.class))),
                    @ApiResponse(responseCode = "400", description = "Invalid request",
                            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
                    @ApiResponse(responseCode = "409", description = "Consent handle already used",
                            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
                    @ApiResponse(responseCode = "422", description = "Cannot update consent",
                            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
                    @ApiResponse(responseCode = "500", description = "Internal server error",
                            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
            }
    )
    public ResponseEntity<UpdateConsentResponse> updateConsent(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @PathVariable String consentId,
            @org.springframework.web.bind.annotation.RequestBody @Valid UpdateConsentRequest updateRequest) throws Exception {

        UpdateConsentResponse response = consentService.updateConsent(consentId, updateRequest, tenantId);

        HttpHeaders responseHeaders = new HttpHeaders();
        if (response.getJwsToken() != null) {
            responseHeaders.set("x-jws-signature", response.getJwsToken());
        }

        return new ResponseEntity<>(response, responseHeaders, HttpStatus.OK);
    }

    @GetMapping("/{consentId}/history")
    @Operation(
            summary = "Get consent version history",
            description = "Retrieves all versions of a consent (latest first). Shows complete audit trail.",
            parameters = {
                    @Parameter(name = "X-Tenant-ID", description = "Tenant ID", example = "pl_123e4567-CXXXXXX...."),
                    @Parameter(name = "consentId", description = "Consent ID", example = "pl_123e4567-CXXXXXX...."),
                    @Parameter(name = "business-id", description = "Business ID", example = "pl_123e4567-CXXXXXX....")
            },
            responses = {
                    @ApiResponse(responseCode = "200", description = "History retrieved successfully",
                            content = @Content(
                                    mediaType = "application/json",
                                    array = @ArraySchema(schema = @Schema(implementation = CookieConsent.class)))),
                    @ApiResponse(responseCode = "400", description = "Invalid consent ID",
                            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
                    @ApiResponse(responseCode = "404", description = "Consent not found",
                            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
                    @ApiResponse(responseCode = "500", description = "Internal server error",
                            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
            }
    )
    public ResponseEntity<List<CookieConsent>> getConsentHistory(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @PathVariable String consentId) throws Exception {

        List<CookieConsent> history = consentService.getConsentHistory(consentId, tenantId);
        return ResponseEntity.ok(history);
    }

    @GetMapping("/{consentId}/versions/{version}")
    @Operation(
            summary = "Get specific consent version",
            description = "Retrieves a specific version of a consent",
            parameters = {
                    @Parameter(name = "X-Tenant-ID", description = "Tenant ID", example = "pl_123e4567-CXXXXXX...."),
                    @Parameter(name = "consentId", description = "Consent ID", example = "cst_123e4567-e89b-12d3-a456-426614174000"),
                    @Parameter(name = "version", description = "Version number", example = "2")
            },
            responses = {
                    @ApiResponse(responseCode = "200", description = "Version retrieved successfully",
                            content = @Content(schema = @Schema(implementation = CookieConsent.class))),
                    @ApiResponse(responseCode = "400", description = "Invalid ID or version",
                            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
                    @ApiResponse(responseCode = "404", description = "Version not found",
                            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
                    @ApiResponse(responseCode = "500", description = "Internal server error",
                            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
            }
    )
    public ResponseEntity<CookieConsent> getConsentVersion(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @PathVariable String consentId,
            @PathVariable Integer version) throws Exception {

        Optional<CookieConsent> consentOpt = consentService.getConsentByIdAndVersion(tenantId, consentId, version);

        if (consentOpt.isEmpty()) {
            throw new IllegalArgumentException("No consent found with ID '" + consentId + "' and version " + version);
        }

        return ResponseEntity.ok(consentOpt.get());
    }

    @GetMapping("/validate-token")
    @Operation(
            summary = "Validate consent token with JWS verification",
            description = """
                Verifies JWS token against vault, then validates consent token.
                
                Error Codes: R4001 (Invalid token), R4011 (JWS failed), R4012 (Token failed), R4041 (Not found), R5000 (Internal)
                """,
            parameters = {
                    @Parameter(name = "consent-token", description = "Consent JWT Token", example = "eyJhbGciOiJIUzI1NiJ9..."),
                    @Parameter(name = "x-jws-signature", description = "JWS Token", example = "eyJhbGciOiJSUzI1NiJ9..."),
                    @Parameter(name = "business-id", description = "Business ID", example = "eyJhbGciOiJSUzI1NiJ9"),
                    @Parameter(name = "txn", description = "Transaction ID", example = "pl_123e4567-CXXXXXX...."),
                    @Parameter(name = "X-Tenant-ID", description = "Tenant ID", example = "pl_123e4567-CXXXXXX....")
            },
            responses = {
                    @ApiResponse(responseCode = "200", description = "Token validated successfully",
                            content = @Content(schema = @Schema(implementation = ConsentTokenValidateResponse.class))),
                    @ApiResponse(responseCode = "400", description = "Validation failed",
                            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
                    @ApiResponse(responseCode = "404", description = "Consent not found",
                            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
                    @ApiResponse(responseCode = "500", description = "Internal server error",
                            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
            }
    )
    public ResponseEntity<ConsentTokenValidateResponse> validateConsent(
            @RequestHeader("consent-token") String consentToken,
            @RequestHeader(value = "x-jws-signature") String jwsToken,
            @RequestHeader("business-id") String businessId,
            @RequestHeader("X-Tenant-ID") String tenantId
            ) throws Exception {
        return new ResponseEntity<>(consentService.validateConsentToken(consentToken, jwsToken, tenantId, businessId),
                HttpStatus.OK);
    }

    @GetMapping("/check")
    @Operation(
            summary = "Check consent status",
            description = """
                Returns consent status: PENDING, REQ_EXPIRED, USED, REJECTED, ACTIVE, REVOKE, EXPIRED, No_Record
                
                Error Codes: R4001 (Missing params), R5001 (Retrieval failed)
                """,
            parameters = {
                    @Parameter(name = "deviceId", description = "Device ID", required = true, example = "92342834928359235"),
                    @Parameter(name = "url", description = "Website URL", required = true, example = "http://www.example.com"),
                    @Parameter(name = "consentId", description = "Consent ID (optional)", example = "pl_123e4567-CXXXXXX...."),
                    @Parameter(name = "X-Tenant-ID", description = "Tenant ID", required = true, in = ParameterIn.HEADER, example = "pl_123e4567-CXXXXXX....")
            },
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Status retrieved successfully",
                            content = @Content(
                                    schema = @Schema(implementation = CheckConsentResponse.class),
                                    examples = @ExampleObject(value = """
                                        {"consentStatus": "ACTIVE", "consentHandleId": "9bb14c63-7ec8-47f5-86b5-4a8c848012c1"}
                                        """)
                            )
                    ),
                    @ApiResponse(responseCode = "400", description = "Missing required parameters",
                            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
                    @ApiResponse(responseCode = "500", description = "Internal server error",
                            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
            }
    )
    public ResponseEntity<CheckConsentResponse> checkConsent(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @RequestParam(value = "deviceId", required = true) String deviceId,
            @RequestParam(value = "url", required = true) String url,
            @RequestParam(value = "consentId", required = false) String consentId) throws Exception {
        return new ResponseEntity<>(consentService.getConsentStatus(deviceId, url, consentId, tenantId), HttpStatus.OK);
    }
}