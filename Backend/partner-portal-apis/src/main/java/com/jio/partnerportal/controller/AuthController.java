package com.jio.partnerportal.controller;

import com.jio.partnerportal.dto.request.*;
import com.jio.partnerportal.dto.response.*;
import com.jio.partnerportal.service.AuthService;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.CrossOrigin;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
/**
 *
 *
 * @author Kirte.Bhatt
 *
 */
@RestController
public class AuthController {

    AuthService authService;

    public AuthController (AuthService authService) {
       this.authService=authService;
    }

    @CrossOrigin(origins = "*")
    @Operation(
            summary = "Initiate OTP",
            description = "Starts the OTP flow for the given email or mobile number. "
                    + "Requires a transaction ID in the header for traceability."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "OTP sent successfully",
                    content = @Content(schema = @Schema(implementation = OtpInitResponse.class))),
            @ApiResponse(
                    responseCode = "400",
                    description = "Bad request",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                        "txnId": null,
                                        "message": "idValue cannot be empty"
                                    }
                                            """
                            ))
            ),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping("/otp/init")
    public ResponseEntity<OtpInitResponse> initiateOtp(@RequestHeader(value = "txn") String txnId,
             @RequestBody OtpInitRequest request, HttpServletRequest req) {
        return authService.initiateOtp(request, txnId,req);
    }

//-----------------------------------------
    @CrossOrigin(origins = "*")
    @PostMapping("/otp/validate")
    @Operation(
            summary = "Validate OTP",
            description = "Validates the OTP received by the user against the provided email, or mobile number. "
                    + "Requires a transaction ID header for tracking."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "OTP validated successfully",
                    content = @Content(schema = @Schema(implementation = OtpValidateResponse.class))),
            @ApiResponse(
                    responseCode = "400",
                    description = "Bad request",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                        "status": "txnId is required",
                                        "retryCount": 0
                                    }
                                            """
                            ))
            ),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<OtpValidateResponse> validateOtp(@RequestHeader(value = "txn") String txnId,
            @RequestBody OtpValidateRequest request,HttpServletRequest req) {
        return authService.validateOtp(request, txnId,req);
    }

    //-----------------------------------------

    @CrossOrigin(origins = "*")
    @PostMapping("/tenant/otp/init")
    @Operation(
            summary = "Tenant OTP Init",
            description = "Initiates OTP flow for a tenant user (mobile/email). "
                    + "Transaction ID (txn) must be passed in the header for traceability."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "OTP initiated successfully for tenant",
                    content = @Content(schema = @Schema(implementation = TenantOtpResponse.class))),
            @ApiResponse(
                    responseCode = "400",
                    description = "Bad request",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                        "status": "txnId is required",
                                        "retryCount": 0
                                    }
                                            """
                            ))
            ),

            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<TenantOtpResponse> tenantInitOtp(
            @RequestHeader(value = "txn") String txnId,
            @RequestBody TenantOtpRequest request,HttpServletRequest req)  {
        return authService.tenantInitOtp(txnId, request,req); // call instance method
    }

    //----------------------------------------
    @CrossOrigin(origins = "*")
    @PostMapping("/tenant/otp/validate")
    @Operation(
            summary = "Tenant OTP Validate",
            description = "Validates an OTP for a tenant user. "
                    + "Requires the transaction ID (txn) header for tracking."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "OTP validated successfully for tenant",
                    content = @Content(schema = @Schema(implementation = TenantValidateOtpResponse.class))),
            @ApiResponse(
                    responseCode = "400",
                    description = "Bad request",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                        "status": "txnId is required",
                                        "retryCount": 0
                                    }
                                            """
                            ))
            ),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<TenantValidateOtpResponse> tenantValidateOtp(
            @RequestHeader("txn") String headerTxnId,
            @RequestBody TenantValidateOtpRequest request,HttpServletRequest req) throws Exception {
        return authService.tenantValidateOtp(headerTxnId, request,req);
    }

}

