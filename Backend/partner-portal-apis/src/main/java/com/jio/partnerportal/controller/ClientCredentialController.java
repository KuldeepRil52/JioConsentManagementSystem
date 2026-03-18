package com.jio.partnerportal.controller;

import com.jio.partnerportal.dto.ErrorResponse;
import com.jio.partnerportal.dto.response.ClientCredentialsResponse;
import com.jio.partnerportal.exception.PartnerPortalException;
import com.jio.partnerportal.service.ClientCredentialsService;
import com.jio.partnerportal.util.LogUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/v1.0/client-credential")
@Slf4j
public class ClientCredentialController {

    private final ClientCredentialsService clientCredentialsService;

    @Autowired
    public ClientCredentialController(ClientCredentialsService clientCredentialsService) {
        this.clientCredentialsService = clientCredentialsService;
    }

    @CrossOrigin(origins = "*")
    @GetMapping("")
    @Operation(
            summary = "Get client credentials by business ID",
            description = "Fetches client credentials (consumer key and secret) for a given business ID. The tenant-id header is used to select the appropriate database.",
            parameters = {
                    @Parameter(name = "businessId", description = "Business ID to fetch credentials for", required = true, in = ParameterIn.QUERY, schema = @Schema(type = "string", format = "uuid", example = "a1b2c3d4-e5f6-7890-1234-567890abcdef")),
                    @Parameter(name = "tenant-id", description = "Tenant ID for database selection", required = true, in = ParameterIn.HEADER, schema = @Schema(type = "string", format = "uuid", example = "a1b2c3d4-e5f6-7890-1234-567890abcdef")),
                    @Parameter(name = "txn", description = "Transaction ID", required = true, in = ParameterIn.HEADER, schema = @Schema(type = "string", format = "uuid", example = "a1b2c3d4-e5f6-7890-1234-567890abcdef")),
                    @Parameter(name = "x-session-token", description = "Session token for authentication", required = true, in = ParameterIn.HEADER, schema = @Schema(type = "string", example = "Bearer <sessionToken>"))
            },
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Client credentials retrieved successfully",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = ClientCredentialsResponse.class),
                                    examples = @ExampleObject(value = """
                                            {
                                              "businessId": "a1b2c3d4-e5f6-7890-1234-567890abcdef",
                                              "businessUniqueId": "wso2-business-unique-id",
                                              "consumerKey": "abcd1234efgh5678",
                                              "consumerSecret": "secret1234",
                                              "scopeLevel": "TENANT",
                                              "status": "ACTIVE",
                                              "tenantId": "tenant-uuid",
                                              "createdAt": "2025-08-20 17:34:20",
                                              "updatedAt": "2025-08-20 17:34:20"
                                            }
                                            """)
                            )
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Bad request - Missing or invalid parameters",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = ErrorResponse.class),
                                    examples = @ExampleObject(value = """
                                            {
                                              "errors": [
                                                {
                                                  "errorMessage": "Body Validation Error: Missing parameter(s) in request body.",
                                                  "errorCode": "JCMP1001"
                                                }
                                              ],
                                              "timestamp": "2025-08-20T17:34:20.243Z"
                                            }
                                            """)
                            )
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "Client credentials not found",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = ErrorResponse.class),
                                    examples = @ExampleObject(value = """
                                            {
                                              "errors": [
                                                {
                                                  "errorMessage": "Not Found: Data could not be found.",
                                                  "errorCode": "JCMP3001"
                                                }
                                              ],
                                              "timestamp": "2025-08-20T17:34:20.243Z"
                                            }
                                            """)
                            )
                    ),
                    @ApiResponse(
                            responseCode = "500",
                            description = "Internal server error",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = ErrorResponse.class),
                                    examples = @ExampleObject(value = """
                                            {
                                              "errors": [
                                                {
                                                  "errorMessage": "An unexpected error occurred. Please refer to the application logs for details.",
                                                  "errorCode": "JCMP0001"
                                                }
                                              ],
                                              "timestamp": "2025-08-20T17:34:20.243Z"
                                            }
                                            """)
                            )
                    )
            }
    )
    public ResponseEntity<ClientCredentialsResponse> getClientCredentials(
            @RequestParam("businessId") String businessId,
            @RequestHeader Map<String, String> headers,
            HttpServletRequest req


    ) throws PartnerPortalException {
        String activity = "Get Client credentials";

        log.info("Received request to fetch client credentials for businessId: {}", businessId);

        ClientCredentialsResponse clientCredentials = this.clientCredentialsService.getClientCredentialsByBusinessId(businessId);
        log.info("Successfully fetched client credentials for businessId: {}", businessId);

        LogUtil.logActivity(req, activity, "Success: Get Client credentials successfully");
        return new ResponseEntity<>(clientCredentials, HttpStatus.OK);
    }
}

