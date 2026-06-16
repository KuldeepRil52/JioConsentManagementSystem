package com.jio.consent.controller;

import com.jio.consent.constant.Constants;
import com.jio.consent.dto.Request.ParentalKycRequest;
import com.jio.consent.dto.Response.ParentalKycResponse;
import com.jio.consent.service.ParentalKycService;
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
@RequestMapping("/api/v1/parental-consent")
@Tag(name = "Parental KYC", description = "Parental KYC operations")
public class ParentalKycController {

    private final ParentalKycService parentalKycService;

    @Autowired
    public ParentalKycController(ParentalKycService parentalKycService) {
        this.parentalKycService = parentalKycService;
    }

    @PostMapping("/kyc")
    @Operation(summary = "Create parental KYC (below 18 years)",
            requestBody = @RequestBody(description = "Parental KYC request", required = true, content = @Content(schema = @Schema(implementation = ParentalKycRequest.class))),
            parameters = {
                    @Parameter(name = "txn", description = "Transaction ID (UUID)", required = true, in = ParameterIn.HEADER, example = "a1b2c3d4-e5f6-7890-1234-567890abcdef"),
                    @Parameter(name = "tenant-id", description = "Tenant identifier for organization", required = true, in = ParameterIn.HEADER),
                    @Parameter(name = "business-id", description = "Business identifier for the service consumer", required = true, in = ParameterIn.HEADER)
            },
            responses = {
                    @ApiResponse(responseCode = "200", description = "Parental KYC created successfully", content = @Content(schema = @Schema(implementation = ParentalKycResponse.class))),
                    @ApiResponse(responseCode = "500", description = "Internal server error")
            }
    )
    public ResponseEntity<ParentalKycResponse> createParentalKyc(@Valid @org.springframework.web.bind.annotation.RequestBody ParentalKycRequest request, @RequestHeader Map<String, String> headers) throws Exception {
        
        Map<String, String> saved = this.parentalKycService.createParentalKyc(request, headers);
        ParentalKycResponse response = ParentalKycResponse.builder()
                .status("success")
                .message("Parental KYC created successfully")
                .parental_kyc(saved.get("parental_kyc"))
                .parental_reference_id(saved.get("parental_reference_id"))
                .build();
        return new ResponseEntity<>(response, HttpStatus.OK);
    }
}