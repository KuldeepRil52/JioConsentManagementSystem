package com.jio.consent.controller;

import com.jio.consent.dto.Request.BulkConsentRequest;
import com.jio.consent.dto.Response.BulkConsentResponse;
import com.jio.consent.dto.Response.BulkConsentStatusResponse;
import com.jio.consent.exception.ConsentException;
import com.jio.consent.service.BulkConsentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/consent")
@Tag(name = "Bulk Consent Management", description = "Operations for bulk consent upload")
public class BulkConsentController {

    private final BulkConsentService bulkConsentService;

    @Autowired
    public BulkConsentController(BulkConsentService bulkConsentService) {
        this.bulkConsentService = bulkConsentService;
    }

    @PostMapping("/consent-bulk-upload")
    @Operation(summary = "Create consents in bulk",
            description = "Create multiple consents in bulk. Each consent entry is processed asynchronously. " +
                    "The API returns immediately with a transaction ID that can be used to track the status of the bulk operation. " +
                    "Duplicate txnId entries are skipped for idempotency.",
            requestBody = @RequestBody(description = "Request body for bulk consent creation", required = true,
                    content = @Content(schema = @Schema(implementation = BulkConsentRequest.class))),
            parameters = {
                    @Parameter(name = "tenant-id", description = "Tenant ID (UUID)", required = true, in = ParameterIn.HEADER, example = "a1b2c3d4-e5f6-7890-1234-567890abcdef"),
                    @Parameter(name = "x-jws-signature", description = "JWS signature", required = false, in = ParameterIn.HEADER, example = "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...")
            },
            responses = {
                    @ApiResponse(responseCode = "202", description = "Bulk consent request accepted for processing",
                            content = @Content(schema = @Schema(implementation = BulkConsentResponse.class))),
                    @ApiResponse(responseCode = "400", description = "Bad request - validation error"),
                    @ApiResponse(responseCode = "500", description = "Internal server error")
            }
    )
    public ResponseEntity<BulkConsentResponse> createBulkConsent(
            @Valid @org.springframework.web.bind.annotation.RequestBody BulkConsentRequest request,
            @RequestHeader Map<String, String> headers) throws ConsentException {
        BulkConsentResponse response = this.bulkConsentService.processBulkConsent(request, headers);
        return new ResponseEntity<>(response, HttpStatus.ACCEPTED);
    }

    @GetMapping("/consent-bulk-upload/{transactionId}")
    @Operation(summary = "Get bulk consent status",
            description = "Get the status of a bulk consent operation by transaction ID. " +
                    "Returns the count of pending, successful, and failed consent creations along with individual item statuses.",
            parameters = {
                    @Parameter(name = "transactionId", description = "Transaction ID returned from bulk-upload API", required = true, in = ParameterIn.PATH, example = "a1b2c3d4-e5f6-7890-1234-567890abcdef"),
                    @Parameter(name = "tenant-id", description = "Tenant ID (UUID)", required = true, in = ParameterIn.HEADER, example = "a1b2c3d4-e5f6-7890-1234-567890abcdef"),
                    @Parameter(name = "x-jws-signature", description = "JWS signature", required = false, in = ParameterIn.HEADER, example = "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...")
            },
            responses = {
                    @ApiResponse(responseCode = "200", description = "Bulk consent status retrieved successfully",
                            content = @Content(schema = @Schema(implementation = BulkConsentStatusResponse.class))),
                    @ApiResponse(responseCode = "404", description = "Transaction not found"),
                    @ApiResponse(responseCode = "500", description = "Internal server error")
            }
    )
    public ResponseEntity<BulkConsentStatusResponse> getBulkConsentStatus(
            @PathVariable("transactionId") String transactionId) throws ConsentException {
        BulkConsentStatusResponse response = this.bulkConsentService.getBulkConsentStatus(transactionId);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }
}
