package com.jio.partnerportal.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.jio.partnerportal.dto.NotificationDetails;
import com.jio.partnerportal.dto.request.ConfigurationRequest;
import com.jio.partnerportal.dto.response.ConfigCreateResponse;
import com.jio.partnerportal.dto.response.ConfigUpdateResponse;
import com.jio.partnerportal.dto.response.CountResponse;
import com.jio.partnerportal.dto.response.SearchResponse;
import com.jio.partnerportal.dto.ErrorResponse;
import com.jio.partnerportal.entity.NotificationConfig;
import com.jio.partnerportal.exception.BodyValidationException;
import com.jio.partnerportal.exception.PartnerPortalException;
import com.jio.partnerportal.service.NotificationService;
import com.jio.partnerportal.util.LogUtil;
import com.jio.partnerportal.util.Validation;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.CrossOrigin;

import java.util.Map;

@RestController
@RequestMapping("/v1.0/notification")
public class NotificationController {

    Validation validation;
    NotificationService notificationService;

    @Autowired
    public NotificationController(Validation validation, NotificationService notificationService) {
        this.validation = validation;
        this.notificationService = notificationService;
    }

    @CrossOrigin(origins = "*")
    @PostMapping("/create")
    @Operation(
            summary = "Create notification configuration",
            description = "Creates a new notification configuration.",
            parameters = {
                    @Parameter(name = "business-id", description = "Business ID", required = true, in = ParameterIn.HEADER, schema = @Schema(type = "string", example = "a1b2c3d4-e5f6-7890-1234-567890abcdef")),
                    @Parameter(name = "scope-level", description = "Scope Level", required = true, in = ParameterIn.HEADER, schema = @Schema(type = "string", allowableValues = {"TENANT", "BUSINESS"}, example = "TENANT")),
                    @Parameter(name = "provider-type", description = "Provider Type", required = false, in = ParameterIn.HEADER, schema = @Schema(type = "string", allowableValues = {"DIGIGOV", "SMTP"}, example = "DIGIGOV")),
                    @Parameter(name = "txn", description = "Transaction ID", required = true, in = ParameterIn.HEADER, schema = @Schema(type = "string", format = "uuid", example = "a1b2c3d4-e5f6-7890-1234-567890abcdef")),
                    @Parameter(name = "tenant-id", description = "Tenant ID", required = true, in = ParameterIn.HEADER, schema = @Schema(type = "string", format = "uuid", example = "a1b2c3d4-e5f6-7890-1234-567890abcdef")),
                    @Parameter(name = "x-session-token", description = "Session token for authentication", required = true, in = ParameterIn.HEADER, schema = @Schema(type = "string", example = "Bearer <sessionToken>"))
            },
            requestBody = @RequestBody(
                    description = "Notification configuration details",
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ConfigurationRequest.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "configurationJson": {
                                        "networkType": "INTERNET",
                                        "baseUrl": "https://notification.example.com/api",
                                        "clientId": "notification_client_123",
                                        "clientSecret": "secret_key_456",
                                        "sid": "SID_001",
                                        "mutualSSL": false,
                                        "mutualCertificate": "base64_encoded_certificate_here",
                                        "mutualCertificateMeta": {
                                          "name": "mutual_cert.pem",
                                          "contentType": "application/x-pem-file",
                                          "size": 2048
                                        },
                                        "callbackUrl": "https://yourapp.example.com/notifications/callback"
                                      }
                                    }
                                    """)
                    )
            ),
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Notification configuration created successfully",
                            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ConfigCreateResponse.class))
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Bad request",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = ErrorResponse.class),
                                    examples = @ExampleObject(value = """
                                            {
                                              "errors": [
                                                {
                                                  "header": "business-id",
                                                  "errorMessage": "Business ID is required",
                                                  "errorCode": "JCMP1001"
                                                }
                                              ],
                                              "timestamp": "2025-08-20T17:34:20.243Z"
                                            }
                                            """
                                    ))
                    ),
                    @ApiResponse(
                            responseCode = "500",
                            description = "Internal server error",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = ErrorResponse.class),
                                    examples = @ExampleObject(value = """
                                            {
                                              "errors": [
                                                {
                                                  "errorMessage": "Internal Server Error",
                                                  "errorCode": "JCMP0001"
                                                }
                                              ],
                                              "timestamp": "2025-08-20T17:34:20.243Z"
                                            }
                                            """
                                    ))
                    )
            }
    )
    public ResponseEntity<ConfigCreateResponse> create(@org.springframework.web.bind.annotation.RequestBody ConfigurationRequest<NotificationDetails> request, @RequestHeader Map<String, String> headers, HttpServletRequest req) throws BodyValidationException, PartnerPortalException, JsonProcessingException {
        this.validation.validateConfigHeader(headers);
        NotificationConfig config = this.notificationService.createConfig(headers, request, req);
        return new ResponseEntity<>(ConfigCreateResponse.builder().configId(config.getConfigId()).message("Notification configuration created successfully!").build(), HttpStatus.CREATED);
    }

    @CrossOrigin(origins = "*")
    @PutMapping("/update/{configId}")
    @Operation(
            summary = "Update notification configuration",
            description = "Updates an existing notification configuration.",
            parameters = {
                    @Parameter(name = "business-id", description = "Business ID", required = true, in = ParameterIn.HEADER, schema = @Schema(type = "string", example = "a1b2c3d4-e5f6-7890-1234-567890abcdef")),
                    @Parameter(name = "scope-level", description = "Scope Level", required = true, in = ParameterIn.HEADER, schema = @Schema(type = "string", allowableValues = {"TENANT", "BUSINESS"}, example = "TENANT")),
                    @Parameter(name = "provider-type", description = "Provider Type", required = false, in = ParameterIn.HEADER, schema = @Schema(type = "string", allowableValues = {"DIGIGOV", "SMTP"}, example = "DIGIGOV")),
                    @Parameter(name = "txn", description = "Transaction ID", required = true, in = ParameterIn.HEADER, schema = @Schema(type = "string", format = "uuid", example = "a1b2c3d4-e5f6-7890-1234-567890abcdef")),
                    @Parameter(name = "tenant-id", description = "Tenant ID", required = true, in = ParameterIn.HEADER, schema = @Schema(type = "string", format = "uuid", example = "a1b2c3d4-e5f6-7890-1234-567890abcdef")),
                    @Parameter(name = "x-session-token", description = "Session token for authentication", required = true, in = ParameterIn.HEADER, schema = @Schema(type = "string", example = "Bearer <sessionToken>"))
            },
            requestBody = @RequestBody(
                    description = "Updated notification configuration details",
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ConfigurationRequest.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "configurationJson": {
                                        "networkType": "INTERNET",
                                        "baseUrl": "https://updated-notification.example.com/api",
                                        "clientId": "updated_notification_client_123",
                                        "clientSecret": "updated_secret_key_456",
                                        "sid": "SID_002",
                                        "mutualSSL": true,
                                        "mutualCertificate": "base64_encoded_certificate_here",
                                        "mutualCertificateMeta": {
                                          "name": "mutual_cert.pem",
                                          "contentType": "application/x-pem-file",
                                          "size": 2048
                                        },
                                        "callbackUrl": "https://yourapp.example.com/notifications/callback"
                                      }
                                    }
                                    """)
                    )
            ),
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Notification configuration updated successfully",
                            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ConfigUpdateResponse.class))
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Bad request",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = ErrorResponse.class),
                                    examples = @ExampleObject(value = """
                                            {
                                              "errors": [
                                                {
                                                  "header": "business-id",
                                                  "errorMessage": "Business ID is required",
                                                  "errorCode": "JCMP1001"
                                                }
                                              ],
                                              "timestamp": "2025-08-20T17:34:20.243Z"
                                            }
                                            """
                                    ))
                    ),
                    @ApiResponse(
                            responseCode = "500",
                            description = "Internal server error",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = ErrorResponse.class),
                                    examples = @ExampleObject(value = """
                                            {
                                              "errors": [
                                                {
                                                  "errorMessage": "Internal Server Error",
                                                  "errorCode": "JCMP0001"
                                                }
                                              ],
                                              "timestamp": "2025-08-20T17:34:20.243Z"
                                            }
                                            """
                                    ))
                    )
            }
    )
    public ResponseEntity<ConfigUpdateResponse> update(@PathVariable String configId, @org.springframework.web.bind.annotation.RequestBody ConfigurationRequest<NotificationDetails> request, @RequestHeader Map<String, String> headers, HttpServletRequest req) throws BodyValidationException, PartnerPortalException, JsonProcessingException {
        NotificationConfig config = this.notificationService.updateConfig(configId, request, headers, req);
        return new ResponseEntity<>(ConfigUpdateResponse.builder().configId(config.getConfigId()).message("Notification configuration updated successfully!").build(), HttpStatus.OK);
    }

    @CrossOrigin(origins = "*")
    @GetMapping("/search")
    @Operation(
            summary = "Search notification configurations",
            description = "Searches for notification configurations based on provided criteria like business ID, scope level, and config ID.",
            parameters = {
                    @Parameter(name = "businessId", description = "ID of the business to search for", in = ParameterIn.QUERY, example = "a1b2c3d4-e5f6-7890-1234-567890abcdef"),
                    @Parameter(name = "scopeLevel", description = "Scope level of the notification configuration", in = ParameterIn.QUERY, example = "TENANT", schema = @Schema(type = "string", allowableValues = {"TENANT", "BUSINESS"})),
                    @Parameter(name = "configId", description = "ID of the notification configuration", in = ParameterIn.QUERY, example = "a1b2c3d4-e5f6-7890-1234-567890abcdef"),
                    @Parameter(name = "txn", description = "Transaction ID", required = true, in = ParameterIn.HEADER, schema = @Schema(type = "string", format = "uuid", example = "a1b2c3d4-e5f6-7890-1234-567890abcdef")),
                    @Parameter(name = "tenant-id", description = "Tenant ID", required = true, in = ParameterIn.HEADER, schema = @Schema(type = "string", format = "uuid", example = "a1b2c3d4-e5f6-7890-1234-567890abcdef")),
                    @Parameter(name = "x-session-token", description = "Session token for authentication", required = true, in = ParameterIn.HEADER, schema = @Schema(type = "string", example = "Bearer <sessionToken>")),
            },
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Notification configurations retrieved successfully",
                            content = @Content(mediaType = "application/json", schema = @Schema(implementation = SearchResponse.class))
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Bad request",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = ErrorResponse.class),
                                    examples = @ExampleObject(value = """
                                            {
                                              "errors": [
                                                {
                                                  "header": "tenant-id",
                                                  "errorMessage": "Tenant ID is required",
                                                  "errorCode": "JCMP1001"
                                                }
                                              ],
                                              "timestamp": "2025-08-20T17:34:20.243Z"
                                            }
                                            """
                                    ))
                    ),
                    @ApiResponse(
                            responseCode = "500",
                            description = "Internal server error",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = ErrorResponse.class),
                                    examples = @ExampleObject(value = """
                                            {
                                              "errors": [
                                                {
                                                  "errorMessage": "Internal Server Error",
                                                  "errorCode": "JCMP0001"
                                                }
                                              ],
                                              "timestamp": "2025-08-20T17:34:20.243Z"
                                            }
                                            """
                                    ))
                    )
            }
    )
    public ResponseEntity<SearchResponse<NotificationConfig>> search(@Parameter(hidden = true) @RequestParam Map<String, String> reqParams, HttpServletRequest req) throws PartnerPortalException {
        SearchResponse<NotificationConfig> response = this.notificationService.search(reqParams, req);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @CrossOrigin(origins = "*")
    @GetMapping("/count")
    @Operation(
            summary = "Get notification configuration count",
            description = "Returns the total count of notification configurations.",
            parameters = {
                    @Parameter(name = "txn", description = "Transaction ID", required = true, in = ParameterIn.HEADER, schema = @Schema(type = "string", format = "uuid", example = "a1b2c3d4-e5f6-7890-1234-567890abcdef")),
                    @Parameter(name = "tenant-id", description = "Tenant ID", required = true, in = ParameterIn.HEADER, schema = @Schema(type = "string", format = "uuid", example = "a1b2c3d4-e5f6-7890-1234-567890abcdef")),
                    @Parameter(name = "x-session-token", description = "Session token for authentication", required = true, in = ParameterIn.HEADER, schema = @Schema(type = "string", example = "Bearer <sessionToken>")),
            },
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Notification configuration count retrieved successfully",
                            content = @Content(mediaType = "application/json", schema = @Schema(implementation = CountResponse.class))
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Bad request",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = ErrorResponse.class),
                                    examples = @ExampleObject(value = """
                                            {
                                              "errors": [
                                                {
                                                  "header": "tenant-id",
                                                  "errorMessage": "Tenant ID is required",
                                                  "errorCode": "JCMP1001"
                                                }
                                              ],
                                              "timestamp": "2025-08-20T17:34:20.243Z"
                                            }
                                            """
                                    ))
                    ),
                    @ApiResponse(
                            responseCode = "500",
                            description = "Internal server error",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = ErrorResponse.class),
                                    examples = @ExampleObject(value = """
                                            {
                                              "errors": [
                                                {
                                                  "errorMessage": "Internal Server Error",
                                                  "errorCode": "JCMP0001"
                                                }
                                              ],
                                              "timestamp": "2025-08-20T17:34:20.243Z"
                                            }
                                            """
                                    ))
                    )
            }
    )
    public ResponseEntity<CountResponse> count(HttpServletRequest req) {

        String activity = "Get Notification configuration count ";

        LogUtil.logActivity(req, activity, "Success: Get Notification configuration count successfully");
        return new ResponseEntity<>(CountResponse.builder().count(this.notificationService.count()).build(), HttpStatus.OK);
    }

}
